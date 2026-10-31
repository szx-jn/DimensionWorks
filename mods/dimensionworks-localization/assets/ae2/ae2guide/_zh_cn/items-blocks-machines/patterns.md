---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 自动合成样板
  icon: crafting_pattern
  position: 410
categories:
- tools
item_ids:
- ae2:blank_pattern
- ae2:crafting_pattern
- ae2:processing_pattern
- ae2:smithing_table_pattern
- ae2:stonecutting_pattern
---

# 自动合成样板

<ItemImage id="crafting_pattern" scale="4" />

使用<ItemLink id="pattern_encoding_terminal" />将配方写入空白样板，之后放进<ItemLink id="pattern_provider" />或<ItemLink id="molecular_assembler" />，让 ME 网络按配方自动生产。

根据用途，样板分为以下几种：

* **<ItemLink id="crafting_pattern" />（合成样板）**：记录工作台配方。可放入分子装配室单独使用，也可交给相邻样板供应器，由它负责提供原料并接收产物。

* **<ItemLink id="smithing_table_pattern" />（锻造样板）**：记录锻造台的配方。与合成样板类似，需要相应的自动合成设备执行。

* **<ItemLink id="stonecutting_pattern" />（切石样板）**：记录切石机的配方，可以让自动合成网络完成切石加工。

* **<ItemLink id="processing_pattern" />（处理样板）**：用于非工作台配方，如熔炉、机械动力、Mekanism 机器等。你只需设定**投入哪些原料，以及最终应该返回哪些产物**。加工过程可以由多台机器完成，ME 网络只负责投料和确认产物回收。

处理样板允许非常复杂的生产线。比如，你可以设置一次投入 8 个圆石、预期产出 8 个石头；样板供应器会按这一批次输出材料，而非逐个发送。多个拥有相同样板的<ItemLink id="pattern_provider" />还能并行生产。

**注意：**处理样板中的产出数量必须与实际产线一致，否则网络可能一直等待不存在的产物。

## 合成配方

<RecipeFor id="blank_pattern" />
