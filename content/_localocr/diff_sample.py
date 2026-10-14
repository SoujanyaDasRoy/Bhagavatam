import sys, random, json
from pathlib import Path
sys.path.insert(0, ".")
import crnn, cv2
from PIL import Image, ImageDraw
sys.stdout.reconfigure(encoding="utf-8")
m, chars = crnn.load_model()
ev = crnn.load_lines("eval")
preds = crnn.read_images(m, chars, [cv2.imread(str(p), cv2.IMREAD_GRAYSCALE) for p, _ in ev])
bad = [(p, t, q) for (p, t), q in zip(ev, preds) if t != q]
print(len(bad), "of", len(ev), "lines differ")
random.seed(int(sys.argv[1]) if len(sys.argv) > 1 else 3)
pick = random.sample(bad, 30)
ims = [Image.open(p).convert("L") for p, _, _ in pick]
W = max(i.width for i in ims) + 60; H = sum(i.height + 12 for i in ims)
sh = Image.new("L", (W, H), 255); d = ImageDraw.Draw(sh); y = 0
for n, im in enumerate(ims):
    d.text((4, y + im.height // 2 - 5), str(n + 1), fill=0); sh.paste(im, (50, y)); y += im.height + 12
sh.save("work/diff_sheet.png")
for n, (p, t, q) in enumerate(pick):
    tw, qw = t.split(), q.split()
    import difflib
    sm = difflib.SequenceMatcher(a=tw, b=qw)
    ch = [(" ".join(tw[i1:i2]), " ".join(qw[j1:j2])) for op, i1, i2, j1, j2 in sm.get_opcodes() if op != "equal"]
    print(n + 1, "LABEL->MODEL:", ch)
