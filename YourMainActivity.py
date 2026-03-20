import os
import json
import re
import requests

CACHE_FILE = "chipset_cache.json"

if os.path.exists(CACHE_FILE):
    with open(CACHE_FILE, "r") as f:
        cache = json.load(f)
else:
    cache = {}

def get_cpu_info() -> str:
    cpu_info = ""
    try:
        with open("/proc/cpuinfo", "r") as f:
            for line in f:
                lower = line.lower()
                if "hardware" in lower or "model name" in lower:
                    cpu_info = line.split(":")[1].strip()
                    break
    except FileNotFoundError:
        cpu_info = os.environ.get("PROCESSOR_IDENTIFIER", "UNKNOWN")

    cpu_code = re.sub(r"[^A-Z0-9]", "", cpu_info.upper())
    return cpu_code or "UNKNOWN"

def get_chipset_data(cpu_code: str):
    if cpu_code in cache:
        return cache[cpu_code]

    if cpu_code.startswith("MT"):
        url = f"https://tolepcoy.pages.dev/tolepcoy_mediatek?code={cpu_code}"
    elif cpu_code.startswith("MSM") or cpu_code.startswith("SM"):
        url = f"https://tolepcoy.pages.dev/tolepcoy_qualcomm?code={cpu_code}"
    else:
        return {"chipset": "Chipset not found!", "gpu": "Unknown"}

    try:
        resp = requests.get(url)
        resp.raise_for_status()
        data = resp.json()

        chipset = data.get("chipset", "Nothing")
        gpu = data.get("gpu", "Nothing")

        cache[cpu_code] = {"chipset": chipset, "gpu": gpu}
        with open(CACHE_FILE, "w") as f:
            json.dump(cache, f, indent=2)

        return cache[cpu_code]

    except Exception:
        return {"chipset": "Chipset not found!", "gpu": "Unknown"}

if __name__ == "__main__":
    cpu_code = get_cpu_info()
    data = get_chipset_data(cpu_code)
    print(f"CPU Code: {cpu_code}")
    print(f"Chipset: {data['chipset']}")
    print(f"GPU: {data['gpu']}")