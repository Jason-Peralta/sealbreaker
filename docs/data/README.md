# Registry schema docs

One file per datapack registry we add (`sb:weapon_archetype`, `sb:damage_class`, `sb:reforge_modifier`, `sb:reforge_pool`, `sb:accessory_combine`, `sb:armor_set`, `sb:rarity`, `sb:seal`, `sb:tier`). Each file: the JSON shape with every field, defaults, an example, and which module reads it. Written with the registry, in the same change. Item data components we add get a file too (`reforge_prefix.md`).

- [kit.md](kit.md): transient equipment capabilities, the server-authoritative Tier 1 dash and its JSON/datagen contract (#20).
