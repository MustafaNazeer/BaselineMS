# Statistical methods, BaselineMS

**Owner:** Biostatistics Reviewer (`agents/17-biostatistics-reviewer.md`).
**Phase:** Phase 5 Part A, Task A2.
**Status:** Locked methodology, written before the user's real walking course recordings arrive. Phase 5 Part B applies these methods unchanged.

This document specifies the statistical methods Phase 5 Part B uses to convert the user's walking course recordings into the validation report and README numbers. It is the single source of truth for how the project computes intraclass correlation coefficients (ICC), error metrics for stride length and cadence, and 95 percent confidence intervals on each. Every quantitative claim in `docs/source/validation-report.md` and in the validation section of `README.md` must trace to a method defined here.

The Biostatistics Reviewer wrote this document with literature anchors in `docs/source/clinical-references.md` and explicit `[BR uncertain]` markers wherever a specific formula or threshold could not be confirmed without re reading the primary source. The Citation Auditor's Phase 5 Part B audit pass will resolve those markers.

## 1. Purpose and scope

### 1.1 What this document is

A peer reviewable methods reference for the gait validation suite (SPEC.md Section 7.2 and Section 9). Phase 5 Part B will reference this document for:

1. The ICC variant choice for test retest reliability across the 5 repeated weekly sessions per participant.
2. The error metric definitions for stride length and cadence vs the measured walking course ground truth.
3. The 95 percent confidence interval methodology for both ICC and the error metrics.
4. The sample size and power caveats that constrain how the achieved numbers may be reported.
5. The structure of the result tables in `docs/source/validation-report.md` and the validation section of `README.md`.

### 1.2 What this document is not

This document does not cover:

1. **Phase 11 beta cohort retention analysis.** Day 1, day 7, day 14, day 30 retention curves on the 10 to 20 person beta cohort (SPEC.md Section 9 layer 5) use a different methods stack (survival analysis or simple proportion estimation with binomial confidence intervals). A separate Phase 11 methods document will lock that in.
2. **Phase 8 voice acoustic feature distribution analyses.** Jitter, shimmer, harmonics to noise ratio, speaking rate, and pause fraction distribution summaries (SPEC.md Section 6.5) have their own methodological choices (mean and SD if approximately normal; median and IQR otherwise) that the Biostatistics Reviewer will append here when Phase 8 starts, in a future Section 7.
3. **Phase 5 synthetic ground truth recovery.** Per fixture stride length and cadence recovery against the synthetic ground truth in `PreCannedFixtures` are already populated by `GaitPipelineIntegrationTest` (Phase 3 close in `STATUS.md`, expanded in Phase 5 Part A Task A1 to cover `slowWalk`, `briskWalk`, and `severeAsymmetry`). Those numbers are point estimates against deterministic synthetic data, not statistical estimates from a sample of human participants, and do not require ICC, error metrics with confidence intervals, or power analysis. The validation report references them directly.

### 1.3 Definition of done for Phase 5 Part B per this document

Phase 5 Part B is methodologically complete when:

1. Every error percentage in the validation report is reported with a 95 percent CI per Section 4.2 of this document.
2. Every ICC value in the validation report is reported with a 95 percent CI per Section 4.1 of this document.
3. The Sample size and power section (Section 5) is referenced in the validation report's Limitations section verbatim, so a future reader can independently audit whether the achieved CI widths support the achieved point estimates.
4. The Citation Auditor has audited every reference cited in this document and resolved every `[BR uncertain]` marker.

## 2. Test retest reliability via ICC

### 2.1 The design

The test retest experiment per SPEC.md Section 7.2 layer 3 and Section 9 layer 3:

- **Participants.** At least 3 people of varying heights. Each participant is the subject of their own ICC calculation.
- **Repeated measurements per participant.** 5 repeated weekly sessions within a short window (the SPEC's Section 9 layer 3 specifies a "short time window"; SPEC Section 7.2 layer 3 cites 48 hours for the older within day variant. Phase 5 will use the weekly variant in Section 9 layer 3 because the application's primary use case is a weekly self administered ritual, and test retest reliability across weeks is the reliability the user actually experiences).
- **Rater.** The application itself, running on the same Android device for the same participant across all 5 sessions. There is one rater (the app), and the rater is held fixed across sessions per participant.
- **Targets of measurement.** Cadence (steps per minute), stride length (meters), and step time variability (coefficient of variation of step times). These are the three features the SPEC names in Section 7.2 as the ICC targets, and the three the README will publish ICC values for.

### 2.2 ICC variant choice

The choice across the six standard ICC variants (Shrout and Fleiss 1979) reduces to three pairs of decisions:

| Decision | Options | Choice for this project |
|----------|---------|------------------------|
| Number of measurements per target | Single (1,1 / 2,1 / 3,1) vs averaged (1,k / 2,k / 3,k) | Single. Each weekly session produces a single value per feature; we do not average within session. |
| Random vs fixed rater | Two way random (2,1) vs two way mixed (3,1) vs one way (1,1) | Two way mixed (3,1). See Section 2.2.1. |
| Absolute agreement vs consistency | Absolute agreement vs consistency | Absolute agreement. See Section 2.2.2. |

**Selected variant: ICC(3,1), absolute agreement.**

#### 2.2.1 Why two way mixed (rater fixed) and not two way random or one way

The ICC(1,1) one way random effects variant assumes each subject is measured by a different rater drawn at random from a population of raters. That does not describe this design: the same app (the same rater) measures every participant across all 5 sessions, so there is no rater to draw at random.

The ICC(2,1) two way random effects variant treats raters as a random sample from a population of raters that we want to generalize to. That description fits a multi human rater study where we want to claim "any rater drawn from the same population would give similar reliability." It does not fit the project's design: the rater is the application, and "the application" is the specific instrument we are measuring reliability of. Phase 5's claim is "this application, on the kind of Android phone supported by SPEC.md Section 12, produces reliable measurements within a participant across 5 weekly sessions." That is a fixed rater claim.

The ICC(3,1) two way mixed effects variant treats raters as fixed and subjects as random. Subjects (participants) are still a random sample from the population of BaselineMS users, but the rater (the app) is the specific instrument under evaluation, not a sample of instruments. This matches the design.

There is a residual question of whether device variance (different Android phones running the app) should be modeled as a separate factor. The Phase 5 Part B test retest experiment holds device fixed within participant (same phone across 5 sessions per participant), so device variance does not enter the ICC. Cross device reliability is a different question, addressed by the Phase 4 close multi device sample rate validation in `docs/observability/sensor-runbook.md` and by the SPEC's documented limitation that voice and vision feature trends are within device, not across devices (per the Clinical Validator Phase 0 sign offs in `docs/source/clinical-references.md` for the Vision Test and the Voice Test). Gait reliability is the question this section addresses; cross device gait validity is outside Phase 5's scope.

#### 2.2.2 Why absolute agreement and not consistency

Absolute agreement asks: "Does session 5 produce the same number as session 1 for the same participant?" Consistency asks: "Does the ranking of participants stay the same across sessions, even if every participant's value shifts up or down by the same amount?"

For the SPEC's stated use case (weekly longitudinal trend tracking on the same person), absolute agreement is what the user cares about. If the application reports a stride length of 1.20 meters in week 1 and 1.30 meters in week 5 for the same person walking at the same pace under the same conditions, that 0.10 meter difference matters whether or not it preserves the ranking against other participants. Consistency would treat that as fine if every participant shifted by the same amount, but a uniform shift like that is itself a reliability problem: it means the application has drifted between sessions.

The SPEC's 0.75 ICC threshold (SPEC.md Section 7.2 and Section 16.2) is therefore an absolute agreement threshold. Reporting consistency ICC alongside is permissible as a sensitivity analysis (it isolates whether the reliability problem is rank instability vs uniform drift), but the primary reported number is ICC(3,1) absolute agreement.

### 2.3 ICC(3,1) absolute agreement: definition and formula

Per Shrout and Fleiss 1979, ICC(3,1) absolute agreement is computed from a two way ANOVA decomposition of the n participants by k sessions data matrix. The variance components are:

- **BMS, between targets mean square.** Variance attributable to participants.
- **JMS, between sessions mean square.** Variance attributable to sessions (the within rater test retest drift).
- **EMS, residual mean square.** Residual error.

For ICC(3,1) absolute agreement (Shrout and Fleiss 1979, formulation labeled `ICC(3,1)` in their Table 2; Koo and Li 2016 Table 4 row "two way mixed, single rater, absolute agreement"):

```
ICC(3,1) absolute agreement
  = (BMS - EMS) / (BMS + (k - 1) * EMS + (k / n) * (JMS - EMS))
```

where n is the number of participants and k is the number of sessions. `[BR uncertain about exact closed form denominator; the absolute agreement variant adjusts the consistency formula by adding the (k/n)(JMS - EMS) term to capture session level systematic differences. Verify against primary source in Phase 5 Part B Citation Auditor pass.]`

The simpler ICC(3,1) consistency formulation (the one many software packages report by default under the name "ICC(C,1)") is:

```
ICC(3,1) consistency
  = (BMS - EMS) / (BMS + (k - 1) * EMS)
```

The two differ exactly by the session level systematic variance term in the denominator. When session level variance is small, the two coincide; when sessions drift systematically, absolute agreement drops below consistency.

### 2.4 Threshold banding

Per Koo and Li 2016 (cited in `docs/source/clinical-references.md` under "Statistical methodology references"), ICC values are conventionally banded as:

| ICC range | Reliability label per Koo and Li 2016 |
|-----------|---------------------------------------|
| ICC < 0.5 | Poor |
| 0.5 ≤ ICC < 0.75 | Moderate |
| 0.75 ≤ ICC < 0.9 | Good |
| ICC ≥ 0.9 | Excellent |

`[BR uncertain about exact phrasing of the four labels; the four band cut points (0.5, 0.75, 0.9) are widely cited from Koo and Li 2016 Section "Interpretation of ICC values" but the Biostatistics Reviewer did not re fetch the primary source in this pass. Verify against Koo and Li 2016 in Phase 5 Part B Citation Auditor pass.]`

The SPEC's success criterion 3 (SPEC.md Section 16.3) requires "ICC above 0.75" on cadence, stride length, and step time variability. That is the Good lower bound per the Koo and Li banding above, and the project's stated target. The validation report reports the achieved point estimate and the 95 percent CI lower bound; if the CI lower bound is below 0.75, the report says so explicitly per the no aspirational claims rule (SPEC.md Section 13 risk row 4 and Phase 5 Task B13 in `docs/plans/phase-5-gait-validation-suite.md`).

### 2.5 Pooling across participants

The 5 repeated weekly sessions are within participant. With at least 3 participants (SPEC.md Section 7.2), the project has at most 3 separate ICC estimates if computed per participant, or one pooled ICC across all 3 participants by 5 sessions (15 measurements total per feature). The pooled estimate is what the validation report publishes, because (a) the SPEC's threshold is a single number per feature, not a per participant number, and (b) per participant ICC with k=5 and effectively n=1 target is not well defined.

The pooled estimate treats the participants as the random subjects in the two way mixed model and the 5 sessions as the fixed factor, which is what ICC(3,1) is designed for. The pooled estimate's 95 percent CI is the Section 4.1 analytic CI applied to the pooled data.

If 4 or more participants enroll (the SPEC says "at least 3"), the pooled estimate uses all participants. The Biostatistics Reviewer recommends reporting the per participant point estimate alongside the pooled estimate as an informational sensitivity analysis (without per participant CIs, since they would be too wide to interpret), so the future reader can see whether one participant dominates the pooled estimate.

## 3. Error metrics for stride length and cadence

### 3.1 The three candidate metrics

For each trial, the recovered application output (stride length in meters, cadence in steps per minute) is compared to the measured ground truth from the 25 meter walking course (video derived step count, total distance walked from the marker at 25 meter end). The three candidate error metrics per trial:

| Metric | Formula | Units | Sign |
|--------|---------|-------|------|
| Mean absolute error (MAE) | `mean over trials of |recovered_i - truth_i|` | Same as the feature (m or steps per minute) | Always positive |
| Root mean square error (RMSE) | `sqrt(mean over trials of (recovered_i - truth_i)^2)` | Same as the feature | Always positive, ≥ MAE |
| Percent error | per trial: `100 * |recovered_i - truth_i| / truth_i` | Percent | Always positive |

Where `recovered_i` is the application's output for trial i and `truth_i` is the measured ground truth for that trial.

The project also reports a per trial signed error (`recovered_i - truth_i`) to a per trial CSV, so a future reader can independently compute bias and 95 percent limits of agreement (Bland and Altman 1986) if desired. The published primary metric is unsigned, however, because the SPEC's stated target is unsigned.

### 3.2 Primary metric: percent error

**Selected primary metric: mean percent error across trials.** This selection is dictated by the SPEC, not by the Biostatistics Reviewer's preference: SPEC.md Section 7.2 layer 2 states "stride length within 5 percent and cadence within 3 percent of measured ground truth," and SPEC.md Section 16.2 restates "within 5 percent on stride length, within 3 percent on cadence." The success criterion is expressed in percent error, so percent error is the primary reporting unit.

Aligning the report's primary metric to the SPEC's success criterion is the unconditional default. Reporting MAE in feature native units (meters and steps per minute) is supplementary information that helps a clinical reader who thinks in those units; reporting RMSE is supplementary information that emphasizes large errors; the headline number that determines pass or fail against SPEC.md Section 16.2 is the mean percent error.

### 3.3 Per trial percent error formula

```
percent_error_i = 100 * |recovered_i - truth_i| / truth_i
```

reported in percent. The denominator is the ground truth (not the mean of recovered and truth, which would be the symmetric percent error variant), because the SPEC's wording "within 5 percent of measured ground truth" anchors on the ground truth.

The mean percent error across N trials is:

```
mean_percent_error = (1 / N) * sum over i of percent_error_i
```

The 95 percent CI on `mean_percent_error` is computed per Section 4.2 of this document.

### 3.4 Pooling across participants and paces

The SPEC's target (SPEC.md Section 7.2 layer 2) is "across at least 20 trials by 3 different people of varying heights" at three self selected paces (slow, normal, brisk). The validation report reports:

- **Headline number per feature.** Pooled mean percent error across all trials by all participants by all paces. This is the number that determines pass or fail against SPEC.md Section 16.2.
- **Per pace breakdown.** Mean percent error split by pace (slow / normal / brisk), so a reader can see whether the pipeline performs differently at different cadences. This is informational; the SPEC does not specify a per pace target.
- **Per participant breakdown.** Mean percent error split by participant. Same reasoning: informational, not a separate target.

The 95 percent CI on the pooled headline number is the primary inference. Per pace and per participant CIs are reported alongside if the trial count per cell supports them (Section 5 discusses what counts as supportive).

### 3.5 Why not Bland Altman alone

Bland and Altman 1986 limits of agreement is a strong agreement analysis when both methods being compared have meaningful measurement error, neither is treated as a true reference, and the goal is to characterize the difference distribution (bias plus 95 percent limits). For the project's question (does the app produce values within a target percent error of a video derived ground truth that the project treats as the truth), the more direct answer is the percent error against the ground truth. Bland Altman is appropriate to report as a supplementary plot in the validation report (so a reader can see whether the residual error is biased or scales with the measurement magnitude), but the primary headline number stays percent error per Section 3.2.

## 4. Confidence interval methodology

### 4.1 95 percent CI on ICC

**Primary method: Shrout and Fleiss 1979 analytic F distribution CI for ICC(3,1).**

Shrout and Fleiss 1979 derives an F distribution based confidence interval for each of their six ICC variants. The general structure for ICC(3,1) consistency is:

```
F_obs = BMS / EMS
F_lower = F_obs / F_critical_upper(alpha/2; df1=n-1, df2=(n-1)(k-1))
F_upper = F_obs / F_critical_lower(alpha/2; df1=n-1, df2=(n-1)(k-1))
ICC_lower = (F_lower - 1) / (F_lower + (k - 1))
ICC_upper = (F_upper - 1) / (F_upper + (k - 1))
```

`[BR uncertain about exact mapping of F_obs to ICC bounds for the absolute agreement variant; the absolute agreement CI in Shrout and Fleiss 1979 uses a different F statistic that incorporates JMS, and the closed form algebra differs from the consistency CI shown above. The Biostatistics Reviewer recommends Phase 5 Part B's Signal Processing Engineer or QA Engineer use a vetted implementation (R's `psych::ICC` or Python's `pingouin.intraclass_corr`) for the ICC(3,1) absolute agreement CI rather than rolling the closed form by hand. Verify against Shrout and Fleiss 1979 primary source in Phase 5 Part B Citation Auditor pass.]`

**Sanity check method: nonparametric bootstrap.**

For the project's planned sample (3 participants by 5 sessions = 15 measurements per feature), the analytic CI is the primary inference because the sample is balanced (every participant has 5 sessions). The Biostatistics Reviewer recommends a percentile bootstrap CI as a sanity check: resample participants with replacement (cluster bootstrap, because the 5 sessions within a participant are not independent) for B = 2000 replicates, recompute ICC(3,1) absolute agreement on each replicate, take the 2.5 percentile and 97.5 percentile of the bootstrap distribution as the 95 percent CI. If the bootstrap CI and the analytic CI disagree by more than the analytic CI's half width, the validation report flags the disagreement and reports both, deferring to the bootstrap as the more defensible interval given the small sample.

### 4.2 95 percent CI on mean percent error

**Primary method: t distribution CI on the mean percent error.**

Per trial percent errors `percent_error_i` for i in 1 to N trials. The mean and standard deviation of the per trial percent errors:

```
mean_percent_error = (1 / N) * sum over i of percent_error_i
sd_percent_error = sqrt((1 / (N - 1)) * sum over i of (percent_error_i - mean_percent_error)^2)
SE = sd_percent_error / sqrt(N)
half_width = t_critical(0.975, df = N - 1) * SE
CI_lower = mean_percent_error - half_width
CI_upper = mean_percent_error + half_width
```

This CI is valid when the per trial percent errors are approximately normally distributed across trials. Two assumption checks the Biostatistics Reviewer requires before reporting the t CI as primary:

1. **Q Q plot of per trial percent errors against a normal distribution.** Visually inspect for heavy tails, skew, or outliers. If the Q Q plot shows obvious non normality, switch to the bootstrap CI.
2. **Trial level independence.** The 20+ trials are spread across 3+ participants, so within participant trials are not independent of each other. The pooled mean percent error CI should be computed with a clustered standard error (cluster on participant), or via a cluster bootstrap. The Biostatistics Reviewer recommends the cluster bootstrap for simplicity.

**Fallback method: percentile bootstrap (cluster bootstrap on participant) when normality fails.**

For B = 2000 bootstrap replicates: resample participants with replacement, take all trials from each resampled participant, compute the mean percent error on the resampled trial set, take the 2.5 and 97.5 percentiles of the bootstrap distribution. The validation report reports whichever CI is primary along with a one line note on which assumption check determined the choice.

### 4.3 Reporting format

Every error percentage and every ICC in the validation report and the README's validation section is reported in the form:

```
<point estimate>, 95% CI [<lower>, <upper>]
```

Example: `Stride length error: 4.2 percent, 95% CI [3.1, 5.3].`

`Example: ICC(3,1) absolute agreement for cadence: 0.82, 95% CI [0.61, 0.93].`

The validation report's table rows that have not yet been measured (Phase 5 Part B is pending) carry `TBD` placeholders per Task A3 of the Phase 5 plan; the placeholders include both the point estimate and the CI cells, so the future session knows to fill both.

## 5. Sample size and power considerations

### 5.1 The planned sample

Per SPEC.md Section 7.2:

- **Walking course validation.** At least 20 trials by at least 3 people of varying heights, at three self selected paces (slow, normal, brisk). Minimum N = 20 trials.
- **Test retest reliability.** 5 repeated weekly sessions per participant. With at least 3 participants, that is 5 sessions per participant by 3 participants = 15 measurements per feature, balanced.

### 5.2 Achievable 95 percent CI half width on mean percent error

For the t distribution CI on the mean percent error with N = 20 trials, df = 19, t critical (0.975, 19) ≈ 2.093. The half width is:

```
half_width ≈ 2.093 * sd_percent_error / sqrt(20)
           ≈ 0.468 * sd_percent_error
```

If the achieved per trial sd of percent errors is approximately 2 percentage points (a plausible value for a well calibrated pipeline against video derived ground truth), the half width is approximately 0.94 percentage points. That is a CI like `4.0 percent, 95% CI [3.1, 4.9]`, which is informative against the 5 percent SPEC target.

If the achieved sd is 4 percentage points (a weaker pipeline or noisier ground truth), the half width is approximately 1.87 percentage points. A point estimate of 4.0 percent then gives `4.0 percent, 95% CI [2.1, 5.9]`, which straddles the 5 percent SPEC target. In that case the validation report reports the point estimate honestly but cannot claim the SPEC target is met with 95 percent confidence; the QA Engineer's Phase 5 sign off (Phase 5 plan Task B13) reports the actual numbers and the Code Reviewer's verdict is APPROVED WITH NOTES rather than REJECTED, per the no aspirational claims rule.

The Biostatistics Reviewer therefore flags: a sample of 20 trials is adequate to estimate mean percent error with a useful CI half width only if the underlying per trial sd is small. If the pipeline is noisy, 20 trials is at the low end of useful. The N = 20 floor in the SPEC is a minimum, not a recommended target; if the user can collect 30 to 40 trials without significant marginal cost, the CI half width shrinks roughly as `1 / sqrt(N)`, so doubling the trials reduces the half width by a factor of about `sqrt(2) ≈ 1.41`.

### 5.3 Achievable 95 percent CI half width on ICC at the 0.75 threshold

For ICC(3,1) absolute agreement with n = 3 participants and k = 5 sessions, the analytic CI's degrees of freedom are df1 = n - 1 = 2 and df2 = (n - 1) * (k - 1) = 8. F critical at alpha = 0.05, df1 = 2, df2 = 8 is approximately 4.46 for the upper tail and the symmetric lower tail value is approximately 0.025, so the F ratio's analytic 95 percent CI spans roughly a factor of `4.46 / 0.025 = 178`. That is an extremely wide F ratio interval, which translates to a wide ICC CI.

`[BR uncertain about the exact ICC CI half width at the 0.75 threshold for n = 3, k = 5 without running the computation; the qualitative claim that the CI is very wide at this sample size is robust, but a future Phase 5 Part B pass should compute the exact CI half width using a vetted implementation (R's `psych::ICC` on a synthetic dataset with point estimate at 0.75) and append the actual CI half width here. The Biostatistics Reviewer's recommendation in advance of that computation is that n = 3 is insufficient to confidently distinguish ICC(3,1) above 0.75 from ICC(3,1) at or below 0.75, and the validation report's Limitations section must say so plainly.]`

**Plain language statement for the validation report's Limitations section:** With 3 participants and 5 sessions per participant, the 95 percent CI on the ICC point estimate is wide enough that the lower bound may fall well below 0.75 even when the point estimate is above 0.75. The validation report reports the point estimate against the SPEC's 0.75 floor honestly, but a reader should treat the point estimate as the central estimate and the CI lower bound as the cautious estimate; declaring the SPEC threshold met requires the CI lower bound to be above 0.75, not just the point estimate. If the CI lower bound is below 0.75, the validation report reports "ICC point estimate above the 0.75 SPEC threshold; CI lower bound below the threshold; recommend additional participants in a future validation pass" and does not declare the threshold met.

### 5.4 Recommendations

The Biostatistics Reviewer flags two underpowered analyses to the PM and the QA Engineer:

1. **Test retest ICC at n = 3 participants, k = 5 sessions.** The 95 percent CI on the ICC point estimate will be wide. The SPEC's at least 3 participants minimum is not refuted, but the project should treat 3 participants as the floor and recruit a 4th and 5th if accessible. Each additional participant reduces the CI half width meaningfully.
2. **Per pace and per participant breakdowns of mean percent error.** With 20 trials split across 3 paces and 3 participants, the average cell holds about 2 trials. A 95 percent CI on a 2 trial mean is not useful. The validation report reports the per pace and per participant breakdowns as point estimates only, with a footnote that the CIs are not reportable at this sample size; the headline number remains the pooled mean percent error across all trials.

These are the Biostatistics Reviewer's standing flags. The role's job is to surface underpowered analyses, not to argue around them.

## 6. What Phase 5 Part B will produce

The validation report at `docs/source/validation-report.md` (scaffolded in Phase 5 Part A Task A3, populated in Phase 5 Part B Tasks B4 and B5) will fill in four result tables. Each table's cells are populated by Phase 5 Part B per the methodology in this document. Until Part B runs, every cell carries a `TBD` placeholder and a one line note pointing at the methods section here.

### 6.1 Table 1: per fixture synthetic ground truth recovery

**Source:** `GaitPipelineIntegrationTest` numbers from the Phase 3 close `STATUS.md` row plus the three new fixtures from Phase 5 Part A Task A1.

**Method reference:** Not statistical estimation; deterministic recovery against synthetic ground truth. No CI applied. See Section 1.2 item 3 for why.

**Cells:** per fixture, recovered cadence, ground truth cadence, cadence percent error, recovered stride length, ground truth stride length, stride length percent error, recovered asymmetry index, ground truth asymmetry index, asymmetry index absolute error.

This table is populated entirely from existing test artifacts; Phase 5 Part B does not alter it.

### 6.2 Table 2: per participant per pace error vs walking course ground truth

**Source:** the 20+ trials at three paces, video derived ground truth per Phase 5 plan Task B2.

**Method reference:** Section 3 (error metrics, percent error primary) plus Section 4.2 (95 percent CI methodology).

**Cells:** per participant by pace, trial count, mean stride length percent error and 95 percent CI, mean cadence percent error and 95 percent CI, plus a pooled headline row across all participants and paces. Per pace and per participant CIs are reported only when the cell trial count is large enough per Section 5.4 item 2; otherwise the cell shows the point estimate with a footnote.

### 6.3 Table 3: ICC for cadence, stride length, and step time variability

**Source:** the 5 repeated weekly sessions per participant per Phase 5 plan Task B1.

**Method reference:** Section 2 (ICC variant: ICC(3,1) absolute agreement) plus Section 4.1 (95 percent CI methodology, analytic primary, bootstrap sanity check).

**Cells:** one row per feature (cadence, stride length, step time variability). Columns: ICC point estimate, 95 percent CI lower, 95 percent CI upper, Koo and Li 2016 banding label, pass / fail against the SPEC.md Section 16.3 0.75 threshold (pass if CI lower bound >= 0.75 per Section 5.3; threshold straddled if point estimate >= 0.75 but CI lower bound < 0.75; fail if point estimate < 0.75).

### 6.4 Table 4: comparison against published MS vs healthy control reference ranges

**Source:** Givon et al. 2009 reference values per `docs/source/clinical-references.md` ("Gait analysis on smartphone IMU" section, attributed to Givon et al. 2009, *Gait Posture* 29(1):138 to 142), plus the project's measured cohort means.

**Method reference:** Section 6.4.1 below. This table is **informational, not diagnostic.**

**Cells:** one row per feature (cadence, stride length). Columns: project's measured cohort mean (from Phase 5 Part B trials), Givon 2009 MS reference value (cadence 94.4 steps per minute, step length 45.3 cm), Givon 2009 healthy control reference value (cadence 115.2 steps per minute, step length 72.1 cm), narrative cell describing where the project's measured mean falls relative to the two published reference values.

#### 6.4.1 Why this table is informational

The validation report does not interpret a participant's cadence or stride length against the Givon 2009 MS vs healthy control values as diagnostic, because:

1. The SPEC explicitly disclaims diagnosis (SPEC.md Section 4 non goal 1 and Section 10 onboarding disclaimer).
2. The Givon 2009 cohort and the project's Phase 5 Part B cohort are different populations (different ages, different clinical severity distributions, different walking environments). A direct comparison is descriptive context, not a clinical inference.
3. The project may not recruit any MS patients in Phase 5 Part B; the SPEC's at least 3 people of varying heights minimum can be satisfied by healthy controls. If the cohort recruits MS patients, the table populates the MS comparison row; if not, the table populates only the healthy control comparison row and notes "MS comparison not available; cohort consisted of healthy controls."

The table's purpose is to give the user (and a future neurologist reading the PDF report) a sense of where the application's outputs fall relative to published norms, framed as descriptive context. The Clinical Outcomes Reviewer will review the narrative cells for clinical literacy in Phase 5 Part B Task B11.

## 7. References

The following references are cited in this document. References 1 and 2 are the load bearing methodological anchors; references 3 to 5 are referenced in passing.

1. **Shrout, P.E. and Fleiss, J.L. 1979.** Intraclass correlations: uses in assessing rater reliability. *Psychological Bulletin* 86(2):420 to 428. The primary methodological anchor for the ICC variant taxonomy and the analytic F distribution confidence intervals.
2. **Koo, T.K. and Li, M.Y. 2016.** A guideline of selecting and reporting intraclass correlation coefficients for reliability research. *Journal of Chiropractic Medicine* 15(2):155 to 163. The primary methodological anchor for the variant selection guidance applied in Section 2.2 and the threshold banding in Section 2.4. Already in the project's bibliography at `docs/source/clinical-references.md` under "Statistical methodology references."
3. **Bland, J.M. and Altman, D.G. 1986.** Statistical methods for assessing agreement between two methods of clinical measurement. *The Lancet* 327(8476):307 to 310. Cited in Section 3.5 as the supplementary plot reference for the validation report's residual analysis. Not the primary metric per Section 3.2.
4. **Givon, U., Zeilig, G., Achiron, A. 2009.** Gait analysis in multiple sclerosis: characterization of temporal-spatial parameters using GAITRite functional ambulation system. *Gait Posture* 29(1):138 to 142. Cited in Section 6.4 as the reference range source for the informational comparison table. Already in `docs/source/clinical-references.md`.
5. **SPEC.md** Sections 7.2, 9, 16. The project specification's stated success criteria and validation methodology. Cited throughout this document as the source of the 5 percent stride length target, the 3 percent cadence target, and the 0.75 ICC threshold.

### 7.1 Citation Auditor Phase 5 Part B audit pending

The following items in this document carry explicit `[BR uncertain]` markers and require Citation Auditor verification in Phase 5 Part B:

1. **Section 2.3 ICC(3,1) absolute agreement closed form denominator.** The Biostatistics Reviewer wrote the formula from familiarity with the Shrout and Fleiss 1979 paper but did not re fetch the primary source. The closed form denominator term `(k / n) * (JMS - EMS)` is conventional but the algebraic form should be verified verbatim against Shrout and Fleiss 1979 Table 2 or the corresponding formula numbers, and the ICC(3,1) absolute agreement label confirmed against Koo and Li 2016 Table 4.
2. **Section 2.4 Koo and Li 2016 four band threshold cut points.** The cut points 0.5, 0.75, 0.9 and the four labels (Poor, Moderate, Good, Excellent) are widely cited but the Biostatistics Reviewer did not re fetch the primary source in this pass. The Citation Auditor should verify the exact wording and exact cut points against Koo and Li 2016.
3. **Section 4.1 ICC(3,1) absolute agreement analytic CI closed form.** The F to ICC bound mapping for the absolute agreement variant differs from the consistency variant. The Biostatistics Reviewer recommends Phase 5 Part B use a vetted implementation (R's `psych::ICC` or Python's `pingouin.intraclass_corr`) for the absolute agreement CI rather than rolling the closed form by hand. The Citation Auditor should still verify the closed form against Shrout and Fleiss 1979 for the project's records.
4. **Section 5.3 ICC CI half width at the 0.75 threshold for n = 3, k = 5.** The Biostatistics Reviewer did not run the computation in this pass. A future Phase 5 Part B pass should compute the exact CI half width using a vetted implementation on a synthetic dataset with point estimate at 0.75, and append the actual CI half width here.

The Bland and Altman 1986 citation in Section 3.5 and reference 3 above is a new reference for the project's bibliography (not previously listed in `docs/source/clinical-references.md`). The Citation Auditor's Phase 5 Part B audit should verify the citation and add it to `docs/source/clinical-references.md` under a new "Statistical methodology references" entry alongside Koo and Li 2016 and Shrout and Fleiss 1979.

Biostatistics Reviewer, Phase 5 Part A Task A2, 2026-05-08.
