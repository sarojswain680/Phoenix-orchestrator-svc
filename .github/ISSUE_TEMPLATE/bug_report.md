---
name: 🐛 Bug Report
about: Report a defect in the orchestrator service
title: "[BUG] "
labels: ["bug", "needs-triage"]
assignees: ""
---

## 🐛 Describe the Bug

<!-- A clear, concise description of what the bug is. -->

## 🔁 Steps to Reproduce

1. Go to '...'
2. Submit '...'
3. See error

## ✅ Expected Behavior

<!-- What you expected to happen. -->

## ❌ Actual Behavior

<!-- What actually happened. Include stack traces / logs. -->


## 🧩 Affected Component

 Ingest (ZIP / Git)
 Scan / Detect Framework
 AST Decomposition
 Build Graph
 Enrichment (Gemini)
 Verification
 Storage (Hot / Warm / Cold)
 API / Other

 ## 🧩 Environment

 Java version: 21
OS:
Deployment:
Redis:

## 🔐 Security Impact?
 ⚠️ This bug may have a security impact — if so, STOP and report privately per SECURITY.md instead.

 ## 📎 Additional Context


### `.github/ISSUE_TEMPLATE/feature_request.md`

```markdown
---
name: ✨ Feature Request
about: Suggest a new feature or enhancement
title: "[FEATURE] "
labels: ["enhancement", "needs-triage"]
assignees: ""
---

## 🎯 Problem Statement

<!-- What problem does this solve? Is it a pain point today? -->

## 💡 Proposed Solution

<!-- Describe the feature and how it should work. -->

## 🧩 Affected Pipeline Stage(s)

- [ ] Ingest
- [ ] Scan / Detect
- [ ] AST Decomposition
- [ ] Build Graph
- [ ] Enrichment
- [ ] Verification
- [ ] Storage
- [ ] API / Cross-cutting

## 🔄 Alternatives Considered

<!-- Other approaches you thought about and why you rejected them. -->

## 📊 Impact / Value

- **Users affected:** <!-- who benefits -->
- **Priority:** <!-- Low / Medium / High -->
- **Effort estimate:** <!-- rough guess -->

## 🔐 Security / Tenant Considerations

<!-- Any impact on tenant isolation, data handling, or LLM data flow? -->

## 📎 Additional Context

<!-- Mockups, references, related issues. -->