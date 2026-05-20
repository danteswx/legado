# Discover Source Smart Sort Design

## Goal

发现页书源列表按用户实际使用习惯排序：优先展示书架中已有书更多、最近阅读活跃、以及发现页中更常使用的书源；没有历史数据时保持现有手动排序。

## Scope

本设计只影响发现页的书源展示顺序：

- 旧发现页书源列表。
- 新发现页书源选择弹窗。
- 发现页搜索和分组过滤后的书源结果。

不改变书源管理页排序，不写回 `customOrder`，不新增可见配置项。

## Existing Behavior

发现页书源来自 `BookSourceDao.flowExplore()`、`flowExplore(key)` 和 `flowGroupExplore(key)`，数据库查询按 `customOrder asc` 返回。

新发现页把返回结果保存到 `ExploreViewModel.sources`，再用 `SourceSelectDialog` 展示。旧发现页直接把结果提交给 `ExploreAdapter`。

书架已有书可以从 `books.origin` 统计。发现页当前没有专门的源使用统计。

## Ranking Inputs

每个发现源按 `bookSourceUrl` 匹配以下输入：

- `shelfCount`：书架中来自该源的正式书籍数量，排除 `BookType.notShelf`。
- `lastReadTime`：该源书架书籍的最大 `durChapterTime`。
- `discoverUseCount`：用户在发现页对该源的有效使用次数。
- `discoverLastUseTime`：该源最近一次发现页有效使用时间。
- `weight`：书源已有智能排序权重，作为弱加成。
- `customOrder`：最终稳定兜底排序。

## Usage Events

只记录明确用户行为，避免自动刷新刷分：

- 新发现页手动选择书源：`+1`。
- 新发现页点击分类并加载列表：`+1`。
- 旧发现页展开书源：`+1`。
- 旧发现页打开某个发现分类：`+2`。
- 从发现结果打开书籍详情：`+3`。

每次记录同时更新最近使用时间。删除书源时清理该源的发现使用统计。

## Score Formula

排序分数为临时计算值，不持久化到书源表：

```text
score =
  min(shelfCount, 20) * 100
+ recencyScore(lastReadTime)
+ min(discoverUseCount, 50) * 20
+ recencyScore(discoverLastUseTime)
+ boundedWeight(weight)
```

`recencyScore(time)`：

```text
7 天内  = 80
30 天内 = 40
90 天内 = 15
其他    = 0
```

`boundedWeight(weight)`：限制在 `-20..20`，避免覆盖书架和使用行为。

最终排序：

```text
score desc
customOrder asc
bookSourceName asc
```

没有书架和使用历史时，所有源分数相同，顺序等同原 `customOrder`。

## Architecture

新增一个纯排序 helper，负责把发现源列表和统计数据组合成排序结果。它不依赖 Android View，便于单元测试。

`BookDao` 增加轻量聚合查询，用于获取书架中每个 `origin` 的书籍数量和最近阅读时间。

新增发现源使用统计配置类，使用 `SharedPreferences` 按源 URL 存储计数和最近使用时间。这个数据体量小，不需要数据库迁移。

`ExploreViewModel` 统一暴露排序方法。新旧发现页拿到 DAO 返回列表后，都先经过排序 helper，再更新 adapter 或源选择列表。

## Data Flow

1. 发现页从 `BookSourceDao` 获取启用发现且有发现 URL 的书源。
2. `ExploreViewModel` 从 `BookDao` 获取书架源统计。
3. `ExploreViewModel` 从发现使用统计配置读取使用数据。
4. `DiscoverSourceSorter` 计算分数并返回排序后的列表。
5. UI 层展示排序后的列表。
6. 用户触发发现页使用事件时，记录该源使用并刷新后续排序。

## Error Handling

排序统计读取失败时，回退到 DAO 原始顺序。

统计中存在已删除源时，不影响排序；删除书源流程会主动清理对应使用统计。

时间戳异常或缺失按 `0` 处理，不给最近使用加分。

## Tests

新增 JVM 单元测试覆盖排序 helper：

- 无统计时保持 `customOrder` 顺序。
- 书架书籍数量更多的源排前。
- 最近阅读过的源在同等数量下排前。
- 发现页使用次数更多的源排前。
- 最近使用过的源在同等次数下排前。
- 同分时按 `customOrder` 和名称稳定排序。

新增配置测试或轻量代码断言覆盖使用统计增量与清理逻辑。

## Verification

实现后执行：

- 排序相关 JVM 单元测试。
- 签名 release 构建。
- 安装 release 到模拟器，验证旧发现页、新发现页源选择弹窗、搜索、分组过滤和打开书籍详情行为。
