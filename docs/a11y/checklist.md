# Accessibility checklist

**Status:** placeholder. The Accessibility Specialist writes this file in Phase 0.

## Criteria

The application is held to WCAG 2.1 AA and Material 3 accessibility guidelines.

1. **Color contrast.** 4.5:1 for normal text, 3:1 for large text. Computed on actual rendered colors, not on token specs.
2. **Dynamic Type.** All screens usable at 200 percent system font scale; no text cut off, no actions obscured.
3. **TalkBack.** Every actionable element has a `contentDescription`. Focus order matches visual order. Announcements occur at meaningful state transitions (test started, test completed, session saved).
4. **Tap target size.** Minimum 48 dp. Test action buttons larger.
5. **Color independence.** Information is not conveyed by color alone (e.g., a chart's "good" vs "bad" state is also distinguished by shape or label, not just color).
6. **Motion.** Animations respect the system reduce motion setting.
7. **Captions and transcripts.** Not applicable to v1 (no video or audio narration in the app).

## Audit log

(Populated by the Accessibility Specialist as each phase ships a screen.)

## Notes for the Accessibility Specialist

Patient population is people with MS, who often have visual impairment, cognitive fatigue, or hand tremor. Treat accessibility as functional: a test the user cannot read or operate is no test at all. Run TalkBack manually before any sign off.
