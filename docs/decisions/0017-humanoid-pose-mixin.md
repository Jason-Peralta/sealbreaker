# 0017 — One Mixin in sb_core for custom humanoid poses
Date: 2026-09-05
Status: accepted
Context: custom swing poses (a left-to-right sweep, an overhead chop) must be applied after vanilla poses the humanoid model, because `HumanoidModel#setupAnim` resets every part at its start. NeoForge's render-state modifiers run before `setupAnim` and can only carry data; render layers run after the body has been submitted; there is no event at the tail of `setupAnim`. Spike S1 confirmed all three.
Decision: `sb_core` carries a single client Mixin, `HumanoidModelMixin`, injecting at the tail of `HumanoidModel#setupAnim(HumanoidRenderState)` and calling `HumanoidPoseHooks.apply(model, state)`. Content modules register posers through `HumanoidPoseHooks` and never add Mixins of their own. Data for a pose travels on the render state via NeoForge render data (`ContextKey`), set by a render-state modifier.
Consequences: the Mixin rule from CLAUDE.md holds (Mixins only in core, each justified); every humanoid on screen pays one hook call per frame, so posers must be cheap; a version bump re-checks this one injection point. Verified on 26.2.0.76: the Mixin applies and the client runs.
