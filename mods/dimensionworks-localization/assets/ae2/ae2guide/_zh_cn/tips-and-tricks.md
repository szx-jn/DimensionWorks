---
navigation:
  title: 实用技巧
  position: 20
---

# 实用技巧

下面是一些能让 AE2 网络更稳定、更容易维护的小建议：

* 不建议与 OptiFine 一起使用。
* 指南中的三维场景支持旋转、缩放以及显示或隐藏标注。
* 尽量将网络布置成树形结构，避免不必要的环路。
* 普通[设备](ae2-mechanics/devices.md)每组最多使用 8 个[频道](ae2-mechanics/channels.md)，设计网络时要提前规划频道分配。
* 在需要精确材料的[样板](items-blocks-machines/patterns.md)中，尽量使用统一的木材等输入材料，避免不同变种导致自动合成失败。
* 在<ItemLink id="pattern_access_terminal" />中整理[样板](items-blocks-machines/patterns.md)，并将它们分配到不同的[样板供应器](items-blocks-machines/pattern_provider.md)，让多道工序可以并行执行。
* 为网络添加[能源元件](items-blocks-machines/energy_cells.md)，缓解瞬时耗能带来的供电波动。
* <ItemLink id="condenser" />可以用于处理多余的物品。
* 尽量不要把大量耐久度、附魔各不相同的武器和护甲随意存入 ME 网络；这些物品会占用额外的[类型](ae2-mechanics/bytes-and-types.md)额度。
* 对于[处理样板](items-blocks-machines/patterns.md)，加工结果必须重新进入 ME 网络，系统才能确认加工完成。可以使用<ItemLink id="import_bus" />、<ItemLink id="interface" />或<ItemLink id="pattern_provider" />接收结果；只把输出导入连接了<ItemLink id="storage_bus" />的外部箱子，并不总能正确完成自动合成。
* <ItemLink id="pattern_provider" />会从同一个输出面一次性送出完整批次的原料，适合要求一次收齐原料的机器。若材料需要分发到多个位置，可以改用<ItemLink id="interface" />、[管道子网](example-setups/pipe-subnet.md)或其他中间容器。
