This repository contains a server-side only modding template called VeinMiner. Specifications:
- Minecraft version: 1.21.11
- Modloader: Fabric
- Mappings: Mojmap (official Mojang mappings)
- Build system: Gradle
- JDK: Eclipse Temurin 21.0.10

You are to implement the following features:
- Config
    - The config system for VeinMiner should make use of the new Dialog functionality introduced to Minecraft recently
        - Data-driven screens can be created by server-side mods and opened by players who have admin permissions using the /dialog command
    - The config system should have the following settings:
        - Enable Vein Mining: boolean. true by default. enables vein mining functionality. toggle with /veinminer toggle/enable/disable
        - Vein Size: int. 64 by default. specifies how many blocks can be mined in a single vein mine operation (this means that when scanning for blocks to be vein mined, stop when the number of blocks to be mined is greater than or equal to this value). modify with /veinminer vein_size <amount>
        - Better Ore Vein Mining: boolean. true by default. allows veins that are a mix of Stone and Deepslate ores to be vein mined as one, essentially meaning that Stone and Deepslate ores are seen as the same block (e.g. Iron Ore == Deepslate Iron Ore). true by default. toggle with /veinminer better_ore_vein_mining toggle/enable/disable
            - NOTE: Extending this functionality to work with modded ores is a plus. Please investigate whether or not it is possible to do this, and implement it if so.
        - Better Tree Vein Mining: boolean. true by default. when logs are vein mined, also applies the vein mine operation to leaves of the same type (e.g. Oak Logs and Oak Leaves) that are adjacent to any of the logs in the vein. true by default. toggle with /veinminer better_tree_vein_mining toggle/enable/disable
        - Whitelist: List of Blocks (List<Block> or Block[], or something else if lists/arrays are not supported) that can be vein mined.
            - Should contain the following blocks by default:
                - All ores (coal, iron, gold, copper, diamond, emerald, lapis, redstone, nether quartz, nether gold, ancient debris)
                - All deepslate ores (coal, iron, gold, copper, diamond, emerald, lapis, redstone)
                - All logs (overworld logs AND nether logs)
                - All leaves (overworld leaves AND nether leaves - i believe these are just the two wart blocks)
            - If the whitelist is empty, nothing can be vein mined.
            - Add block with /veinminer whitelist add <block>
            - Remove block with /veinminer whitelist remove <block>
            - List blocks with /veinminer whitelist list
            - Clear list with /veinminer whitelist clear
                - When this command is run, it should prompt user with a confirmation asking "ARE YOU SURE? This operation cannot be undone! Run the command again within 10 seconds to confirm this operation."
                - If the user runs the command again within 10 seconds, clear the whitelist
                - If the user does not run the command again within 10 seconds, send a message at the 10 second mark saying "Operation cancelled."
                - If the user run the command again but after the 10 second mark, ask for confirmation as usual
                - Confirmation should be tracked on a per-player basis (so two admins don't race).
    - Persistence/save to disk/load from disk can be handled using a config file and the Gson library (but if you have a better idea, I'm all ears)
    - If, for whatever reason, an admin chooses to make changes to the config via the config file, these can be loaded using /veinminer load_from_disk

- Functionality
    - Vein Mining prerequisites:
        - Enable Vein Mining setting is true
        - Player is holding the correct tool to mine the block (if applicable)
        - Player is shifting
    - If all of these prerequisites are fulfilled, and the player mines a block, a vein mining operation should be performed at the BlockPos of the mined block. This should start a scan for a "vein" (any blocks directly adjacent in all 3 dimensions, including diagonals, for a total of 26 adjacent block positions. this scan should be applied to those scanned blocks, and so on, until no more blocks are left to scan or the number of blocks to be vein mined is greater than or equal to the Vein Size value, whichever one happens first).
    - It is CRITICAL that this algorithm is as performant as possible. There are many Minecraft mods that implement this functionality, and almost all of them are terribly performant when it comes to vein mining large veins (e.g. Spruce/Jungle trees, large veins of Iron Ore).
    - Once the blocks in the vein have been finalised, each of these blocks should be broken as if they were broken by the player normally, so that enchants such as Fortune or Silk Touch are properly applied.
    - All of the drops from all of the blocks broken should be teleported to the location of the block originally broken by the player
    - Once all of the block drops have been teleported, item entities should be combined where possible (e.g. if there are 50 item entities with a type of Iron Ore and a count of 1, they should be combined into one item entity with a count of 50 to improve performance)
        - NOTE: I am almost certain that this functionality exists in vanilla Minecraft, however it only occurs periodically (every 20-40 ticks?). I would like this functionality to be applied instantly to all items at the location of the block originally broken by the player
    - All of this should be done within the same tick that the block originally broken by the player was broken. If this is not possible, please let me know. If this is possible but there are performance/lag concerns, please let me know.

Constraints:
- Keep changes minimal and idiomatic for Fabric.
- Prefer Mojmap names consistent with the project.
- Don’t introduce new dependencies unless necessary.

Definition of done:
- `./gradlew build` passes
- The mod launches and exits gracefully in dev client (`timeout 1m ./gradlew runClient --no-daemon`)
- Briefly summarize what you changed and where.

Proceed autonomously: edit files, run Gradle tasks, fix issues until done.
If something is ambiguous, make a reasonable assumption and state it.