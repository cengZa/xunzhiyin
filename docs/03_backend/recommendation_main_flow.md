# 推荐主链路伪代码设计

## 1. RecommendationService.recommend()

伪代码：

1. 校验请求参数 userId / topK
2. profile = profileService.getProfile(userId)
   - 若无画像，则调用 buildProfile(userId, "init")
3. candidateUserIds = recallService.recallCandidateUserIds(profile)
4. rankingList = rankingService.rank(userId, candidateUserIds)
5. rerankedList = rerankService.rerank(userId, rankingList)
6. topList = rerankedList 按 finalScore 倒序取前 topK
7. traceId = 生成 requestTraceId
8. 批量保存 recommendation_result
9. explanationService 为 topList 生成 explanation
10. 组装 RecommendationDetailVO 返回

## 2. RankingService.rank()

伪代码：

1. sourceProfile = profileService.getProfile(requestUserId)
2. targetProfiles = profileService.listProfiles(candidateUserIds)
3. 对每个 targetProfile:
   3.1 计算公共标签集合
   3.2 计算点积
   3.3 计算向量模长
   3.4 得到 rankScore
   3.5 记录 contributionItem
   3.6 构造 RankingCandidateModel
4. 返回 rankingList

## 3. RerankService.rerank()

伪代码：

1. 批量查询候选用户基础信息
2. 对每个 candidate:
   2.1 判断 gradeDiff 规则
   2.2 判断 majorRelated 规则
   2.3 判断 clubOverlap 规则
   2.4 汇总 rerankScore
   2.5 finalScore = rankScore + rerankScore
   2.6 记录 ruleHits
3. 按 finalScore 降序排序
4. 返回结果

## 4. ExplanationService.generate()

伪代码：

1. 从 candidate.contributions 中取前 2~3 个贡献最高标签
2. 从 candidate.ruleHits 中取命中的规则
3. 组装 evidenceJson
4. 基于模板生成中文解释文本
5. 返回 ExplanationVO

## 5. FeedbackService.submitFeedback()

伪代码：

1. 校验 recommendationId
2. 落库 feedback
3. 读取 recommendationExplanation
4. 解析 contributionJson
5. 对 requestUserId 对应画像做轻量调整
6. 保存新画像
7. 刷新 Redis 缓存