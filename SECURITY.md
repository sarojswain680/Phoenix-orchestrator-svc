# Security Policy

## Our Commitment

The Orchestrator Service ingests and processes **untrusted customer codebases**.
Security is a first-class concern, not an afterthought. We treat every customer's
code and data as confidential and isolated.

---

## 🛡️ Supported Versions

| Version | Supported |
| :--- | :--- |
| Latest `main` | ✅ Yes |
| Older releases | ❌ No (upgrade to latest) |

---

## 🚨 Reporting a Vulnerability

**Please do NOT open a public GitHub issue for security vulnerabilities.**

Instead, report privately via one of these channels:

1. **Preferred:** [GitHub Private Vulnerability Reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
   (Security tab → "Report a vulnerability")
2. **Email:** `security@<your-domain>.com` <!-- REPLACE -->

### What to include
- A clear description of the vulnerability.
- Steps to reproduce (proof of concept if possible).
- Affected component / file / endpoint.
- Potential impact (data leak, RCE, tenant bypass, etc.).

### Our response commitment

| Stage | Timeline |
| :--- | :--- |
| Acknowledgment of report | Within **48 hours** |
| Initial assessment | Within **5 business days** |
| Fix & disclosure coordination | Based on severity (see below) |

---

## 🎯 Severity & Response Targets

| Severity | Examples | Target Fix Time |
| :--- | :--- | :--- |
| 🔴 **Critical** | Tenant isolation bypass, RCE, secret exposure, customer data leak | **24–72 hours** |
| 🟠 **High** | Privilege escalation, injection, auth bypass | **7 days** |
| 🟡 **Medium** | DoS, information disclosure (non-sensitive) | **30 days** |
| 🔵 **Low** | Minor misconfigurations, defense-in-depth | **Next release** |

---

## 🔐 Our Security Controls

We enforce security throughout the SDLC:

- **Secret scanning** — Gitleaks on every PR (`.gitleaks.toml`).
- **Dependency scanning** — OWASP Dependency Check for known CVEs.
- **AI security review** — Automated architecture/security review on every PR.
- **Tenant isolation** — All storage operations are tenant-scoped.
- **LLM data redaction** — Customer code is redacted of secrets before any LLM call.
- **Least privilege** — Services run with minimal required permissions.
- **Input validation** — ZIP safety validation (zip-bomb / path-traversal),
  Git URL validation, and secret scanning on ingested code.

---

## 🤝 Responsible Disclosure

We are committed to working with security researchers. If you report responsibly:
- We will keep you informed of remediation progress.
- We will credit you (with your permission) once the issue is resolved.
- We ask that you give us reasonable time to fix the issue before public disclosure.

**Thank you for helping keep our customers safe.**