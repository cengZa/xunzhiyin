# Frontend Demo Console

## Goal
Provide a same-origin demo console for the current backend MVP without introducing a separate frontend build toolchain.

## Entry
- Page route: `/`
- Static files:
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/app.css`
  - `src/main/resources/static/app.js`

## Scope
- Initialize mock data
- Rebuild all profiles
- Rebuild recall index
- Load a user profile
- Run recommendation requests
- View recommendation explanation
- Submit follow feedback
- Load offline evaluation summary
- Export the latest evaluation snapshot
- Export the topK experiment matrix

## Information Architecture
- Hero: project positioning and current stage
- Data and evaluation panel: initialization, rebuild, export
- User workspace: user input, profile, evaluation summary
- Recommendation panel: candidate cards, detail JSON, explanation JSON
- Run log: action replay for demo and debugging

## Design Constraints
- Single page, static assets only
- Reuse existing backend APIs directly
- Optimize for demo clarity, not product-grade social UI
- Keep the page understandable for reviewers who are new to the project
