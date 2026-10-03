import bn_extract as b

cfg = b.load_config()
pages = b.load_pages(cfg)
s10 = [p for p in pages if p["_vol"] == "2" and 129 <= p["_pg"] <= 280]

cur = 0
for p in s10:
    ch = b.parse_chapter_num(p.get("chapter_number"))
    tit = p.get("chapter_title") or ""
    if ch is not None:
        print(f"p{p['_pg']:04d}: ch={ch} | tit={tit[:25]}")
