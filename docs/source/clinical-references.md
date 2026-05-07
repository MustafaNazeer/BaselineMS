# Clinical references

This file is the annotated source bibliography for every test in the battery and every validation target. Each entry names the reference, summarizes its relevance, and (where applicable) cites the specific number or methodology used in this project.

Citations follow the format used in clinical literature reviews (author year; journal). DOIs are listed where available. The Clinical Validator is responsible for keeping this file accurate; the PM only adds entries on the Clinical Validator's instruction.

## Disease and assessment baseline

### Multiple Sclerosis epidemiology
- Walton et al. 2020, *Multiple Sclerosis Journal*. Global prevalence approximately 2.8 million as of 2020. Used in `SPEC.md` Section 2.

### Expanded Disability Status Scale (EDSS)
- Kurtzke 1983, *Neurology*. The EDSS itself, the standard clinical disability scale in MS. Administered by neurologists in clinic at three to six month intervals. The application aims to fill the gap between EDSS visits with weekly digital measurements.

### FLOODLIGHT (Roche / Genentech research program)
- Montalban et al. 2021, *npj Digital Medicine*, "Smartphone-based remote assessment of MS." The original Floodlight precedent. Research only, locked to specific studies. The MS Neuro Battery aims to be the open community equivalent. Cited in `SPEC.md` Sections 1 and 2.
- Montalban et al. 2024, *Scientific Reports*, "Floodlight Open, an open access global longitudinal smartphone based study of multiple sclerosis: Cohort characteristics and study design." DOI 10.1038/s41598-023-49299-4. Analysis of 1,350 self declared MS plus 1,133 non MS participants from 17 countries, 2018 to 2021. Two findings load bearing for this project:
  - **4 of 7 active tests significantly discriminated MS from non MS after age and sex adjustment**: Information Processing Speed Test (p < 0.001), IPS Digit Digit Test (p < 0.001), Pinching Test (p < 0.001), U Turn Test (p < 0.05). The 3 that did not discriminate after adjustment: Draw a Shape Test, Static Balance Test, and Two Minute Walk Test (2MWT).
  - **The 2MWT did not significantly distinguish MS from non MS in this cohort.** This finding supports the MS Battery design choice to compute richer per stride gait features (stride length, cadence, asymmetry, step time variability, double support time) from a 30 second walk rather than reporting only a global distance metric. Cited in `SPEC.md` Section 7.
- Galati et al. 2024, *JMIR Human Factors* 11:e57033, "User Experience of a Large-Scale Smartphone-Based Observational Study in Multiple Sclerosis: Global, Open-Access, Digital-Only Study." DOI 10.2196/57033. The user experience companion paper to Montalban 2024. Three findings load bearing for this project:
  - **Reminders roughly triple day 30 retention.** US MS cohort, daily assessment series: with reminders enabled (n=172) day 1 65.7%, day 7 32.6%, day 14 30.8%, day 30 30.8%. Without reminders (n=350) day 30 fell to 9.7%. This is the empirical retention floor for this population.
  - **Onboarding redesign moved registration to activation conversion from 53.9% to 74.6%**, a 20 point retention floor that exists before any test runs. Onboarding deserves Phase 0 attention.
  - **Qualitative quit reasons** (Table 4) clustered into: activation and onboarding confusion, forgetting without reminders, competing life demands, physical or contextual barriers (including unsafe locations for the gait test), test burden and repetitiveness, instruction comprehension, and 2MWT scheduling friction specifically. The 2MWT had the lowest day 1 retention of any test (32.34% without reminders, 40.7% with).
- **Used in:** `SPEC.md` Sections 7, 9, 16; `docs/plan.md` Phase 0; `agents/04-ui-ux-designer.md` Phase 0.

## Test specific references

### Bilateral Tap Test (proxy for 9 Hole Peg Test)
- Mathiowetz et al. 1985, *American Journal of Occupational Therapy*. The original 9HPT. Standard EDSS upper limb measure.
- Bays et al. 2015 and follow ups: validation of digital tap-based proxies against the 9HPT in MS. Specifically, alternating bilateral tap rates correlate with 9HPT completion times.
- **Used in:** Phase 2 Tap Test design.

### Gait analysis on smartphone IMU
- Buckley et al. 2020, *Sensors*. Review of smartphone gait analysis methods. Reports stride length error in the 4 to 7 percent range across published methods. Justifies the project's 5 percent target.
- Madgwick 2010, "An efficient orientation filter for inertial and inertial/magnetic sensor arrays." The orientation filter used in Phase 3 of this project. Open source reference implementations exist on GitHub.
- Skog et al. 2010, *IEEE Transactions on Biomedical Engineering*. Zero velocity update (ZUPT) method for stride length integration without GPS or external markers.
- **Used in:** Phases 3, 4, 5.

### Timed 25 Foot Walk (T25FW)
- Cutter et al. 1999, *Brain*. The standard EDSS lower limb measure. The project's gait test does not directly replicate T25FW but produces complementary cadence and stride length numbers.

### Sloan low contrast acuity
- Balcer et al. 2003, *Neurology*. Validation of low contrast acuity at 100, 25, 5, and 1.25 percent for tracking optic neuritis recovery and progression in MS. The contrast levels in `SPEC.md` Section 6.3 come directly from this.

### Symbol Digit Modalities Test (SDMT)
- Smith 1968, *Western Psychological Services*. The original paper SDMT.
- Benedict et al. 2017, *Multiple Sclerosis Journal*. Validation of digital SDMT adaptations in MS. Used to justify the 90 second duration and digital format in `SPEC.md` Section 6.4.

### Voice biomarkers in MS
- Rusz et al. 2018, *Sleep Medicine*, plus subsequent MS specific papers. Acoustic features (jitter, shimmer, harmonics to noise ratio) correlate with bulbar function and global fatigue.
- **Used in:** Phase 8 Voice Test feature selection.

## Statistical methodology references

### Intraclass Correlation Coefficient (ICC)
- Koo and Li 2016, *Journal of Chiropractic Medicine*. Guidelines for selecting and reporting ICC. Used to justify the 0.75 ICC threshold for primary feature test retest reliability in `SPEC.md` Section 9.

## Notes from the Clinical Validator

(This section is appended to by the Clinical Validator during Phase 0 and subsequent phases. Each note dates and signs off on a specific test design or validation target.)

(Empty until Phase 0 begins.)
