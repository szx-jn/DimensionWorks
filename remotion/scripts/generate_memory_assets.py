"""复制仓库原创 DDR 贴图供视频使用，不读取或复制第三方 Mod 资源。"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'mods/dimensionworks-mek-stress/src/main/resources/assets/dimensionworks_mek_stress/textures'
DESTINATION = ROOT / 'public/memory'
TYPES = ('economy', 'balanced', 'high_speed', 'high_storage', 'defective', 'final')


def generate():
    paths = []
    for tier in range(1, 6):
        paths.append((SOURCE / f'block/memory_drive_ddr{tier}_front.png', DESTINATION / f'drive_ddr{tier}_front.png'))
        for kind in TYPES:
            paths.append((SOURCE / f'item/memory_card_ddr{tier}_{kind}.png', DESTINATION / f'card_ddr{tier}_{kind}.png'))
    missing = [str(source) for source, _ in paths if not source.is_file()]
    if missing:
        raise SystemExit('缺少仓库原创贴图：' + ', '.join(missing))
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for source, destination in paths:
        shutil.copyfile(source, destination)
    print('已同步 5 代驱动器与 30 张内存卡原创贴图。')


if __name__ == '__main__':
    generate()
