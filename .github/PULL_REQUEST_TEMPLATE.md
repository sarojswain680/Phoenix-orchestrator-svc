## 📝 Description

<!-- What does this PR do? Provide context and link the issue/ticket. -->

Closes #<!-- issue number -->

---

## 🔧 Type of Change

<!-- Check all that apply with an [x] -->

- [ ] 🐛 Bug fix (non-breaking change that fixes an issue)
- [ ] ✨ New feature (non-breaking change that adds functionality)
- [ ] 💥 Breaking change (fix or feature that changes existing behavior)
- [ ] ♻️ Refactor (no functional change)
- [ ] 🧪 Tests (adding or updating tests)
- [ ] 📄 Documentation
- [ ] ⚙️ CI / build / infra

---

## 🧩 Affected Pipeline Stage(s)

<!-- Which part of the orchestrator does this touch? -->

- [ ] Ingest (ZIP / Git)
- [ ] Scan / Partition
- [ ] Detect Framework
- [ ] AST Decomposition
- [ ] Build Graph
- [ ] Enrichment (Gemini / Spring AI)
- [ ] Verification
- [ ] Storage (Hot / Warm / Cold)
- [ ] N/A

---

## ✅ Author Checklist

<!-- Confirm before requesting review. All boxes should be checked. -->

- [ ] Code compiles on **JDK 21** (`./gradlew clean build`)
- [ ] All new/changed code has **unit tests**
- [ ] Test **coverage is ≥ 95%** (CI enforces this)
- [ ] No **hardcoded secrets** / API keys (uses env vars / `${...}`)
- [ ] **Tenant isolation** is respected where data is read/written
- [ ] Native / off-heap **AST resources are closed** (try-with-resources)
- [ ] No **blocking calls** on async / reactive threads
- [ ] Ran locally and verified behavior
- [ ] Updated relevant **documentation** (if applicable)

---

## 🔒 Security Considerations

<!-- Since we ingest untrusted customer code, call out any security impact. -->

- [ ] This PR does **not** introduce new external inputs, OR
- [ ] New inputs are **validated / sanitized**
- [ ] No customer code/data is sent to the LLM **without redaction**

---

## 🧪 How Was This Tested?

<!-- Describe the tests you ran. Include commands/output if helpful. -->
