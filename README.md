# OpenBlocks Reborn redux — 1.21.1 NeoForge

基于 Noped01/OpenBlocks-Reborn-redux 的 `1.21.1` 分支，基线提交为
`f4265a23b37b6dca146663cffff038983218d636`。

## 本次修改

- 指南强制使用单独安装的 Patchouli；保留 `openblocks_reborn:info_book` 物品 ID。
- 目录沿用旧版：欢迎与致谢、方块、物品、杂项、更新日志。
- 98 个物品条目、附魔说明和五个章节条目；中英文说明、配方与明确标注的 1.12.2 历史参考。
- 完整引入 ElevatorMod `1.21.1-1.11.4` 对应分支的 common 和 NeoForge 实现及资源，改为 OpenBlocks 自身的 16 色电梯。
- 新电梯使用 `openblocks_reborn:elevator_white` 等 ID。普通/定向行为在同一方块上配置，伪装、界面、渲染、网络、音效和原有配置均包含在内。
- 移除旧 `openblocks_reborn:elevator`、`openblocks_reborn:elevator_rotating` 的注册、旧传送事件、配方与展示条目。
- 音波眼镜恢复声音可视化，增加字幕、距离和现代声音分类；不再给周围生物施加共享的发光效果。
- 村庄高亮仪：指向兴趣点方块查看一位实际关联村民；全部模式通过按钮获取静态快照，显示与冷却均为 200 tick（正常运行约 10 秒），最多 16 位村民，无自动轮询。支持兴趣点、关联线、铁傀儡搜索框及可选的局部村庄区段边界。
- 附魔补齐提示、分页手册和配置：图腾优先于屹立不倒，之后才按经验尝试保命；震荡不定默认不破坏方块；尔虞我诈六项恶作剧默认全关。三个效果总开关默认开，默认非宝藏，宝藏属性独立可配。详见 [附魔说明](docs/ENCHANTMENTS.md)。

独立 ElevatorMod **不再是依赖**。Patchouli 仍是独立必需依赖，本地验收使用 `Patchouli-1.21.1-93-NEOFORGE`。

## 使用与存档

本分支改变了电梯 ID。已有 `elevatorid:*` 或上述旧电梯的存档，需要在副本上做数据迁移后再切换。
迁移需要单独执行并保留回滚数据，范围见 [电梯迁移说明](docs/ELEVATOR_MIGRATION.md)。

电梯配置位于存档的 `serverconfig/openblocks_reborn-elevator-server.toml`；
音波眼镜配置位于客户端的 `config/openblocks_reborn-client.toml`。

## 构建与验证

标准工程使用 Java 21、Gradle 和 NeoForge ModDev：

```sh
./gradlew build -PopenblocksNoRuntimeHelpers
```

如果构建环境已经安装带命名类的 NeoForge 1.21.1，可使用 `tools/build_local.py`，
传入本地 runtime JSON 和 Patchouli JAR。这个工具不会把环境路径或依赖 JAR 打包。
`tools/validate_resources.py` 检查书籍引用、翻译键、配方、命名空间和交付 JAR。

`src/acceptance` 是独立测试模组，只通过 `tools/run_client_acceptance.py` 加载，
不打进发布 JAR。所有自动测试的存档与日志都在本工程 `build/` 下。

本机标准 Gradle 构建的依赖下载遇到 Maven 连接超时；实际编译及运行验证使用本地已安装的命名运行库。
验证结果与边界见 [验收记录](docs/VALIDATION.md)。

## 已确认的原分支缺口

此分支仍不是旧 OpenBlocks 全内容的完整还原。收音机、自动散架脚手架未注册；
村庄高亮仪使用新版兴趣点与村民机制；黄色框仅代表搜索体积，不保证可生成。范围和关联受已加载区域及采样上限限制，详见 [高亮仪说明](docs/VILLAGE_HIGHLIGHTER.md)。
旧物品栏快照恢复命令和 B 键消化玩法也没有对应实现。
音波眼镜恢复了核心声音可视化，但没有逐项重现旧版所有材质/生物面部图标及整屏暗化效果。

代码、素材和引入清单见 `THIRD_PARTY_NOTICES.md`、`LICENSE-OpenBlocks`、
`LICENSE-ElevatorMod`、`docs/elevator-import.json`。
