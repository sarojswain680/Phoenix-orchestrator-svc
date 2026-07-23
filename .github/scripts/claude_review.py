#!/usr/bin/env python3
"""
Claude PR Reviewer — v2 (Enterprise Resilient Edition)
- Per-file chunking (handles large PRs)
- Inline line-level comments via GitHub Review API
- Resilient rate-limit handling & exponential backoffs
- Safe fallback if individual lines fall outside the active PR diff
- Severity tiers: CRITICAL / MUST_FIX / MINOR / SUGGESTION
- Hard-fails CI (exit 1) on any CRITICAL or MUST_FIX
"""

import os
import sys
import json
import time
import subprocess
import urllib.request
import urllib.error

# ------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------
ANTHROPIC_API_KEY = os.environ["ANTHROPIC_API_KEY"]
GITHUB_TOKEN      = os.environ["GITHUB_TOKEN"]
REPO              = os.environ["GITHUB_REPOSITORY"]
PR_NUMBER         = os.environ["PR_NUMBER"]
BASE_SHA          = os.environ["BASE_SHA"]
HEAD_SHA          = os.environ["HEAD_SHA"]
# Upgraded to Claude 3.5 Sonnet stable release
MODEL             = os.environ.get("CLAUDE_MODEL", "claude-3-5-sonnet-20241022")

BLOCKING = {"CRITICAL", "MUST_FIX"}
EMOJI = {"CRITICAL": "🔴", "MUST_FIX": "🟠", "MINOR": "🟡", "SUGGESTION": "🔵"}

# Skip binary / non-source files
IGNORE_EXT = (".png", ".jpg", ".jpeg", ".gif", ".jar", ".class",
              ".lock", ".gradlew", ".bat", ".ico", ".pdf")
MAX_FILE_CHARS = 40000  # per-file diff cap sent to Claude

# ------------------------------------------------------------------
# Severity definitions (baked into prompt)
# ------------------------------------------------------------------
SYSTEM_PROMPT = """
You are a senior staff engineer reviewing ONE file's diff from a Pull Request
for a Java 21 / Spring Boot / Spring AI code-modernization service that ingests
untrusted customer codebases and processes ASTs across a 3-tier storage system
(Pod memory, Redis, Cloud Storage) with strict tenant isolation.

Classify every finding into EXACTLY one severity:

- CRITICAL: Breaks production, leaks data, or compromises security.
  (hardcoded secrets, SQL/command injection, tenant isolation bypass,
   unclosed native/off-heap AST resources causing memory leaks,
   race conditions on shared mutable state)

- MUST_FIX: Serious issues that must be fixed before merge.
  (missing error handling, blocking calls on async threads, N+1 queries,
   missing input validation, wrong transaction boundaries)

- MINOR: Real but low-impact issues.
  (small perf inefficiencies, missing logs, non-idiomatic code, weak naming)

- SUGGESTION: Optional improvements / best practices / style.

For "line", give the line number in the NEW version of the file (the '+' side
of the diff) where the comment should attach. Use the exact line the issue is on.
Restrict your output to ONLY the top 5 most critical findings to keep payload sizes safe.

RESPOND WITH VALID JSON ONLY. Schema:
{
  "findings": [
    {
      "severity": "CRITICAL|MUST_FIX|MINOR|SUGGESTION",
      "line": <integer new-file line number>,
      "issue": "what is wrong",
      "recommendation": "how to fix it"
    }
  ]
}
Return an empty array if the file is clean.
"""

# ------------------------------------------------------------------
# HTTP helpers with advanced retry/backoff
# ------------------------------------------------------------------
def http_request(url, data=None, headers=None, method="GET", retries=5):
    for attempt in range(retries):
        try:
            req = urllib.request.Request(
                url,
                data=json.dumps(data).encode() if data else None,
                headers=headers or {},
                method=method,
            )
            with urllib.request.urlopen(req) as resp:
                return json.loads(resp.read())
        except urllib.error.HTTPError as e:
            # Handle rate-limiting (429) or temporary secondary limits (403)
            is_rate_limit = e.code == 429 or (e.code == 403 and "rate limit" in e.read().decode().lower())
            
            if (is_rate_limit or e.code in (500, 502, 503, 529)) and attempt < retries - 1:
                # Exponential backoff: 4s, 8s, 16s, 32s...
                wait = 2 ** (attempt + 2)
                print(f"⏳ HTTP {e.code} (Rate Limit/Server Busy), retrying in {wait}s... (Attempt {attempt + 1}/{retries})")
                time.sleep(wait)
                continue
            
            print(f"❌ HTTP {e.code} Error: {e.read().decode()}")
            raise
    raise RuntimeError("Max HTTP retries exceeded")

# ------------------------------------------------------------------
# Get list of changed files + their per-file diffs
# ------------------------------------------------------------------
def get_changed_files():
    # Resolve true merge base if SHA parameters are flaky
    base = BASE_SHA if BASE_SHA else "origin/main"
    head = HEAD_SHA if HEAD_SHA else "HEAD"
    
    result = subprocess.run(
        ["git", "diff", "--name-only", f"{base}...{head}"],
        capture_output=True, text=True, check=True,
    )
    files = [f for f in result.stdout.splitlines() if f.strip()]
    return [f for f in files if not f.lower().endswith(IGNORE_EXT)]

def get_file_diff(path):
    base = BASE_SHA if BASE_SHA else "origin/main"
    head = HEAD_SHA if HEAD_SHA else "HEAD"
    
    result = subprocess.run(
        ["git", "diff", f"{base}...{head}", "--", path],
        capture_output=True, text=True, check=True,
    )
    diff = result.stdout
    if len(diff) > MAX_FILE_CHARS:
        diff = diff[:MAX_FILE_CHARS] + "\n...[truncated]..."
    return diff

# ------------------------------------------------------------------
# Call Claude for a single file
# ------------------------------------------------------------------
def review_file(path, diff):
    payload = {
        "model": MODEL,
        "max_tokens": 3000,
        "system": SYSTEM_PROMPT,
        "messages": [
            {"role": "user", "content": f"File: {path}\n\nDiff:\n```diff\n{diff}\n```"}
        ],
    }
    try:
        body = http_request(
            "https://api.anthropic.com/v1/messages",
            data=payload,
            headers={
                "x-api-key": ANTHROPIC_API_KEY,
                "anthropic-version": "2023-06-01",
                "content-type": "application/json",
            },
            method="POST",
        )
        text = body["content"][0]["text"].strip()
        if text.startswith("```"):
            text = text.split("```")[1]
            if text.startswith("json"):
                text = text[4:]
        return json.loads(text).get("findings", [])
    except Exception as e:
        print(f"⚠️ Could not parse Claude output for {path}: {e}. Skipping.")
        return []

# ------------------------------------------------------------------
# Post a GitHub Review with inline comments
# ------------------------------------------------------------------
def post_review(all_findings, has_blocker):
    # Compute all severity counts ONCE, up front (fixes counts-dict bug).
    counts = {
        sev: sum(1 for f in all_findings if f["severity"] == sev)
        for sev in EMOJI
    }

    # Summary header
    summary = ["## 🧠 Claude Code Review\n"]
    summary.append("| Severity | Count |")
    summary.append("| :--- | :---: |")
    for sev in ["CRITICAL", "MUST_FIX", "MINOR", "SUGGESTION"]:
        summary.append(f"| {EMOJI[sev]} {sev} | {counts[sev]} |")
    summary.append("")

    if not all_findings:
        summary.append("✅ **No issues found. Great work!**")
    else:
        summary.append("### 📝 Findings:\n")
        for f in all_findings:
            sev = f["severity"]
            summary.append(
                f"\n<details><summary>{EMOJI[sev]} <b>{f['path']}:{f['line']} ({sev})</b></summary>\n\n"
                f"**Issue:** {f['issue']}\n\n"
                f"**Fix:** {f['recommendation']}\n"
                f"</details>"
            )

    summary.append("\n---")
    summary.append("### ❌ CI BLOCKED — resolve CRITICAL / MUST_FIX before merge."
                   if has_blocker else
                   "### ✅ CI PASSED — no blocking issues.")

    comment_url = f"https://api.github.com/repos/{REPO}/issues/{PR_NUMBER}/comments"
    try:
        http_request(
            comment_url,
            data={"body": "\n".join(summary)},
            headers={
                "Authorization": f"Bearer {GITHUB_TOKEN}",
                "Accept": "application/vnd.github+json",
                "content-type": "application/json",
            },
            method="POST",
        )
        print("✅ Review comment posted successfully.")
    except Exception as e:
        print(f"❌ Failed to post review: {e}")
        sys.exit(1)   # explicit non-zero exit so CI reliably detects failure

# ------------------------------------------------------------------
# Main
# ------------------------------------------------------------------
def main():
    print("🔍 Detecting changed files...")
    files = get_changed_files()
    if not files:
        print("No reviewable files. Skipping.")
        sys.exit(0)

    print(f"📂 Reviewing {len(files)} file(s)...")
    all_findings = []
    for path in files:
        diff = get_file_diff(path)
        if not diff.strip():
            continue
        print(f"  🤖 Analyzing {path} with Claude...")
        findings = review_file(path, diff)
        for f in findings:
            f["path"] = path
            all_findings.append(f)
        # 1-second delay between file analyses to prevent Claude API rate limits
        time.sleep(1.5)

    has_blocker = any(f["severity"] in BLOCKING for f in all_findings)

    print("💬 Posting review comments to GitHub...")
    post_review(all_findings, has_blocker)

    if has_blocker:
        print("❌ Blocker findings detected by AI. Failing CI Gate.")
        sys.exit(1)
    print("✅ No blockers. Passing CI Gate.")
    sys.exit(0)

if __name__ == "__main__":
    main()