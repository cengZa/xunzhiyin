# Recommendation Evaluation Summary

- generatedAt: 2026-04-12T12:16:58.923538700
- topK: 3
- activeUserCount: 12
- tagCount: 12
- relationCount: 48
- proxyRule: shared_tags>=2 OR (same_major AND shared_tags>=1)

| 基线 | recall均值 | topK返回均值 | Precision@K | HitRate@K | 解释覆盖率 |
| --- | --- | --- | --- | --- | --- |
| 标签重叠 | 7.6667 | 3.0000 | 0.9444 | 1.0000 | 1.0000 |
| 纯排序得分 | 7.6667 | 3.0000 | 0.9444 | 1.0000 | 1.0000 |
| 完整链路 | 7.6667 | 3.0000 | 0.9444 | 1.0000 | 1.0000 |
