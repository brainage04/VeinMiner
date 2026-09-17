"""Local-only VeinMiner tip-pivot variants, derived from blender-s cyan selection.
No Git commit: portfolio explicitly forbids commits/tracked-file edits.
Run adjacent candidate scripts with Blender background, -noaudio, CPU affinity 2.
"""
import bpy
import hashlib
import itertools
import json
import math
import os
import sys
import time
from pathlib import Path
from mathutils import Matrix, Vector
from bpy_extras.object_utils import world_to_camera_view

ROOT = Path(__file__).resolve().parent
CANDIDATES = [
    ('01-yaw-minus20-pitch40', -20.0, 40.0),
    ('02-yaw-minus10-pitch42', -10.0, 42.0),
    ('03-yaw-zero-pitch44', 0.0, 44.0),
    ('04-yaw-plus10-pitch44', 10.0, 44.0),
    ('05-yaw-plus20-pitch44', 20.0, 44.0),
]
ANCHOR = Vector((0.008, 0.008, 1.992))
TIP_TEXEL = Vector((14.0, 0.0, 4.0))
TEXEL_SIZE = 0.17
DEPTH_TEXELS = 2.0


def save_json(filename, value):
    (ROOT / filename).write_text(json.dumps(value, indent=2) + '\n')


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def texture_material(name, image, fill):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    shader = mat.node_tree.nodes.get('Principled BSDF')
    shader.inputs['Roughness'].default_value = .92
    shader.inputs['Specular IOR Level'].default_value = .08
    tex = mat.node_tree.nodes.new('ShaderNodeTexImage')
    tex.image = image
    tex.interpolation = 'Closest'
    mat.node_tree.links.new(tex.outputs['Color'], shader.inputs['Base Color'])
    mat.node_tree.links.new(tex.outputs['Color'], shader.inputs['Emission Color'])
    shader.inputs['Emission Strength'].default_value = fill
    return mat


def emission_material(name, strength):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    nodes.clear()
    output = nodes.new('ShaderNodeOutputMaterial')
    emission = nodes.new('ShaderNodeEmission')
    emission.inputs['Color'].default_value = (.075, .8, 1.0, 1.0)
    emission.inputs['Strength'].default_value = strength
    links.new(emission.outputs[0], output.inputs['Surface'])
    return mat


def box(name, low, high, material):
    x, y, z = low
    X, Y, Z = high
    verts = [(x,y,z),(X,y,z),(X,Y,z),(x,Y,z),(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]
    faces = [(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7),(0,3,2,1)]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(material)
    uv = mesh.uv_layers.new(name='Vanilla block UV')
    for poly in mesh.polygons:
        for index, coord in zip(poly.loop_indices, [(0,0),(1,0),(1,1),(0,1)]):
            uv.data[index].uv = coord
    return obj


def selection_edge(name, endpoints, material, radius):
    # Every curve is exactly one straight cluster-bound selection edge, never a trail.
    curve = bpy.data.curves.new(name, 'CURVE')
    curve.dimensions = '3D'
    curve.resolution_u = 1
    curve.bevel_depth = radius
    curve.bevel_resolution = 1
    poly = curve.splines.new('POLY')
    poly.points.add(1)
    for point, co in zip(poly.points, endpoints):
        point.co = (*co, 1)
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    curve.materials.append(material)
    obj['role'] = 'cluster_selection_only'
    return obj


def pickaxe(image, material):
    """Vanilla alpha silhouette extruded by two texels; all sidewalls are real quads.
    Origin is the exact middle of the lower edge of terminal diamond texel (13,4).
    A Y rotation <=45 degrees keeps every texel on/above the top contact plane.
    """
    pixels = list(image.pixels)
    opaque = {(x,z) for z in range(16) for x in range(16) if pixels[(z*16+x)*4+3] >= .5}
    assert (13,4) in opaque and (13,3) not in opaque
    verts, faces, uvs = [], [], []
    for x, z in sorted(opaque):
        y, Y = -DEPTH_TEXELS/2, DEPTH_TEXELS/2
        candidates = [
            (((x,y,z),(x+1,y,z),(x+1,y,z+1),(x,y,z+1)), True),
            (((x,Y,z),(x,Y,z+1),(x+1,Y,z+1),(x+1,Y,z)), True),
            (((x,y,z),(x,Y,z),(x+1,Y,z),(x+1,y,z)), (x,z-1) not in opaque),
            (((x,y,z+1),(x+1,y,z+1),(x+1,Y,z+1),(x,Y,z+1)), (x,z+1) not in opaque),
            (((x,y,z),(x,y,z+1),(x,Y,z+1),(x,Y,z)), (x-1,z) not in opaque),
            (((x+1,y,z),(x+1,Y,z),(x+1,Y,z+1),(x+1,y,z+1)), (x+1,z) not in opaque),
        ]
        for coords, exposed in candidates:
            if not exposed:
                continue
            start = len(verts)
            verts.extend([tuple((Vector(co)-TIP_TEXEL)*TEXEL_SIZE) for co in coords])
            faces.append(tuple(range(start, start+4)))
            uvs.extend([((x+.5)/16, (z+.5)/16)]*4)
    mesh = bpy.data.meshes.new('Authentic diamond pickaxe two-texel extrusion')
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new('Diamond pickaxe rigid tip swivel', mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(material)
    uv = mesh.uv_layers.new(name='Nearest authentic sprite texel')
    for loop, coord in zip(uv.data, uvs):
        loop.uv = coord
    obj['minecraft_item'] = 'minecraft:diamond_pickaxe'
    obj['opaque_texels'] = len(opaque)
    obj['thickness_texels'] = DEPTH_TEXELS
    obj['tip_local'] = (0.0, 0.0, 0.0)
    obj['tip_source_texel_edge'] = list(TIP_TEXEL)
    obj['pose'] = 'Static rigid item. Tip on ore top-left-front corner. Handle up-left.'
    return obj


def pose(obj, yaw, pitch):
    rotation = Matrix.Rotation(math.radians(yaw), 4, 'Z') @ Matrix.Rotation(math.radians(pitch), 4, 'Y')
    obj.matrix_world = Matrix.Translation(ANCHOR) @ rotation
    bpy.context.view_layer.update()


def project(point):
    scene = bpy.context.scene
    p = world_to_camera_view(scene, scene.camera, point)
    return [p.x*1024, (1-p.y)*1024]


def projections(objects):
    return [[project(obj.matrix_world @ obj.data.vertices[i].co) for i in poly.vertices]
            for obj in objects for poly in obj.data.polygons]


def fit_all_candidates(camera, tool, blocks):
    # One camera fit over all five poses. Nothing except the item rotation varies later.
    inv = camera.matrix_world.inverted()
    points = [inv @ (o.matrix_world @ v.co) for o in blocks for v in o.data.vertices]
    for _, yaw, pitch in CANDIDATES:
        pose(tool, yaw, pitch)
        points.extend(inv @ (tool.matrix_world @ v.co) for v in tool.data.vertices)
    low = [min(p[i] for p in points) for i in (0,1)]
    high = [max(p[i] for p in points) for i in (0,1)]
    camera.data.ortho_scale = max(high[i]-low[i] for i in (0,1))/.82
    camera.location += camera.matrix_world.to_3x3() @ Vector(((low[0]+high[0])/2, (low[1]+high[1])/2, 0))
    bpy.context.view_layer.update()


def build(label):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = 'CYCLES'
    scene.cycles.device = 'CPU'
    scene.cycles.samples = 64
    scene.cycles.use_denoising = True
    scene.cycles.seed = 20260915
    scene.cycles.max_bounces = 5
    scene.render.threads_mode = 'FIXED'
    scene.render.threads = 1
    scene.render.resolution_x = scene.render.resolution_y = 1024
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    scene.render.film_transparent = False
    scene.render.use_sequencer = False
    scene.view_settings.view_transform = 'Standard'
    scene.view_settings.look = 'None'
    scene.view_settings.exposure = 0
    scene.world = bpy.data.worlds.new('Source charcoal background')
    scene.world.use_nodes = True
    background = scene.world.node_tree.nodes['Background']
    background.inputs['Color'].default_value = (.009, .014, .021, 1)
    background.inputs['Strength'].default_value = 1
    ore_image = bpy.data.images.load(str(ROOT/'assets/deepslate_diamond_ore.png'))
    pick_image = bpy.data.images.load(str(ROOT/'assets/diamond_pickaxe.png'))
    ore_material = texture_material('Vanilla deepslate diamond ore', ore_image, .55)
    pick_material = texture_material('Vanilla diamond pickaxe', pick_image, .5)
    rim = emission_material('Cyan selection outer contour', 3.5)
    seam = emission_material('Cyan selection block grid', 1.225)
    blocks = []
    for cell in itertools.product(range(2), repeat=3):
        obj = box('Ore block %d %d %d' % cell, [i+.008 for i in cell], [i+.992 for i in cell], ore_material)
        obj['minecraft_block'] = 'minecraft:deepslate_diamond_ore'
        obj['grid_cell'] = cell
        blocks.append(obj)
    # Twelve outer cube edges, and two straight grid seams on each of six faces.
    for axis in range(3):
        other = [i for i in range(3) if i != axis]
        for a, b in itertools.product((0.,2.), repeat=2):
            start, end = [0.,0.,0.], [0.,0.,0.]
            start[other[0]] = end[other[0]] = a
            start[other[1]] = end[other[1]] = b
            end[axis] = 2.
            selection_edge('Selection outer %d %g %g' % (axis,a,b), (start,end), rim, .012)
    for normal in range(3):
        tangent = [i for i in range(3) if i != normal]
        for plane in (0.,2.):
            for direction in tangent:
                other = next(i for i in tangent if i != direction)
                start, end = [0.,0.,0.], [0.,0.,0.]
                start[normal] = end[normal] = plane
                start[other] = end[other] = 1.
                end[direction] = 2.
                selection_edge('Selection seam %d %g %d' % (normal,plane,direction), (start,end), seam, .0084)
    tool = pickaxe(pick_image, pick_material)
    for name, loc, energy, size, color in [('Soft vanilla key',(-3,-4,7),430,5,(.88,.97,1)),('Ore face fill',(4,-5,3),130,4,(1,1,1))]:
        data = bpy.data.lights.new(name, 'AREA')
        data.energy, data.size, data.shape, data.color = energy, size, 'DISK', color
        obj = bpy.data.objects.new(name, data)
        scene.collection.objects.link(obj)
        obj.location = loc
        obj.rotation_euler = (Vector((0,0,1))-obj.location).to_track_quat('-Z','Y').to_euler()
    data = bpy.data.cameras.new('Fixed orthographic camera across all five rotations')
    camera = bpy.data.objects.new('Camera', data)
    scene.collection.objects.link(camera)
    camera.location = (6,-10,7)
    camera.rotation_euler = Vector((-6,10,-7)).to_track_quat('-Z','Y').to_euler()
    data.type = 'ORTHO'
    scene.camera = camera
    bpy.context.view_layer.update()
    fit_all_candidates(camera, tool, blocks)
    _, yaw, pitch = next(row for row in CANDIDATES if row[0] == label)
    pose(tool, yaw, pitch)
    group = bpy.data.node_groups.new('Source restrained cyan halo', 'CompositorNodeTree')
    scene.compositing_node_group = group
    group.interface.new_socket(name='Image', in_out='OUTPUT', socket_type='NodeSocketColor')
    layers = group.nodes.new('CompositorNodeRLayers')
    glare = group.nodes.new('CompositorNodeGlare')
    glare.inputs['Type'].default_value = 'Fog Glow'
    glare.inputs['Quality'].default_value = 'High'
    glare.inputs['Threshold'].default_value = 1.1
    glare.inputs['Size'].default_value = .25
    glare.inputs['Strength'].default_value = .5
    output = group.nodes.new('NodeGroupOutput')
    group.links.new(layers.outputs['Image'], glare.inputs['Image'])
    group.links.new(glare.outputs['Image'], output.inputs['Image'])
    return scene, tool, blocks, yaw, pitch


def verify_geometry(scene, tool, blocks, yaw, pitch):
    tip = tool.matrix_world @ Vector(tool['tip_local'])
    error = (tip - ANCHOR).length
    cells = sorted([list(o['grid_cell']) for o in blocks])
    assert cells == [list(c) for c in itertools.product(range(2), repeat=3)]
    assert error < 1e-7
    # The chosen source edge is really on the terminal diamond voxel's surface.
    edge_a = (Vector((14,-1,4))-TIP_TEXEL)*TEXEL_SIZE
    edge_b = (Vector((14,1,4))-TIP_TEXEL)*TEXEL_SIZE
    edge_vertex_errors = [min((v.co-e).length for v in tool.data.vertices) for e in (edge_a,edge_b)]
    assert max(edge_vertex_errors) < 1e-7
    lowest = min((tool.matrix_world @ v.co).z for v in tool.data.vertices)
    assert lowest >= ANCHOR.z - 1e-6, (lowest, ANCHOR.z)
    curves = [o for o in scene.objects if o.type == 'CURVE']
    assert len(curves) == 24
    curve_proof = []
    for obj in curves:
        spline = obj.data.splines[0]
        endpoints = [list((obj.matrix_world @ Vector(p.co[:3]))) for p in spline.points]
        assert len(obj.data.splines) == 1 and len(endpoints) == 2
        assert obj.get('role') == 'cluster_selection_only'
        assert all(-1e-7 <= v <= 2.0000001 for p in endpoints for v in p)
        fixed_axes = [i for i in range(3) if abs(endpoints[0][i]-endpoints[1][i]) < 1e-7]
        assert len(fixed_axes) == 2
        assert any(endpoints[0][i] in (0.0,2.0) for i in fixed_axes)
        curve_proof.append({'name':obj.name,'endpoints':endpoints})
    assert not any(o.animation_data for o in scene.objects)
    assert not any(word in o.name.lower() for o in scene.objects for word in ('motion','swing','lightning','trail'))
    tool_polys, block_polys = projections([tool]), projections(blocks)
    pixels = [p for polygon in tool_polys+block_polys for p in polygon]
    bounds = [min(p[0] for p in pixels), min(p[1] for p in pixels), max(p[0] for p in pixels), max(p[1] for p in pixels)]
    assert all(40 < value < 984 for value in bounds)
    handle = tool.matrix_world @ ((Vector((2.5,1.5,2))-TIP_TEXEL)*TEXEL_SIZE)
    hp, ap = project(handle), project(tip)
    handle_angle = math.degrees(math.atan2(ap[1]-hp[1], ap[0]-hp[0]))
    assert hp[0] < ap[0] and hp[1] < ap[1]
    # Full unchanged-scene payload includes geometry, transforms, materials, lights,
    # camera, world, compositor values, render settings; only tool rotation excluded.
    invariant = {'objects':[], 'materials':[], 'camera_scale':scene.camera.data.ortho_scale,
                 'world':[.009,.014,.021,1.0], 'glare':[1.1,.25,.5], 'renderer':['CYCLES','CPU',64,1,1024,1024,20260915]}
    for obj in sorted(scene.objects, key=lambda o:o.name):
        row = {'name':obj.name,'type':obj.type,'matrix':None if obj == tool else [list(r) for r in obj.matrix_world]}
        if obj.type == 'MESH':
            row.update(vertices=[list(v.co) for v in obj.data.vertices], polygons=[list(p.vertices) for p in obj.data.polygons], uvs=[list(u.uv) for u in obj.data.uv_layers.active.data], materials=[m.name for m in obj.data.materials])
        elif obj.type == 'LIGHT':
            row.update(energy=obj.data.energy,size=obj.data.size,color=list(obj.data.color))
        elif obj.type == 'CURVE':
            row.update(points=[list(p.co) for p in obj.data.splines[0].points],radius=obj.data.bevel_depth,materials=[m.name for m in obj.data.materials])
        invariant['objects'].append(row)
    for mat in sorted(bpy.data.materials, key=lambda m:m.name):
        if not mat.use_nodes:
            continue
        row = {'name':mat.name,'nodes':[],'links':[(l.from_node.name,l.from_socket.name,l.to_node.name,l.to_socket.name) for l in mat.node_tree.links]}
        for node in mat.node_tree.nodes:
            values = {}
            for inp in node.inputs:
                if hasattr(inp,'default_value'):
                    value = inp.default_value
                    values[inp.name] = list(value) if hasattr(value,'__len__') and not isinstance(value,str) else value
            row['nodes'].append({'type':node.bl_idname,'values':values,'image':node.image.name if node.type == 'TEX_IMAGE' else None})
        invariant['materials'].append(row)
    save_json('diagnostics/'+scene['candidate']+'-projection.json', {'tool_polygons':tool_polys,'ore_polygons':block_polys,'anchor_pixel':ap,'handle_pixel':hp})
    return {'ore_blocks':len(blocks),'grid_cells':cells,'cluster_bounds':[[.008,.008,.008],[1.992,1.992,1.992]],
            'cluster_dimensions':[1.984]*3,'individual_block_dimensions':[.984]*3,'anchor_world':list(ANCHOR),
            'tip_local':list(tool['tip_local']),'tip_world':list(tip),'tip_anchor_distance_world':error,
            'tip_is_midpoint_of_real_mesh_edge':True,'tip_edge_vertex_errors':edge_vertex_errors,
            'minimum_pickaxe_world_z':lowest,'no_penetration_below_contact_plane':True,
            'rotation':{'yaw_about_world_Z_degrees':yaw,'pitch_about_world_Y_degrees':pitch,'order':'T(anchor) @ Rz(yaw) @ Ry(pitch)'},
            'handle_screen_angle_degrees_up_from_left':handle_angle,'anchor_pixel':ap,
            'pickaxe_opaque_texels':tool['opaque_texels'],'pickaxe_depth_texels':DEPTH_TEXELS,
            'pickaxe_depth_world':TEXEL_SIZE*DEPTH_TEXELS,'pickaxe_mesh_faces':len(tool.data.polygons),
            'selection_curves':curve_proof,'motion_swing_lightning_curves':0,'animated_objects':0,
            'composition_bounds_pixels':bounds,'nonrotation_invariant_sha256':digest(invariant),
            'pickaxe_mesh_sha256':digest({'v':[list(v.co) for v in tool.data.vertices],'p':[list(p.vertices) for p in tool.data.polygons]})}


def run(label):
    scene, tool, blocks, yaw, pitch = build(label)
    scene['candidate'] = label
    geometry = verify_geometry(scene, tool, blocks, yaw, pitch)
    scene.render.filepath = str(ROOT/(label+'.png'))
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.file.pack_all()
    images = [{'name':i.name,'packed':bool(i.packed_file),'sha256':hashlib.sha256(i.packed_file.data).hexdigest()} for i in bpy.data.images if i.source == 'FILE']
    assert len(images) == 2 and all(row['packed'] for row in images)
    assert scene.render.engine == 'CYCLES' and scene.cycles.device == 'CPU'
    assert scene.render.threads == 1 and set(os.sched_getaffinity(0)) == {2}
    assert not os.environ.get('DISPLAY') and not os.environ.get('WAYLAND_DISPLAY')
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/(label+'.blend')))
    info = {'label':label,'source':'../blender-s/03-cyan-selection.png','geometry':geometry,'packed_textures':images,
            'reproduction':{'entrypoint':label+'.py','implementation':'render_scenes.py','script_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),'local_only_not_git_committed':'Explicit no-commit / no-tracked-file-edit portfolio policy.'}}
    if '--build-only' in sys.argv:
        save_json(label+'-build.json', info)
        print('BUILD_COMPLETE', label, flush=True)
        return
    started = time.perf_counter()
    bpy.ops.render.render(write_still=True)
    duration = time.perf_counter()-started
    image = bpy.data.images.load(scene.render.filepath, check_existing=False)
    assert tuple(image.size) == (1024,1024)
    info['renderer'] = {'Blender':bpy.app.version_string,'engine':'Cycles','device':'CPU','samples':scene.cycles.samples,
                        'threads':scene.render.threads,'duration_seconds':duration,'resolution':[1024,1024],
                        'noaudio':True,'display_server':False,'GPU_allocated':False,
                        'affinity':sorted(os.sched_getaffinity(0)),'cgroup':Path('/proc/self/cgroup').read_text().strip(),
                        'resource_policy':'CPU2 affinity, one thread; shared render-blender.service CPUQuota 300%, CPUWeight20 (watchdog joins process).'}
    info['PNG'] = {'sha256':hashlib.sha256(Path(scene.render.filepath).read_bytes()).hexdigest(),'dimensions':list(image.size)}
    save_json(label+'-metadata.json', info)
    print('RENDER_COMPLETE',label,round(duration,3),flush=True)


if __name__ == '__main__':
    labels = sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [row[0] for row in CANDIDATES]
    for label in labels:
        if not label.startswith('--'):
            run(label)
