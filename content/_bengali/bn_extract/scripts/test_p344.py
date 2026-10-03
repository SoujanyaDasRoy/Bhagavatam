import json, os, sys, base64, requests
import pymupdf

cfg = json.load(open('config.json', encoding='utf-8'))
pdf_file = cfg['pdf_dir'] + '\\' + cfg['volumes']['2']['file']
doc = pymupdf.open(pdf_file)

# Load env for API key
api_key = ''
with open('.env') as f:
    for line in f:
        if line.startswith('GEMINI_API_KEY='):
            api_key = line.strip().split('=', 1)[1]

page = doc[343] # PDF page 344 (1-indexed)
pix = page.get_pixmap(dpi=200)
img_bytes = pix.tobytes('png')
b64_img = base64.b64encode(img_bytes).decode('utf-8')

prompt = """Transcribe all text on this page in detail, especially the chapter heading, the Sanskrit verses, and the Bengali translation paragraphs from top to bottom."""

url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={api_key}"
payload = {
    "contents": [{
        "parts": [
            {"text": prompt},
            {"inlineData": {"mimeType": "image/png", "data": b64_img}}
        ]
    }]
}

resp = requests.post(url, json=payload, timeout=60)
print("=== PAGE 344 FULL TRANSCRIPTION ===")
if resp.status_code == 200:
    print(resp.json()["candidates"][0]["content"]["parts"][0]["text"])
else:
    print(f"Error {resp.status_code}: {resp.text}")
