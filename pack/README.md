# pack/ — the modpack players install

This directory becomes a [packwiz](https://packwiz.infra.link/) project: a git-friendly TOML description of the pack (loader, our mod jars, library mods, configs, datapack overrides) that packwiz-installer keeps up to date on every client launch and on the server.

It is not initialised yet because packwiz is a separate command-line tool. Milestone 0 step (see `docs/03-roadmap.md`, M0 deliverable 6):

1. Install packwiz from https://github.com/packwiz/packwiz/releases (or `go install github.com/packwiz/packwiz@latest`).
2. In this directory:
   ```
   packwiz init --name "Sealbreaker" --author "Sealbreaker team" --mc-version 26.2 --modloader neoforge --neoforge-latest
   packwiz modrinth add geckolib
   packwiz modrinth add curios
   packwiz modrinth add jei
   ```
3. Our own jars are referenced from GitHub Releases (`packwiz url add ...`) once CI publishes them; until then, drop `sb_*.jar` files into `pack/mods/` as local files.
4. Serve locally with `packwiz serve`, point a Prism Launcher instance at `http://localhost:8080/pack.toml` through the packwiz-installer pre-launch command, and the server start script at the same URL with `-g -s server`.
5. Commit everything here except `.packwiz/` (already ignored).
