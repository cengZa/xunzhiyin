# 2026-04-22 Lightweight Exploration

## Goal
- Add a lightweight exploration slot without changing the project boundary.
- Keep the recommendation story focused on campus user matching, explanation quality, and defense stability.

## Scope
- Add `ExplorationService` to the recommendation pipeline.
- Enable exploration only in `interest_partner`.
- Keep the top 2 stable and use only the last slot as an exploration position.
- Expose `exploration` / `explorationScore` / `explorationReason` to API, explanation output, compare view, and homepage.

## Result
- Added:
  - `src/main/java/com/lcj/campusreco/service/ExplorationService.java`
  - `src/main/java/com/lcj/campusreco/service/impl/ExplorationServiceImpl.java`
  - `src/test/java/com/lcj/campusreco/service/impl/ExplorationServiceImplTest.java`
- Updated:
  - `RecommendationServiceImpl`
  - `DemoComparisonServiceImpl`
  - `RecommendationServiceImplTest`
  - `DemoComparisonServiceImplTest`
  - `ApiFlowIntegrationTest`
  - `app.js`
  - `docs/04_test/metrics_definition.md`
  - `docs/04_test/result_analysis.md`
  - `docs/04_test/test_cases.md`

## Verification
- `mvn -Dtest=ExplorationServiceImplTest,RecommendationServiceImplTest,DemoComparisonServiceImplTest test`
- `mvn -Dtest=ApiFlowIntegrationTest test`

## Notes
- Exploration is intentionally conservative.
- It is not enabled for `study_partner` or `club_partner`.
- It is not a bandit or online-learning system; it is a defense-oriented lightweight exploration mechanism.
