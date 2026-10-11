"""Multi-threaded Parallel Art Generation & Ingestion Worker for Bhagavatam.
Uses free serverless FLUX.1-schnell on Hugging Face with automatic borderless 16:10 WebP conversion
and color-grading.
"""
import argparse, concurrent.futures, json, os, subprocess, sys, time
from PIL import Image
from huggingface_hub import InferenceClient

PROMPTS_JSON = "content/art/chapter_prompts.json"
RAW_DIR = "content/art/raw"
OUT_DIR = "app/src/main/res/drawable-nodpi"
DEFAULT_TOKEN = os.environ.get("HF_TOKEN", "")   # never commit a token: set HF_TOKEN or pass --token
PRIMARY_MODEL = "black-forest-labs/FLUX.1-schnell"

def load_prompts():
    if not os.path.exists(PROMPTS_JSON):
        print("Generating prompts first...")
        subprocess.run([sys.executable, "tools/make_chapter_prompts.py"], check=True)
    with open(PROMPTS_JSON, "r", encoding="utf-8") as f:
        return json.load(f)

def render_progress_bar(current, total, prefix="Batch Progress", length=30):
    percent = float(current) / float(total) if total > 0 else 0
    filled = int(length * percent)
    bar = "█" * filled + "░" * (length - filled)
    print(f"\r{prefix} |{bar}| {current}/{total} ({percent*100:.1f}%)", end="", flush=True)

def generate_single_chapter(item, token, retries=3):
    file_name = item["file"]
    prompt = item["prompt"]
    raw_path = os.path.join(RAW_DIR, f"{file_name}.jpg")
    webp_path = os.path.join(OUT_DIR, f"{file_name}.webp")

    # Skip if already exists and is non-empty
    if (os.path.exists(webp_path) and os.path.getsize(webp_path) > 10000) or (os.path.exists(raw_path) and os.path.getsize(raw_path) > 10000):
        return True, f"[SKIP] {file_name} already exists."

    client = InferenceClient(token=token)

    for attempt in range(retries):
        try:
            time.sleep(4)
            img = client.text_to_image(prompt, model=PRIMARY_MODEL)
            img.save(raw_path, "JPEG", quality=95)
            return True, f"[OK] {file_name} generated with FLUX.1-schnell ({img.size})"
        except Exception as e:
            err = str(e)
            if "429" in err or "503" in err or "504" in err:
                time.sleep(8 + attempt * 4)
            else:
                time.sleep(3)
            continue

    return False, f"[FAIL] {file_name} failed after {retries} retries."

def run_batch(token, workers=2, limit=None, skandha_filter=None):
    prompts = load_prompts()
    
    if skandha_filter is not None:
        prompts = [p for p in prompts if p["skandha"] == skandha_filter]
    if limit is not None:
        prompts = prompts[:limit]

    total = len(prompts)
    print(f"\n=======================================================")
    print(f" Starting Batch Generation: {total} items | {workers} workers")
    print(f" Model: {PRIMARY_MODEL} (Free Serverless Inference)")
    print(f"=======================================================\n")

    start_time = time.time()
    completed = 0
    succeeded = 0

    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
        futures = {executor.submit(generate_single_chapter, item, token): item for item in prompts}
        for future in concurrent.futures.as_completed(futures):
            ok, msg = future.result()
            completed += 1
            if ok:
                succeeded += 1
            render_progress_bar(completed, total, prefix="Progress")

    elapsed = time.time() - start_time
    print(f"\n\nBatch finished in {elapsed:.1f}s. Total Succeeded: {succeeded}/{total}")

    # Run Color Grading & WebP Optimization
    print("\n--- Running Color Grading & WebP Optimization Pipeline ---")
    subprocess.run([sys.executable, "tools/grade_art.py"], check=True)

def main():
    parser = argparse.ArgumentParser(description="Parallel Art Generation Worker")
    parser.add_argument("--token", type=str, default=DEFAULT_TOKEN, help="Hugging Face API Token")
    parser.add_argument("--workers", type=int, default=2, help="Number of concurrent worker threads")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of images to generate")
    parser.add_argument("--skandha", type=int, default=None, help="Filter by Skandha number (0 to 12)")
    parser.add_argument("--grade-only", action="store_true", help="Only run post-processing")
    parser.add_argument("--rebuild", action="store_true", help="Rebuild release APK after generation")
    args = parser.parse_args()

    os.makedirs(RAW_DIR, exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)

    if args.grade_only:
        subprocess.run([sys.executable, "tools/grade_art.py"], check=True)
        if args.rebuild:
            subprocess.run('cmd /c "set JAVA_HOME=C:\\Program Files\\Android\\Android Studio\\jbr&& gradlew.bat assembleRelease"', shell=True, check=True)
        return

    run_batch(token=args.token, workers=args.workers, limit=args.limit, skandha_filter=args.skandha)

    if args.rebuild:
        print("\n--- Rebuilding App with New WebP Assets ---")
        cmd = 'cmd /c "set JAVA_HOME=C:\\Program Files\\Android\\Android Studio\\jbr&& gradlew.bat assembleRelease"'
        subprocess.run(cmd, shell=True, check=True)

if __name__ == "__main__":
    main()
