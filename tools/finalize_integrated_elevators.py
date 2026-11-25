"""Finalize the standalone integrated elevator registry and its guide entries."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
COLORS = "white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split()

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

pickaxe = RES / "data/minecraft/tags/block/mineable/pickaxe.json"
tag = json.loads(pickaxe.read_text(encoding="utf-8"))
tag["values"] = [value for value in tag["values"] if value not in
                 ("openblocks_reborn:elevator", "openblocks_reborn:elevator_rotating")]
tag["values"] = list(dict.fromkeys(tag["values"] + ["openblocks_reborn:elevator_" + color for color in COLORS]))
write(pickaxe, tag)

# Remove the two retired registrations' assets and transitional dependency recipes.
for name in ("elevator", "elevator_rotating"):
    for folder in ("assets/openblocks_reborn/blockstates", "assets/openblocks_reborn/models/block",
                   "assets/openblocks_reborn/models/item", "data/openblocks_reborn/loot_table/blocks",
                   "data/openblocks_reborn/recipe"):
        path = RES / folder / (name + ".json")
        if path.exists():
            path.unlink()
    recipe = RES / f"data/openblocks_reborn/recipe/convert_{name}.json"
    if recipe.exists():
        recipe.unlink()
    for locale in ("en_us", "zh_cn"):
        path = RES / f"assets/openblocks_reborn/patchouli_books/openblocks_guide/{locale}/entries/world_blocks/{name}.json"
        if path.exists():
            path.unlink()

path = ROOT / "tools/guide_descriptions.json"
desc = json.loads(path.read_text(encoding="utf-8"))
for name in ("elevator", "elevator_rotating"):
    desc.pop(name, None)
for color in COLORS:
    desc[f"elevator_{color}"] = {
        "zh_cn": "完整并入的高级电梯。站在上面跳跃上升、潜行下降。空手右键打开设置，可启用定向传送和隐藏箭头；手持方块右键伪装，潜行空手右键清除伪装。可重新染色。同色限制、范围、到达居中、视角和经验消耗由 OpenBlocks 电梯配置控制，无需独立 ElevatorMod。",
        "en_us": "The fully integrated elevator. Jump up, sneak down. Use empty-handed for direction and arrow settings. Use a block to camouflage; sneak-use empty-handed to clear it. Re-dye with crafting. Matching colors, range, centering, pitch and XP follow OpenBlocks' elevator config. No ElevatorMod dependency."
    }
write(path, desc)

for locale in ("zh_cn", "en_us"):
    path = RES / f"assets/openblocks_reborn/lang/{locale}.json"
    lang = json.loads(path.read_text(encoding="utf-8-sig"))
    if locale == "zh_cn":
        color_names = "白色 橙色 品红色 淡蓝色 黄色 黄绿色 粉红色 灰色 淡灰色 青色 紫色 蓝色 棕色 绿色 红色 黑色".split()
        for color, translated in zip(COLORS, color_names):
            lang[f"block.openblocks_reborn.elevator_{color}"] = translated + "高级电梯"
        lang.update({
            "screen.openblocks_reborn.elevator": "电梯设置",
            "screen.openblocks_reborn.elevator.directional": "定向传送",
            "screen.openblocks_reborn.elevator.directional_north": "北",
            "screen.openblocks_reborn.elevator.directional_east": "东",
            "screen.openblocks_reborn.elevator.directional_south": "南",
            "screen.openblocks_reborn.elevator.directional_west": "西",
            "screen.openblocks_reborn.elevator.hide_arrow": "隐藏箭头",
            "screen.openblocks_reborn.elevator.reset_camo": "清除伪装",
            "openblocks_reborn.subtitle.teleport": "电梯传送",
            "openblocks_reborn.subtitle.camouflage": "电梯伪装",
            "openblocks_reborn.message.missing_xp": "使用电梯所需的经验不足",
            "tag.item.openblocks_reborn.elevators": "高级电梯"
        })
    for key in list(lang):
        if key in ("block.openblocks_reborn.elevator", "block.openblocks_reborn.elevator_rotating"):
            del lang[key]
        elif key.startswith(("message.openblocks_reborn.book.detail.elevator", "patchouli.openblocks_reborn.current.elevator.",
                             "patchouli.openblocks_reborn.current.elevator_rotating.", "patchouli.openblocks_reborn.legacy.elevator.")):
            del lang[key]
    lang["patchouli.openblocks_reborn.book.landing"] = (
        "开放式方块是一个开源模组，它将大量创意引入 Minecraft 世界，包括滑翔翼、电梯、蓄水槽、墓碑、雕像，以及更多有趣的小玩意！"
        if locale == "zh_cn" else
        "OpenBlocks is an open source mod that introduces a variety of ideas into Minecraft, including hang gliders, elevators, tanks, graves, paints, cranes, liquid XP, cartographers, trophies and more!"
    )
    lang["patchouli.openblocks_reborn.book.subtitle"] = "奇思妙想，随手可得" if locale == "zh_cn" else "Little inventions, endless possibilities"
    write(path, lang)

print("Removed legacy elevators; configured 16 integrated colors")
