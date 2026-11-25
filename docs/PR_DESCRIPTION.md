# Title

Restore guide and sonic visualization, integrate elevators, and configure village inspection and enchantments

# Description

The guide now opens through required Patchouli and follows the 1.12.2 manual's sections: welcome/credits, blocks, items, miscellaneous and changelogs. Current instructions and recipes are separated from explicitly marked historical text, and known unrecovered features are documented.

The complete common and NeoForge implementation/resources from ElevatorMod's 1.21.1-1.11.4 source branch are incorporated under OpenBlocks' package and registry namespace, with MIT attribution. Sixteen colored `openblocks_reborn:elevator_*` blocks include directional settings, camouflage, model wrapping, configuration, input, networking and sound. The two retired OpenBlocks elevator registrations and their old handlers/recipes are removed; no standalone `elevatorid` dependency or second mod is registered. Existing worlds require the documented ID/NBT migration before switching.

Sonic Glasses visualize actually started positional sounds with fading legacy icons, localized subtitles and distance. Modern sculk/Warden, Allay, amethyst, copper and wind sounds receive categories. The previous server-side glowing effect and particle broadcast are removed. Sound callbacks use post-resolution source events, avoiding unresolved-sound volume access.

The source also includes focused server game tests, an opt-in separate client acceptance mod, resource validation and source/license provenance. See `docs/VALIDATION.md` for the exact checks and limits. World migration is a separate offline operation; no private worlds or backups are included.

Village Highlighter inspects a targeted POI block and follows one actual memory-linked villager. All mode is a manually requested static snapshot, with a server-enforced 200-tick cooldown and 200-tick display lifetime; it never polls automatically. Loaded-area queries, cached memory indexes and capped lists bound work. Snapshots include at most 16 villagers, with truncation warnings. Optional modern village-section boundaries are distinct from golem search boxes and guaranteed spawn positions. New headless tests cover ownership, sampling limits, cooldown and packet bounds; client visuals and FPS for this feature have not been tested.

Enchantment descriptions and the paginated guide now match actual levels and mechanics. Last Stand attempts XP-funded rescue only after vanilla totems, with configurable cost multiplier/cooldown and all damage sources eligible. Unstable has bounded explosion scaling and defaults to no terrain damage. Flim Flam retains its PvP counterplay, with six individual pranks off by default. All three have independent effect switches and configurable treasure classification (legacy non-treasure defaults), implemented with additive vanilla tags. A standard NeoForge config screen and description-mod-compatible tooltips are included. Headless tests cover actual totem priority, insufficient funds, cooldown, absorption, void/kill, treasure tags and individual status toggles.
