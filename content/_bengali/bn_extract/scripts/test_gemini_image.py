import requests, json, base64, os

with open('.env') as f:
    for line in f:
        if line.startswith('GEMINI_API_KEY='):
            api_key = line.strip().split('=', 1)[1]

prompt = "A traditional Kangra Pahari miniature painting of young Lord Krishna with flute in Vrindavan garden with cows, mineral pigments, gold accents, intricate decorative borders."

for model in ["gemini-2.5-flash-image", "gemini-3-pro-image-preview", "gemini-3.1-flash-image"]:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    payload = {
        "contents": [{
            "parts": [{"text": prompt}]
        }],
        "generationConfig": {
            "responseModalities": ["IMAGE"]
        }
    }
    print(f"Testing {model}...")
    resp = requests.post(url, json=payload, timeout=60)
    print(f"  Status: {resp.status_code}")
    if resp.status_code == 200:
        data = resp.json()
        candidates = data.get("candidates", [])
        if candidates:
            parts = candidates[0].get("content", {}).get("parts", [])
            for p_idx, part in enumerate(parts):
                if "inlineData" in part:
                    mime = part["inlineData"].get("mimeType")
                    b64 = part["inlineData"].get("data")
                    print(f"  SUCCESS! Received {mime} ({len(b64)} chars base64)")
                    os.makedirs("work/test_art", exist_ok=True)
                    ext = "png" if "png" in mime else "jpg"
                    with open(f"work/test_art/test_{model}.{ext}", "wb") as f:
                        f.write(base64.b64decode(b64))
                    print(f"  Saved to work/test_art/test_{model}.{ext}")
                    break
        break
    else:
        print(f"  Error: {resp.text[:200]}")
