"""Visual asset regressions. Run with the Pillow asset-generator environment."""
import importlib.util
import json
from pathlib import Path
import unittest

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'mods/dimensionworks-mek-stress/src/main/resources/assets/dimensionworks_mek_stress/textures'


class MemoryAssetsTest(unittest.TestCase):
    def test_drive_models_cover_full_cube_with_opaque_face_textures(self):
        for level in range(1, 6):
            model_path = ASSETS.parent / f'models/block/memory_drive_ddr{level}.json'
            model = json.loads(model_path.read_text())
            self.assertEqual(1, len(model['elements']))
            element = model['elements'][0]
            self.assertEqual([0, 0, 0], element['from'])
            self.assertEqual([16, 16, 16], element['to'])
            self.assertEqual({'north', 'south', 'east', 'west', 'up', 'down'}, set(element['faces']))
            for face in element['faces'].values():
                texture = model['textures'][face['texture'].removeprefix('#')]
                namespace, relative = texture.split(':')
                self.assertEqual('dimensionworks_mek_stress', namespace)
                with self.subTest(level=level, texture=relative), Image.open(ASSETS / f'{relative}.png') as image:
                    self.assertEqual((16, 16), image.size)
                    self.assertEqual((255, 255), image.convert('RGBA').getchannel('A').getextrema())

    def test_card_padding_is_transparent_for_all_thirty_variants(self):
        cards = list((ASSETS / 'item').glob('memory_card_ddr[1-5]_*.png'))
        self.assertEqual(30, len(cards))
        for path in cards:
            with self.subTest(card=path.name), Image.open(path) as card:
                self.assertEqual((64, 64), card.size)
                alpha = card.getchannel('A')
                for box in ((0, 0, 9, 64), (55, 0, 64, 64), (0, 0, 64, 1), (0, 63, 64, 64)):
                    self.assertEqual((0, 0), alpha.crop(box).getextrema())
                self.assertEqual(255, alpha.getpixel((32, 32)))

    def test_keying_preserves_enclosed_silver_highlights(self):
        spec = importlib.util.spec_from_file_location('draw_memory_assets', ROOT / 'scripts/draw_memory_assets.py')
        generator = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(generator)
        reference = Image.new('RGBA', (220, 800), (220, 220, 220, 255))
        draw = ImageDraw.Draw(reference)
        draw.rectangle((30, 515, 190, 775), fill=(20, 30, 45, 255))
        draw.rectangle((40, 525, 180, 765), fill=(240, 240, 240, 255))
        card = generator.extract_card(reference, 0)
        self.assertEqual(255, card.getpixel((32, 32))[3])
        self.assertEqual((0, 0, 0, 0), card.getpixel((0, 32)))

    def test_gui_slot_pixels_match_item_positions_and_remain_inside_frame(self):
        with Image.open(ASSETS / 'gui/memory_drive.png') as gui:
            self.assertEqual((176, 215), gui.size)
            positions = [(8 + col * 18, 26 + row * 18) for row in range(5) for col in range(4)]
            positions += [(8 + col * 18, 133 + row * 18) for row in range(3) for col in range(9)]
            positions += [(8 + col * 18, 193) for col in range(9)]
            for x, y in positions:
                self.assertEqual((139, 139, 139, 255), gui.getpixel((x, y)))
                self.assertEqual((139, 139, 139, 255), gui.getpixel((x + 15, y + 15)))
                self.assertEqual((255, 255, 255, 255), gui.getpixel((x + 16, y + 16)))
                self.assertLess(x + 16, gui.width - 4)
                self.assertLess(y + 16, gui.height - 4)


if __name__ == '__main__':
    unittest.main()
