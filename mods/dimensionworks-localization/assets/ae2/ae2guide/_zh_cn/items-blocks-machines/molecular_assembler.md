---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 分子装配室
  icon: molecular_assembler
  position: 310
categories:
- machines
item_ids:
- ae2:molecular_assembler
---

# 分子装配室

<BlockImage id="molecular_assembler" scale="8" />

分子装配室可以根据配方将输入材料加工为物品。它既能独立使用，也能与<ItemLink id="pattern_provider" />配合进行自动合成。

例如，把<ItemLink id="crafting_pattern" />、<ItemLink id="smithing_table_pattern" />或<ItemLink id="stonecutting_pattern" />等样板用于对应加工方式，就能让系统按照样板处理材料。

在独立使用的示例中，可以将“1 个橡木原木 → 4 个橡木木板”的样板提供给装配室；原木从一侧输入，加工后的木板从另一侧输出。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/standalone_assembler.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 与样板供应器协同工作

分子装配室最常见的用途是与<ItemLink id="pattern_provider" />配合进行自动合成。

当样板供应器旁边连接了分子装配室时，供应器会将配方信息和原料直接交给装配室，装配室加工完成后把产物送回相邻的供应器或容器。这样就构成了一套基本的自动合成单元。

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/assembler_tower.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 升级

分子装配室支持[升级卡](upgrade_cards.md)：

* <ItemLink id="speed_card" />：提高加工速度。

## 合成配方

<RecipeFor id="molecular_assembler" />

## 兼容性提示

OptiFine 可能破坏分子装配室将产物推送到相邻容器的功能，因此不建议将其与 AE2 一起使用。
