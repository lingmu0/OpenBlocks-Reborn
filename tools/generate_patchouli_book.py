"""Generate the optional Patchouli manual from the authoritative registries.

The generated book deliberately mirrors InfoBookItem's six categories. Running
this script after adding a block or item keeps the Patchouli manual complete.
"""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java/net/xuwu/openblocks_reborn/registry"
RESOURCE_ROOT = ROOT / "src/main/resources"
BOOK_ID = "openblocks_guide"
BOOK_ROOT = RESOURCE_ROOT / "assets/openblocks_reborn/patchouli_books" / BOOK_ID / "en_us"
DESCRIPTION_PATH = ROOT / "tools/guide_descriptions.json"

MACHINES = {
    "tank", "sprinkler", "cannon", "vacuum_hopper", "fan", "xp_bottler",
    "auto_anvil", "auto_enchantment_table", "xp_drain", "block_breaker",
    "block_placer", "item_dropper", "donation_station", "paint_mixer",
    "drawing_table", "projector", "xp_shower",
}
EQUIPMENT = {
    "hang_glider", "sonic_glasses", "pencil_glasses", "crayon_glasses",
    "technicolor_glasses", "serious_glasses", "crane_backpack", "sleeping_bag",
}
COMPONENTS = {
    "generic", "generic_unstackable", "glider_wing", "beam", "crane_engine",
    "crane_magnet", "line", "map_controller", "map_memory", "assistant_base",
    "unprepared_stencil", "tasty_clay",
}
CATEGORIES = {
    "machines": ("openblocks_reborn:auto_anvil", 0),
    "world_blocks": ("openblocks_reborn:guide", 10),
    "equipment": ("openblocks_reborn:hang_glider", 20),
    "tools": ("openblocks_reborn:golden_eye", 30),
    "components": ("openblocks_reborn:crane_engine", 40),
    "fluids": ("openblocks_reborn:xp_bucket", 50),
}


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def registry_ids(source_name: str) -> list[str]:
    source = (JAVA_ROOT / source_name).read_text(encoding="utf-8")
    return re.findall(r'\bregister\("([a-z0-9_]+)"', source)


def standalone_item_ids() -> list[str]:
    source = (JAVA_ROOT / "ModItems.java").read_text(encoding="utf-8")
    return re.findall(r'\b(?:register|item)\("([a-z0-9_]+)"', source)


def category(item_id: str, is_block: bool) -> str:
    if item_id == "xp_bucket":
        return "fluids"
    if item_id in MACHINES:
        return "machines"
    if is_block:
        return "world_blocks"
    if item_id in EQUIPMENT:
        return "equipment"
    if item_id in COMPONENTS:
        return "components"
    return "tools"


def main() -> None:
    block_ids = registry_ids("ModBlocks.java")
    item_ids = standalone_item_ids()
    entries = [(item_id, True) for item_id in block_ids]
    entries.extend((item_id, False) for item_id in item_ids)
    entries.append(("xp_bucket", False))
    if len(entries) != len({item_id for item_id, _ in entries}):
        raise RuntimeError("Duplicate OpenBlocks registry IDs cannot be represented in the manual")
    descriptions = json.loads(DESCRIPTION_PATH.read_text(encoding="utf-8"))
    entry_ids = {item_id for item_id, _ in entries}
    missing = sorted(entry_ids - descriptions.keys())
    extra = sorted(descriptions.keys() - entry_ids)
    if missing or extra:
        raise RuntimeError(
            f"Guide descriptions do not match registries; missing={missing}, extra={extra}"
        )
    for item_id, localized in descriptions.items():
        if set(localized) != {"zh_cn", "en_us"} or not all(localized.values()):
            raise RuntimeError(f"Guide description for {item_id} must contain non-empty zh_cn and en_us")

    for locale in ("zh_cn", "en_us"):
        lang_path = RESOURCE_ROOT / f"assets/openblocks_reborn/lang/{locale}.json"
        lang = json.loads(lang_path.read_text(encoding="utf-8-sig"))
        for item_id, _ in entries:
            lang[f"message.openblocks_reborn.book.detail.{item_id}"] = descriptions[item_id][locale]
        write_json(lang_path, lang)

    write_json(
        RESOURCE_ROOT / "data/openblocks_reborn/patchouli_books" / BOOK_ID / "book.json",
        {
            "name": "item.openblocks_reborn.info_book",
            "landing_text": "patchouli.openblocks_reborn.book.landing",
            "subtitle": "patchouli.openblocks_reborn.book.subtitle",
            "version": "1",
            "book_texture": "patchouli:textures/gui/book_green.png",
            "use_resource_pack": True,
            "dont_generate_book": True,
            "custom_book_item": "openblocks_reborn:info_book",
            "show_progress": False,
            "show_toasts": False,
            "use_blocky_font": True,
            "i18n": True,
        },
    )

    for category_id, (icon, sortnum) in CATEGORIES.items():
        title_key = f"message.openblocks_reborn.book.category.{category_id}"
        write_json(
            BOOK_ROOT / "categories" / f"{category_id}.json",
            {
                "name": title_key,
                "description": f"{title_key}.description",
                "icon": icon,
                "sortnum": sortnum,
            },
        )

    counts = {category_id: 0 for category_id in CATEGORIES}
    recipe_root = RESOURCE_ROOT / "data/openblocks_reborn/recipe"
    for item_id, is_block in entries:
        category_id = category(item_id, is_block)
        counts[category_id] += 1
        description = f"message.openblocks_reborn.book.detail.{item_id}"
        pages: list[dict[str, object]] = [
            {
                "type": "patchouli:spotlight",
                "item": f"openblocks_reborn:{item_id}",
                "text": description,
                "link_recipe": True,
            }
        ]
        if (recipe_root / f"{item_id}.json").is_file():
            pages.append({
                "type": "patchouli:crafting",
                "recipe": f"openblocks_reborn:{item_id}",
            })
        name_prefix = "block" if is_block else "item"
        write_json(
            BOOK_ROOT / "entries" / category_id / f"{item_id}.json",
            {
                "name": f"{name_prefix}.openblocks_reborn.{item_id}",
                "icon": f"openblocks_reborn:{item_id}",
                "category": f"openblocks_reborn:{category_id}",
                "read_by_default": True,
                "sortnum": counts[category_id],
                "pages": pages,
            },
        )

    print(f"Generated Patchouli manual: {len(entries)} entries in {len(CATEGORIES)} categories")
    print("Category counts: " + ", ".join(f"{key}={value}" for key, value in counts.items()))


if __name__ == "__main__":
    main()
