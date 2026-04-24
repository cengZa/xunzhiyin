# 2026-04-22 Demo Pipeline Transparency

## Goal
- Make the recommendation chain transparent on the frontend for any user.
- Keep `2001` only as the default defense user, not as a hard-coded target.

## Result
- Added backend aggregation endpoint:
  - `GET /api/admin/demo/pipeline`
- Added frontend page:
  - `/pipeline.html`
- The page now shows:
  - input tags
  - profile construction
  - recall pool
  - ranking stage
  - rerank / trust / exploration stage
  - final Top-K with explanations

## Verification
- `mvn -Dtest=DemoPipelineServiceImplTest,ApiFlowIntegrationTest test`
