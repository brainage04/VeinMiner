# VeinMiner icon

## What this is

`docs/icon/icon.png` — the mod's icon: 1024x1024 PNG, 8-bit RGBA, non-interlaced,
1,287,433 bytes, sha256
`8418673887b0f338e9fd9b7f636320fc1c83aa900a272d3a8c3c33a661fbdc44`.

## How it was made

Blender render (Blender **5.1.1**, headless CLI, **Cycles on CPU**, 64 samples, 1 render
thread, 1024x1024, `-noaudio`, no display server, no GPU; 43.4 s).

The scene is a staged 2x2x2 deepslate-diamond-ore cluster with a diamond pickaxe tip
resting on its front-top corner. It is **not** a game screenshot and there is **no shader
pack**: it is Blender Cycles rendering genuine Minecraft assets.

- Ore: 8 real block cells at grid cells (0,0,0) … (1,1,1), each block 0.984 units inset by
  0.008; 24 straight cyan selection edges (12 cluster boundary edges + 12 interior grid
  seams) as 3D curves; two area lights; a Fog Glow compositor glare; orthographic camera.
- Pickaxe: a real **two-texel voxel extrusion** of the genuine 16x16 `diamond_pickaxe`
  sprite (68 opaque texels -> 208 faces, 0.34 units thick), posed statically — no swing,
  no motion lines.
- Imagery provenance — genuine client-jar textures from the **Minecraft 26.2** merged
  client jar `/home/thomas/.gradle/caches/fabric-loom/26.2/minecraft-merged.jar`:
  - `assets/minecraft/textures/block/deepslate_diamond_ore.png`, sha256 `0acd146e99716a104c6053198fa51fe0d1ef464501c123ae9a56ba3f6f957dd7`
  - `assets/minecraft/textures/item/diamond_pickaxe.png`, sha256 `dfec8e1467b335133521a42c8fde0f124ba7b5c45c0a84fba789736df4f92325`

  Both are packed inside the `.blend` and were re-verified byte-for-byte against the jar
  members (`provenance/source/texture-verification.json`). Re-extract with:

  ```
  unzip -p /home/thomas/.gradle/caches/fabric-loom/26.2/minecraft-merged.jar \
        assets/minecraft/textures/block/deepslate_diamond_ore.png > deepslate_diamond_ore.png
  unzip -p /home/thomas/.gradle/caches/fabric-loom/26.2/minecraft-merged.jar \
        assets/minecraft/textures/item/diamond_pickaxe.png    > diamond_pickaxe.png
  ```

- This icon is the **yaw +60 deg / pitch 30 deg** pickaxe pose. The candidate was produced
  by byte-patching only `Object["Diamond pickaxe rigid tip swivel"].rot[3]` (12 serialized
  bytes at offset 377183) with `T(anchor) * Rz(+60 deg) * Ry(+30 deg)`, anchor
  (0.008, 0.008, 1.992); the other 780,135 decompressed bytes are identical
  (non-rotation sha256 `5b1b700177395c4608460940ea42a407f943e1cfb7c7741a4602760f8a15a4a7`).
  The pickaxe tip coincides exactly with the ore corner: tip-to-anchor distance 0.0, and the
  tip projects onto pixel (428.7, 489.1).
- The camera is unchanged from the approved base scene for every candidate (it is one of
  the bytes proven identical above); open `provenance/source/approved-base.blend` in
  Blender to read its matrix.

## Provenance files

`provenance/` mirrors the round-3 authoring tree `round3/blender-x/vein-miner/`:

| file | what it is |
|---|---|
| `render_scenes.py` | author script: reads the SDNA field offset, patches the rotation bytes, re-opens the candidate, verifies geometry, renders |
| `yaw-plus60-pitch30.py` | entrypoint for this icon |
| `yaw-plus60-pitch30.blend` | the exact byte-patched candidate scene that was rendered |
| `yaw-plus60-pitch30-metadata.json` | byte-identity proof (changed byte offsets, hashes), geometry, packed texture hashes, renderer settings, PNG sha256 |
| `verify_images.py` | Pillow verification script (geometry masks, no unsupported cyan strokes, PNG hash) |
| `image-verification.json`, `rotation-proof.json`, `visual-review.json` | decoded-image checks, whole-file byte comparison, visual review of all four candidates |
| `manifest.json`, `render-report.json`, `reproduction.json`, `cleanup-report.json`, `blockers.json` | round-3 records: candidates, render timings/cgroup, exact commands, cleanup, no blockers |
| `source/approved-base.blend` | the approved base scene (sha256 `a370ed212eafe42cdbce31c09aa7c6754bfed100517f0688bd5c586edc21bdf6`), packed textures included |
| `source/approved-render_scenes.py` | the earlier script that authored that base scene (block cluster, pickaxe extrusion, lights, camera) |
| `source/approved-metadata.json` | that scene's metadata |
| `source/texture-provenance.json`, `source/texture-verification.json` | jar member paths + sha256 and their re-verification against the installed jar |

## How to regenerate

From `docs/icon/provenance`:

```
nice -n 19 nix shell nixpkgs#blender --command taskset -c 2 blender --background -noaudio --threads 1 --python yaw-plus60-pitch30.py
```

Verify the result against `yaw-plus60-pitch30.png`'s sha256 above.

## Notes

- Environment asserts: the script requires Blender **5.1.1**, `DISPLAY`/`WAYLAND_DISPLAY`
  unset, `-noaudio`, background mode, CPU affinity of at most 2 CPUs, and (when rendering)
  membership of the shared `render-blender.service` cgroup with `cpu.weight == 20`. Outside
  that cgroup the assert fires after rendering — relax that assert if you render elsewhere.
- `verify_images.py` walks all four round-3 candidates (30/40/50/60) and therefore cannot
  run unmodified with only the selected candidate present; the per-image facts it checks are
  recorded in `image-verification.json`.
- `manifest.json` still lists all four candidates; only the selected candidate's files were
  copied here. Deliberately excluded: the three non-selected rotations (+30/+40/+50) with
  their scripts, `.blend`, PNG and metadata, and `diagnostics/` (a four-candidate contact
  sheet, per-candidate geometry-check screenshots, pickaxe masks and projection JSON dumps).

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
