import sys,re
from multiprocessing import Pool
sys.path.insert(0,'.')
import odia_ocr as o
def go(f):
    m=re.search(r"v(\d)_p(\d+)",f); r,raw=o.extract_page(int(m[1]),int(m[2])); o.save_page(r,raw)
if __name__=="__main__":
    fs=open('work/redo.txt').read().split()
    with Pool(2) as p: p.map(go,fs)
