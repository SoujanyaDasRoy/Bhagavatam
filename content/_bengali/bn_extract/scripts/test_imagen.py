import json, os, sys, requests, base64

with open('.env') as f:
    for line in f:
        if line.startswith('GEMINI_API_KEY='):
            api_key = line.strip().split('=', 1)[1]

url = f"https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key={api_key}"
payload = {
    "instances": [
        {"prompt": "A beautiful traditional Kangra Pahari miniature painting of young Krishna with glowing blue-complexion and peacock feather in Vrindavan forest with sacred cows, mineral pigments, gold leaf detailing, intricate floral border, peaceful devotional atmosphere."}
    ],
    "parameters": {
        "sampleCount": 1,
        "aspectRatio": "16:9",
        "outputMimeType": "image/jpeg"
    }
}

resp = requests.post(url, json=payload, timeout=60)
print(f"Status: {resp.status_code}")
if resp.status_code == 200:
    data = resp.json()
    predictions = data.get("predictions", [])
    if predictions:
        img_b64 = predictions[0].get("bytesBase64Encoded")
        if img_b64:
            print(f"Success! Image received (length {len(img_b64)} bytes base64)")
            os.makedirs("work/test_art", exist_ok=True)
            with open("work/test_art/test_krishna.jpg", "wb") as f:
                f.write(base64.b64decode(img_b64))
            print("Saved test image to work/test_art/test_krishna.jpg")
else:
    print(f"Error: {resp.text}")
