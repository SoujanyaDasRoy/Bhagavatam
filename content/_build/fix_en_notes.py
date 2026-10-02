import sys,glob,os,re,shutil,json
sys.path.insert(0,'.')
from parse import *
BK=os.path.join(ROOT,'_build','backup_pre_notes'); os.makedirs(BK,exist_ok=True)
rep=re.compile(r'((?:\S+ ){1,8}?)\1{2,}')
def collapse(s): return rep.sub(lambda m:m.group(1),s)
report=[]
for p in sorted(glob.glob(ROOT+'/en/skandha-*/chapter-*.md')):
    raw=open(p,encoding='utf8').read()
    d,h,v=parse_en(p)
    prev=0; keep=[]; notes=[]; changed=False
    for x in v:
        t=x['text']
        m=re.search(r'\n*### (Footnotes|Notes)\s*\n',t)
        if m: notes.append(t[m.end():].strip()); t=t[:m.start()].strip(); x['text']=t; changed=True
        a,b=nums(x['label'])
        if prev==0 and a>1: ok=True   # first block (10.20 starts at 2: source omission)
        else: ok=(a==prev+1)
        if not ok and '-' in x['label'] and a<=prev<b:   # '1-3' after '1' -> '2-3'
            x['label']=f'{prev+1}-{b}' if b>prev+1 else str(b); a=prev+1; ok=True; changed=True
        if ok:
            c=collapse(x['text']+' ')
            if c.strip()!=x['text']: x['text']=c.strip(); changed=True
            keep.append(x); prev=b
        else:
            notes.append(f"[footnote fragment, was labelled verse {x['label']}, p.{x['page']}] "+t); changed=True
            report.append((os.path.relpath(p,ROOT),x['label'],x['page'],t[:70]))
    if not changed: continue
    shutil.copy(p,os.path.join(BK,os.path.relpath(p,ROOT).replace('/','_')))
    # rebuild file: keep original header part up to first '#### Verse'
    i=raw.index('#### Verse')
    out=raw[:i].rstrip()+'\n\n'
    # need ids: regenerate
    sk=int(d.get('skandha',0) or 0) if d.get('skandha','').isdigit() else None
    for x in keep:
        out+=f"#### Verse {x['label']}\n<!-- page: {x['page']} -->\n\n{x['text']}\n\n"
    if notes:
        out+='### Notes\n\n'+'\n\n'.join('- '+n.replace('\n',' ') for n in notes)+'\n'
    open(p,'w',encoding='utf8').write(out)
json.dump(report,open('en_notes_report.json','w'),ensure_ascii=False,indent=1)
print(len(report),'fragments moved')
for r in report: print(r)
