---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: 样板修改器
    icon: expatternprovider:pattern_modifier
categories:
- extended items
item_ids:
- expatternprovider:pattern_modifier
---

# 样板修改器

样板修改器可以批量修改处理样板，省去逐一重新编码的麻烦。

<ItemImage id="expatternprovider:pattern_modifier" scale="4"></ItemImage>

右键使用物品即可打开修改界面。

## 批量调整材料数量

点击对应按钮，可将处理样板中输入和输出的物品数量按指定倍率放大或缩小。 

![样板修改器界面](../pic/pm.png)

原始样板：

![原始配方](../pic/pm1.png)

数量乘以 10 后：

![放大后的配方](../pic/pm2.png)

点击“清空”可以清除样板内的全部配方内容，将其恢复为空白样板。

### 注意

- 只有材料数量能够被目标倍率整除时，除法操作才会生效。例如样板需要 3 个圆石，除以 2 会得到 1.5 个，因此该操作无效。

- 单项材料的数量上限为 999999。超过这个数值的倍率操作不会生效。

## 批量替换材料

将样板中的指定输入材料或输出产物批量替换为其他物品。

![材料替换界面](../pic/pm4.png)

A 槽放入**需要替换的原材料**，B 槽放入**替换后的材料**。

例如，下面的设置会将样板中的木板替换为煤炭。

![替换设置示例](../pic/pm6.png)

![替换前的样板](../pic/pm5.png)

确认设置后，点击替换按钮即可批量处理。

![替换完成的样板](../pic/pm7.png)

## 复制样板

在此模式中，可以复制放入的样板，将其内容写入其他空白样板。

![样板复制界面](../pic/pm3.png)

