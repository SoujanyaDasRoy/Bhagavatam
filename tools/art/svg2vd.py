"""Convert Lucide SVG icons (24x24, stroke based) into Android VectorDrawables. Run from the project root."""
import json, re, sys, xml.etree.ElementTree as ET
from pathlib import Path
NUM = re.compile(r'[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?')
ARGC = dict(M=2, L=2, H=1, V=1, C=6, S=4, Q=4, T=2, A=7, Z=0)
def norm(d):
    out = []; i = 0; cmd = None; n = len(d)
    while i < n:
        ch = d[i]
        if ch.isalpha():
            cmd = ch; i += 1
            if cmd in 'Zz': out.append('Z')
            else: out.append(cmd)
            continue
        if ch in ' ,\n\t': i += 1; continue
        k = ARGC[cmd.upper()]; vals = []
        for j in range(k):
            while i < n and d[i] in ' ,\n\t': i += 1
            if cmd.upper() == 'A' and j in (3, 4):
                vals.append(d[i]); i += 1
            else:
                m = NUM.match(d, i); vals.append(m.group(0)); i = m.end()
        out.append(' '.join(vals))
        if cmd in 'Mm': cmd = 'L' if cmd == 'M' else 'l'
    return ' '.join(out)
def f(v): return ('%g' % float(v))
def elem_path(e):
    t = e.tag.split('}')[1]; a = e.attrib
    if t == 'path': return norm(a['d'])
    if t == 'circle':
        cx, cy, r = float(a['cx']), float(a['cy']), float(a['r'])
        return f'M {f(cx-r)} {f(cy)} a {f(r)} {f(r)} 0 1 0 {f(2*r)} 0 a {f(r)} {f(r)} 0 1 0 {f(-2*r)} 0'
    if t == 'ellipse':
        cx, cy, rx, ry = [float(a[k]) for k in ('cx', 'cy', 'rx', 'ry')]
        return f'M {f(cx-rx)} {f(cy)} a {f(rx)} {f(ry)} 0 1 0 {f(2*rx)} 0 a {f(rx)} {f(ry)} 0 1 0 {f(-2*rx)} 0'
    if t == 'line': return f"M {a['x1']} {a['y1']} L {a['x2']} {a['y2']}"
    if t in ('polyline', 'polygon'):
        p = [float(x) for x in NUM.findall(a['points'])]
        s = 'M ' + ' L '.join(f'{f(p[i])} {f(p[i+1])}' for i in range(0, len(p), 2))
        return s + (' Z' if t == 'polygon' else '')
    if t == 'rect':
        x, y, w, h = [float(a.get(k, 0)) for k in ('x', 'y', 'width', 'height')]
        rx = float(a.get('rx', a.get('ry', 0))); ry = float(a.get('ry', rx))
        if rx == 0: return f'M {f(x)} {f(y)} H {f(x+w)} V {f(y+h)} H {f(x)} Z'
        return (f'M {f(x+rx)} {f(y)} H {f(x+w-rx)} A {f(rx)} {f(ry)} 0 0 1 {f(x+w)} {f(y+ry)} V {f(y+h-ry)} '
                f'A {f(rx)} {f(ry)} 0 0 1 {f(x+w-rx)} {f(y+h)} H {f(x+rx)} A {f(rx)} {f(ry)} 0 0 1 {f(x)} {f(y+h-ry)} '
                f'V {f(y+ry)} A {f(rx)} {f(ry)} 0 0 1 {f(x+rx)} {f(y)} Z')
    return None
FILLED = {'play', 'pause', 'skip-back', 'skip-forward', 'bookmark', 'star', 'heart'}
icons = json.load(open(sys.argv[1])); out = Path(sys.argv[2]); out.mkdir(parents=True, exist_ok=True)
for name, svg in icons.items():
    root = ET.fromstring(re.sub(r'<!--.*?-->', '', svg, flags=re.S))
    paths = [p for p in (elem_path(e) for e in root.iter() if e is not root) if p]
    fill = ' android:fillColor="#FF000000"' if name in FILLED else ''
    body = '\n'.join(f'    <path android:pathData="{p}" android:strokeColor="#FF000000" android:strokeWidth="2" '
                     f'android:strokeLineCap="round" android:strokeLineJoin="round"{fill} />' for p in paths)
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n' + body + '\n</vector>\n')
    (out / ('ic_' + name.replace('-', '_') + '.xml')).write_text(xml, encoding='utf-8')
print(len(icons), 'icons ->', out)
