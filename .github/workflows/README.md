# 🚀 CI/CD Pipeline Documentation

This document explains the automated CI pipeline for the **Orchestrator Service**.
Read this before opening your first PR.

---

## 📑 Table of Contents

1. [Overview](#-overview)
2. [Pipeline Stages](#-pipeline-stages)
3. [Job Reference](#-job-reference)
4. [How to Debug Failures](#-how-to-debug-failures)
5. [Running Checks Locally](#-running-checks-locally)
6. [Required Secrets](#-required-secrets)
7. [FAQ](#-faq)

---

## 🎯 Overview

Every Pull Request against `main` triggers `ci.yml`. The pipeline follows a
**fail-fast** philosophy: cheap checks run first, and expensive checks (AI review,
mutation testing, container scans) only run if the cheap ones pass.

**You cannot merge until the `✅ CI Success Gate` passes** and a Code Owner approves.

### Pipeline Files

| File | Trigger | Purpose |
| :--- | :--- | :--- |
| `ci.yml` | PR to `main` | The main quality + security gate (15 jobs) |
| `13-release.yml` | Push to `main` | Automated versioning & changelog (post-merge) |
| `../dependabot.yml` | Scheduled | Weekly dependency update PRs |

---

## 🔄 Pipeline Stages

```
STAGE 0 · CHEAP GATES (~30s)
├── PR Title Lint
├── Format (Spotless)
└── Secret Scan (Gitleaks)
              │
STAGE 1 · COMPILE (~1min)  ◄── gate for everything below
              │
STAGE 2 · PARALLEL FAST CHECKS (~3-5min)
├── Tests + 95% Coverage
├── SpotBugs + FindSecBugs
├── CodeQL SAST
├── License Compliance
├── IaC Scan (Checkov)
└── OWASP Dependency Check
              │
STAGE 3 · BUILD ARTIFACT (~2min)
└── Build JAR + Docker Image
              │
STAGE 4 · SLOW / EXPENSIVE (~10-30min)
├── Trivy Image Scan
├── SBOM Generation
├── Mutation Testing (PITest)
└── Claude AI Review
              │
STAGE 5 · FINAL GATE
└── ✅ CI Success Gate  ◄── branch protection requires THIS
```

### Why This Order?

We **fail fast and fail cheap**. If your code isn't formatted (a 5-second check),
the pipeline stops immediately — we never waste 20 minutes running mutation tests
or spend money on AI review. Each stage gates the next via `needs:` dependencies.

---

## 📋 Job Reference

### Stage 0 — Cheap Gates

| Job | What It Checks | Common Failure Cause |
| :--- | :--- | :--- |
| **PR Title Lint** | PR title follows [Conventional Commits](https://www.conventionalcommits.org/) | Title like `"fixed bug"` instead of `"fix: resolve AST leak"` |
| **Format (Spotless)** | Code is formatted (Google Java Format) | Forgot to run `./gradlew spotlessApply` |
| **Secret Scan (Gitleaks)** | No secrets/API keys in the diff or history | Committed an API key, password, or token |

### Stage 1 — Compile

| Job | What It Checks | Common Failure Cause |
| :--- | :--- | :--- |
| **Compile (JDK 21)** | Code compiles on Java 21 | Syntax errors, missing imports, wrong dependency |

### Stage 2 — Parallel Fast Checks

| Job | What It Checks | Common Failure Cause |
| :--- | :--- | :--- |
| **Tests + 95% Coverage** | All tests pass; ≥95% line coverage (overall + changed files) | Failing test, or new code without enough tests |
| **SpotBugs + FindSecBugs** | Bug patterns: resource leaks, null derefs, injection, weak crypto | Unclosed AST resource, potential NPE, security anti-pattern |
| **CodeQL SAST** | Security vulnerabilities in *our* code (path traversal, injection) | Unsafe handling of untrusted input |
| **License Compliance** | No forbidden (copyleft: GPL/AGPL) dependency licenses | Added a dependency with a GPL license |
| **IaC Scan (Checkov)** | Dockerfile & K8s manifest misconfigurations | Container runs as root, missing resource limits |
| **OWASP Dependency Check** | Known CVEs in third-party dependencies (CVSS ≥ 7.0) | Using a library version with a known vulnerability |

### Stage 3 — Build Artifact

| Job | What It Checks | Common Failure Cause |
| :--- | :--- | :--- |
| **Build JAR + Docker Image** | App packages into a runnable JAR + Docker image builds | Dockerfile error, bootJar packaging issue |

### Stage 4 — Slow / Expensive

| Job | What It Checks | Common Failure Cause |
| :--- | :--- | :--- |
| **Trivy Image Scan** | CVEs in the Docker image OS packages & libraries | Vulnerable base image or bundled library |
| **SBOM Generation** | Generates Software Bill of Materials; Grype scans it | High-severity CVE found in the dependency tree |
| **Mutation Testing (PITest)** | Test *quality* — do tests actually assert? (≥70% kill rate) | Tests run code but don't assert on the results |
| **Claude AI Review** | AI reviews the diff for architecture/security issues | A `CRITICAL` or `MUST_FIX` finding (see severity table below) |

### Stage 5 — Final Gate

| Job | What It Checks |
| :--- | :--- |
| **✅ CI Success Gate** | Aggregates all 15 jobs. Fails if any upstream job failed. |

---

## 🤖 Claude AI Review — Severity Tiers

The AI reviewer classifies findings into four tiers. **Only CRITICAL and MUST_FIX block the merge.**

| Severity | Meaning | Blocks Merge? |
| :--- | :--- | :---: |
| 🔴 **CRITICAL** | Breaks prod, leaks data, security compromise (secret exposure, tenant bypass, memory leaks, race conditions) | ❌ Yes |
| 🟠 **MUST_FIX** | Serious issue to resolve before merge (missing error handling, N+1 queries, missing validation) | ❌ Yes |
| 🟡 **MINOR** | Low-impact issue (small perf, missing logs, naming) | ⚠️ No (advisory) |
| 🔵 **SUGGESTION** | Optional improvement / best practice | ℹ️ No (advisory) |

The reviewer posts **inline comments** on the exact lines. Resolve all
CRITICAL/MUST_FIX findings, then re-push to re-trigger the review.

---

## 🔧 How to Debug Failures

### Step 1: Find the Failing Job

1. Open your PR → scroll to the **checks** section at the bottom.
2. Click **"Details"** next to the ❌ failing check.
3. This opens the GitHub Actions log. The failing **step** is highlighted red.

### Step 2: Read the Logs

Expand the red step. The error is usually near the **bottom** of the output.

### Step 3: Common Failures & Fixes

<details>
<summary>❌ <b>Format (Spotless) failed</b></summary>

**Fix:** Auto-format your code locally, then commit:
```bash
./gradlew spotlessApply
git add .
git commit -m "style: apply spotless formatting"
git push
```
</details>

<details>
<summary>❌ <b>PR Title Lint failed</b></summary>

**Fix:** Edit your PR title to follow Conventional Commits:
- ✅ `feat(ingest): add ZIP bomb protection`
- ✅ `fix(ast): close native Tree resource`
- ❌ `updated code` / `WIP` / `fixes`

Valid types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`
</details>

<details>
<summary>❌ <b>Secret Scan (Gitleaks) failed</b></summary>

**A secret was detected in your commits.**

1. **Remove the secret** from your code (use env vars: `${GEMINI_API_KEY}`).
2. **Rotate the key immediately** — assume it's compromised.
3. If it's a false positive, add it to `.gitleaks.toml` allowlist.
4. If it's in git history, you may need to rewrite history — **ask a maintainer**.
</details>

<details>
<summary>❌ <b>Tests + 95% Coverage failed</b></summary>

**Case A — A test failed:**
Run locally to see which one:
```bash
./gradlew test
# Open the report:
open build/reports/tests/test/index.html
```

**Case B — Coverage below 95%:**
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```
The report highlights uncovered lines in **red**. Add tests for them.
Note: `config/`, `dto/`, `model/`, `entity/`, and the main class are excluded
from the coverage denominator.
</details>

<details>
<summary>❌ <b>SpotBugs failed</b></summary>

**Fix:** Download the SpotBugs report artifact (or run locally):
```bash
./gradlew spotbugsMain
open build/reports/spotbugs/main.html
```
Common issues: unclosed resources (use try-with-resources), potential NPEs,
security patterns flagged by FindSecBugs.
</details>

<details>
<summary>❌ <b>CodeQL / Trivy / OWASP failed (security)</b></summary>

**A vulnerability was found.**

- **CodeQL:** Check the **Security → Code scanning** tab for details on the flagged code.
- **OWASP:** A dependency has a known CVE. Upgrade it, or if it's a false positive,
  suppress it in the OWASP suppression file (ask a maintainer).
- **Trivy:** The Docker image has a CVE. Upgrade the base image or the affected library.

**Never bypass security failures without maintainer sign-off.**
</details>

<details>
<summary>❌ <b>License Compliance failed</b></summary>

**A dependency uses a forbidden (copyleft) license (GPL/AGPL).**

This is a **legal risk** for our proprietary code. Either:
- Find an alternative dependency with a permissive license (Apache/MIT/BSD), or
- If the license is actually permissible, add it to `config/allowed-licenses.json`
  (requires maintainer + legal review).
</details>

<details>
<summary>❌ <b>Mutation Testing (PITest) failed</b></summary>

**Your tests run the code but don't assert on results well enough (< 70% kill rate).**

```bash
./gradlew pitest
open build/reports/pitest/index.html
```
**"Surviving mutants"** = code changes your tests didn't catch. Add stronger
assertions (verify actual return values / side effects, not just that code ran).
</details>

<details>
<summary>❌ <b>Claude AI Review failed</b></summary>

**A CRITICAL or MUST_FIX issue was found.**

1. Read the **inline comments** Claude posted on your PR.
2. Fix each 🔴 CRITICAL and 🟠 MUST_FIX finding.
3. Push your changes — the review re-runs automatically.
4. 🟡 MINOR and 🔵 SUGGESTION findings are advisory (won't block), but consider them.
</details>

<details>
<summary>❌ <b>Build JAR + Docker Image failed</b></summary>

**Fix:** Test the build locally:
```bash
./gradlew bootJar
docker build -t orchestrator-service:test .
```
Check the `Dockerfile` and ensure the JAR builds without test dependencies.
</details>

### Step 4: Re-run a Failed Job

If a failure looks like a **flake** (network timeout, runner issue):
1. Go to the failed check → **"Re-run failed jobs"** (top right).
2. If it fails consistently, it's a real issue — don't keep re-running.

---

## 💻 Running Checks Locally (Before You Push)

Save yourself CI round-trips. Run the full fast gate locally:

```bash
# The "pre-flight" — run this before every push
./gradlew spotlessApply spotlessCheck spotbugsMain test jacocoTestCoverageVerification
```

Individual checks:

```bash
./gradlew spotlessApply              # Auto-fix formatting
./gradlew spotbugsMain               # Bug detection
./gradlew test                       # Unit tests
./gradlew jacocoTestReport           # Coverage report
./gradlew jacocoTestCoverageVerification  # Enforce 95%
./gradlew pitest                     # Mutation testing (slow)
./gradlew checkLicense               # License compliance
./gradlew dependencyCheckAnalyze     # OWASP CVE scan
./gradlew cyclonedxBom               # Generate SBOM
```

> 💡 **Tip:** Add a git pre-push hook to run `spotlessCheck` + `test` automatically.

---

## 🔑 Required Secrets

These are configured at **Settings → Secrets and variables → Actions**.
Team members don't set these — maintainers do.

| Secret | Used By | Notes |
| :--- | :--- | :--- |
| `ANTHROPIC_API_KEY` | Claude AI Review | Required for AI review |
| `NVD_API_KEY` | OWASP Dependency Check | Free, speeds up scans |
| `GITHUB_TOKEN` | Multiple | **Auto-provided** by GitHub |

---

## ❓ FAQ

**Q: My PR passed all checks but I still can't merge.**
A: You also need (1) a Code Owner approval, and (2) all review conversations resolved
(including Claude's inline comments). Check the merge box for what's blocking.

**Q: A check is stuck "Expected — Waiting for status."**
A: An upstream job it `needs:` probably hasn't finished or was skipped. Check Stage 0/1.

**Q: How long does the full pipeline take?**
A: Typically **10–20 minutes**. Stage 4 (mutation/AI/scans) is the slowest part.

**Q: Can I skip CI for a trivial change?**
A: No. All PRs to `main` run the full gate. This is intentional for a service that
handles untrusted customer code.

**Q: The AI review flagged something I disagree with.**
A: MINOR/SUGGESTION findings are advisory — you can proceed. For CRITICAL/MUST_FIX,
discuss with a maintainer. If it's a genuine false positive, they can help override.

**Q: Why did a security scan fail on code I didn't touch?**
A: A dependency you rely on may have had a *new* CVE published. Upgrade it, or a
maintainer will help with a suppression.

**Q: Where do I see the coverage/SBOM/mutation reports?**
A: On the failed/completed run page → scroll to **"Artifacts"** at the bottom → download.

---

## 🆘 Still Stuck?

1. Check this doc's [debug section](#-how-to-debug-failures).
2. Search closed PRs for similar failures.
3. Ask in the team channel or tag a maintainer / Code Owner on the PR.

---

*This pipeline enforces A+ grade, production-ready, secure code. Thanks for keeping
the bar high! 🚀*