"""Validate generated book references, integration namespaces and packaged resources."""
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
book = RES / "assets/openblocks_reborn/patchouli_books/openblocks_guide"
languages = {locale: json.loads((RES / f"assets/openblocks_reborn/lang/{locale}.json").read_text(encoding="utf-8-sig"))
             for locale in ("en_us", "zh_cn")}
errors = []
docs = {path: json.loads(path.read_text(encoding="utf-8-sig")) for path in RES.rglob("*.json")}
categories = {path.stem for path in (book / "en_us/categories").glob("*.json")}

def walk(value, path):
    if isinstance(value, dict):
        for key, text in value.items():
            if isinstance(text, str):
                if "elevatorid:" in text:
                    errors.append(f"Retained external ElevatorMod reference in {path}: {text}")
                if text in ("openblocks_reborn:elevator", "openblocks_reborn:elevator_rotating"):
                    errors.append(f"Retired elevator reference in {path}")
                if book in path.parents:
                    if key == "category" and text.partition(":")[2] not in categories:
                        errors.append(f"Missing category {text} in {path}")
                    if key.startswith("recipe") and text.startswith("openblocks_reborn:"):
                        target = RES / "data/openblocks_reborn/recipe" / (text.partition(":")[2] + ".json")
                        if not target.exists():
                            errors.append(f"Missing recipe {text} in {path}")
                    if text.startswith(("patchouli.openblocks_reborn.", "message.openblocks_reborn.")):
                        for locale, lang in languages.items():
                            if text not in lang:
                                errors.append(f"Untranslated {text} ({locale}) in {path}")
            walk(text, path)
    elif isinstance(value, list):
        for child in value:
            if child in ("openblocks_reborn:elevator", "openblocks_reborn:elevator_rotating"):
                errors.append(f"Retired elevator tag member in {path}")
            walk(child, path)

for path, data in docs.items():
    walk(data, path)
assert categories == {"introduction", "blocks", "items", "misc", "changelogs"}, categories
entries = list((book / "en_us/entries").rglob("*.json"))
assert len(entries) == 104, len(entries)  # 98 item entries, enchantments, five section entries.
for color in "white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split():
    for folder in ("assets/openblocks_reborn/blockstates", "assets/openblocks_reborn/models/item",
                   "data/openblocks_reborn/recipe", "data/openblocks_reborn/loot_table/blocks"):
        path = RES / folder / ("elevator_" + color + ".json")
        if not path.exists():
            errors.append("Missing integrated elevator resource: " + str(path))
java = ROOT / "src/main/java/net/xuwu/openblocks_reborn/elevator"
for path in java.rglob("*.java"):
    if "com.vsngarcia" in path.read_text(encoding="utf-8") or '"elevatorid"' in path.read_text(encoding="utf-8"):
        errors.append("Retained external code/registry namespace: " + str(path))
props = dict(re.findall(r"^([\w_]+)=(.*)$", (ROOT / "gradle.properties").read_text(), re.M))
jar_path = ROOT / "build/libs" / (props["mod_id"] + "-" + props["mod_version"] + ".jar")
with zipfile.ZipFile(jar_path) as jar:
    assert jar.testzip() is None
    names = jar.namelist()
    for required in ("LICENSE-OpenBlocks", "LICENSE-ElevatorMod", "THIRD_PARTY_NOTICES.md",
                     "net/xuwu/openblocks_reborn/client/SonicGlassesClient.class"):
        if required not in names:
            errors.append("Missing packaged file " + required)
    if any(n.startswith("com/vsngarcia/") or n.endswith("block/ElevatorBlock.class") for n in names):
        errors.append("Stale original classes in distributable JAR")
    metadata = jar.read("META-INF/neoforge.mods.toml").decode()
    if 'modId = "elevatorid"' in metadata or 'modId="elevatorid"' in metadata:
        errors.append("Standalone ElevatorMod metadata/dependency still packaged")
if errors:
    raise SystemExit("\n".join(errors))
print(f"PASS: {len(docs)} JSON files; {len(entries)} guide entries; five legacy sections; sixteen integrated elevator colors")
print("PASS: zh_cn/en_us keys, crafting references, licenses, JAR integrity and retired-ID checks")
