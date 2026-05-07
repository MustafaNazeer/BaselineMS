# Compliance Review, MS Neuro Battery

This is a living document maintained by the Compliance Reviewer (agent 21). Every entry records the date, the regulatory regime considered, the artifact reviewed, the specific wording or behavior reviewed, the relevant regulatory text with citation, and a verdict of COMPLIANT, NON COMPLIANT (with recommended remediation), or AMBIGUOUS (needs legal counsel beyond what this agent can decide).

The Compliance Reviewer holds veto power on regulatory drift. The veto can only be overridden by the user.

## Entry 1, Phase 0 initial review

**Date:** 2026-05-07
**Reviewer:** Compliance Reviewer (agent 21)
**Artifacts reviewed:**
- `SPEC.md` (full document, with focus on Section 1 vision, Section 2 problem statement, Section 4 non goals, Section 6 test descriptions, Section 9 validation, Section 10 privacy and safety, Section 13 risks).
- `README.md` (full document, current stub state from the GitHub initial commit).

**Authoritative sources consulted in this session:**
- FDA, "General Wellness: Policy for Low Risk Devices," final guidance, originally issued 2016-07-29, superseded by the 2019 update, reissued on 2026-01-06. Published at https://www.fda.gov/regulatory-information/search-fda-guidance-documents/general-wellness-policy-low-risk-devices. Two factor test and chronic disease language quoted below.
- FDA, "Step 3: Is the Software Function Intended For Maintaining or Encouraging a Healthy Lifestyle?" decision page on the Digital Health Center of Excellence site, https://www.fda.gov/medical-devices/digital-health-center-excellence/step-3-software-function-intended-maintaining-or-encouraging-healthy-lifestyle. (Page returned a transient 404 to the WebFetch tool during this review session; the policy text quoted below is taken from the FDA guidance document and from secondary summaries that quote the guidance verbatim. Re verify before Phase 5 sign off.)
- Google Play, "Health Content and Services" policy, https://support.google.com/googleplay/android-developer/answer/16679511, current as of the 2026-04-15 effective date for the Developer Program Policies.
- Google Play, "Health app categories and additional information," https://support.google.com/googleplay/android-developer/answer/13996367, recognizing three categories: Health and Fitness, Medical, and Human Subjects Research.
- 45 CFR 160.103 (HIPAA definitions), via the HHS HIPAA portal and the Cornell Legal Information Institute mirror at https://www.law.cornell.edu/cfr/text/45/160.103.
- GDPR Article 4 (definitions), https://gdpr-info.eu/art-4-gdpr/.

## 1. Regulatory framing assessment

### 1.1 FDA Software as a Medical Device classification

**Quoted policy text (FDA General Wellness guidance, two factor test):**

> "For purposes of this guidance, CDRH defines general wellness products as products that meet the following two factors: (1) are intended for only general wellness use, as defined in this guidance, and (2) present a low risk to the safety of users and other persons."

> "A general wellness product has (1) an intended use that relates to maintaining or encouraging a general state of health or a healthy activity, or (2) an intended use that relates the role of healthy lifestyle with helping to reduce the risk or impact of certain chronic diseases or conditions and where it is well understood and accepted that healthy lifestyle choices may play an important role in health outcomes."

> "If the product is invasive, involves an intervention or technology that may pose a risk to a user's safety if device controls are not applied (such as risks from lasers, radiation exposure, or implants), raises novel questions of usability, or raises questions of biocompatibility, then the device is not covered by this guidance."

**The 21st Century Cures Act, codified at section 520(o)(1)(B) of the FD&C Act,** statutorily excludes from the medical device definition software functions that are "intended for maintaining or encouraging a healthy lifestyle and is unrelated to the diagnosis, cure, mitigation, prevention, or treatment of a disease or condition."

**Application to MS Battery:**

| Factor | MS Battery position |
|--------|---------------------|
| Invasive? | No. Touchscreen, IMU, microphone, camera. |
| Lasers, radiation, implants? | No. |
| Novel usability or biocompatibility risks? | No. Standard Android sensors. |
| Intended use claims diagnosis, cure, mitigation, prevention, or treatment of MS? | No. Section 4 lists "Diagnose or treat MS or any other condition" and "Replace clinical assessment" as explicit non goals. |
| Intended use is to maintain or encourage a healthy activity (self administered weekly tests for self awareness and to share with a neurologist)? | Yes, when worded carefully. |

**Verdict: COMPLIANT, with specific wording fixes recommended in Section 4 of this review.** The application as designed sits within the General Wellness framework provided certain phrases in `SPEC.md` and `README.md` are tightened. The application does not diagnose, does not treat, does not prescribe, does not replace clinical assessment, does not declare the `INTERNET` permission, does not transmit data off device, and explicitly defers FDA clearance as a non goal. The five tests are positioned as an objective, self administered record that the user can share with a neurologist.

**Floodlight MS (Roche)** ships in 17 countries on iOS and Android without FDA clearance, classifying as wellness, with similar test composition (gait, cognition, vision, hand function). MS Battery's positioning is strictly more conservative than Floodlight's because (a) it does no cloud sync, (b) it does not aggregate population data, and (c) it does not run as a Roche-supervised study. **MS Sherpa (Orikami, Netherlands)** is a CE Class I medical device prescribed by a doctor; MS Battery deliberately avoids that path and `SPEC.md` Section 4 non goal 3 ("Earn FDA clearance in this initial scope") confirms this.

**Note on the 2026-01-06 FDA reissue.** The General Wellness guidance was reissued by FDA on January 6, 2026. Per industry summaries (Faegre Drinker, January 2026; Kendall PC, 2026), the 2026 reissue takes "a broader approach of what constitutes nondevice wellness products than the previous guidance," explicitly extending the framework to "noninvasive wearable products that measure activity, recovery, sleep, pulse, or fitness related biomarkers" when "intended solely for wellness use." The two factor test from the 2016 and 2019 versions remains in force. MS Battery's positioning continues to clear the 2026 framework.

### 1.2 HIPAA scope

**Quoted policy text (45 CFR 160.103):**

> "Covered entity means: (1) A health plan. (2) A health care clearinghouse. (3) A health care provider who transmits any health information in electronic form in connection with a transaction covered by this subchapter."

> "Business associate means, with respect to a covered entity, a person who: (i) On behalf of such covered entity ... creates, receives, maintains, or transmits protected health information for a function or activity regulated by this subchapter."

**Application to MS Battery:**

- MS Battery is not a health plan.
- MS Battery is not a health care clearinghouse.
- MS Battery is not a health care provider. It does not deliver care, does not bill, does not transmit health information in electronic form in connection with a HIPAA transaction (claims, eligibility, payment, etc.).
- MS Battery is not a business associate. There is no covered entity on whose behalf MS Battery creates, receives, maintains, or transmits protected health information. The application stores test results on the user's own device. The user, acting on their own initiative, may export a PDF and share it with their neurologist; that exchange is between two natural persons, the patient and the patient's clinician, and does not put the application or its developer into HIPAA scope.

**Verdict: COMPLIANT.** MS Battery is outside HIPAA scope as currently designed. This conclusion holds as long as: (a) no backend is added that receives data from the application, (b) no contractual arrangement with a covered entity is entered into where MS Battery would receive PHI on the entity's behalf, (c) no functionality is added that transmits health information electronically in connection with a HIPAA transaction.

`SPEC.md` Section 10's claim, "This shape avoids HIPAA scope (the application is not a covered entity or business associate, and never receives data on a server)," is accurate.

### 1.3 GDPR scope

**Quoted policy text (GDPR Article 4):**

> "'personal data' means any information relating to an identified or identifiable natural person ('data subject')"
>
> "'processing' means any operation or set of operations which is performed on personal data or on sets of personal data, whether or not by automated means, such as collection, recording, organisation, structuring, storage, adaptation or alteration, retrieval, consultation, use, disclosure by transmission, dissemination or otherwise making available, alignment or combination, restriction, erasure or destruction"
>
> "'data concerning health' means personal data related to the physical or mental health of a natural person, including the provision of health care services, which reveal information about his or her health status"
>
> "'controller' means the natural or legal person, public authority, agency or other body which, alone or jointly with others, determines the purposes and means of the processing of personal data"

**Application to MS Battery:**

- The application processes special category data within the meaning of Article 9(1) (data concerning health) when it computes and stores test features such as cadence, stride length, jitter, shimmer, SDMT correct count, and Sloan score. That processing happens locally on the data subject's own device.
- Recital 18 of the GDPR ("This Regulation does not apply to the processing of personal data by a natural person in the course of a purely personal or household activity") and Article 2(2)(c) of the GDPR exempt processing carried out by a natural person in the course of a purely personal or household activity from GDPR scope. Self administered tests recorded on the user's own device for the user's own benefit fit that exemption from the data subject's perspective.
- From the developer's perspective, however, the question is whether the developer is a "controller" of personal data. As long as the application does not transmit data off device, does not collect telemetry, does not declare the `INTERNET` permission, and the developer therefore never receives any personal data, the developer is not a controller of any data subject's personal data through the application's normal operation.
- The Phase 11 beta cohort changes this. If a beta tester is in the EU, and the developer collects survey responses, that survey response collection is a processing activity for which the developer is a controller. That activity is governed by GDPR. See Section 3 of this review for the consent text requirements that follow.

**Verdict: COMPLIANT for the application's normal operation. Conditionally compliant for the Phase 11 beta cohort, conditional on the consent text described in Section 3.** No remote processing of personal data occurs through the application. `SPEC.md` Section 10's privacy posture is sufficient. Any wording that implies remote processing or any future telemetry would change this verdict immediately.

### 1.4 Google Play Health Apps policy

**Quoted policy text (Google Play, Health Content and Services):**

> "We don't allow apps with health and medical related functionalities that are misleading or potentially harmful."
>
> Apps offering medical features must include "a clear disclaimer in their app description indicating that the app is 'not a medical device and does not diagnose, treat, cure, or prevent any medical condition.'"
>
> Developers must "remind users to consult a healthcare professional for medical advice, diagnosis, or treatment."
>
> "Apps that are regulated as a medical device must provide proof of approval, clearance or certification by the relevant authority upon request."

**Quoted policy text (Google Play, Health App Categories):**

> Health and Fitness Apps: "Apps that help users manage their health and fitness. These apps usually inform or let users track or sync information about their personal health and fitness" including fitness, nutrition, wellness, and sleep tracking.
>
> Medical Apps: "Apps that provide medical information, resources, or tools to users to enhance medical care, facilitate diagnosis and treatment, and improve overall health outcomes." This category includes "Software as a Medical Device (SaMD) apps regulated by entities like the FDA."

**Application to MS Battery:**

- MS Battery is a Health and Fitness app per Play's category definitions, not a Medical app. It tracks self administered objective test results for the user's personal benefit. It does not provide medical information, does not facilitate diagnosis, and does not facilitate treatment.
- The disclaimer required by Play Health Content and Services policy is already specified in `SPEC.md` Section 10: "This is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on these results. Share with your neurologist for clinical decisions." This is verbatim within the policy's expected disclaimer language.
- The Play Console listing copy, drafted in Phase 11, must mirror this disclaimer text and must not contain any clinical claim. Final Play Console review by the Compliance Reviewer is scheduled for Phase 11.
- The Health apps declaration form on the App content page in Play Console must be completed accurately by the DevOps Engineer in Phase 11. The Compliance Reviewer reviews the declaration before submission.

**Verdict: COMPLIANT, conditional on Phase 11 Play Console listing copy matching the SPEC Section 10 disclaimer and the Health apps declaration being completed honestly.**

## 2. Disclaimer text review

**Proposed text (SPEC.md Section 10, first launch disclaimer):**

> "This is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on these results. Share with your neurologist for clinical decisions."

**Evaluation:**

The proposed text covers:
- Negation of medical device status: "This is not a medical device."
- Negation of diagnostic claim: "It does not diagnose ... any condition."
- Negation of treatment claim: "It does not ... treat any condition."
- Direction not to alter treatment based on the application: "Do not change your treatment based on these results."
- Direction to involve the clinician: "Share with your neurologist for clinical decisions."

This is the minimum required by the Google Play Health Content and Services policy and tracks the FDA non device positioning closely.

**Recommended additions, ranked by priority:**

1. **(Recommended.)** Add an explicit non validation statement: "These results are not validated for any clinical use." This reduces the chance that a user, or a clinician viewing the exported PDF, treats a single number as a clinical measurement. The application's own Phase 5 validation work establishes accuracy against measured ground truth, not clinical fitness for any specific clinical decision; the disclaimer should make that boundary clear.

2. **(Recommended.)** Explicitly direct the user not to make any treatment decision, not just not to "change" treatment. Some users have not yet started treatment. Suggested addition: "Do not start, stop, or change any treatment based on these results."

3. **(Recommended.)** Explicitly point the user to neurologist led care as the primary clinical pathway, not a generic clinician. The current text already says "neurologist," which is the right specialty for MS; keep that specificity.

4. **(Optional, considered.)** A pointer to call the patient's clinician or emergency services in the case of a sudden change in symptoms. This is standard for some health apps but risks crossing into clinical advice itself ("if your symptoms suddenly worsen, call ..."). Recommendation: do not include this in the first launch disclaimer; if a "what to do if symptoms change" pointer is wanted, route the user to public, neutral resources rather than the application itself making the recommendation. Defer this to the Patient Advocate's review.

5. **(Optional, considered.)** A statement that the application is "for personal informational use only." This is conventional in wellness disclaimers and harmless to include.

**Recommended final disclaimer text:**

> "This is not a medical device. It does not diagnose or treat any condition. Results are not validated for any clinical use. Do not start, stop, or change any treatment based on these results. Share results with your neurologist for clinical decisions."

This rewrite is offered for the Documentation Engineer (or Android Engineer when the disclaimer screen is implemented) to consider; the final wording is the user's call. The Compliance Reviewer's veto applies to wording that drifts in the opposite direction (toward clinical claims), not to the user's editorial preferences within the wellness frame.

## 3. Beta cohort consent text scope

The beta cohort runs in Phase 11. Per `SPEC.md` Section 9 item 4, the cohort is 10 to 20 people including a target of 2 to 3 self identified MS patients recruited through an MS support community. The full consent text is Phase 11 work. The Compliance Reviewer specifies the must have elements now so that Phase 11 can draft against a fixed checklist.

**Mandatory elements of the beta cohort consent text:**

1. **Purpose of the beta.** State plainly: gathering feedback on usability, perceived value, and clarity of the report. State explicitly that the beta is not a clinical trial and is not regulated as one.

2. **Status of the application.** Restate the wellness positioning verbatim: "This is not a medical device. It does not diagnose or treat any condition. Results are not validated for any clinical use."

3. **Data on device only.** State that all test data is stored on the participant's own device, that the application does not transmit data to any server, and that the participant's results are visible only to the participant unless the participant chooses to share them.

4. **What the developer receives.** State exactly what the developer receives from the participant: survey responses on usability and value, plus any free text feedback the participant chooses to send. State that no test results, no audio recordings, no IMU traces, and no PDF exports are sent to the developer unless the participant explicitly attaches them to a feedback message.

5. **Right to withdraw.** State that the participant may withdraw at any time, for any reason, without giving a reason, by uninstalling the application and notifying the developer. State that withdrawal incurs no penalty.

6. **Contact information.** Provide a real, monitored email address for the developer. State the response time expectation honestly (for example, "within a week").

7. **No compensation.** State explicitly that the beta is unpaid. Do not imply that participation is a service to the MS community in language that would obligate participation.

8. **No clinical advice from the developer.** State explicitly that the developer is not a clinician, does not provide clinical advice, and cannot interpret the participant's results clinically. Direct any clinical questions to the participant's own neurologist.

9. **GDPR specific elements (if any EU participant joins the cohort).** If even one beta participant is in the EU, GDPR Article 7 conditions for consent apply, plus the lawful basis for processing under Article 6, plus the special category basis under Article 9(2)(a) (explicit consent for processing data concerning health) for any survey response that touches health. The consent must be:
   - Freely given, specific, informed, and unambiguous.
   - Separable from other terms (a single tick box, not bundled with general app terms).
   - Withdrawable as easily as it was given.
   - Documented (the developer must be able to demonstrate that the participant consented).
   The consent text must also list the data subject rights in Articles 15 to 21 (access, rectification, erasure, restriction, portability, objection) and the right to lodge a complaint with a supervisory authority (Article 77).

10. **Risk acknowledgment.** State the realistic risks of beta participation: the application is pre release, may crash, may produce incorrect results, and may not work on every device. Direct the participant not to rely on the application for any clinical purpose during the beta.

11. **Identification.** Identify the developer (Mustafan4x, individual developer, plus an email address). Do not present the project as institutionally backed; it is not.

The full consent text and the Phase 11 review pass on it are deferred to Phase 11. The above is the scope checklist.

## 4. Specific wording drift to fix now

The Compliance Reviewer reviewed `SPEC.md` and `README.md` line by line for wording that drifts toward clinical claims and identified the following items. None are individually fatal; collectively they are worth fixing before Phase 1 begins because the Documentation Engineer will be asked in Wave 2 to finalize the README, and these issues should feed directly into that pass.

### Drift 4.1, README.md line 3, "neurological battery"

**Quoted phrase:**

> "lets people living with Multiple Sclerosis self administer a five test neurological battery once a week"

**Why it drifts:**

"Neurological battery" is conventional clinical terminology for a clinician administered assessment that produces results used in clinical decision making (for example, in a neurologist's exam room or in a clinical trial). Used in user facing copy on a wellness app, the phrase is on the boundary, because it implies clinical equivalence to a neurologist's assessment.

**Recommended replacement:**

> "lets people living with Multiple Sclerosis self administer a short, sensor backed set of five tests once a week"

The phrase "sensor backed" is honest about what the application actually does (uses phone sensors), and "set of five tests" avoids the term of art "battery" if the Documentation Engineer wants the strictest possible wording. If "battery" is retained for brand and project name purposes, prefer "wellness battery" or "self administered battery" over "neurological battery" in user facing prose. The internal `SPEC.md` Section 1 and the project name "MS Neuro Battery" can keep the neuro framing because the spec is an internal document; the user facing README is what a Play reviewer or a regulator would read.

### Drift 4.2, README.md line 5, "validated gait analysis pipeline"

**Quoted phrase:**

> "The technical centerpiece is a validated gait analysis pipeline that turns 30 seconds of phone IMU data into stride length, cadence, step time variability, and stride asymmetry, validated against a measured walking course."

**Why it drifts:**

"Validated" is a term of art with two meanings: (a) engineering validation, meaning the pipeline's outputs match measured ground truth on a known input, and (b) clinical validation, meaning the pipeline's outputs predict a clinical outcome. The Phase 5 validation work in `SPEC.md` Section 7.2 is purely (a). Used without qualification, "validated" can read as (b) to a clinician or a Play reviewer.

The phrase also uses "validated" twice in one sentence, which compounds the problem.

**Recommended replacement:**

> "The technical centerpiece is a gait analysis pipeline that turns 30 seconds of phone IMU data into stride length, cadence, step time variability, and stride asymmetry. Pipeline accuracy is validated against a measured walking course; the validation methodology and achieved error numbers are documented in the project's Validation section."

This rephrasing makes it explicit that what is validated is the pipeline's engineering accuracy, not a clinical claim. It also signposts to the Validation section instead of leaving "validated" as a free standing claim.

### Drift 4.3, SPEC.md Section 2 lines 16 and 18, "tracked", "deteriorate silently", "detected late"

**Quoted phrases:**

> "Disease activity and disability are tracked using the Expanded Disability Status Scale (EDSS), which is administered by a neurologist during clinic visits scheduled every three to six months. Between those visits patients deteriorate silently, with no objective record of how their walking, hand dexterity, vision, cognition, or speech is changing day by day or week by week."

> "The clinical consequence is that subtle relapses or gradual progression are often detected late."

**Why it drifts:**

These phrases sit in the problem statement, which explains why the project exists. They describe a clinical reality (disease activity is tracked clinically; relapses can be missed between visits; progression is silent). They do not, on their face, claim that the application diagnoses or treats. However, the implicit narrative is that the application fills the "tracked" and "detected" gap: the reader is meant to infer that the application offers an objective record that helps catch what is otherwise missed.

The internal SPEC.md is allowed to be more direct about its motivation than the user facing README; SPEC.md is not a Play listing or a marketing surface. **No change is required to `SPEC.md` Section 2 itself.** The drift only matters if these phrases bleed into the README or the Play listing.

**Recommended action:**

- Leave `SPEC.md` Section 2 as is.
- Phase 0 README finalization (Documentation Engineer in Wave 2) **must not** carry the phrases "deteriorate silently," "detected late," or "monitor disease activity" into the README or any user facing surface. The README's framing of the problem should be either (a) silent on disease detection and focused on the patient's experience of self awareness and clinic prep, or (b) phrased in clearly informational terms ("MS clinics typically see patients every three to six months; some people want a record between visits to discuss with their neurologist").

### Drift 4.4, SPEC.md Section 2 line 18, "self administered clinical tool"

**Quoted phrase:**

> "There are research apps in this space, most notably Roche's FLOODLIGHT, but they are study only, locked to specific protocols, and not available to the broader MS community as a self administered clinical tool."

**Why it drifts:**

"Self administered clinical tool" implies that MS Battery is a clinical tool. A clinical tool is a tool used in clinical decision making. The application is positioned as wellness, not as a clinical tool.

**Recommended replacement (for the SPEC.md):**

> "There are research apps in this space, most notably Roche's FLOODLIGHT, but they are study only, locked to specific protocols, and not available to the broader MS community as a self administered self tracking application."

Substituting "self tracking application" for "clinical tool" preserves the comparison to Floodlight without claiming MS Battery is a clinical instrument.

### Drift 4.5, README.md line 9, "Phase 0 (project scaffold) is the next phase"

**Quoted phrase:**

> "Bootstrap complete. Phase 0 (project scaffold) is the next phase."

**Why it drifts (not regulatory; status accuracy):**

This is not a compliance issue but is worth flagging in the same pass: per `STATUS.md`, Phase 0 is in progress (started 2026-05-07), not "the next phase." The Documentation Engineer's Wave 2 README pass should reconcile.

**Recommended replacement:**

> "Phase 0 (bootstrap setup) is in progress. See `STATUS.md` for the current state."

### Drift 4.6, SPEC.md Section 6.3 line 159, "validated for tracking optic neuritis recovery and progression"

**Quoted phrase:**

> "Sloan low contrast acuity, validated for tracking optic neuritis recovery and progression in MS."

**Why it drifts:**

This phrase describes the **clinical literature on the Sloan chart**, not a claim about MS Battery's app. Read in context (Section 6.3 is the clinical basis subsection of the test description), it is accurate: Sloan is in fact validated in MS literature for that purpose. **No change required to `SPEC.md`.** This sentence must not be lifted verbatim into the README without context, because in user facing copy it would read as a claim that the application tracks optic neuritis recovery and progression. The Documentation Engineer should describe what the test does ("a low contrast letter chart") in the README and reserve the clinical pedigree language for the SPEC and the docs/source/clinical-references.md.

### Drift 4.7, SPEC.md Section 6.4 line 167, "sensitivity to MS related cognitive change"

**Quoted phrase:**

> "Strong test retest reliability and sensitivity to MS related cognitive change."

**Why it drifts:**

Same pattern as 4.6: this is a description of the SDMT's clinical pedigree, accurate in context, not a claim that MS Battery detects cognitive change. **No change required to `SPEC.md`.** Same caveat: this language should not be lifted into user facing copy without rephrasing.

### Drift 4.8, SPEC.md Section 7.1.1 line 211, "highly discriminating in the MS literature"

**Quoted phrase:**

> "Per stride features ... are highly discriminating in the MS literature"

**Why it drifts (in context of a derived user facing artifact):**

Same pattern. Accurate description of the literature, allowed in `SPEC.md` and `docs/source/clinical-references.md`, must not bleed into user facing copy as a claim that the application discriminates MS from non MS in any individual user.

### Summary of wording drift

| Item | Artifact | Verdict | Action owner |
|------|----------|---------|--------------|
| 4.1 "neurological battery" | README | NON COMPLIANT for Phase 0 final README | Documentation Engineer in Wave 2 |
| 4.2 "validated gait analysis pipeline" | README | NON COMPLIANT for Phase 0 final README | Documentation Engineer in Wave 2 |
| 4.3 "deteriorate silently," "detected late" | SPEC.md (internal); risk on README | COMPLIANT in SPEC.md, must not propagate to README | Documentation Engineer in Wave 2 |
| 4.4 "self administered clinical tool" | SPEC.md | NON COMPLIANT in SPEC.md; recommend rephrase | PM or Documentation Engineer |
| 4.5 status line | README | Inaccurate, not a compliance issue | Documentation Engineer in Wave 2 |
| 4.6 Sloan clinical pedigree | SPEC.md Section 6.3 | COMPLIANT in SPEC.md, must not propagate to README without context | Documentation Engineer in Wave 2 |
| 4.7 SDMT clinical pedigree | SPEC.md Section 6.4 | COMPLIANT in SPEC.md, must not propagate to README without context | Documentation Engineer in Wave 2 |
| 4.8 per stride features discriminating in MS literature | SPEC.md Section 7.1.1 | COMPLIANT in SPEC.md, must not propagate to user facing copy | Documentation Engineer in Wave 2 |

## 5. Veto register

The Compliance Reviewer will exercise veto power on any of the following changes from this point forward. Each veto can be overridden only by the user (per `agents/21-compliance-reviewer.md` and `docs/plan.md` coordination rules).

1. **Adding any clinical claim to user facing copy.** Includes phrases of the form: "detects MS progression," "monitors disease activity," "tracks your MS," "diagnoses optic neuritis," "predicts relapse," "measures your disability," or any equivalent wording in the application UI, README, Play listing, PDF report, or beta consent text.

2. **Adding the `INTERNET` permission to `AndroidManifest.xml`.** This veto is held jointly with the Security Engineer (agent 07). Adding `INTERNET` would convert the application from a self contained on device wellness application to a connected application, which would invalidate the HIPAA scope analysis in Section 1.2 and require re evaluation under Play Health Apps policy. The veto applies regardless of stated purpose for adding the permission.

3. **Adding cloud sync, telemetry, analytics SDK, or any backend service that receives data from the application.** Same reason as item 2; same joint veto with Security Engineer.

4. **Adding an account, login, or remote identifier system.** Same reason; would put the developer into a controller relationship for personal data and require GDPR compliance work that is currently scoped out.

5. **Suggesting in any user facing surface that the application be used to make treatment decisions.** Includes phrases of the form: "if your stride length drops, talk to your neurologist about adjusting your treatment," or "your test scores indicate disease activity is increasing." The application is allowed to display objective results; it is not allowed to recommend treatment changes.

6. **Aggregating user data into population statistics that are then displayed back to the user as comparisons.** "Your stride length is in the bottom 25 percent of MS patients your age" implies a clinical comparison and is closer to a clinical claim. Reference range overlays (Phase 9) on charts must be drawn from published norms in `docs/source/clinical-references.md` and labeled as informational, not diagnostic, with a visible disclaimer on each chart.

7. **Allowing the PDF report to imply diagnosis or recommend action.** The PDF report (Phase 10) is a record of objective measurements over time. It can include reference ranges from published literature with informational labels. It must not include sentences of the form "your results suggest worsening MS," "consider increasing your dose," "this is consistent with a relapse," or anything else a Play reviewer or an FDA reviewer would read as a clinical interpretation.

8. **Releasing to a Play Store track (internal, closed, open, production) without a final compliance pass on the Play Console listing copy and the Health apps declaration form.** Phase 11 only.

9. **Recruiting EU based beta participants without a GDPR compliant consent text.** See Section 3 above.

10. **Any change in the framing of the application as a clinical, medical, or diagnostic product, in any artifact under the project root.** This is a catch all for cases not enumerated above.

## 6. Coordination notes for Wave 2

The Documentation Engineer (agent 14) is dispatched in Wave 2 to finalize the README and write `docs/architecture.md`. The following items from this review feed directly into that work:

- Apply the rewrites in Section 4.1, 4.2, 4.5 to the README.
- Do not propagate the phrases listed in Section 4.3, 4.6, 4.7, 4.8 from the SPEC.md into the README.
- The recommended final disclaimer text in Section 2 may be used as the on screen disclaimer copy when the Android Engineer implements the first launch disclaimer screen in Phase 1. The Documentation Engineer should consider whether the same wording belongs in the README's Disclaimer section. Today's README Disclaimer section is acceptable but does not include the "results are not validated for any clinical use" line; recommended to add it.
- The Compliance Reviewer is available for a quick review of the finalized README before Phase 0 close.

The PM (agent 00) should consider whether to also dispatch the Compliance Reviewer for a quick re read of `SPEC.md` Section 2 to apply the recommended rewrite in Section 4.4 of this review (replacing "self administered clinical tool" with "self administered self tracking application"). This is a one line edit.

## 7. Open questions and uncertainty

The Compliance Reviewer flags the following items where certainty was not reached and judgment is deferred to the user or to Phase 11:

1. **The 2026-01-06 FDA General Wellness reissue.** The FDA's own page returned a 404 to the WebFetch tool during this review session. The two factor test and the chronic disease language quoted in Section 1.1 of this review are taken from the FDA guidance text as historically published and from secondary summaries that quote it verbatim. The 2026 reissue is reported by industry summaries (Faegre Drinker, Kendall PC) to broaden the wellness scope rather than narrow it, which strengthens MS Battery's position rather than weakening it. The Compliance Reviewer recommends a re fetch of the FDA page before the Phase 5 sign off and again before the Phase 11 sign off, in case the reissue contains specific examples or new restrictions that affect MS Battery's positioning. Stale recollection is not an acceptable basis for a Phase 11 sign off.

2. **EU residency of beta participants.** The Phase 11 beta cohort plan in `SPEC.md` Section 9 does not specify recruitment geography. If the MS support community used for recruitment is global, EU participants are likely. The full GDPR consent obligations in Section 3 item 9 of this review activate as soon as one EU participant joins. The PM should decide ahead of Phase 11 whether to (a) restrict beta recruitment to non EU participants and document that restriction, (b) prepare the GDPR compliant consent text for any participant, or (c) have the Compliance Reviewer flag any specific recruitment channel before it is used.

3. **State law overlay in the United States.** Some US states (notably California with the CCPA and CMIA, Washington with the My Health My Data Act, and Connecticut, Colorado, Utah, Virginia with their respective privacy acts) have health data laws that are stricter than HIPAA and that apply to entities outside HIPAA scope. The Compliance Reviewer has not done a state by state review in this Phase 0 pass. As long as the application processes data only on the user's own device and the developer receives no personal data, the state law exposure is limited; but the moment the beta cohort survey responses are collected, US state privacy laws may apply. Recommend a focused state law review before Phase 11.

4. **The PDF export and the user's neurologist.** When the user shares the PDF with a neurologist, the neurologist may be a HIPAA covered entity. The neurologist's receipt of the PDF is not a HIPAA covered transaction (it is a patient providing the patient's own information to the patient's own provider). The application is not in the loop. This is the conventional analysis and the Compliance Reviewer is confident in it. Flagging it here for the record so that future reviewers do not raise it as a new question.

5. **The "Clinical basis" subsection in each Section 6 test description.** These are accurate descriptions of the published clinical literature for each test, not claims about MS Battery. They are appropriate in `SPEC.md` (an internal design document) and would also be appropriate in a "References" or "Test design rationale" section of the README that is clearly framed as referencing the literature, not as making a claim about the application. If the Documentation Engineer wants to include this material in the README, the framing must be: "these tests are designed to mirror the following published clinical instruments," not: "this application detects what the following clinical instruments detect."

## 8. Verdict for Phase 0 close

- **`SPEC.md` Section 4 non goals:** COMPLIANT. The non goals explicitly disclaim diagnosis, treatment, FDA clearance, and replacement of clinical assessment.
- **`SPEC.md` Section 10 privacy and safety:** COMPLIANT. The disclaimer text is sufficient and the privacy posture is sufficient. Recommended additions in Section 2 of this review are non blocking.
- **`SPEC.md` Section 13 risks and mitigations:** COMPLIANT. The mitigation for "Google Play review friction for a health app" correctly identifies wellness positioning and disclaimer presence as the route through review.
- **`SPEC.md` other sections:** Drift items 4.4 (one phrase), 4.6, 4.7, 4.8 are acceptable in the SPEC.md as an internal design document; they must not propagate to user facing surfaces.
- **`README.md` (current stub state):** NON COMPLIANT for Phase 0 final state. Drift items 4.1 and 4.2 require fixing in the Wave 2 Documentation Engineer pass. The current Disclaimer section is acceptable; the recommended addition of the non validation line is non blocking.

**Recommendation to PM:** Phase 0 may close on the SPEC.md side without further changes (or with the optional one line edit in 4.4). Phase 0 may not close on the README side until the Wave 2 Documentation Engineer pass applies fixes 4.1, 4.2, and 4.5, after which the Compliance Reviewer will perform a quick re read.

**Sign off (Phase 0, partial):** The Compliance Reviewer signs off on the regulatory framing of `SPEC.md` Section 4 non goals and Section 10 privacy and safety, conditional on the README revisions described above being completed before Phase 0 closes.

---

End of Entry 1.

## Entry 2, Phase 0 carryover, finalized README re read

**Date:** 2026-05-07
**Reviewer:** Compliance Reviewer (agent 21)
**Artifact reviewed:**
- `/home/mustafa/src/MS-Battery/README.md` (current finalized state, post Wave 2 Documentation Engineer pass).

**Authoritative sources consulted in this session:**
- Entry 1 of this document (the prior Phase 0 review, in particular Section 4 wording drift items and Section 8 Phase 0 close verdict).
- `SPEC.md` Section 4 non goals (no diagnosis, no treatment, no FDA clearance, no cloud sync, no EHR integration) and Section 10 privacy and safety. No new external policy fetches were necessary for this re read because the regulatory frame established in Entry 1 has not changed (the 2026-01-06 FDA General Wellness reissue, the Google Play Health Content and Services policy current as of 2026-04-15, 45 CFR 160.103, and GDPR Article 4 are all the same as during Entry 1, which was authored on the same date). Re fetch is still recommended ahead of Phase 5 sign off and Phase 11 sign off per Entry 1 Section 7 item 1.

## 1. Verification of Wave 2 fixes

The following items from Entry 1 Section 4 were tracked into the finalized README:

| Drift item | Entry 1 verdict | Finalized README state | Verification |
|------------|-----------------|------------------------|--------------|
| 4.1 "neurological battery" in user facing copy | NON COMPLIANT for Phase 0 final README | Line 3 reads "self administer a short, sensor backed set of five tests once a week," matching the recommended replacement. The phrase "neurological battery" no longer appears in the README. The project name "MS Neuro Battery" remains in the title (allowed: project name, not a clinical claim). | FIXED |
| 4.2 "validated gait analysis pipeline" | NON COMPLIANT for Phase 0 final README | Line 5 reads "The technical centerpiece is a gait analysis pipeline ... Pipeline accuracy is validated against a measured walking course; methodology and error numbers documented in the project's Validation section." "Validated" appears once, qualified as engineering validation against a measured ground truth, with a signpost to the Validation section. | FIXED |
| 4.3 "deteriorate silently," "detected late," "monitor disease activity" must not propagate to README | Allowed in SPEC.md, must not appear in README | None of these phrases appear in the README. The Problem section frames the gap in patient experience terms ("a simple, self directed way to keep an objective record ... so that they can bring something concrete to their next neurology appointment") without claiming the application detects what is otherwise missed. | FIXED |
| 4.5 "Phase 0 ... is the next phase" status line | Inaccurate, not regulatory | Line 21 reads "Phase 0 (bootstrap setup) is complete; Phase 1 (Foundation) is the next phase," matching `STATUS.md`. | FIXED |
| 4.6, 4.7, 4.8 SPEC.md clinical pedigree language must not propagate to README | Allowed in SPEC.md, must not appear in README without rephrasing | None of the phrases "validated for tracking optic neuritis recovery and progression," "sensitivity to MS related cognitive change," or "highly discriminating in the MS literature" appear in the README. | FIXED |

## 2. Section by section regulatory drift sweep on the finalized README

The Compliance Reviewer re read every section of the README looking for any new drift introduced during Wave 2 finalization that was not anticipated in Entry 1.

**Title and header (line 1).** "MS Neuro Battery" is the project name. Acceptable per Entry 1 Section 4.1.

**Lead paragraphs (lines 3 to 5).**
- "track results longitudinally on device" uses "track" as in tracking the user's own data points over time, parallel to "track your steps" or "track your sleep," not as in "tracking your MS" or "tracking disease activity." Acceptable wellness framing under FDA General Wellness guidance and Google Play Health and Fitness category.
- "share a clinician facing PDF report" describes the format and intended reader of the export (a PDF that a neurologist can read), not a clinical instrument claim. The PDF is the same artifact that `SPEC.md` Section 8.3 describes; the Compliance Reviewer's veto in Entry 1 Section 5 item 7 applies to PDF *content* (no diagnostic interpretation, no treatment recommendation), not to the framing that the PDF is designed to be readable by a clinician. Acceptable.
- "Pipeline accuracy is validated against a measured walking course; methodology and error numbers documented in the project's Validation section" is the recommended replacement for the prior "validated gait analysis pipeline" phrasing.

**Problem section (lines 7 to 9).** The Problem section reframes the gap as "people living with MS often want a simple, self directed way to keep an objective record of how their walking, hand dexterity, vision, cognition, and speech are changing, so that they can bring something concrete to their next neurology appointment." This is patient experience framing, not disease detection framing. The closing sentence, "MS Neuro Battery aims to fill that gap as a personal record keeping tool, not as a diagnostic instrument," explicitly disclaims diagnostic status. Acceptable, in fact stronger than the minimum required.

**Solution section (lines 11 to 13).** "Each test design mirrors a published clinical instrument; the application is not a substitute for any of those instruments and does not produce clinical interpretations." This is the framing pattern endorsed in Entry 1 Section 7 item 5: "these tests are designed to mirror the following published clinical instruments," not "this application detects what those instruments detect." Acceptable.

**What this is not section (lines 15 to 17).** Reads: "This application is not a medical device. It does not diagnose or treat any condition. Results are not validated for any clinical use. Do not start, stop, or change any treatment based on these results. Share results with your neurologist for clinical decisions." This is verbatim the recommended final disclaimer text from Entry 1 Section 2, including the non validation line and the "do not start, stop, or change any treatment" expansion. Strongest acceptable form. Satisfies Google Play Health Content and Services disclaimer requirements (medical device disclaimer present, direction to consult a healthcare professional present).

**Status section (lines 19 to 21).** Status accurate per `STATUS.md`. No regulatory content.

**How it is built section (lines 23 to 31).** Architecture and pointer index. No regulatory content.

**Privacy section (lines 33 to 35).** Factual statements about technical posture: no INTERNET permission, no cloud sync, no account, no analytics SDK, in memory audio processing, discarded camera frames, user initiated Share Intent. All claims are technically verifiable against the manifest and the source. No HIPAA, GDPR, or FDA implications. Acceptable.

**Validation section (lines 37 to 39).** Reads: "Reserved for the validation numbers produced in Phase 5. Stride length error percent against a measured 25 meter walking course, cadence error percent, and test retest reliability (intraclass correlation coefficient) for the gait pipeline's primary features will be reported here once the experiments are complete." Frames the numbers as engineering accuracy against a measured ground truth, not as clinical validation. ICC is a standard test retest reliability statistic, not a clinical fitness claim. The section title is simply "Validation" rather than "Engineering validation" or "Pipeline accuracy validation"; the section body makes the scope unambiguous in the first sentence (stride length error against a measured walking course is an engineering metric). Acceptable. The Compliance Reviewer notes that future Phase 5 prose written into this section must not drift toward clinical validation language ("clinical accuracy," "diagnostic accuracy," "predictive value for MS progression," etc.). Phase 5 will be reviewed under the post test feedback wording task in `agents/21-compliance-reviewer.md`.

**Retention section (lines 41 to 43).** Frames retention as product engagement (whether the user keeps using the application), not as clinical efficacy. The Galati et al. 2024 figure is cited as an empirical floor for product engagement, not as a clinical outcome. Acceptable.

**Acknowledgments section (lines 45 to 52).** Two Floodlight Open citations. The framing "draw on two published analyses of the Floodlight Open dataset" describes the project's design influences, not clinical equivalence claims. Acceptable.

**Repository section (lines 54 to 56).** GitHub URL only. No regulatory content.

## 3. Verdict

**`README.md` (finalized state):** COMPLIANT for Phase 0 close.

The finalized README:
- Sits cleanly within the FDA General Wellness framework (the application is positioned as wellness or research, not as a Software as a Medical Device; the wording avoids diagnosis, treatment, prevention, cure, and mitigation claims; the disclaimer is verbatim within the policy's expected language).
- Sits cleanly outside HIPAA scope (no claim or hint of any data leaving the user's device).
- Sits cleanly within the GDPR Recital 18 and Article 2(2)(c) household exemption analysis from Entry 1 Section 1.3, since the README does not introduce any backend, account, or remote identifier that would put the developer into a controller relationship.
- Satisfies the Google Play Health Content and Services policy's disclaimer requirements (medical device disclaimer present, direction to consult a healthcare professional present, no medical claims, no clinical claims).
- Categorizes cleanly into Google Play's Health and Fitness category (objective self administered test results tracked for the user's personal benefit), not the Medical category.
- Does not propagate any of the Entry 1 Section 4.6, 4.7, 4.8 clinical pedigree language from `SPEC.md` into user facing copy.

**Phase 0 carryover from Entry 1 Section 8:** CLEARED. The Wave 2 Documentation Engineer pass applied all recommended fixes and introduced no new drift.

## 4. Open items carried forward

The following items from Entry 1 remain open and are tracked for later phases. They are not Phase 0 carryover.

- The 2026-01-06 FDA General Wellness reissue should be re fetched ahead of Phase 5 sign off and Phase 11 sign off (Entry 1 Section 7 item 1).
- The PM should decide ahead of Phase 11 whether to restrict beta recruitment geography or prepare GDPR compliant consent text (Entry 1 Section 7 item 2).
- A focused US state law review (CCPA, CMIA, My Health My Data Act, and other state privacy acts) is recommended before Phase 11 (Entry 1 Section 7 item 3).
- The optional one line edit to `SPEC.md` Section 2 ("self administered clinical tool" rephrased to "self administered self tracking application") from Entry 1 Section 4.4 was not part of this re read scope; verify status with the PM separately if a SPEC.md re read is requested.

## 5. Sign off

**Sign off (Phase 0, README portion):** The Compliance Reviewer signs off on the finalized README. The conditional component of the Entry 1 Phase 0 sign off (the Wave 2 README revisions) is now satisfied. Phase 0 close is COMPLIANT on the README side.

---

End of Entry 2.

## Entry 3, Phase 2 disclaimer copy review, 2026-05-07

**Date:** 2026-05-07
**Reviewer:** Compliance Reviewer (agent 21)
**Phase:** Phase 2 (Tap Test); reviewing a Patient Advocate Phase 1 carryover wording proposal that requires Compliance ratification before it ships.

**Artifact reviewed:**
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/DisclaimerScreen.kt`, lines 30 to 41 (the disclaimer body Text composable and the acknowledgment Button label).
- The Patient Advocate's proposed replacement copy from `/home/mustafa/src/MS-Battery/docs/qa/patient-advocate-reviews.md` Issues 5 and 6 in the 2026-05-07 Phase 1 entry. The on disk Patient Advocate text was verified verbatim against the dispatch text; no discrepancy.

**Scope of this review:**

The PM has separately accepted the structural change in Issue 5 (chunk one centered paragraph into three left aligned `Text` lines with `TextAlign.Start`). The structural change is independent of regulatory framing. This review covers only:

1. The wording change in the third sentence: replacing "Share with your neurologist for clinical decisions" (current `DisclaimerScreen.kt`, copied verbatim from `SPEC.md` Section 10) with "When you visit your neurologist, you can share these results to help the conversation."
2. The button copy change in Issue 6: replacing "I understand" with "Got it, continue".

**Specific wording reviewed:**

Current `DisclaimerScreen.kt` body (lines 31 to 33):

> "This app is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on these results. Share with your neurologist for clinical decisions."

Current `DisclaimerScreen.kt` button (line 40):

> "I understand"

Patient Advocate proposed body (Issue 5, third sentence is the load bearing change):

> This app is not a medical device. It does not diagnose or treat any condition.
>
> Please do not change your treatment based on what you see here.
>
> When you visit your neurologist, you can share these results to help the conversation.

Patient Advocate proposed button (Issue 6):

> "Got it, continue"

**Authoritative sources consulted in this session:**

- FDA, "General Wellness: Policy for Low Risk Devices," final guidance, reissued 2026-01-06. The FDA's own landing page at https://www.fda.gov/regulatory-information/search-fda-guidance-documents/general-wellness-policy-low-risk-devices and the guidance PDF at https://www.fda.gov/media/90652/download both returned 404 to WebFetch on this date, the same transient state Entry 1 Section 7 item 1 of this document recorded on Phase 0. The 2026 reissue text was therefore triangulated from three independent industry summaries that quote the guidance verbatim:
  - Faegre Drinker Biddle and Reath LLP, "Key Updates in FDA's 2026 General Wellness and Clinical Decision Support Software Guidance," https://www.faegredrinker.com/en/insights/publications/2026/1/key-updates-in-fdas-2026-general-wellness-and-clinical-decision-support-software-guidance, accessed 2026-05-07.
  - King and Spalding, "FDA Updates General Wellness and Clinical Decision Support Guidance Documents," https://www.kslaw.com/news-and-insights/fda-updates-general-wellness-and-clinical-decision-support-guidance-documents, accessed 2026-05-07.
  - Covington and Burling LLP, "FDA Issues Revised Guidance on General Wellness Products," https://www.cov.com/en/news-and-insights/insights/2026/01/fda-issues-revised-guidance-on-general-wellness-products, accessed 2026-05-07.
- Google Play, "Health Content and Services" policy, https://support.google.com/googleplay/android-developer/answer/16679511, accessed 2026-05-07. (The Entry 1 fetch was on 2026-04-15 effective date; today's fetch returns the same disclaimer language quoted in Entry 1 Section 1.4.)
- Google Play, "Health app categories and additional information," https://support.google.com/googleplay/android-developer/answer/13996367, accessed 2026-05-07.
- 45 CFR 160.103 (HIPAA definitions). Not re fetched in this session; the Entry 1 Section 1.2 analysis still applies and is unchanged by a wording tweak that does not affect data flow.

**Re fetch note:** Entry 1 Section 7 item 1 already flagged the FDA URL as transiently 404 and recommended re fetch ahead of Phase 5 and Phase 11 sign offs. The 2026-01-06 reissue's text on healthcare provider notifications (quoted below) is consistent across all three industry summaries consulted today, which is sufficient to ground a Phase 2 wording verdict. A direct fetch from FDA is still required before Phase 5 sign off.

## 1. Regulatory regime considered

The wording change is evaluated against:

- **FDA General Wellness: Policy for Low Risk Devices, 2026-01-06 reissue.** This is the controlling federal regime for whether the application crosses the line from non device wellness into Software as a Medical Device.
- **Google Play Health Content and Services policy.** This is the controlling distribution regime for whether the application clears Play review.
- **HIPAA scope.** Considered for completeness; not affected by a wording tweak that does not move data off device. No new analysis is needed; Entry 1 Section 1.2 conclusion stands.

GDPR is not implicated by an in app disclaimer wording change; the disclaimer is presented to the user, no personal data leaves the device, no controller relationship arises from rendering the text. Entry 1 Section 1.3 conclusion stands.

## 2. Relevant regulatory text with citation

### 2.1 FDA two factor test for general wellness products

Quoted in Entry 1 Section 1.1 from the 2016 and 2019 versions of the guidance, and confirmed unchanged in the 2026-01-06 reissue per the King and Spalding 2026 summary:

> "(i) the device is intended only for general wellness use, and (ii) the device presents a low risk to the safety of users."

(Source: King and Spalding, "FDA Updates General Wellness and Clinical Decision Support Guidance Documents," accessed 2026-05-07.)

### 2.2 FDA 2026 reissue, healthcare professional notification clause

This is the load bearing new clause for the Phase 2 question. Quoted from the King and Spalding 2026 summary, which quotes the guidance directly:

> "may notify a user that evaluation by a healthcare professional may be appropriate"

Such a notification must not:
> "identify a specific disease or condition; characterize output as abnormal or diagnostic; include clinical thresholds or treatment recommendations; provide ongoing alerts intended to manage disease."

(Source: King and Spalding, accessed 2026-05-07. The Faegre Drinker 2026 summary phrases the same constraint as: "limited notifications suggesting professional evaluation allowed only if they do not reference specific diseases or diagnostic thresholds." The Covington 2026 summary phrases it as: "a notification informing a user that evaluation by a healthcare professional may be helpful when outputs fall outside ranges appropriate for general wellness use" with the same four constraints. All three independent summaries agree on the substance.)

### 2.3 FDA 2026 reissue, scope of enforcement discretion

> "Enforcement discretion does not apply if the device is intended for the management of a disease or condition."

(Source: King and Spalding, accessed 2026-05-07.)

### 2.4 21st Century Cures Act, section 520(o)(1)(B) of the FD&C Act

Quoted in Entry 1 Section 1.1 and unchanged:

> "intended for maintaining or encouraging a healthy lifestyle and is unrelated to the diagnosis, cure, mitigation, prevention, or treatment of a disease or condition."

### 2.5 Google Play Health Content and Services policy

Quoted in Entry 1 Section 1.4 and reconfirmed via fetch on 2026-05-07:

> Apps offering medical features must include "a clear disclaimer in their app description indicating that the app is 'not a medical device and does not diagnose, treat, cure, or prevent any medical condition.'"

> Developers must "remind users to consult a healthcare professional for medical advice, diagnosis, or treatment."

(Source: Google Play, "Health Content and Services," accessed 2026-05-07.)

### 2.6 Google Play Health App Categories

Quoted in Entry 1 Section 1.4 and reconfirmed via fetch on 2026-05-07:

> Health and Fitness Apps: "Apps that help users manage their health and fitness. These apps usually inform or let users track or sync information about their personal health and fitness."

(Source: Google Play, "Health app categories and additional information," accessed 2026-05-07.)

## 3. Analysis of the proposed sentence 3 wording

The original sentence: "Share with your neurologist for clinical decisions."

The proposed sentence: "When you visit your neurologist, you can share these results to help the conversation."

**Direction of drift, FDA framing.** The proposed sentence is regulatorily *safer* than the original, not riskier. The phrase "for clinical decisions" in the original explicitly couples the application's outputs to clinical decision making. The 2026 FDA reissue's enforcement discretion clause (Section 2.3 above) is forfeited if the application is "intended for the management of a disease or condition," and "for clinical decisions" is a phrase that argues, on the face of the app, that the outputs are intended as inputs to clinical decisions. The Phase 0 review (Entry 1 Section 1.1) accepted the original wording as compliant because it sat in a screen full of explicit non device disclaimers and the cumulative posture clearly framed the application as wellness, but the phrase itself pulls in the medical device direction.

The proposed sentence does the opposite: it reframes the role of the export from "input to a clinical decision" to "input to a conversation." A conversation is not a medical decision. Neither the 2016, 2019, nor 2026 versions of the FDA General Wellness guidance treat conversational support between a patient and a clinician as a regulated function. The 2026 reissue's specific list of forbidden notification properties (Section 2.2 above) does not apply at all to a static disclaimer line, because a disclaimer line is not a sensor output notification; but even read as if it did, the proposed sentence does not identify a disease or condition, does not characterize an output as abnormal or diagnostic, does not include clinical thresholds, and does not provide ongoing alerts. It is, by every test in the 2026 reissue, further from the medical device boundary than the original.

The proposed sentence also fits comfortably inside the 21st Century Cures Act exclusion (Section 2.4 above) for software functions that maintain or encourage a healthy lifestyle and are unrelated to diagnosis, cure, mitigation, prevention, or treatment. Helping a patient bring a record to a neurology visit and discuss it is a healthy lifestyle activity (clinical follow through). It is not diagnosis, cure, mitigation, prevention, or treatment.

**Direction of drift, Google Play framing.** Play's Health Content and Services policy requires (Section 2.5 above) that the app "remind users to consult a healthcare professional for medical advice, diagnosis, or treatment." The proposed sentence directs the user to share results when they visit their neurologist. Visiting a neurologist is consulting a healthcare professional. The proposed wording satisfies the Play "remind to consult" requirement; it is arguably softer in tone than the original, but Play's policy does not specify a mandatory phrase or prescribed strength of language (verified via re fetch of the policy page on 2026-05-07; only the "not a medical device" disclaimer phrase is quoted as required wording in the policy text). The "consult a healthcare professional" requirement is satisfied by direction, not by any specific verb. "When you visit your neurologist, you can share these results" is a direction.

**Categorization stability under Play.** The application remains in the Health and Fitness category (Section 2.6 above) under the proposed wording. The proposed sentence does not introduce medical information, does not facilitate diagnosis, and does not facilitate treatment. The "help the conversation" framing actually reinforces Health and Fitness positioning by characterizing the artifact as supporting the user's personal health management, not as a clinical instrument.

**HIPAA framing.** No data flow change. Entry 1 Section 1.2 conclusion is unaffected.

**One drafting concern, minor.** The proposed sentence loses the explicit cue that clinical decisions are the neurologist's province, not the application's. The original sentence "Share with your neurologist for clinical decisions" implicitly says clinical decisions belong to the neurologist; the proposed sentence drops that pointer. The rest of the disclaimer (sentence 1: "not a medical device. It does not diagnose or treat any condition." Sentence 2: "Please do not change your treatment based on what you see here.") still establishes the boundary, so this is not a regulatory failure; it is a defense in depth observation. The Compliance Reviewer is satisfied that sentences 1 and 2 carry the regulatory weight; sentence 3 is permitted to be conversational.

**Recommendation if the PM wants belt and suspenders.** Two acceptable alternatives that retain the warmth of the proposed wording while preserving the explicit pointer:

- (a) "When you visit your neurologist, you can share these results to help the conversation. Clinical decisions belong with your neurologist."
- (b) "When you visit your neurologist, you can share these results to help the conversation about your care."

Both are compliant. The proposed Patient Advocate wording is also compliant on its own. The choice between the bare proposed wording and an augmented variant is editorial, not regulatory; the Compliance Reviewer does not assert a preference, only that any of the three options is regulatorily acceptable.

## 4. Analysis of the proposed button copy change

Original button label: "I understand."

Proposed button label: "Got it, continue."

Neither phrase is a regulated artifact. There is no FDA, HIPAA, GDPR, or Google Play requirement that the acknowledgment button on a wellness app's first launch disclaimer use any specific verb. Both phrases acknowledge the user has read the disclaimer and is proceeding. Both are equally acceptable to a Play reviewer. The choice is purely editorial.

The Compliance Reviewer notes one trivial observation: "Got it, continue" reads a fraction more cheerful than "I understand," which slightly reduces the felt seriousness of the screen. This is not a regulatory concern. The screen's gravity is established by the disclaimer body, not by the button label. The Patient Advocate's stated rationale (that "Got it, continue" acknowledges reading and confirms forward motion in one phrase, and is warmer for fatigued users) is a usability call, which is the Patient Advocate's domain. The Compliance Reviewer defers to that judgment.

## 5. Verdict

**Sentence 3 wording change ("for clinical decisions" replaced with "to help the conversation"): COMPLIANT.**

The proposed wording sits cleanly inside the FDA General Wellness 2026 reissue framework, satisfies the Google Play Health Content and Services policy's "remind to consult a healthcare professional" requirement, leaves the application in the Health and Fitness Play category, and does not touch HIPAA scope. The proposed wording is regulatorily *softer* (further from the medical device boundary) than the original, not stricter. The Compliance Reviewer signs off on the proposed sentence 3.

**Button copy change ("I understand" replaced with "Got it, continue"): COMPLIANT.**

No regulatory regime governs the verb used on an acknowledgment button. The choice is editorial. The Compliance Reviewer signs off on either phrase; the Patient Advocate's rationale for "Got it, continue" is reasonable and unobjectionable.

**Phase 2 sign off scope.** This sign off covers only the two wording items above. The Compliance Reviewer has not in this entry reviewed the Phase 2 tap test post test feedback copy or any other Phase 2 user facing surface; those will be reviewed under the per phase post test feedback wording task in `agents/21-compliance-reviewer.md` when they are produced.

## 6. Decisions for PM review

1. **Which sentence 3 wording to ship.** Three options are compliant: the bare Patient Advocate proposal ("When you visit your neurologist, you can share these results to help the conversation."), augmented variant (a) ("...to help the conversation. Clinical decisions belong with your neurologist."), or augmented variant (b) ("...to help the conversation about your care."). The Compliance Reviewer does not prefer any of the three; the choice is editorial.

2. **Which button copy to ship.** Both "I understand" and "Got it, continue" are compliant. The Patient Advocate prefers "Got it, continue." The Compliance Reviewer defers to that preference.

The Android Engineer, when implementing in Phase 2B, can ship the bare Patient Advocate proposal for sentence 3 plus "Got it, continue" for the button without further Compliance Reviewer review, or, if the PM elects an augmented sentence 3 variant, the variant text in Section 3 of this entry is pre approved as written.

## 7. Open items and uncertainty

1. **FDA URL state.** As in Phase 0, the FDA General Wellness landing page and the guidance PDF returned 404 today. The 2026-01-06 reissue text was triangulated from three independent law firm summaries that quote the guidance directly, and all three summaries agree on the substance of the healthcare professional notification clause. This is sufficient for a Phase 2 wording sign off because the wording in question is a static disclaimer line, not a sensor output notification, and the analysis does not turn on the new 2026 reissue's specific notification rules. A direct FDA fetch is still required before Phase 5 and Phase 11 sign offs per Entry 1 Section 7 item 1.

2. **No new state law uncertainty introduced.** The wording change does not affect data flow, does not collect new categories of data, and does not create any new relationship between the developer and the user beyond what Entry 1 already analyzed. Entry 1 Section 7 item 3 (US state law overlay) is unchanged.

3. **Beta cohort consent text.** The Phase 11 beta cohort consent text remains scoped to Entry 1 Section 3. No new consent text obligations arise from this disclaimer wording change.

## 8. Confirmation of append only protocol

This entry is a new Entry 3 appended after Entry 2's "End of Entry 2." marker. Entries 1 and 2 were not edited in this session. The compliance review log preserves the full history of regulatory decisions per the agent brief.

---

End of Entry 3.
