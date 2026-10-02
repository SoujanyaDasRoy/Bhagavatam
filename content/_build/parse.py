import re, os, json, glob
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEV=str.maketrans('०१२३४५६७८९','0123456789')
def fm(text):
    m=re.match(r'---\n(.*?)\n---\n',text,re.S); d={}
    if m:
        for l in m.group(1).split('\n'):
            if ':' in l:
                k,v=l.split(':',1); d[k.strip()]=v.strip().strip('"')
        text=text[m.end():]
    return d,text
def parse_en(path):
    d,t=fm(open(path,encoding='utf8').read())
    parts=re.split(r'^#### Verse ([0-9]+(?:\s*[-–]\s*[0-9]+)?)\s*$',t,flags=re.M)
    head=parts[0]; verses=[]
    for i in range(1,len(parts),2):
        lab=parts[i]; body=parts[i+1]
        m=re.search(r'<!--\s*id:[^>]*-->',body); 
        page=re.search(r'page:\s*(\d+)',body)
        notes=re.findall(r'<!--\s*(repaired|note)[^>]*-->',body)
        body2=re.sub(r'<!--.*?-->','',body,flags=re.S).strip()
        verses.append(dict(label=lab.replace('–','-').replace(' ',''),text=body2,page=int(page.group(1)) if page else None))
    return d,head,verses
def parse_hi(path):
    d,t=fm(open(path,encoding='utf8').read())
    # strip page-source frontmatter-like blocks
    t=re.sub(r'\n---\nsource_pdf:.*?\n---\n','\n',t,flags=re.S)
    t=re.sub(r'\n---\n','\n',t)
    notes=''
    m=re.search(r'^### पाठभेद एवं पाद-टिप्पणी\s*$',t,flags=re.M)
    if m: notes=t[m.end():].strip(); t=t[:m.start()]
    parts=re.split(r'^### श्लोक ([०-९]+(?:\s*[-–]\s*[०-९]+)?)\s*$',t,flags=re.M)
    verses=[]
    for i in range(1,len(parts),2):
        lab=parts[i].translate(DEV).replace('–','-').replace(' ','')
        body=re.sub(r'<!--.*?-->','',parts[i+1],flags=re.S).strip()
        speaker=''
        m=re.match(r'\*\*(.+?)\*\*\s*\n',body)
        if m and 'भावार्थ' not in m.group(1): speaker=m.group(1).strip(); body=body[m.end():].strip()
        if '**भावार्थ:**' in body:
            sa,hi=body.split('**भावार्थ:**',1)
        else: sa,hi=body,''
        verses.append(dict(label=lab,sa=sa.strip(),hi=hi.strip(),speaker=speaker))
    return d,parts[0],verses,notes
def nums(label):
    a=label.split('-'); return int(a[0]),int(a[-1])
