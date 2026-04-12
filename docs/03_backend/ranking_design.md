# Ranking Design
# 排序与重排实现设计

## 1. 排序目标

对召回后的候选用户进行相似度计算，并输出基础排序结果。

## 2. 排序输入

- 目标用户画像向量
- 候选用户画像向量

## 3. 排序方法

采用余弦相似度：

cosine(A, B) = (A · B) / (|A| * |B|)

其中：
- A：目标用户兴趣向量
- B：候选用户兴趣向量

## 4. 标签贡献项记录

在计算点积时，同时记录每个公共标签的贡献值：

contribution(tag) = weightA(tag) * weightB(tag)

然后按 contribution 倒序排列，供解释模块使用。

## 5. 输出结构

每个候选对象输出：
- targetUserId
- rankScore
- contributions[]

## 6. 重排目标

在基础排序结果上引入校园场景规则，使结果更符合场景需求。

## 7. 重排规则

### 规则 1：年级差限制
- gradeDiff <= 1：加分
- gradeDiff > 1：减分或不加分

### 规则 2：专业相关性
- major 相同：较高加分
- major 属于同方向：中等加分
- 其他：不加分

### 规则 3：社团重合度
- 若有共同社团标签：加分

### 规则 4：轻度多样性（可选）
- 如果前几名高度同质，可轻度打散

## 8. 最终分数

finalScore = rankScore + rerankScore

其中：
- rankScore：余弦相似度得分
- rerankScore：规则加减权得分

## 9. 输出结构

每个候选对象最终输出：
- targetUserId
- rankScore
- rerankScore
- finalScore
- contributions[]
- ruleHits[]

## 10. 注意事项

1. 规则不宜过强，否则会压制基础相似度
2. 重排只是微调，不是推翻排序
3. ruleHits 需要结构化记录，便于解释和论文展示