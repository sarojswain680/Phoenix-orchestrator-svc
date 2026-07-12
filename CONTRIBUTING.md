# Contributing to Orchestrator Service

Thank you for contributing! This document defines our branch strategy, commit
conventions, and local development setup. Following these keeps our codebase
**A+ grade, secure, and production-ready**.

---

## 📋 Prerequisites

- **Java 21** (Temurin recommended)
- **Gradle** (use the wrapper `./gradlew` — do not install Gradle globally)
- **Redis** running locally (warm-tier storage)
- **Git**

---

## 🚀 Local Development Setup

```bash
# 1. Clone
git clone https://github.com/sarojswain680/Phoenix-orchestrator-svc.git
cd Phoenix-orchestrator-svc

# 2. Set required environment variables
export GEMINI_API_KEY="your-key"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"

# 3. Start Redis (via Docker if you don't have it locally)
docker run -d -p 6379:6379 --name redis redis:7-alpine

# 4. Build & run tests
./gradlew clean build

# 5. Run the app
./gradlew bootRun
```

> ⚠️ **Never** hardcode secrets. Use environment variables or a local,
> git-ignored `application-local.properties`.

---

## 🌿 Branch Strategy

We follow a **trunk-based, short-lived branch** model.

```
main  ──●────●────●────●──────────►   (protected, always deployable)
         \        \
          feature/ hotfix/           (short-lived, merged via PR)
```

| Branch prefix | Purpose | Example |
| :--- | :--- | :--- |
| `feature/` | New functionality | `feature/zip-ingest-validation` |
| `fix/` | Bug fix | `fix/ast-resource-leak` |
| `hotfix/` | Urgent production fix | `hotfix/tenant-isolation-bypass` |
| `chore/` | Tooling, deps, config | `chore/bump-spring-ai` |
| `docs/` | Documentation only | `docs/update-readme` |
| `refactor/` | Non-functional refactor | `refactor/extract-graph-builder` |

**Rules:**
- Branch off the latest `main`.
- Keep branches **small and short-lived** (< 3 days ideally).
- `main` is **protected** — no direct pushes. All changes go through a PR.
- Rebase on `main` before merging to keep history linear.

---

## 📝 Commit Conventions (Conventional Commits)

We use [**Conventional Commits**](https://www.conventionalcommits.org/). This
enables automated changelogs and semantic versioning.

**Format:**
```
<type>(<optional scope>): <short description>

[optional body]

[optional footer(s)]
```

**Types:**

| Type | Meaning |
| :--- | :--- |
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `style` | Formatting (no logic change) |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `build` | Build system / dependency changes |
| `ci` | CI configuration changes |
| `chore` | Other maintenance |
| `revert` | Reverts a previous commit |

**Examples:**
```bash
feat(ingest): add ZIP bomb protection to ZipSafetyValidator
fix(ast): close native Tree resource in JavaAstAdapter
docs(readme): document Redis Sentinel setup
ci: enforce 95% JaCoCo coverage gate
```

**Breaking changes:** add `!` after the type/scope OR a `BREAKING CHANGE:` footer.
```bash
feat(api)!: change JobResponse schema to include tenantId
```

---

## ✅ Pull Request Process

1. Ensure your branch is up to date with `main`.
2. Run the full local gate before pushing:
   ```bash
   ./gradlew clean build jacocoTestCoverageVerification
   ```
3. Open a PR against `main` and fill out the PR template completely.
4. All **automated gates must pass**:
   - 🛠️ Build & 95% Coverage
   - 🔒 Gitleaks + OWASP Dependency Check
   - 🧠 Claude Architecture Review
5. A **Code Owner** must approve.
6. Resolve all review conversations before merge.
7. **Squash and merge** to keep `main` history clean.

---

## 🧪 Testing Standards

- Every new class with logic requires **unit tests**.
- **Minimum 95% line coverage** (enforced by CI).
- Test naming: `methodName_whenCondition_thenExpectedResult`.
- Prefer JUnit 5 + Mockito. Use `@SpringBootTest` only for integration tests.

---

## 🔐 Security Rules (Non-Negotiable)

Since we ingest **untrusted customer code**:
- Never log or send customer source/data to the LLM **without redaction**.
- Always scope reads/writes by **tenant ID** (tenant isolation).
- Close all native / off-heap AST resources (try-with-resources).
- No hardcoded secrets — ever.

See [SECURITY.md](./SECURITY.md) for our full policy.

---

## ❓ Questions?

Open a [Discussion](https://github.com/sarojswain680/Phoenix-orchestrator-svc/discussions)
or reach out to a maintainer.