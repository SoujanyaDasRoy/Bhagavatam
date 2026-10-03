import json, os, re

# Check all pages 129..789 for vision files
wrong_pages = []
for p in range(129, 790):
    fn = f'work/vision/v2_p{p:04d}.json'
    if not os.path.exists(fn):
        continue
    d = json.load(open(fn, encoding='utf-8'))
    ch = d.get('chapter_number')
    sk_h = d.get('skandha_heading') or ''
    title = d.get('chapter_title') or ''
    before = d.get('paragraphs_before_chapter_heading')
    
    # Specific known mistranscriptions in vision metadata:
    # P313: "অথ ত্রয়োবিংশোহধ্যায়ঃ" -> ch was 20, should be 23
    # P336: "শ্রীকৃষ্ণের অভিষেক" -> ch was 20, should be 27
    # P344: "দশম স্কন্ধ (ঊনত্রিশ অধ্যায়)" -> ch was null, should be 29
    # P363: "অথৈকত্রিংশোহধ্যায়ঃ" -> ch was 30, should be 31
    # P613: "ষট্ষষ্টিতম অধ্যায়" -> ch was 60/66 (let's check)
