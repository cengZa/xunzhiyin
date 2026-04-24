# LLM Explanation Integration

## Goal
- Keep recommendation ranking deterministic and reproducible.
- Use Zhipu `GLM-4.7` only to rewrite explanation text.
- Preserve rule-based fallback so the system remains demo-safe when the external API is unavailable.

## Scope
- Add an explanation provider abstraction:
  - `AiExplanationClient`
  - `AiExplanationRequest`
  - `ZhipuAiExplanationClient`
- Read API key from environment variable `ZAI_API_KEY`.
- Extend explanation response with:
  - `ruleReasonText`
  - `llmReasonText`
  - `reasonSource`
- Update the frontend explanation panel to expose explanation source.

## Design Constraints
- LLM does not participate in recall, ranking, rerank, trust score, or exploration.
- `reasonText` remains the main outward-facing field.
- If Zhipu returns a valid rewrite:
  - `reasonText = llmReasonText`
  - `reasonSource = llm`
- If key is missing, request fails, or response is invalid:
  - `reasonText = ruleReasonText`
  - `reasonSource = rule`
- No database schema change.

## Runtime Config
Configured in `application.yml`:

- `app.ai.explanation.enabled`
- `app.ai.zhipu.base-url`
- `app.ai.zhipu.api-key`
- `app.ai.zhipu.model`
- `app.ai.zhipu.temperature`
- `app.ai.zhipu.timeout-ms`

## API Surface
`GET /api/recommendations/{recommendationId}/explanation` now returns:

- `recommendationId`
- `reasonText`
- `ruleReasonText`
- `llmReasonText`
- `reasonSource`
- `evidenceJson`
- `contributionJson`
- `evidence`
- `contribution`

## Verification Focus
- Rule fallback must always work without `ZAI_API_KEY`.
- Response JSON should not leak meaningless `null` fields.
- Frontend should clearly distinguish `LLM 改写解释` and `规则解释`.
- Home page and pipeline page should both support `答辩模式 / 调试模式`.
- In `答辩模式`, raw JSON panels should be hidden by default.
- In `调试模式`, raw JSON panels and detailed payloads should remain visible.
