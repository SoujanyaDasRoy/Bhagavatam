import re,json,os,glob,sys,shutil
sys.path.insert(0,'.')
from parse import *
APPLY='--apply' in sys.argv
issues=json.load(open('build_issues.json',encoding='utf8'))
gaps={}
for sk,c,kind,n in issues:
    if kind=='no hindi for': gaps.setdefault((sk,c),[]).append(n)
D2A=str.maketrans('0123456789','०१२३४५६७८९')
def norm(s): return re.sub(r'[\s\-–॥।०-९0-9‌‍*]','',s)
MK=re.compile(r'॥\s*([०-९]+)(?:\s*[-–]\s*([०-९]+))?\s*॥')
BK=os.path.join(ROOT,'_build','backup_hi_tail'); os.makedirs(BK,exist_ok=True)
for (sk,c),ns in sorted(gaps.items()):
    p=f'{ROOT}/hi/skandha-{sk:02d}/chapter-{c:02d}.md'
    t=open(p,encoding='utf8').read()
    d,h,vhi,nh=parse_hi(p)
    sa_norm=set()
    for v in vhi:
        for l in v['sa'].split('\n'): 
            if norm(l): sa_norm.add(norm(l))
    out=f'/tmp/hit_{sk}_{c}'
    L=[]
    for f in sorted(glob.glob(out+'/raw/page-*.txt')):
        for l in open(f,encoding='utf8').read().split('\n'):
            l=l.strip()
            if not l or re.fullmatch(r'[०-९]{1,3}|\*',l): continue
            L.append(l)
    # cut at chapter end
    for i,l in enumerate(L):
        if l.startswith('इति श्रीमद') and 'ऽध्यायः' in (l+(L[i+1] if i+1<len(L) else '')):
            L=L[:i]; break
    keep=[l for l in L if norm(l) not in sa_norm]
    H=re.sub(r'\s+',' ',' '.join(keep))
    segs=[]; pos=0
    for m in MK.finditer(H):
        a=int(m.group(1).translate(str.maketrans('०१२३४५६७८९','0123456789'))); b=int((m.group(2) or m.group(1)).translate(str.maketrans('०१२३४५६७८९','0123456789')))
        segs.append((a,b,H[pos:m.start()].strip())); pos=m.end()
    got=[(a,b,x) for a,b,x in segs if a>=ns[0] and b<=ns[-1]]
    cov=set(k for a,b,_ in got for k in range(a,b+1))
    status='OK' if cov>=set(ns) else f'PARTIAL cov={sorted(cov)}'
    print(f'{sk}.{c}',ns,status)
    for a,b,x in got: print('   ',a,b,x[:90],'...',x[-40:])
    if APPLY and got:
        shutil.copy(p,os.path.join(BK,f'hi_{sk:02d}_{c:02d}.md'))
        for a,b,x in got:
            pat=re.compile(r'(### श्लोक '+str(b).translate(D2A)+r'\n(?:<!--.*?-->\n)?(?:.*?\n)*?)(?=\n### |\Z)',re.S)
            m=pat.search(t)
            if not m: print('  !! block not found',b); continue
            blk=m.group(1)
            if '**भावार्थ:**' in blk: print('  !! already has hindi',b); continue
            new=blk.rstrip()+f"\n<!-- recovered: Hindi translation of verses {a}-{b} was missing from the first extraction; taken from the page text -->\n\n**भावार्थ:**\n\n{x}\n"
            t=t.replace(blk,new,1)
        open(p,'w',encoding='utf8').write(t)
