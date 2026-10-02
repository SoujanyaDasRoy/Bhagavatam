import sys,os,re,json,sqlite3,glob,time,hashlib
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from parse import *
from indic_transliteration.sanscript import transliterate, DEVANAGARI, IAST
OUT=sys.argv[1] if len(sys.argv)>1 else ROOT+'/content.db'
VERSION=int(sys.argv[2]) if len(sys.argv)>2 else 1
TITLE_FIX={(1,15):"Stricken with grief at their separation from Śrī Kṛṣṇa, the Pāṇḍavas install Parīkṣit on the throne of Hastināpura and ascend to heaven",
(2,1):"The process of meditation and the cosmic form of the Lord described",
(4,6):"Brahmā and the other gods proceed to Kailāsa and appease Lord Śiva",
(4,15):"Descent and coronation of king Pṛthu",
(6,2):"The messengers of Lord Viṣṇu expound the Bhāgavata Dharma (the cult of Devotion) and Ajāmila ascends to the Lord’s supreme Abode",
(6,3):"A dialogue between Yama (the god of retribution) and his messengers",
(7,8):"Lord Nṛsiṃha extolled on the death of the demon king (at His hands)",
(8,17):"The Lord manifests Himself before Aditi and grants her desired boon",
(11,14):"Glory of the Path of Devotion and the Process of Meditation described",
(12,6):"Parīkṣit attains the supreme goal and Janamejaya commences a snake-sacrifice; Classification of the Vedas into so many Śākhās or schools",
(12,7):"Different Schools of Atharvaveda and the Characteristics of the Purāṇas"}
SK=[ # num, nameSa, titleEn, titleHi, titleBn (themes; Bengali from prototype)
(0,"श्रीमद्भागवत माहात्म्य","Glory of the Bhagavatam","श्रीमद्भागवत माहात्म्य","শ্রীমদ্ভাগবত মাহাত্ম্য"),
(1,"प्रथम स्कन्ध","Sages at Naimisharanya","नैमिषारण्य के ऋषि","নৈমিষারণ্যের ঋষিগণ"),
(2,"द्वितीय स्कन्ध","Shuka begins","शुकदेवजी का आरम्भ","শুকদেবের সূচনা"),
(3,"तृतीय स्कन्ध","Vidura and Maitreya","विदुर और मैत्रेय","বিদুর ও মৈত্রেয়"),
(4,"चतुर्थ स्कन्ध","Dhruva and Prithu","ध्रुव और पृथु","ধ্রুব ও পৃথু"),
(5,"पञ्चम स्कन्ध","Rishabha and Bharata","ऋषभ और भरत","ঋষভ ও ভরত"),
(6,"षष्ठ स्कन्ध","Ajamila and Vritra","अजामिल और वृत्र","অজামিল ও বৃত্র"),
(7,"सप्तम स्कन्ध","Prahlada and Narasimha","प्रह्लाद और नृसिंह","প্রহ্লাদ ও নৃসিংহ"),
(8,"अष्टम स्कन्ध","Gajendra and the churning","गजेन्द्र और समुद्र-मन्थन","গজেন্দ্র ও সমুদ্রমন্থন"),
(9,"नवम स्कन्ध","Rama and the royal lines","श्रीराम और राजवंश","শ্রীরাম ও রাজবংশ"),
(10,"दशम स्कन्ध","Krishna’s life","श्रीकृष्ण-लीला","শ্রীকৃষ্ণলীলা"),
(11,"एकादश स्कन्ध","Uddhava Gita","उद्धव गीता","উদ্ধব গীতা"),
(12,"द्वादश स्कन्ध","Kali yuga and the close","कलियुग और उपसंहार","কলিযুগ ও উপসংহার")]
def hyph_join(lines):
    out=[]
    for l in lines:
        if out and out[-1].endswith('-') and l: out[-1]=out[-1][:-1]+l
        else: out.append(l)
    return out
def clean_hi(t):
    t=re.sub(r'-\n(?=\S)','',t)
    paras=[re.sub(r'\s*\n\s*',' ',p).strip() for p in re.split(r'\n\s*\n',t)]
    return '\n\n'.join(p for p in paras if p)
def clean_sa(sa):
    lines=[l.strip() for l in sa.split('\n') if l.strip() and l.strip()!='---']
    return hyph_join(lines)
def iast(lines): return [transliterate(l,DEVANAGARI,IAST) for l in lines]
db_path=OUT
if os.path.exists(db_path): os.remove(db_path)
db=sqlite3.connect(db_path)
db.executescript('''
CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT);
CREATE TABLE skandha(num INTEGER PRIMARY KEY, name_sa TEXT, title_en TEXT, title_hi TEXT, title_bn TEXT, chapters INTEGER);
CREATE TABLE chapter(skandha INTEGER, chapter INTEGER, title_en TEXT, title_hi TEXT, verse_count INTEGER, notes_en TEXT, notes_hi TEXT, colophon_en TEXT, PRIMARY KEY(skandha,chapter));
CREATE TABLE verse(skandha INTEGER, chapter INTEGER, num INTEGER, num_end INTEGER, speaker TEXT, sa TEXT, iast TEXT, hi TEXT, en TEXT, en_from INTEGER, hi_from INTEGER, PRIMARY KEY(skandha,chapter,num));
''')
ix=json.load(open(ROOT+'/index.json',encoding='utf8'))
secs=[s for s in ix['sections'] if s['dir']!='mahatmya-skanda']
stats=dict(chapters=0,verses=0,en_missing=0,hi_missing=0,sa_missing=0)
issues=[]
for sec in secs:
    sk=sec['skandha']; rec=[x for x in SK if x[0]==sk][0]
    db.execute('INSERT INTO skandha VALUES(?,?,?,?,?,?)',(*rec,len(sec['chapters'])))
    for ch in sec['chapters']:
        c=ch['chapter']
        den,hen,ven=parse_en(ROOT+'/'+ch['files']['en'])
        dhi,hhi,vhi,nhi=parse_hi(ROOT+'/'+ch['files']['hi'])
        # split english colophon and notes
        notes_en=''
        m=re.search(r'\n### Notes\s*\n(.*)$',open(ROOT+'/'+ch['files']['en'],encoding='utf8').read(),re.S)
        if m: notes_en=m.group(1).strip()
        colo=''
        for x in ven:
            mm=re.search(r'\n\s*---\s*\n+(.*)$',x['text'],re.S)
            if mm: colo=mm.group(1).strip().strip('*').strip(); x['text']=x['text'][:mm.start()].strip()
        # english entries: [a,b,text]
        en=[(nums(x['label'])[0],nums(x['label'])[1],x['text']) for x in ven]
        # hindi blocks
        blocks=[]
        for x in vhi:
            a,b=nums(x['label'])
            blocks.append(dict(a=a,b=b,speaker=x['speaker'],sa=clean_sa(x['sa']),hi=clean_hi(x['hi']),en=[]))
        # drop duplicate hindi labels (keep first)
        seen=set(); bl2=[]
        for b in blocks:
            if b['a'] in seen: issues.append((sk,c,'dup hindi label',b['a'])); continue
            seen.add(b['a']); bl2.append(b)
        blocks=sorted(bl2,key=lambda b:b['a'])
        starts=[b['a'] for b in blocks]
        import bisect
        for a,b,t in en:
            i=bisect.bisect_right(starts,a)-1
            if i<0: i=0
            if not blocks: continue
            blocks[i]['en'].append(t)
        # english ranges: verses inside a range point back to its first verse
        en_from={}
        for a,b,t in en:
            for k in range(a+1,b+1): en_from[k]=a
        nxt_hi={}
        nh=None
        for b in reversed(blocks):
            if b['hi']: nh=b['a']
            nxt_hi[b['a']]=nh
        for b in blocks:
            enx='\n\n'.join(b['en'])
            ef=None; hf=None
            if not enx:
                # covered by an english range that started in an earlier hindi block?
                cov=[k for k in range(b['a'],b['b']+1) if k in en_from]
                if cov: ef=en_from[cov[0]]
                else: stats['en_missing']+=1; issues.append((sk,c,'no english for',b['a']))
            if not b['hi']:
                if nxt_hi[b['a']] is not None: hf=nxt_hi[b['a']]
                else: stats['hi_missing']+=1; issues.append((sk,c,'no hindi for',b['a']))
            if not b['sa']: stats['sa_missing']+=1; issues.append((sk,c,'no sanskrit for',b['a']))
            db.execute('INSERT INTO verse VALUES(?,?,?,?,?,?,?,?,?,?,?)',(sk,c,b['a'],b['b'],b['speaker'],'\n'.join(b['sa']),'\n'.join(iast(b['sa'])),b['hi'],enx,ef,hf))
            stats['verses']+=1
        # english verses that start beyond hindi? (all handled by bisect)
        title=TITLE_FIX.get((sk,c)) or ch.get('title') or f'Chapter {c}'
        title=re.sub(r'\s+',' ',title).strip()
        title_hi=ch.get('title_hi') or ''
        db.execute('INSERT INTO chapter VALUES(?,?,?,?,?,?,?,?)',(sk,c,title,title_hi,len(blocks),notes_en,nhi,colo))
        stats['chapters']+=1
db.execute("INSERT INTO meta VALUES('content_version',?)",(str(VERSION),))
db.execute("INSERT INTO meta VALUES('built_at',?)",(time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),))
db.execute("INSERT INTO meta VALUES('stats',?)",(json.dumps(stats),))
db.execute("INSERT INTO meta VALUES('languages','sa,hi,en')",)
db.commit(); db.execute('VACUUM'); db.close()
json.dump(issues,open(os.path.dirname(os.path.abspath(__file__))+'/build_issues.json','w'),ensure_ascii=False,indent=0)
import add_bengali
bn_stats,_=add_bengali.apply(OUT,version=VERSION)   # Bengali from content/bn (checked chapters only)
print('bengali',bn_stats)
print(stats,'issues',len(issues),'size',os.path.getsize(OUT)//1024,'KB')
