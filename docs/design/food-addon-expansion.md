# 森罗物语与农夫乐事附属扩展

## 授权范围与选型

按用户选择加入五个森罗附属，以及之前列出的四组农夫乐事附属，共 20 个内容/联动模组，另补两项独立必需库。客户端与服务端均必装，清单固定发布版本和资源 ID。

- 森罗：国味、烟火、酱料、幽匿、KubeJS 森罗联动。
- 维度料理：End’s Delight、Ocean’s Delight、Miner’s Delight、My Nether’s Delight。
- 作物与菜系：Fruits Delight、Cultural Delights、Rustic Delight、Ube’s Delight、Corn Delight。
- 饮品：Farmer’s Respite、Coffee Delight、Cocktails Delight。
- 烹饪玩法：Cuisine Delight、Barbeque’s Delight、Dungeon’s Delight。

厨房家具、食品展示、玩偶、进食动画、矿物葡萄、暮色森林和群峦兼容不在这次选择中。下界料理选择 My Nether’s Delight，不同时叠加原 Nether’s Delight。已有 Create Slice & Dice、森罗自动化 DimensionWorks fork、森罗兼容和齿轮与酒馆继续保留，不重复安装其他同用途自动化套件。

这些内容需要第三方新增方块、食物与烹饪系统，不能仅靠现有模组或 KubeJS 配方事件实现，因此使用用户指定附属及其必需运行库，不新增自研 Java 模组。

## 固定依赖

| 模组 | 固定版本 | 来源 |
| --- | --- | --- |
| KaleidoscopeChineseFood | `1.1.16-1.20.1` | [modrinth](https://modrinth.com/mod/kaleidoscopechinesefood) |
| Kaleidoscope Grilling | `1.0.1` | [modrinth](https://modrinth.com/mod/kaleidoscope-grilling) |
| Kaleidoscope Sauce | `0.1.2` | [modrinth](https://modrinth.com/mod/kaleidoscope-sauce) |
| Kaleidoscope Sculk | `1.4.2` | [modrinth](https://modrinth.com/mod/kaleidoscope_sculk) |
| Kubejs Kaleidoscope | `1.1.0-mc1.20.1-forge` | [modrinth](https://modrinth.com/mod/kubejs-kaleidoscope) |
| End's Delight | `2.6.1+forge.1.20.1` | [modrinth](https://modrinth.com/mod/ends-delight) |
| Ocean's Delight | `1.0.2-1.20` | [modrinth](https://modrinth.com/mod/oceans-delight) |
| Miner's Delight | `1.20.1-1.4.5-backport` | [modrinth](https://modrinth.com/mod/miners-delight) |
| My Nether's Delight | `1.20.1-1.10.4-backport.1` | [modrinth](https://modrinth.com/mod/my-nethers-delight) |
| Fruits Delight | `1.1.3` | [modrinth](https://modrinth.com/mod/fruits-delight) |
| Cultural Delights | `0.16.7` | [modrinth](https://modrinth.com/mod/cultural-delights) |
| Rustic Delight | `1.7.0` | [modrinth](https://modrinth.com/mod/rustic-delight) |
| Ube's Delight | `forge-1.20.1-0.4.15` | [modrinth](https://modrinth.com/mod/ubes-delight) |
| Corn Delight | `1.2.11-1.20.1` | [modrinth](https://modrinth.com/mod/corn-delight) |
| Coffee Delight | `1.5.2` | [modrinth](https://modrinth.com/mod/coffee-delight) |
| Cocktails Delight | `1.0.3` | [modrinth](https://modrinth.com/mod/cocktails-delight) |
| Cuisine Delight | `1.1.20` | [modrinth](https://modrinth.com/mod/cuisine-delight) |
| Barbeque's Delight [Forge/NeoForge] | `1.0.6` | [modrinth](https://modrinth.com/mod/barbeques-delight-forge) |
| Dungeon's Delight | `1.3.1` | [modrinth](https://modrinth.com/mod/dungeons_delight) |
| Farmer's Respite | `1.20.1-2.1.2` | [curseforge](https://www.curseforge.com/minecraft/mc-mods/farmers-respite) |
| RunicLib | `4.3.11` | [modrinth](https://modrinth.com/mod/runiclib) |
| Mysterious Mountain Lib | `1.6.34-1.20.1` | [modrinth](https://modrinth.com/mod/mmlib) |

酱料的 `kaleidoscope_chinesefood` 必需前置由本次国味满足。Corn Delight 需要 Mysterious Mountain Lib，本次固定为 1.6.34；Dungeon’s Delight 需要 RunicLib >=4.3.7，本次固定为 4.3.11。

Fruits Delight 已内嵌 `META-INF/jarjar/l2harvester-0.1.2.jar`，不再单独分发同一个 L2 Harvester。它内嵌的 05x/60x Create 收割兼容由 Mixin 插件按 Create 版本选择；当前 Create 6 不启用旧 05x 分支。料理、果园与烧烤附属也携带部分 L2 库，保留上游 Jar-in-Jar 元数据，沿用包里已有的 L2Library 2.5.3；构建/打包校验不等于已验证所有运行时链接。

森罗厨房从 1.5.1 更新到 1.6.0：幽匿 1.4.2 的镰刀直接引用本体 `ModEnchantments.SWEEP`，旧本体不存在该类。酒馆 1.2.0、农夫乐事 1.3.4、Create 6.0.8、Forge 47.4.23 保持现有基线。

烟火固定 1.0.1：1.1.0/1.1.1 的调味自动化仍调用 Create 0.5 的静态 `FilterItem.getFilterItems`，与当前 Create 6 的实例方法签名不匹配；不引入已知无法工作的调味自动化。末地料理固定 2.6.1：旧 2.4 末影炉调用当前农夫乐事已不存在的 `ItemUtils.isInventoryEmpty`，新版本已移除此引用。Farmer’s Respite 的实际文件名为小写 `farmersrespite-1.20.1-2.1.2.jar`，固定 CurseForge 文件 5543547，避免用展示标题的大写文件名拼出无效下载链接。

## Curios 默认容量

`config/curios-common.toml` 对当前 19 类普通槽位使用 `size=3;operation=SET`，`gear_heart` 使用 `size=1;operation=SET`。覆盖标准槽位、天境专用槽位以及女仆联动注册的 feet/scroll/spellbook。Curios COMMON 配置在数据包与旧 IMC 槽位声明之后应用，避免旧的 ADD 或较大 SET 数量覆盖整合包数量。

这是当前已注册类型的固定清单，不是 Curios 不支持的通配符。Curios 会把配置声明的槽位提供给玩家；已有图标、验证器、原生 GUI 属性与具体物品的装备限制继续沿用原类型。未来新增槽位类型时需要补充此清单。

齿轮之心仍使用已有独立标签与装备限制，保持专属槽位单格。天境继续使用仓库已有的默认 Curios 菜单设置，不切换原生菜单。服务端更新配置后需重启并让客户端重新登录；只修改客户端配置不能改变服务器槽位。旧存档若使用过扩大槽位的临时属性或命令，需要另行核对，不把基础默认数量冒充所有附加修饰后的最终数量。

## 集成与性能边界

不新增自定义世界生成、强加载区块或常驻机器阵列。附属自带的作物、野生植物、战利品和相关世界生成采用其默认行为；Quark 的世界/新生物类别仍按先前要求关闭。新的野生资源是否出现在具体工业维度需游戏内核对，不能把注册成功等同于在所有维度均可自然获得。

新厨房和自动化设备按需部署，控制附近动物、物品实体、并行炊具和长期加载机器数量，优先复用已有物流与自动化体系。本次不擅自改菜肴效果、机器速度、工业考核配方或资源价值。

## 验证

提交前核对清单 JSON、COMMON 配置 TOML、固定产物哈希、必需依赖与内嵌库、当前槽位数据与旧 IMC 类型。静态检查不能替代 Minecraft 加载和游戏内测试。禁止本地构建，自动化构建、打包和发布统一由整合包正式发布工作流执行。

游戏内验收：客户端和服务器启动、JEI 分类与配方、森罗炒锅/汤锅/研磨/烧烤/酱料、农夫烹饪与饮品、Create 加工与收割联动、Curios 普通槽位 3 格及齿轮之心 1 格、旧存档已有饰品保留，以及新增资源获取路线。

本次静态结果：清单固定版本/重复 ID、JEI 提示元数据、Curios 20 类默认容量、所选 Modrinth 内容模组 Jar 哈希与 TOML 解析通过；新增内容模组未声明额外 Curios 槽位。对新增附属和已有森罗附属共检查 2,370 个 Create/农夫/森罗 API 引用；上述三处版本问题已按选型处理，旧 L2Harvester 05x 分支由版本条件禁用。Forge/Minecraft 继承方法未据此宣称完整运行时验证。

遵照用户“先别继续打包”的要求，本轮只修改清单、配置和文档，暂不提交推送或触发正式发布；根版本保持 0.2.69。
