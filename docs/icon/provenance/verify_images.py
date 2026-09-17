"""Decode all PNGs; verify contact and geometry-aligned absence of motion lines.
Uses the approved blender-v diagnostic's projected mesh masks and cyan guards.
"""
import hashlib
import json
import math
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent
LABELS = [f'yaw-plus{yaw}-pitch30' for yaw in (30, 40, 50, 60)]
reports, metadata, manifest, render_report = [], [], [], []
for label in LABELS:
    info = json.loads((ROOT / (label + '-metadata.json')).read_text())
    geo = info['geometry']
    image = Image.open(ROOT / (label + '.png'))
    image.load()
    assert image.format == 'PNG' and image.size == (1024, 1024)
    assert hashlib.sha256((ROOT / (label + '.png')).read_bytes()).hexdigest() == info['png']['sha256']
    rgb = image.convert('RGB')
    tool_mask, ore_mask = Image.new('L', image.size), Image.new('L', image.size)
    for key, mask in [('tool_polygons', tool_mask), ('ore_polygons', ore_mask)]:
        draw = ImageDraw.Draw(mask)
        for polygon in geo[key]:
            draw.polygon([tuple(p) for p in polygon], fill=255)
    tool_guard = tool_mask.filter(ImageFilter.MaxFilter(9))
    ore_guard = ore_mask.filter(ImageFilter.MaxFilter(73))
    allowed = ImageChops.lighter(tool_guard, ore_guard)
    unsupported = []
    for y in range(1024):
        for x in range(1024):
            if allowed.getpixel((x, y)):
                continue
            r, g, b = rgb.getpixel((x, y))
            if min(g, b) > 70 and g > r * 1.15 and b > r * 1.15:
                unsupported.append((x, y))
    assert not unsupported, (label, len(unsupported), unsupported[:10])
    ax, ay = geo['anchor_pixel']
    anchor_rgb = rgb.getpixel((round(ax), round(ay)))
    background_rgb = rgb.getpixel((0, 0))
    contact_distance = math.dist(anchor_rgb, background_rgb)
    assert contact_distance > 8, (label, 'Background gap at contact', anchor_rgb)
    assert geo['tip_to_anchor_distance_world'] == 0.0
    assert geo['ore_blocks'] == 8 and geo['selection_curves'] == 24 and geo['motion_lines'] == 0
    assert geo['true_voxel_depth_world'] == .34 and geo['projected_thickness_pixels'] > 8
    assert info['byte_identity']['all_other_serialized_bytes_identical']
    overlay = rgb.copy()
    painter = ImageDraw.Draw(overlay)
    painter.rectangle(tool_mask.getbbox(), outline=(255, 170, 50), width=2)
    painter.ellipse((ax-5, ay-5, ax+5, ay+5), outline=(255, 80, 80), width=2)
    overlay.save(ROOT / 'diagnostics' / (label + '-geometry-check.png'))
    tool_guard.save(ROOT / 'diagnostics' / (label + '-pickaxe-mask.png'))
    reports.append({'label': label, 'PNG_opened_and_decoded': True, 'resolution': [1024, 1024],
                    'unsupported_cyan_pixels_whole_frame': len(unsupported),
                    'anchor_RGB': list(anchor_rgb), 'background_RGB': list(background_rgb),
                    'contact_foreground_color_distance_RGB': contact_distance,
                    'tip_to_anchor_distance_world': 0.0, 'composition_bounds_pixels': geo['composition_bounds_pixels'],
                    'visible_real_sidewall_quads': geo['visible_sidewall_quads'], 'projected_thickness_pixels': geo['projected_thickness_pixels']})
    renderer = info['renderer']
    render_report.append({'label': label, **renderer, 'duration_definition': 'Wall time around bpy.ops.render.render(write_still=False), including Cycles and compositor; excludes subsequent PNG encoding.'})
    manifest.append({'project': 'VeinMiner', 'label': f'Static anchored 2x2x2 — yaw +{geo["yaw_degrees"]}, pitch 30',
                     'path': f'blender-x/vein-miner/{label}.png',
                     'method': f'Blender {renderer["Blender"]}; Cycles CPU; {renderer["samples"]} samples; {renderer["threads"]} thread; 1024x1024 PNG; {renderer["duration_seconds"]:.6f} seconds',
                     'source': 'blender-v/05-yaw-plus20-pitch44.png and its packed .blend; genuine Minecraft 26.2 client-jar ore/pickaxe textures; source/texture-provenance.json',
                     'notes': 'Only the pickaxe Object.rot[3] serialized bytes changed from approved base. T(anchor) @ Rz(yaw) @ Ry(pitch); tip-anchor distance exactly 0.0. Every other decompressed .blend byte identical, including camera/materials/ore/outline/mesh/packed textures/render settings. Real two-texel voxel extrusion, no motion lines. Adjacent .py and packed .blend; original internal source labels/filepath deliberately retained to preserve byte identity. Local-only, not Git committed.'})
    metadata.append(info)
assert len({m['byte_identity']['candidate_nonrotation_sha256'] for m in metadata}) == 1
assert len({tuple(m['geometry']['anchor_world']) for m in metadata}) == 1
assert len({tuple(m['geometry']['anchor_pixel']) for m in metadata}) == 1
assert len({m['png']['sha256'] for m in metadata}) == 4
result = {'method': 'Pillow decodes every final PNG. Rasterized Blender-projected actual mesh polygons permit 4px item AA and 36px ore halo; no unsupported cyan pixels elsewhere. Object geometry separately proves 24 straight cluster-bound curves and zero trails.',
          'candidates': reports, 'only_rotation_changes': True, 'maximum_tip_to_anchor_distance_world': 0.0,
          'common_nonrotation_serialized_sha256': metadata[0]['byte_identity']['candidate_nonrotation_sha256']}
(ROOT / 'image-verification.json').write_text(json.dumps(result, indent=2) + '\n')
(ROOT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
(ROOT / 'render-report.json').write_text(json.dumps(render_report, indent=2) + '\n')
sheet = Image.new('RGB', (1024, 1104), (23, 33, 39))
draw = ImageDraw.Draw(sheet)
for i, label in enumerate(LABELS):
    x, y = (i % 2) * 512, (i // 2) * 552
    with Image.open(ROOT / (label + '.png')) as image:
        sheet.paste(image.convert('RGB').resize((512, 512), Image.Resampling.LANCZOS), (x, y))
    draw.text((x+18, y+520), f'Yaw +{metadata[i]["geometry"]["yaw_degrees"]}, pitch 30; tip error 0', fill=(200, 244, 248))
sheet.save(ROOT / 'diagnostics' / 'four-candidate-contact.png')
print(json.dumps(result, indent=2))
