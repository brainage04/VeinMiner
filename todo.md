# VeinMiner todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [x] **Medium, both loaders:** `/veinminer admin selection tags allow|deny add|remove <tag>` rejects namespaced ids such as `c:ores` or `minecraft:planks` (the README's format) with "Expected whitespace to end one argument". `VeinMinerCommand.java:160,163,171,174` use `StringArgumentType.word()`; use an identifier argument. Fixed: the `<tag>` arguments are identifier arguments (un-namespaced ids still mean `minecraft:`), `add` tab-completes block tags and `remove` the current entries; shared GameTest `selection_tag_commands_accept_namespaced_ids` covers add/remove on both loaders.
