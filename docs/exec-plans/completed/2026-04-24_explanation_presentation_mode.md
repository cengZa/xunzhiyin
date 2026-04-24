# 2026-04-24 Explanation Presentation Mode

## Goal
- Make homepage and pipeline page suitable for defense use.
- Surface `规则解释` and `LLM 改写解释` side by side.
- Add `答辩模式 / 调试模式` switch without changing recommendation logic.

## Delivered
- Homepage:
  - Added `答辩模式` toggle.
  - Added three explanation blocks:
    - 当前展示解释
    - 规则依据
    - LLM 改写解释
  - Hid raw debug panels in defense mode.
- Pipeline page:
  - Rebuilt the page with normalized Chinese outward-facing text.
  - Added `答辩模式` toggle.
  - Added explanation comparison area.
  - Added per-card `查看解释对照` action.
  - Auto-loads the first final recommendation explanation.

## Verification
- `mvn -gs .mvn/temp-settings.xml -Dmaven.repo.local=D:\.projects\xunzhiyin\.m2repo -Dsurefire.useFile=false test`
- `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`

## Notes
- Defense mode is the default.
- Debug mode keeps raw JSON available for implementation walkthroughs.
