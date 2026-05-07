# Design tokens

**Status:** placeholder. The UI/UX Designer writes this file in Phase 0.

## Sections to be written in Phase 0

1. **Color palette.** Primary, secondary, surface, error, plus the corresponding `on*` colors. Computed contrast ratios on every text on background pairing, all passing WCAG 2.1 AA.
2. **Typography scale.** Material 3 type scale, with minimum body text 16sp. Justification for any deviation.
3. **Spacing scale.** 4 / 8 / 16 / 24 / 32 dp. Where each is used.
4. **Tap target sizes.** Minimum 48 dp; larger (64 dp recommended) on test action buttons.
5. **High contrast theme.** Spec for the optional opt in high contrast theme.
6. **Motion.** Reduce motion respected; default animations subtle.

## Notes for the UI/UX Designer

The application is mobile, native Android, Material 3. There is no web frontend, no Claude Design round trip, and no design reference folder at the project root. v1 ships with Material 3 baseline plus the accessibility token set. Custom UI is captured in `future-ideas.md`.

Coordinate with the Accessibility Specialist (`agents/12-accessibility-specialist.md`) on contrast and tap target choices.
