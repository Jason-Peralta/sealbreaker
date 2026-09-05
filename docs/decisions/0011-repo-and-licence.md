# 0011 — One private repo, one Gradle build, MIT code, ARR assets
Date: 2026-09-04
Status: accepted; module count refined by 0016 ("as many modules as needed", eight is the starting plan, not a rule)
Context: two developers on a private server; independent mod versioning would only add release overhead.
Decision: one Git repository with one Gradle multi-project build and one version for every module; eight modules with the sb_ prefix (core, combat, gear, bosses, world, realms, hub, blocks) plus pack/; private GitHub repo; our code MIT, our assets all-rights-reserved, revisited before any public release. Branching: short-lived branches squash-merged to main; tags at milestone ends; version 0.<milestone>.<build> until v1.0.
Consequences: CI uses cache-provider basic; a rename of the working title is one search-and-replace before Milestone 1 content starts.
