#!/usr/bin/env python3
"""Bake the film's actual Minecraft assets into small, texture-grouped meshes.

Repository resources take precedence over installed releases of our own mods.
Third-party JSON/OBJ/Bedrock assets are extracted locally, never checked in.
Unsupported or missing models fail the preparation instead of becoming cubes.
"""
import argparse
import hashlib
import io
import json
import math
import re
import urllib.request
import zipfile
from collections import OrderedDict
from pathlib import Path, PurePosixPath

from PIL import Image

REPO = Path(__file__).resolve().parents[2]
OUT = REPO / 'remotion/public/promo/3d'
SOURCES = REPO / 'remotion/src/promo/model-sources.json'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def resource_path(resource, category='models', extension='.json'):
    namespace, name = resource.split(':', 1) if ':' in resource else ('minecraft', resource)
    return f'assets/{namespace}/{category}/{name}{extension}'


def rotate(point, axis, radians, origin=(0, 0, 0)):
    values = [point[i] - origin[i] for i in range(3)]
    a, b = {'x': (1, 2), 'y': (2, 0), 'z': (0, 1)}[axis]
    c, s = math.cos(radians), math.sin(radians)
    values[a], values[b] = values[a] * c - values[b] * s, values[a] * s + values[b] * c
    return [values[i] + origin[i] for i in range(3)]


def box_faces(lo, hi):
    x, y, z = lo
    X, Y, Z = hi
    # Each face starts at its upper-left UV corner, viewed from outside.
    return {
        'north': [[X, Y, z], [X, y, z], [x, y, z], [x, Y, z]],
        'south': [[x, Y, Z], [x, y, Z], [X, y, Z], [X, Y, Z]],
        'east': [[X, Y, Z], [X, y, Z], [X, y, z], [X, Y, z]],
        'west': [[x, Y, z], [x, y, z], [x, y, Z], [x, Y, Z]],
        'up': [[x, Y, z], [x, Y, Z], [X, Y, Z], [X, Y, z]],
        'down': [[x, y, Z], [x, y, z], [X, y, z], [X, y, Z]],
    }


def default_uv(side, lo, hi):
    x, y, z = lo
    X, Y, Z = hi
    return {
        'north': [16 - X, 16 - Y, 16 - x, 16 - y],
        'south': [x, 16 - Y, X, 16 - y],
        'east': [16 - Z, 16 - Y, 16 - z, 16 - y],
        'west': [z, 16 - Y, Z, 16 - y],
        'up': [x, z, X, Z], 'down': [x, 16 - Z, X, 16 - z],
    }[side]


def skin_uv(u, v, w, h, d, width, height):
    rects = {
        'north': [u + d, v + d, u + d + w, v + d + h],
        'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
        'east': [u + d + w, v + d, u + 2 * d + w, v + d + h],
        'west': [u, v + d, u + d, v + d + h],
        'up': [u + d, v, u + d + w, v + d],
        'down': [u + d + w, v, u + d + 2 * w, v + d],
    }
    return {side: [a / (width if i % 2 == 0 else height) * 16 for i, a in enumerate(rect)]
            for side, rect in rects.items()}


class Resources:
    def __init__(self, minecraft, instance):
        self.archives = []
        self.provenance = OrderedDict()
        self.textures = {}
        self.model_cache = {}
        self.roots = [REPO / 'kubejs'] + sorted((REPO / 'mods').glob('*/src/main/resources'))
        mods = minecraft / 'versions' / instance / 'mods'
        patterns = [
            'create-1.20.1-6.0.8.jar', 'Mekanism-1.20.1-10.4.16.80.jar',
            'MekanismGenerators-1.20.1-10.4.16.80.jar', 'appliedenergistics2-forge-15.4.10.jar',
            'appliedcreate-1.20.1-1.1.5.jar', 'vintageimprovements-1.20.1-0.3.7.9.jar',
            'createdieselgenerators-1.20.1-1.3.12.jar', 'twilightforest-1.20.1-4.3.2508-universal.jar',
            'Draconic-Evolution-1.20.1-3.1.2.621-universal.jar',
            'touhoulittlemaid-1.5.3-forge+mc1.20.1.jar',
            'create_mobile_packages-1.20.1-0.6.1-dimensionworks.15.jar',
            'kaleidoscope_chinesefood-1.1.16-1.20.1-all.jar',
            'alexscaves-2.0.2.jar',
            'ae2omnicells-1.20.1-forge-1.1.6.jar',
        ]
        for filename in patterns:
            path = mods / filename
            self.archives.append((path.name, zipfile.ZipFile(path)))
        vanilla = list((minecraft / 'libraries/net/minecraft/client').glob('1.20.1-*/*extra.jar'))
        if len(vanilla) != 1:
            raise RuntimeError(f'Expected exactly one vanilla resource archive: {vanilla}')
        self.archives.append((vanilla[0].name, zipfile.ZipFile(vanilla[0])))

    def read(self, path, archive=None):
        if archive is None:
            for root in self.roots:
                candidate = root / path
                if candidate.is_file():
                    data = candidate.read_bytes()
                    self.record(path, data, str(candidate.relative_to(REPO)))
                    return data
        for name, jar in self.archives:
            if archive is not None and name != archive:
                continue
            try:
                data = jar.read(path)
            except KeyError:
                continue
            self.record(path, data, name)
            return data
        raise FileNotFoundError(path)

    def record(self, path, data, source):
        key = (source, path)
        if key not in self.provenance:
            target = OUT / 'raw-assets' / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            self.provenance[key] = {'resource': path, 'source': source,
                                    'sha256': hashlib.sha256(data).hexdigest()}

    def texture(self, resource, archive=None, raw_path=False):
        path = resource if raw_path else resource_path(resource, 'textures', '.png')
        if path in self.textures:
            return path
        data = self.read(path, archive)
        image = Image.open(io.BytesIO(data)).convert('RGBA')
        target = OUT / 'textures' / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        metadata = {'file': f'promo/3d/textures/{path}', 'width': image.width, 'height': image.height,
                    'cutout': image.getextrema()[3][0] < 255, 'frames': [], 'columns': 1, 'rows': 1}
        try:
            animation = json.loads(self.read(path + '.mcmeta', archive)).get('animation')
        except FileNotFoundError:
            animation = None
        if animation is not None:
            tw = animation.get('width', min(image.size))
            th = animation.get('height', tw)
            columns, rows = image.width // tw, image.height // th
            frametime = animation.get('frametime', 1)
            frames = animation.get('frames', list(range(columns * rows)))
            metadata.update(columns=columns, rows=rows,
                            frames=[{'index': f if isinstance(f, int) else f['index'],
                                     'ticks': frametime if isinstance(f, int) else f.get('time', frametime)} for f in frames])
        self.textures[path] = metadata
        return path

    def model(self, resource):
        if resource in self.model_cache:
            return self.model_cache[resource]
        if resource in ('minecraft:builtin/generated', 'builtin/generated'):
            return {'generated': True}
        node = json.loads(self.read(resource_path(resource)))
        parent = self.model(node['parent']) if 'parent' in node else {}
        merged = {**parent, **node, 'textures': {**parent.get('textures', {}), **node.get('textures', {})}}
        self.model_cache[resource] = merged
        return merged


class Mesh:
    def __init__(self, resources):
        self.resources = resources
        self.groups = OrderedDict()

    def triangle(self, positions, uv, texture, emissive=False, tint='#ffffff'):
        key = (texture, emissive, tint)
        group = self.groups.setdefault(key, {'texture': texture, 'emissive': emissive, 'tint': tint,
                                             'positions': [], 'uv': []})
        group['positions'].extend(round(value, 6) for point in positions for value in point)
        group['uv'].extend(round(value, 6) for point in uv for value in point)

    def quad(self, positions, rect, texture, rotation=0, emissive=False, tint='#ffffff'):
        u, v, U, V = rect
        coords = [[u / 16, 1 - v / 16], [u / 16, 1 - V / 16],
                  [U / 16, 1 - V / 16], [U / 16, 1 - v / 16]]
        turns = rotation // 90 % 4
        coords = coords[turns:] + coords[:turns]
        for indices in ((0, 1, 2), (0, 2, 3)):
            self.triangle([positions[i] for i in indices], [coords[i] for i in indices], texture, emissive, tint)

    def bake(self):
        if not self.groups:
            raise RuntimeError('Model has no renderable geometry')
        return {'groups': list(self.groups.values())}


def texture_reference(reference, textures):
    visited = set()
    while reference.startswith('#'):
        if reference in visited:
            raise ValueError(f'Cyclic texture reference: {reference}')
        visited.add(reference)
        reference = textures[reference[1:]]
    return reference


def bake_json(mesh, node):
    textures = node.get('textures', {})
    if node.get('loader') == 'forge:composite':
        for child in node['children'].values():
            resolved = mesh.resources.model(child['parent']) if 'parent' in child else child
            bake_json(mesh, {**resolved, **child,
                             'textures': {**textures, **resolved.get('textures', {}), **child.get('textures', {})}})
        return
    if node.get('loader') == 'forge:obj':
        bake_obj(mesh, node['model'], textures=textures, block_origin=True, flip_v=node.get('flip_v', False))
        return
    if 'loader' in node:
        raise ValueError(f'Unsupported custom model loader: {node["loader"]}')
    if not node.get('elements') and node.get('generated'):
        bake_item(mesh, texture_reference(textures['layer0'], textures))
        return
    for element in node.get('elements', []):
        lo, hi = element['from'], element['to']
        for side, face in element['faces'].items():
            vertices = box_faces(lo, hi)[side]
            if 'rotation' in element:
                rot = element['rotation']
                if rot.get('rescale'):
                    factor = 1 / math.cos(math.radians(rot['angle']))
                    axis = 'xyz'.index(rot['axis'])
                    vertices = [[point[i] if i == axis else rot['origin'][i] + (point[i] - rot['origin'][i]) * factor
                                 for i in range(3)] for point in vertices]
                vertices = [rotate(point, rot['axis'], math.radians(rot['angle']), rot['origin']) for point in vertices]
            vertices = [[(p[0] - 8) / 16, p[1] / 16, (p[2] - 8) / 16] for p in vertices]
            ref = texture_reference(face['texture'], textures)
            texture = mesh.resources.texture(ref)
            emissive = face.get('forge_data', {}).get('block_light', 0) > 0 or element.get('forge_data', {}).get('block_light', 0) > 0
            tint = '#789f50' if face.get('tintindex', -1) >= 0 else '#ffffff'
            mesh.quad(vertices, face.get('uv', default_uv(side, lo, hi)), texture,
                      face.get('rotation', 0), emissive, tint)


def bake_obj(mesh, resource, texture=None, textures=None, block_origin=False, flip_v=False):
    path = resource_path(resource, '', '').replace('//', '/')
    lines = mesh.resources.read(path).decode().splitlines()
    material_textures = {}
    if texture:
        default_texture = mesh.resources.texture(texture)
    else:
        default_texture = None
    for line in lines:
        if line.startswith('mtllib ') and not texture:
            material_path = str(PurePosixPath(path).parent / line.split(maxsplit=1)[1])
            material_name = None
            for material_line in mesh.resources.read(material_path).decode().splitlines():
                fields = material_line.split()
                if fields and fields[0] == 'newmtl':
                    material_name = fields[1]
                elif fields and fields[0] == 'map_Kd' and not texture:
                    ref = texture_reference(fields[1], textures or {})
                    material_textures[material_name] = mesh.resources.texture(ref)
    vertices, coords = [], []
    current = None
    for line in lines:
        fields = line.split()
        if not fields:
            continue
        if fields[0] == 'v':
            point = list(map(float, fields[1:4]))
            if block_origin:
                point[0] -= .5
                point[2] -= .5
            vertices.append(point)
        elif fields[0] == 'vt':
            u, v = map(float, fields[1:3])
            coords.append([u, 1 - v if flip_v else v])
        elif fields[0] == 'usemtl':
            current = fields[1]
        elif fields[0] == 'f':
            entries = [p.split('/') for p in fields[1:]]
            tex = default_texture or material_textures.get(current)
            # Some upstream OBJ files contain explicitly untextured helper faces.
            if tex is None:
                if current == 'none':
                    continue
                raise ValueError(f'No texture for {path}: {current}')
            for i in range(1, len(entries) - 1):
                corners = [entries[j] for j in (0, i, i + 1)]
                mesh.triangle([vertices[int(c[0]) - 1] for c in corners],
                              [coords[int(c[1]) - 1] if len(c) > 1 and c[1] else [0, 0] for c in corners], tex)


def bake_item(mesh, resource):
    texture = mesh.resources.texture(resource)
    image = Image.open(OUT / 'textures' / texture).convert('RGBA')
    width, height = image.size
    pixels = image.load()
    depth = 1 / 16
    mesh.quad([[-.5, 1, -depth / 2], [-.5, 0, -depth / 2], [.5, 0, -depth / 2], [.5, 1, -depth / 2]],
              [0, 0, 16, 16], texture)
    mesh.quad([[.5, 1, depth / 2], [.5, 0, depth / 2], [-.5, 0, depth / 2], [-.5, 1, depth / 2]],
              [0, 0, 16, 16], texture)
    for y in range(height):
        for x in range(width):
            if pixels[x, y][3] < 128:
                continue
            lo = [x / width - .5, 1 - (y + 1) / height, -depth / 2]
            hi = [(x + 1) / width - .5, 1 - y / height, depth / 2]
            edges = [('west', x - 1, y), ('east', x + 1, y), ('up', x, y - 1), ('down', x, y + 1)]
            for side, nx, ny in edges:
                if not (0 <= nx < width and 0 <= ny < height) or pixels[nx, ny][3] < 128:
                    u, v = (x + .5) / width * 16, (y + .5) / height * 16
                    mesh.quad(box_faces(lo, hi)[side], [u, v, u, v], texture)


def bake_bedrock(mesh, entry):
    node = json.loads(mesh.resources.read(entry['model'], entry['archive']))
    geometry = node.get('geometry.model')
    if geometry is None:
        geometry = node['minecraft:geometry'][0]
    bones = {bone['name']: bone for bone in geometry['bones']}
    description = geometry.get('description', geometry)
    width = description.get('texturewidth', description.get('texture_width'))
    height = description.get('textureheight', description.get('texture_height'))
    tex = mesh.resources.texture(entry['texture'], entry['archive'], raw_path=True)
    for bone in bones.values():
        # Blink/alternate-expression overlays are optional render layers, not the resting model.
        if bone['name'] in ('blink', 'eyes_blink', 'sleep'):
            continue
        for cube in bone.get('cubes', []):
            inflate = cube.get('inflate', 0)
            lo = [v - inflate for v in cube['origin']]
            hi = [cube['origin'][i] + cube['size'][i] + inflate for i in range(3)]
            uv = skin_uv(*cube['uv'], *cube['size'], width, height) if isinstance(cube['uv'], list) else cube['uv']
            for side, points in box_faces(lo, hi).items():
                if isinstance(cube['uv'], dict):
                    if side not in uv:
                        continue
                    face = uv[side]
                    u, v = face['uv']; w, h = face['uv_size']
                    rect = [u / width * 16, v / height * 16, (u + w) / width * 16, (v + h) / height * 16]
                else:
                    rect = uv[side]
                if cube.get('mirror', bone.get('mirror', False)):
                    rect = [rect[2], rect[1], rect[0], rect[3]]
                transforms = []
                if cube.get('rotation'):
                    transforms.append((cube['pivot'], cube['rotation']))
                ancestor = bone
                while ancestor:
                    transforms.append((ancestor.get('pivot', [0, 0, 0]), ancestor.get('rotation', [0, 0, 0])))
                    ancestor = bones.get(ancestor.get('parent'))
                for pivot, angles in transforms:
                    for axis, angle in zip('xyz', angles):
                        points = [rotate(p, axis, math.radians(angle), pivot) for p in points]
                mesh.quad([[p[0] / 16, p[1] / 16, p[2] / 16] for p in points], rect, tex)


def bake_wind(resources, entry):
    cache = OUT / 'raw-assets/ModelWindGenerator.java'
    if cache.exists():
        data = cache.read_bytes()
    else:
        request = urllib.request.Request(entry['url'], headers={'User-Agent': 'DimensionWorks-video-assets'})
        with urllib.request.urlopen(request, timeout=30) as response:
            data = response.read()
        cache.parent.mkdir(parents=True, exist_ok=True)
        cache.write_bytes(data)
    resources.provenance[('wind-java', entry['url'])] = {'resource': entry['url'], 'source': 'Mekanism 10.4.16.80 model source',
                                                      'sha256': hashlib.sha256(data).hexdigest()}
    text = data.decode()
    matches = re.findall(r'new ModelPartData\("([^"]+)", CubeListBuilder.create\(\)(.*?)PartPose\.(offset(?:AndRotation)?)\((.*?)\)\);', text, re.S)
    if len(matches) != 21:
        raise ValueError(f'Unexpected wind generator source geometry: {len(matches)} parts')
    texture = resources.texture(entry['texture'], raw_path=entry.get('texture_raw', False))
    meshes = {'wind_base': Mesh(resources), 'wind_blades': Mesh(resources)}
    for name, builder, _, pose in matches:
        values = lambda s: [float(x.strip().rstrip('F')) for x in s.split(',')]
        box = values(re.search(r'\.addBox\((.*?)\)', builder).group(1))
        offs = re.search(r'\.texOffs\((.*?)\)', builder)
        u, v = values(offs.group(1)) if offs else [0, 0]
        transform = values(pose)
        angles = transform[3:] if len(transform) == 6 else [0, 0, 0]
        lo = box[:3]; hi = [lo[i] + box[i + 3] for i in range(3)]
        rects = skin_uv(u, v, *box[3:], 128, 128)
        blade = name.startswith('blade')
        for side, points in box_faces(lo, hi).items():
            for axis, angle in zip('xyz', angles):
                points = [rotate(p, axis, angle) for p in points]
            points = [[(p[0] + transform[0]) / 16, (24 - p[1] - transform[1]) / 16,
                       (p[2] + transform[2]) / 16] for p in points]
            if blade:
                points = [[p[0], p[1] - 4.5, p[2] + .375] for p in points]
            meshes['wind_blades' if blade else 'wind_base'].quad(points, rects[side], texture)
    return {name: mesh.bake() for name, mesh in meshes.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', type=Path, default=Path.home() / 'Library/Application Support/minecraft')
    parser.add_argument('--instance', default='DimensionWorks')
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    resources = Resources(args.minecraft, args.instance)
    source = json.loads(SOURCES.read_text())
    models = dict(source['models'])
    families = source['families']
    for i in range(1, 6):
        models[f'ddr{i}'] = families['ddr'].format(generation=i)
        for kind in ('economy', 'balanced', 'high_speed', 'low_loss', 'defective', 'final'):
            models[f'card_{i}_{kind}'] = families['cards'].format(generation=i, type=kind)
    for i in range(1, 4):
        models[f'endpoint{i}'] = families['endpoints'].format(generation=i)
    for kind in ('basic', 'advanced', 'elite', 'ultimate'):
        models[f'factory_{kind}'] = families['factory_tiers'].format(type=kind)
    for kind in ('enriching', 'smelting', 'crushing', 'compressing', 'combining', 'purifying', 'injecting', 'infusing', 'sawing'):
        models[f'factory_{kind}'] = families['factory_processes'].format(type=kind)
    for kind in ('low', 'medium', 'high'):
        models[f'crystal_{kind}'] = families['crystals'].format(type=kind)
    baked = {}
    for name, resource in models.items():
        mesh = Mesh(resources)
        try:
            bake_json(mesh, resources.model(resource))
            baked[name] = mesh.bake()
        except Exception as error:
            raise RuntimeError(f'Cannot bake {name} ({resource}): {error}') from error
    for name, entry in source['obj'].items():
        mesh = Mesh(resources)
        bake_obj(mesh, entry['resource'], texture=entry['texture'])
        baked[name] = mesh.bake()
    for name, texture in source['item_textures'].items():
        mesh = Mesh(resources)
        bake_item(mesh, texture)
        baked[name] = mesh.bake()
    for i in range(1, 8):
        mesh = Mesh(resources)
        bake_item(mesh, families['fragments'].format(generation=i))
        baked[f'fragment{i}'] = mesh.bake()
    for name, entry in source['bedrock'].items():
        mesh = Mesh(resources)
        bake_bedrock(mesh, entry)
        baked[name] = mesh.bake()
    baked.update(bake_wind(resources, source['java']['wind']))
    skin = resources.texture('minecraft:entity/player/wide/steve')
    for name, size, origin in (
        ('head', (8, 8, 8), (0, 0)), ('body', (8, 12, 4), (16, 16)),
        ('right_arm', (4, 12, 4), (40, 16)), ('left_arm', (4, 12, 4), (32, 48)),
        ('right_leg', (4, 12, 4), (0, 16)), ('left_leg', (4, 12, 4), (16, 48)),
    ):
        mesh = Mesh(resources)
        w, h, d = size
        rects = skin_uv(*origin, w, h, d, 64, 64)
        for side, points in box_faces((-w / 2, -h, -d / 2), (w / 2, 0, d / 2)).items():
            mesh.quad([[p / 16 for p in point] for point in points], rects[side], skin)
        baked[f'player_{name}'] = mesh.bake()
    grain = Image.effect_noise((256, 256), 32).convert('RGBA')
    grain.putalpha(45)
    grain.save(OUT / 'grain.png')
    provenance = list(resources.provenance.values())
    (OUT / 'models.json').write_text(json.dumps({'models': baked, 'textures': resources.textures}, separators=(',', ':')) + '\n')
    (OUT / 'sources.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + '\n')
    triangles = sum(len(g['positions']) // 9 for model in baked.values() for g in model['groups'])
    print(f'Baked {len(baked)} actual models, {len(resources.textures)} textures, {triangles:,} triangles; {len(provenance)} traced source resources.')


if __name__ == '__main__':
    main()
