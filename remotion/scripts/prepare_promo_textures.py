"""Stage selected existing Minecraft/Mod textures for the promotional animation.

Reads local installed archives and this repository; never modifies game files.
The staging directory and its provenance manifest are generated, ignored assets.
"""
import argparse
import json
import struct
import subprocess
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
OUT = REPO / 'remotion/public/promo/textures'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', type=Path,
                        default=Path.home() / 'Library/Application Support/minecraft')
    parser.add_argument('--instance', default='DimensionWorks')
    args = parser.parse_args()
    mods = args.minecraft / 'versions' / args.instance / 'mods'
    OUT.mkdir(parents=True, exist_ok=True)
    provenance = []

    def stage(name, data, source, member=None):
        target = OUT / (name + '.png')
        target.write_bytes(data)
        width, height = struct.unpack('>II', data[16:24])
        # Vertical Minecraft animations: retain the first tile as a static skin.
        if height > width and height % width == 0:
            cropped = OUT / (name + '.first.png')
            subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y',
                            '-i', str(target), '-vf', f'crop={width}:{width}:0:0',
                            '-frames:v', '1', str(cropped)], check=True)
            cropped.replace(target)
        provenance.append({'file': target.name, 'source': source,
                           'archivePath': member, 'originalSize': [width, height]})

    def archive(pattern, namespace, entries):
        files = sorted(mods.glob(pattern))
        if len(files) != 1:
            raise RuntimeError(f'Expected one {pattern}, found {files}')
        with zipfile.ZipFile(files[0]) as jar:
            for name, resource in entries.items():
                member = f'assets/{namespace}/textures/{resource}.png'
                stage(name, jar.read(member), files[0].name, member)

    archive('create-1.20.1-*.jar', 'create', {
        'press_front': 'block/mechanical_press_side',
        'press_top': 'block/mechanical_press_top',
        'deployer': 'block/deployer',
        'basin': 'block/basin',
        'millstone': 'block/millstone',
        'crushing_wheel': 'block/crushing_wheel_insert',
        'copper_sheet': 'item/copper_sheet',
        'cinder_flour': 'item/cinder_flour',
        'packager': 'block/packager_horizontal_unpowered',
    })
    archive('Mekanism-1.20.1-*.jar', 'mekanism', {
        'factory_front': 'block/factory/enriching/enriching_factory_front',
        'factory_top': 'block/factory/enriching/enriching_factory_top',
        'combiner_front': 'block/combiner/front',
        'combiner_top': 'block/combiner/top',
        'purifier_front': 'block/purification_chamber/front',
        'dust_iron': 'item/dust_iron',
        'dust_copper': 'item/dust_copper',
        'dust_sulfur': 'item/dust_sulfur',
        'alloy_infused': 'item/alloy_infused',
        'ingot_tin': 'item/ingot_tin',
        'ingot_lead': 'item/ingot_lead',
        'ingot_osmium': 'item/ingot_osmium',
        'fluorite': 'item/fluorite_gem',
    })
    archive('appliedenergistics2-forge-*.jar', 'ae2', {
        'me_controller': 'block/controller',
        'vibration_front': 'block/vibration_chamber_front',
        'engineering_processor': 'item/engineering_processor',
    })
    archive('twilightforest-1.20.1-4.3.2508*.jar', 'twilightforest', {
        'twilight_oak_log': 'block/twilight_oak_log',
        'naga_trophy': 'item/naga_trophy',
    })
    vanilla_files = sorted((args.minecraft / 'libraries/net/minecraft/client').glob(
        '1.20.1-*/*-extra.jar'))
    if len(vanilla_files) != 1:
        raise RuntimeError(f'Expected one 1.20.1 client resources archive: {vanilla_files}')
    with zipfile.ZipFile(vanilla_files[0]) as jar:
        for name in ['raw_iron', 'iron_ingot', 'copper_ingot', 'gold_ingot',
                     'redstone', 'quartz', 'lapis_lazuli', 'amethyst_shard',
                     'netherite_scrap', 'charcoal', 'bucket', 'honey_bottle']:
            member = f'assets/minecraft/textures/item/{name}.png'
            stage(name, jar.read(member), vanilla_files[0].name, member)
        for name in ['netherrack', 'oak_leaves', 'cobblestone']:
            member = f'assets/minecraft/textures/block/{name}.png'
            stage(name, jar.read(member), vanilla_files[0].name, member)
    own = {
        'gear_heart_repair_fragment_7': 'kubejs/assets/kubejs/textures/item/gear_heart_repair_fragment_7.png',
        'bounce_tape': 'kubejs/assets/kubejs/textures/item/bounce_tape.png',
        'elastic_compound': 'kubejs/assets/kubejs/textures/item/elastic_compound.png',
        'dimension_key': 'kubejs/assets/kubejs/textures/item/dimension_key.png',
        'steeleaf_seal_strip': 'kubejs/assets/kubejs/textures/item/steeleaf_seal_strip.png',
    }
    for mod, namespace, names, category in [
        ('pressure-crystals', 'pressure_crystals', ['low_pressure_crystal', 'medium_pressure_crystal', 'high_pressure_crystal'], 'item'),
        ('transfer', 'transfer', ['twilight_dispatch_upgrade'], 'item'),
        ('mek-stress', 'mek_stress', ['matter_ball'], 'item'),
    ]:
        for name in names:
            own[name] = f'mods/dimensionworks-{mod}/src/main/resources/assets/dimensionworks_{namespace}/textures/{category}/{name}.png'
    for name, relative in own.items():
        stage(name, (REPO / relative).read_bytes(), relative)
    (OUT / 'sources.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + '\n')
    print(f'Staged {len(provenance)} textures into {OUT}')


if __name__ == '__main__':
    main()
