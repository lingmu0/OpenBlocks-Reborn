"""Vendor the complete common + NeoForge ElevatorMod 1.21.1-1.11.4 implementation.

Usage: python tools/import_elevatormod.py path/to/VsnGamer-ElevatorMod-129614f
Only package names, resource/registry namespaces and integration entry points change.
Fabric platform code is deliberately excluded from this NeoForge-only project.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = Path(sys.argv[1])
PACKAGE = "net.xuwu.openblocks_reborn.elevator"
JAVA = ROOT / "src/main/java" / PACKAGE.replace(".", "/")
RES = ROOT / "src/main/resources"

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

copied = []
for module in ("common", "neoforge"):
    source = UPSTREAM / module / "src/main/java/com/vsngarcia"
    for path in source.rglob("*.java"):
        if path.name == "ElevatorModNeoForge.java":
            continue
        rel = path.relative_to(source)
        dest = JAVA / rel
        text = path.read_text(encoding="utf-8").replace("com.vsngarcia", PACKAGE)
        text = text.replace("elevatorid", "openblocks_reborn")
        text = text.replace("org.jetbrains.annotations.Nullable", "javax.annotation.Nullable")
        text = text.replace("org.jetbrains.annotations.NotNull", "javax.annotation.Nonnull").replace("@NotNull", "@Nonnull")
        text = text.replace("@EventBusSubscriber(bus =", "@EventBusSubscriber(modid = net.xuwu.openblocks_reborn.OpenBlocksReborn.MOD_ID, bus =")
        if rel.as_posix() == "neoforge/init/Registry.java":
            # The parent mod's creative tab exposes the complete 16-color family.
            start = text.index("    private static final DeferredRegister<CreativeModeTab>")
            end = text.index("    public static void init(", start)
            text = text[:start] + text[end:]
            text = text.replace("        CREATIVE_TABS.register(modEventBus);\n", "")
        if rel.as_posix() == "ElevatorMod.java":
            text = text.replace('public static final String ID = "openblocks_reborn";',
                                'public static final String ID = net.xuwu.openblocks_reborn.OpenBlocksReborn.MOD_ID;')
        if rel.as_posix() == "client/gui/ElevatorScreen.java":
            text = text.replace("dirButton.selected = tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL);",
                                "((net.xuwu.openblocks_reborn.mixin.client.CheckboxAccessor)(Object)dirButton)"
                                ".openblocks$setSelected(tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL));")
            text = text.replace("hideArrowButton.selected = !tile.getBlockState().getValue(ElevatorBlockBase.SHOW_ARROW);",
                                "((net.xuwu.openblocks_reborn.mixin.client.CheckboxAccessor)(Object)hideArrowButton)"
                                ".openblocks$setSelected(!tile.getBlockState().getValue(ElevatorBlockBase.SHOW_ARROW));")
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text("// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.\n" + text.rstrip() + "\n",
                        encoding="utf-8")
        copied.append(dest.relative_to(ROOT).as_posix())

for module in ("common", "neoforge"):
    resource_root = UPSTREAM / module / "src/main/resources"
    for tree in ("assets/elevatorid", "data/elevatorid"):
        source = resource_root / tree
        if not source.exists():
            continue
        for path in source.rglob("*"):
            if not path.is_file():
                continue
            rel = path.relative_to(resource_root)
            dest = RES / str(rel).replace("elevatorid", "openblocks_reborn")
            dest.parent.mkdir(parents=True, exist_ok=True)
            if path.suffix == ".json":
                data = json.loads(path.read_text(encoding="utf-8-sig").replace("elevatorid", "openblocks_reborn"))
                if dest.is_file() and ("/lang/" in dest.as_posix() or dest.name == "sounds.json"):
                    previous = json.loads(dest.read_text(encoding="utf-8-sig"))
                    previous.update(data)
                    data = previous
                write_json(dest, data)
            else:
                dest.write_bytes(path.read_bytes())
            copied.append(dest.relative_to(ROOT).as_posix())

doc = ROOT / "docs/elevator-import.json"
write_json(doc, {"repository": "https://github.com/VsnGamer/ElevatorMod",
                 "revision": "129614f8e54de6ddca3c5db757fa8ebc33f40a9d",
                 "branch": "1.21.1/input_patch", "declared_version": "1.21.1-1.11.4",
                 "license": "MIT", "files": copied})
print("Imported", len(copied), "source/resource files from ElevatorMod 1.21.1-1.11.4")
