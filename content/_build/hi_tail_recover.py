import re,json,os,subprocess,glob,sys
sys.path.insert(0,'.')
from parse import *
issues=json.load(open('build_issues.json',encoding='utf8'))
gaps={}
for sk,c,kind,n in issues:
    if kind=='no hindi for': gaps.setdefault((sk,c),[]).append(n)
SH=os.path.expanduser('~/mnt/Startups/Shastra')
D2A=str.maketrans('0123456789','०१२३४५६७८९')
res={}
for (sk,c),ns in sorted(gaps.items()):
    p=f'{ROOT}/hi/skandha-{sk:02d}/chapter-{c:02d}.md'
    t=open(p,encoding='utf8').read()
    pages=[int(x) for x in re.findall(r'source_page:\s*(\d+)',t)]
    vol=1 if sk<=8 or (sk==9 and False) else 2
    # figure volume from source_pdf
    vol=2 if 'Volume 2' in t else 1
    pdf=f'{SH}/Hindi/Srimad Bhagavat Mahapuran Volume {vol} Sanskrit Hindi.pdf'
    lo,hi=min(pages),max(pages)+1
    out=f'/tmp/hit_{sk}_{c}'
    subprocess.run(['python3',SH+'/convert.py',pdf,'-o',out,'-l','hindi','-p',f'{lo}-{hi}'],capture_output=True,cwd=SH)
    L=[]
    for f in sorted(glob.glob(out+'/raw/page-*.txt')):
        for l in open(f,encoding='utf8').read().split('\n'):
            l=l.strip()
            if l and not re.fullmatch(r'[०-९]{1,3}',l): L.append(l)
    H=re.sub(r'\s+',' ',' '.join(L))
    a=ns[0]; b=ns[-1]
    pat=re.compile(r'॥\s*%s\s*[-–]\s*%s\s*॥'%(str(a).translate(D2A),str(b).translate(D2A)))
    m=pat.search(H)
    cand=None
    if m:
        # segment start = end of previous marker
        prevs=list(re.finditer(r'॥\s*[०-९]+(?:\s*[-–]\s*[०-९]+)?\s*॥',H[:m.start()]))
        s=prevs[-1].end() if prevs else 0
        cand=H[s:m.start()].strip()
    res[(sk,c)]=(ns,cand,bool(m),pages,vol)
    print(sk,c,ns,'FOUND' if m else 'NOT FOUND',(cand or '')[:160].replace('\n',' '))
json.dump({f'{k[0]}.{k[1]}':[v[0],v[1],v[2],v[3],v[4]] for k,v in res.items()},open('hi_tail_cands.json','w'),ensure_ascii=False,indent=1)
