"""Reproducible, local-only exact-base swivel sweep.

No Git commit: the explicit portfolio policy forbids commits/tracked-file edits.
We patch ONLY Object.rot[3] in the approved serialized scene. Blender's normal
save operation rewrites unrelated data, so candidates instead retain every
other uncompressed source byte, including packed real Minecraft textures.
Render output is saved directly from Render Result, without changing filepath.
"""
import bpy
import hashlib
import itertools
import json
import math
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import time
from mathutils import Matrix, Vector
from bpy_extras.object_utils import world_to_camera_view

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / 'source' / 'approved-base.blend'
SOURCE_SHA256 = 'a370ed212eafe42cdbce31c09aa7c6754bfed100517f0688bd5c586edc21bdf6'
TOOL_NAME = 'Diamond pickaxe rigid tip swivel'
ANCHOR = Vector((0.008, 0.008, 1.992))
CANDIDATES = [(f'yaw-plus{yaw}-pitch30', yaw, 30) for yaw in (30, 40, 50, 60)]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def save_json(name, data):
    (ROOT / name).write_text(json.dumps(data, indent=2) + '\n')


def raw_blend(data):
    if data.startswith(b'\x28\xb5\x2f\xfd'):
        return subprocess.run(['zstd', '-d', '-c'], input=data, capture_output=True, check=True).stdout
    assert data.startswith(b'BLENDER')
    return data


def rotation_offset(data):
    """Read embedded SDNA, never infer a field offset from a coincidental float."""
    assert data[:17] == b'BLENDER17-01v0501', data[:17]
    blocks = []
    pos = 17
    while pos < len(data):
        code, sdna, old, size, count = struct.unpack_from('<4sIQQQ', data, pos)
        start = pos + 32
        assert start + size <= len(data)
        blocks.append((code, sdna, start, size, count))
        pos = start + size
    assert pos == len(data) and blocks[-1][0] == b'ENDB'
    _, _, start, size, _ = next(b for b in blocks if b[0] == b'DNA1')
    dna = data[start:start + size]
    assert dna[:8] == b'SDNANAME'
    pos = 8
    count = struct.unpack_from('<I', dna, pos)[0]
    pos += 4
    names = []
    for _ in range(count):
        end = dna.index(b'\0', pos)
        names.append(dna[pos:end].decode())
        pos = end + 1
    pos = (pos + 3) & ~3
    assert dna[pos:pos + 4] == b'TYPE'
    count = struct.unpack_from('<I', dna, pos + 4)[0]
    pos += 8
    types = []
    for _ in range(count):
        end = dna.index(b'\0', pos)
        types.append(dna[pos:end].decode())
        pos = end + 1
    pos = (pos + 3) & ~3
    assert dna[pos:pos + 4] == b'TLEN'
    pos += 4
    lengths = struct.unpack_from('<' + str(count) + 'H', dna, pos)
    pos += 2 * count
    pos = (pos + 3) & ~3
    assert dna[pos:pos + 4] == b'STRC'
    count = struct.unpack_from('<I', dna, pos + 4)[0]
    pos += 8
    structures = []
    for _ in range(count):
        type_index, field_count = struct.unpack_from('<HH', dna, pos)
        pos += 4
        fields = [struct.unpack_from('<HH', dna, pos + 4 * i) for i in range(field_count)]
        pos += field_count * 4
        structures.append((type_index, fields))
    object_index = next(i for i, (type_index, _) in enumerate(structures) if types[type_index] == 'Object')
    type_index, fields = structures[object_index]
    field_offsets = {}
    offset = 0
    for type_id, name_id in fields:
        name = names[name_id]
        elements = math.prod(int(n) for n in re.findall(r'\[(\d+)\]', name))
        size = (8 if '*' in name else lengths[type_id]) * elements
        field_offsets[name] = (offset, size, types[type_id])
        offset += size
    assert offset == lengths[type_index]
    matches = [b for b in blocks if b[0] == b'OB\0\0' and b[1] == object_index
               and (b'OB' + TOOL_NAME.encode() + b'\0') in data[b[2]:b[2] + b[3]]]
    assert len(matches) == 1
    _, _, start, size, count = matches[0]
    assert count == 1 and size == lengths[type_index]
    offset, size, field_type = field_offsets['rot[3]']
    assert size == 12 and field_type == 'float'
    assert struct.unpack_from('<h', data, start + field_offsets['rotmode'][0])[0] == 1  # XYZ Euler
    return start + offset


def patch_candidate(base_data, yaw, pitch):
    offset = rotation_offset(base_data)
    target = Matrix.Translation(ANCHOR) @ Matrix.Rotation(math.radians(yaw), 4, 'Z') @ Matrix.Rotation(math.radians(pitch), 4, 'Y')
    rotation_bytes = struct.pack('<3f', *target.to_euler('XYZ'))
    result = base_data[:offset] + rotation_bytes + base_data[offset + 12:]
    assert len(result) == len(base_data)
    assert result[:offset] == base_data[:offset] and result[offset + 12:] == base_data[offset + 12:]
    return result, {
        'only_mutated_DNA_field': 'Object[Diamond pickaxe rigid tip swivel].rot[3]',
        'rotation_byte_interval_zero_based_half_open': [offset, offset + 12],
        'source_euler_xyz_radians': list(struct.unpack_from('<3f', base_data, offset)),
        'candidate_euler_xyz_radians': list(struct.unpack_from('<3f', result, offset)),
        'all_other_serialized_bytes_identical': True,
        'unchanged_byte_count': len(base_data) - 12,
        'raw_blend_byte_count': len(base_data),
        'source_nonrotation_sha256': sha(base_data[:offset] + base_data[offset + 12:]),
        'candidate_nonrotation_sha256': sha(result[:offset] + result[offset + 12:]),
        'changed_byte_offsets': [i for i, (a, b) in enumerate(zip(base_data, result)) if a != b],
        'comparison_scope': 'Entire decompressed .blend, not a selected property subset. Only 12 Object.rot bytes excluded; camera, mesh, UVs, materials, packed textures, compositor, lights, render settings, source labels, output filepath and UI state all retained.'}


def project(scene, point):
    p = world_to_camera_view(scene, scene.camera, point)
    return [p.x * 1024, (1 - p.y) * 1024]


def geometry(scene, tool, yaw, pitch):
    bpy.context.view_layer.update()
    blocks = [o for o in scene.objects if o.get('minecraft_block') == 'minecraft:deepslate_diamond_ore']
    cells = sorted(list(o['grid_cell']) for o in blocks)
    assert cells == [list(c) for c in itertools.product(range(2), repeat=3)]
    tip = tool.matrix_world @ Vector(tool['tip_local'])
    distance = (tip - ANCHOR).length
    assert distance == 0.0
    assert tuple(tool['tip_local']) == (0., 0., 0.)
    edge_errors = [min((v.co - Vector((0, y, 0))).length for v in tool.data.vertices) for y in (-.17, .17)]
    assert max(edge_errors) < 1e-7
    assert sorted({round(v.co.y, 7) for v in tool.data.vertices}) == [-.17, .17]
    assert len(tool.data.polygons) == 208 and tool['opaque_texels'] == 68
    expected = Matrix.Translation(ANCHOR) @ Matrix.Rotation(math.radians(yaw), 4, 'Z') @ Matrix.Rotation(math.radians(pitch), 4, 'Y')
    matrix_error = max(abs(tool.matrix_world[i][j] - expected[i][j]) for i in range(4) for j in range(4))
    assert matrix_error < 3e-7
    minimum_z = min((tool.matrix_world @ v.co).z for v in tool.data.vertices)
    assert minimum_z >= ANCHOR.z - 1e-6
    curves = [o for o in scene.objects if o.type == 'CURVE']
    assert len(curves) == 24
    for curve in curves:
        assert curve.get('role') == 'cluster_selection_only'
        assert len(curve.data.splines) == 1 and len(curve.data.splines[0].points) == 2
        points = [curve.matrix_world @ Vector(p.co[:3]) for p in curve.data.splines[0].points]
        assert all(-1e-7 <= v <= 2.0000001 for p in points for v in p)
        assert sum(points[0][i] == points[1][i] for i in range(3)) == 2
    assert not any(o.animation_data for o in scene.objects)
    assert not any(w in o.name.lower() for o in scene.objects for w in ('motion', 'swing', 'lightning', 'trail'))
    def polygons(objects):
        return [[project(scene, o.matrix_world @ o.data.vertices[i].co) for i in p.vertices] for o in objects for p in o.data.polygons]
    tool_polys, ore_polys = polygons([tool]), polygons(blocks)
    points = [p for poly in tool_polys + ore_polys for p in poly]
    bounds = [min(p[0] for p in points), min(p[1] for p in points), max(p[0] for p in points), max(p[1] for p in points)]
    assert 0 < bounds[0] < bounds[2] < 1024 and 0 < bounds[1] < bounds[3] < 1024, bounds
    sidewalls = []
    camera_normal = scene.camera.matrix_world.to_3x3() @ Vector((0, 0, 1))
    for polygon in tool.data.polygons:
        if len({round(tool.data.vertices[i].co.y, 7) for i in polygon.vertices}) < 2:
            continue
        if (tool.matrix_world.to_3x3() @ polygon.normal).dot(camera_normal) <= 0:
            continue
        pts = tool_polys[polygon.index]
        area = abs(sum(pts[i][0] * pts[(i+1) % len(pts)][1] - pts[(i+1) % len(pts)][0] * pts[i][1] for i in range(len(pts)))) / 2
        if area > 1:
            sidewalls.append(area)
    assert len(sidewalls) >= 10 and sum(sidewalls) > 1000
    depth = math.dist(*[project(scene, tool.matrix_world @ Vector((0, y, 0))) for y in (-.17, .17)])
    assert depth > 8
    return {'ore_blocks': len(blocks), 'grid_cells': cells, 'tip_world': list(tip), 'anchor_world': list(ANCHOR),
            'tip_to_anchor_distance_world': distance, 'actual_terminal_edge_vertex_errors': edge_errors,
            'rotation_convention': 'T(anchor) @ Rz(yaw) @ Ry(pitch)', 'yaw_degrees': yaw, 'pitch_degrees': pitch,
            'inherited_base_scale_unchanged': list(tool.scale), 'matrix_max_float_error_from_convention': matrix_error,
            'minimum_pickaxe_world_z': minimum_z, 'no_ore_penetration': True, 'selection_curves': len(curves), 'motion_lines': 0,
            'true_voxel_depth_world': .34, 'visible_sidewall_quads': len(sidewalls), 'visible_sidewall_area_pixels': sum(sidewalls),
            'projected_thickness_pixels': depth, 'composition_bounds_pixels': bounds, 'anchor_pixel': project(scene, tip),
            'tool_polygons': tool_polys, 'ore_polygons': ore_polys}


def run(label, render=True):
    _, yaw, pitch = next(row for row in CANDIDATES if row[0] == label)
    source_bytes = SOURCE.read_bytes()
    assert sha(source_bytes) == SOURCE_SHA256
    base_data = raw_blend(source_bytes)
    candidate, proof = patch_candidate(base_data, yaw, pitch)
    blend_path = ROOT / (label + '.blend')
    blend_path.write_bytes(candidate)
    bpy.ops.wm.open_mainfile(filepath=str(blend_path), load_ui=False, use_scripts=False)
    assert bpy.app.version_string == '5.1.1', bpy.app.version_string
    scene = bpy.context.scene
    tool = bpy.data.objects[TOOL_NAME]
    geometry_proof = geometry(scene, tool, yaw, pitch)
    assert scene.render.engine == 'CYCLES' and scene.cycles.device == 'CPU'
    assert scene.cycles.samples == 64 and scene.render.threads_mode == 'FIXED' and scene.render.threads == 1
    assert (scene.render.resolution_x, scene.render.resolution_y, scene.render.resolution_percentage) == (1024, 1024, 100)
    assert not os.environ.get('DISPLAY') and not os.environ.get('WAYLAND_DISPLAY')
    assert '-noaudio' in sys.argv and bpy.app.background
    assert len(os.sched_getaffinity(0)) <= 2
    textures = [{'name': i.name, 'sha256': sha(i.packed_file.data), 'packed': True} for i in bpy.data.images if i.source == 'FILE' and i.packed_file]
    provenance = json.loads((ROOT / 'source' / 'texture-provenance.json').read_text())
    assert len(textures) == 2 and {t['sha256'] for t in textures} == {t['sha256'] for t in provenance}
    report = {'label': label, 'source': 'blender-v/05-yaw-plus20-pitch44.png', 'source_blend_sha256': SOURCE_SHA256,
              'blend_sha256': sha(candidate), 'byte_identity': proof, 'geometry': geometry_proof, 'packed_real_minecraft_textures': textures}
    save_json('diagnostics/' + label + '-projection.json', geometry_proof)
    if not render:
        save_json(label + '-metadata.json', report)
        return report
    started = time.perf_counter()
    bpy.ops.render.render(write_still=False)
    duration = time.perf_counter() - started
    output = ROOT / (label + '.png')
    bpy.data.images['Render Result'].save_render(str(output), scene=scene)
    image = bpy.data.images.load(str(output), check_existing=False)
    assert tuple(image.size) == (1024, 1024)
    bpy.data.images.remove(image)
    cgroup_line = Path('/proc/self/cgroup').read_text().strip()
    cgroup_path = Path('/sys/fs/cgroup') / cgroup_line.split('::', 1)[1].lstrip('/')
    controls = {name: (cgroup_path / name).read_text().strip() for name in ('cpu.weight', 'cpu.max', 'cpuset.cpus.effective') if (cgroup_path / name).exists()}
    assert controls.get('cpu.weight') == '20', controls
    assert blend_path.read_bytes() == candidate  # Render did not mutate our exact-byte .blend.
    report['renderer'] = {'Blender': bpy.app.version_string, 'engine': 'Cycles', 'device': 'CPU', 'samples': scene.cycles.samples,
                          'threads': scene.render.threads, 'duration_seconds': duration, 'resolution': [1024, 1024],
                          'noaudio': True, 'display_server': False, 'affinity': sorted(os.sched_getaffinity(0)),
                          'cgroup': cgroup_line, 'cgroup_controls': controls}
    report['png'] = {'sha256': sha(output.read_bytes()), 'dimensions': [1024, 1024]}
    save_json(label + '-metadata.json', report)
    print('RENDER_COMPLETE', label, duration, flush=True)
    return report


def main(labels=None):
    labels = labels or [c[0] for c in CANDIDATES]
    results = []
    try:
        for label in labels:
            results.append(run(label, render='--build-only' not in sys.argv))
        save_json('rotation-proof.json', {
            'source_blend_sha256': SOURCE_SHA256,
            'all_other_serialized_properties_byte_identical_to_approved_base': True,
            'method': 'Compare every byte of decompressed source/candidate .blend, excluding only the 12 SDNA Object.rot[3] bytes. No save/rebuild, camera fit, output path, metadata, material or texture mutation.',
            'common_nonrotation_sha256': results[0]['byte_identity']['source_nonrotation_sha256'],
            'maximum_tip_to_anchor_distance_world': max(r['geometry']['tip_to_anchor_distance_world'] for r in results),
            'candidates': [{'label': r['label'], 'byte_identity': r['byte_identity'], 'tip_to_anchor_distance_world': r['geometry']['tip_to_anchor_distance_world'], 'yaw_degrees': r['geometry']['yaw_degrees'], 'pitch_degrees': r['geometry']['pitch_degrees']} for r in results]})
        save_json('blockers.json', [])
    except Exception as error:
        save_json('blockers.json', [{'stage': 'exact-base swivel render', 'error_type': type(error).__name__, 'evidence': str(error)}])
        raise


if __name__ == '__main__':
    args = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    main([arg for arg in args if not arg.startswith('--')] or None)
