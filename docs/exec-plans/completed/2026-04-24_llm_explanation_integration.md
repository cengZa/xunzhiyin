# 2026-04-24 LLM Explanation Integration

## Goal
- Add Zhipu `GLM-4.7` to the explanation retrieval path.
- Keep ranking logic unchanged.
- Keep fallback stable for demo and defense use.

## Delivered
- Added `AiExplanationClient` abstraction and `ZhipuAiExplanationClient`.
- Switched API key loading to environment variable `ZAI_API_KEY`.
- Extended explanation response with:
  - `ruleReasonText`
  - `llmReasonText`
  - `reasonSource`
- Updated the frontend explanation panel to show explanation source.
- Ensured `ExplanationVO` omits null fields in JSON.

## Verification
- `mvn -gs .mvn/temp-settings.xml -Dmaven.repo.local=D:\.projects\xunzhiyin\.m2repo -Dsurefire.useFile=false -Dtest=ExplanationServiceImplTest,ApiFlowIntegrationTest test`
- `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`

## Notes
- This phase intentionally does not use LLM for ranking or rerank decisions.
- The system stays usable when Zhipu is disabled or unavailable.
