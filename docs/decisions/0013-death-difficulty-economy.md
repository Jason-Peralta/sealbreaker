# 0013 — Death drops all coins, gear is kept; one difficulty made scalable
Date: 2026-09-04
Status: accepted
Context: alternatives were vanilla drop-everything, a softer partial coin drop, and two difficulty presets.
Decision: on death the player drops all carried coins as an ordinary pickup anyone can collect and keeps all gear; both are config options. One tuned default difficulty with live-reloadable config scalars (enemy health and damage, coin rates, death coin loss, boss health per participant, crit multiplier).
Consequences: no per-player item protection logic; balance tuning targets the single default.
