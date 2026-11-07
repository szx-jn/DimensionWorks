#!/usr/bin/env python3
"""Static checks for the first Twilight source/cost graph and owned resource references."""
from pathlib import Path
import json
import re
import struct

ROOT = Path(__file__).resolve().parents[1]
MOD = ROOT / 'mods/dimensionworks-cave-factory'
ASSET = MOD / 'src/main/resources/assets/dimensionworks_cave_factory'
DATA = ROOT / 'kubejs/data/dimensionworks_cave_factory'
NS = 'dimensionworks_cave_factory:'


def main():
    java = (MOD / 'src/main/java/dev/szx/dimensionworks/cavefactory/twilight/TwilightIndustry.java').read_text()
    items = set(re.findall(r'simpleItem\("([^"]+)"\)', java)) | {'forest_leacher', 'boar_conditioner'}
    recipes = {p.stem: json.loads(p.read_text()) for p in (DATA / 'recipes/twilight').glob('*.json')}
    assert len(recipes) == 11, 'Unexpected first-stage recipe set'
    leaching, conditioning = recipes['leaching'], recipes['conditioning']
    assert leaching['station'] == 'leaching' and conditioning['station'] == 'conditioning'
    assert leaching['first']['count'] > 0 and leaching['second']['count'] > 0 and leaching['water'] > 0
    assert leaching['fluid_result'] == {'fluid': 'dimensionworks:forest_extract', 'amount': 1000}
    assert conditioning['first']['count'] > 0 and conditioning['second']['count'] > 0
    assert conditioning['result'] == {'item': NS + 'mature_humus', 'count': 4}
    basic, filtered = recipes['basic_separation'], recipes['filtered_separation']
    assert basic['ingredients'] == [{'fluid': 'dimensionworks:forest_extract', 'amount': 1000}]
    assert filtered['ingredients'] == basic['ingredients'] + [{'item': NS + 'forest_filter'}]
    assert basic['heatRequirement'] == filtered['heatRequirement'] == 'heated'
    assert basic['results'][1] == filtered['results'][1] == {'item': NS + 'root_fiber', 'count': 2}
    assert filtered['results'][0]['count'] > basic['results'][0]['count']
    assert recipes['forest_filter']['results'] == [{'item': NS + 'forest_filter', 'count': 2}]
    assert not any(ingredient.get('item') in [NS + 'forest_filter', NS + 'ironwood_frame']
                   for ingredient in recipes['humus_by_hand']['ingredients'])
    # A usable independent branch, with no new discount recipes for existing industrial outputs.
    allowed_outputs = items | {'rooted_dirt'}
    for name, recipe in recipes.items():
        outputs = recipe.get('results', [recipe['result']] if 'result' in recipe else [])
        for out in outputs:
            assert out.get('item', '').split(':')[-1] in allowed_outputs, (name, out)

    def check_ids(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key == 'item' and isinstance(child, str) and child.startswith(NS):
                    assert child[len(NS):] in items, f'Unknown owned item: {child}'
                check_ids(child)
        elif isinstance(value, list):
            for child in value: check_ids(child)

    for recipe in recipes.values(): check_ids(recipe)
    for name in items:
        model = ASSET / 'models/item' / f'{name}.json'
        assert model.exists(), f'Missing item model: {name}'
    for folder in ['models/block', 'models/item']:
        for path in (ASSET / folder).glob('*.json'):
            if path.stem not in items: continue
            obj = json.loads(path.read_text())
            for texture in obj.get('textures', {}).values():
                if texture.startswith(NS): assert (ASSET / 'textures' / (texture[len(NS):] + '.png')).exists(), texture
    for name in ['forest_leacher', 'boar_conditioner']:
        assert (ASSET / 'blockstates' / f'{name}.json').exists()
        assert (MOD / 'src/main/resources/data/dimensionworks_cave_factory/loot_tables/blocks' / f'{name}.json').exists()
    langs = [json.loads((ASSET / 'lang' / f'{lang}.json').read_text()) for lang in ['zh_cn', 'en_us']]
    for name in items:
        key = ('block.' if name in ['forest_leacher', 'boar_conditioner'] else 'item.') + NS.replace(':', '.') + name
        assert all(key in lang for lang in langs), f'Missing translation: {key}'
    ui_keys = {key for key in langs[0] if '.twilight.' in key}
    assert all(key in langs[1] for key in ui_keys), 'English UI translation missing'
    cf_items = (MOD / 'src/main/java/dev/szx/dimensionworks/cavefactory/registry/CFItems.java').read_text()
    assert 'startsWith("dimensionworks_")' in cf_items and '"main"' in cf_items, 'Shared creative tab rule missing'
    for path in (ASSET / 'textures').rglob('*.png'):
        if path.stem not in items | {'twilight_casing', 'twilight_shaft', 'forest_leacher_top',
                                   'boar_conditioner_top', 'forest_extract_still', 'forest_extract_flow'}: continue
        header = path.read_bytes()
        assert header[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', header[16:24]) == (16, 16), path
    print('Twilight static checks passed: 11 recipes, source costs, branch outputs, assets and translations.')


if __name__ == '__main__':
    main()
