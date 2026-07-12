#!/usr/bin/env python3
"""
Claude PR Reviewer — v2
- Per-file chunking (handles large PRs)
- Inline line-level comments via GitHub Review API
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
MODEL             = os.environ.get("CLAUDE_MODEL", "claude-sonnet-4-5")

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
# HTTP helpers with retry/backoff
# ------------------------------------------------------------------
def http_request(url, data=None, headers=None, method="GET", retries=3):
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
            if e.code in (429, 500, 502, 503, 529) and attempt < retries - 1:
                wait = 2 ** (attempt + 1)
                print(f"⏳ HTTP {e.code}, retrying in {wait}s...")
                time.sleep(wait)
                continue
            print(f"❌ HTTP {e.code}: {e.read().decode()}")
            raise
    raise RuntimeError("Max retries exceeded")

# ------------------------------------------------------------------
# Get list of changed files + their per-file diffs
# ------------------------------------------------------------------
def get_changed_files():
    result = subprocess.run(
        ["git", "diff", "--name-only", f"{BASE_SHA}...{HEAD_SHA}"],
        capture_output=True, text=True, check=True,
    )
    files = [f for f in result.stdout.splitlines() if f.strip()]
    # Filter out binaries / ignored types
    return [f for f in files if not f.lower().endswith(IGNORE_EXT)]

def get_file_diff(path):
    result = subprocess.run(
        ["git", "diff", f"{BASE_SHA}...{HEAD_SHA}", "--", path],
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
    try:
        return json.loads(text).get("findings", [])
    except json.JSONDecodeError:
        print(f"⚠️ Could not parse Claude output for {path}, skipping.")
        return []

# ------------------------------------------------------------------
# Post a GitHub Review with inline comments
# ------------------------------------------------------------------
def post_review(all_findings, has_blocker):
    """
    all_findings: list of dicts with keys: path, line, severity, issue, recommendation
    """
    counts = {k: 0 for k in EMOJI}
    inline_comments = []

    for f in all_findings:
        sev = f["severity"]
        counts[sev] = counts.get(sev, 0) + 1
        body = (f"{EMOJI[sev]} **{sev}**\n\n"
                f"**Issue:** {f['issue']}\n\n"
                f"**Fix:** {f['recommendation']}")
        inline_comments.append({
            "path": f["path"],
            "line": int(f["line"]),
            "side": "RIGHT",   # comment on the new version of the code
            "body": body,
        })

    # Summary body
    summary = ["## 🧠 Claude Code Review\n"]
    summary.append("| Severity | Count |")
    summary.append("| :--- | :---: |")
    for sev in ["CRITICAL", "MUST_FIX", "MINOR", "SUGGESTION"]:
        summary.append(f"| {EMOJI[sev]} {sev} | {counts[sev]} |")
    summary.append("")
    if not all_findings:
        summary.append("✅ **No issues found. Great work!**")
    summary.append("\n---")
    summary.append("### ❌ CI BLOCKED — resolve CRITICAL / MUST_FIX before merge."
                   if has_blocker else
                   "### ✅ CI PASSED — no blocking issues.")

    review_body = {
        "commit_id": HEAD_SHA,
        "body": "\n".join(summary),
        "event": "REQUEST_CHANGES" if has_blocker else "COMMENT",
        "comments": inline_comments,
    }

    url = f"https://api.github.com/repos/{REPO}/pulls/{PR_NUMBER}/reviews"
    try:
        http_request(
            url,
            data=review_body,
            headers={
                "Authorization": f"Bearer {GITHUB_TOKEN}",
                "Accept": "application/vnd.github+json",
                "content-type": "application/json",
            },
            method="POST",
        )
        print("✅ Review with inline comments posted.")
    except urllib.error.HTTPError:
        # Fallback: some inline lines may be outside the diff. Post summary only.
        print("⚠️ Inline post failed (line may be outside diff). Posting summary only.")
        http_request(
            f"https://api.github.com/repos/{REPO}/issues/{PR_NUMBER}/comments",
            data={"body": "\n".join(summary)},
            headers={
                "Authorization": f"Bearer {GITHUB_TOKEN}",
                "Accept": "application/vnd.github+json",
                "content-type": "application/json",
            },
            method="POST",
        )

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
        print(f"  🤖 {path}")
        findings = review_file(path, diff)
        for f in findings:
            f["path"] = path
            all_findings.append(f)
        time.sleep(1)  # gentle pacing to avoid rate limits

    has_blocker = any(f["severity"] in BLOCKING for f in all_findings)

    print("💬 Posting review...")
    post_review(all_findings, has_blocker)

    if has_blocker:
        print("❌ Blocker findings. Failing CI.")
        sys.exit(1)
    print("✅ No blockers. Passing CI.")
    sys.exit(0)

if __name__ == "__main__":
    main()