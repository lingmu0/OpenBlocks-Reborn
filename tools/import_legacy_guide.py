"""Import MIT-licensed OpenBlocks 1.12.2 guide text and sound icons from its JAR."""
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
with zipfile.ZipFile(sys.argv[1]) as old:
    legacy = {}
    for locale in ("zh_cn", "en_us"):
        lang = dict(line.split("=", 1) for line in
                    old.read(f"assets/openblocks/lang/{locale}.lang").decode("utf-8-sig").splitlines()
                    if "=" in line and not line.startswith("#"))
        legacy[locale] = {key: value.replace("\\n", "\n") for key, value in lang.items()
                          if key.endswith(".description")}
    path = ROOT / "tools/legacy_guide.json"
    path.write_text(json.dumps(legacy, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    dest = ROOT / "src/main/resources/assets/openblocks_reborn/textures/gui/sonic"
    dest.mkdir(parents=True, exist_ok=True)
    for name in old.namelist():
        if name.startswith("assets/openblocks/textures/items/sound_") and name.endswith(".png"):
            (dest / name.rsplit("/", 1)[-1]).write_bytes(old.read(name))
    print("Imported legacy guide descriptions:", {k: len(v) for k, v in legacy.items()})
