# 0021 — The repository is public
Date: 2026-09-06
Status: accepted (supersedes the private-repository clause of 0011)
Context: GitHub refuses branch protection and rulesets on a free-plan private repository, and Milestone 0 wants main protected by green CI and code-owner review. The choices were GitHub Pro, a public repository, or no enforcement. The code was already MIT and the assets all-rights-reserved (LICENSE), so nothing in the licence had to change.
Decision: the repository is public. Branch protection on main: pull requests required, code-owner review for the paths in .github/CODEOWNERS, stale reviews dismissed, conversations resolved, no force pushes or deletions, the build workflow as a required status check once it exists, administrators not exempt from review but able to push hotfixes.
Consequences: the design documents, the decision log and the spike write-ups are readable by anyone (they contain placeholders, not lore); anything sensitive (server addresses, credentials, the finance of hosting) never enters the repository; assets remain all rights reserved even though they are visible; a rename of the working title becomes a public event.
