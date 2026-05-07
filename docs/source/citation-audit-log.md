# Citation Audit Log

This file is an append only audit log produced by the Citation Auditor (agent 16). Each entry records a citation that has been verified against primary source material. Verdicts are one of:

- `confirmed`: the cited paper exists, the citation is correct as written, and the source supports the claim being made.
- `confirmed with revision`: the source exists and supports the claim, but a bibliographic detail (journal, year, volume, pages, DOI, author order) needs correction.
- `replace`: the cited paper does not support the claim, but a different paper does. The replacement is named.
- `remove`: the citation does not support what is claimed of it and no suitable replacement was identified.
- `not found`: a paper matching the citation could not be located. The Citation Auditor exercises veto: this citation must not be added to project artifacts until the underlying paper is identified or the claim is sourced elsewhere.
- `confirmed for inclusion`: the unverified candidate was located and is appropriate to add to the bibliography.

Citation Auditor, Phase 0 audit, 2026-05-07. All web fetches and searches were performed in this session.

## Section A. Existing bibliography entries in clinical-references.md

### A.1. Walton et al. 2020, Multiple Sclerosis Journal (global prevalence 2.8 million)

- Cited claim: global prevalence approximately 2.8 million as of 2020.
- Source consulted: PMC PMC7720355 (full text via PubMed Central).
- Full citation verified: Walton C, King R, Rechtman L, Kaye W, Leray E, Marrie RA, Robertson N, La Rocca N, Uitdehaag B, van der Mei I, Wallin M, Helme A, Angood Napier C, Rijke N, Baneke P. Rising prevalence of multiple sclerosis worldwide: Insights from the Atlas of MS, third edition. Multiple Sclerosis Journal. 2020;26(14):1816-1821. doi:10.1177/1352458520970841.
- Source quote: "A total of 2.8 million people are estimated to live with MS worldwide (35.9 per 100,000 population)."
- Verdict: `confirmed`. The bibliography entry is correct in substance. Recommend adding the volume, issue, page numbers, and DOI to the bibliography.

### A.2. Kurtzke 1983, Neurology (EDSS)

- Cited claim: original EDSS reference, the standard clinical disability scale in MS.
- Source consulted: PubMed PMID 6685237 (abstract).
- Full citation verified: Kurtzke JF. Rating neurologic impairment in multiple sclerosis: an expanded disability status scale (EDSS). Neurology. 1983;33(11):1444-1452. doi:10.1212/WNL.33.11.1444.
- Verdict: `confirmed`. Recommend adding volume, issue, page numbers, and DOI to the bibliography.

### A.3. Montalban et al. 2021, npj Digital Medicine, "Smartphone-based remote assessment of MS"

- Cited claim: the original Floodlight precedent, smartphone based remote assessment of MS.
- Sources consulted: PubMed PMID 34259588, search of npj Digital Medicine 2021 archives.
- Finding: there is no Montalban et al. 2021 paper in npj Digital Medicine matching this title. The paper most likely intended is Montalban X, Graves J, Midaglia L, et al. A smartphone sensor-based digital outcome assessment of multiple sclerosis. Multiple Sclerosis Journal. 2022;28(4):654-664. doi:10.1177/13524585211028561 (online 14 July 2021). The journal is *Multiple Sclerosis Journal*, not *npj Digital Medicine*.
- The 2021 npj Digital Medicine paper that does exist on smartphone SDMT in MS is by Pham L, Harris T, Varosanec M, Morgan V, Kosa P, Bielekova B. Smartphone-based symbol-digit modalities test reliably captures brain damage in multiple sclerosis. npj Digital Medicine. 2021;4(1):36. doi:10.1038/s41746-021-00401-y. First author is Pham, not Montalban.
- Verdict: `replace`. Replace the existing entry with Montalban et al. 2022 in *Multiple Sclerosis Journal*, doi 10.1177/13524585211028561 (this is the correct "original Floodlight precedent" paper). Pham et al. 2021 npj Digital Medicine should be cited separately if a smartphone SDMT validation reference is wanted.
- Veto exercised: the bibliography must not retain a citation to "Montalban 2021 npj Digital Medicine, Smartphone-based remote assessment of MS" as written. Either the journal name and year are wrong (Mult Scler 2022) or the first author is wrong (Pham 2021). The current entry conflates two distinct papers.

### A.4. Montalban et al. 2024, Scientific Reports (Floodlight Open cohort)

- Cited claim: Floodlight Open cohort 1,350 MS plus 1,133 non MS users from 17 countries 2018-2021. 4 of 7 active tests significantly discriminated MS from non MS after age and sex adjustment (IPS, IPS Digit Digit, Pinching, U Turn). 3 did not discriminate (Draw a Shape, Static Balance, 2MWT). DOI 10.1038/s41598-023-49299-4.
- Source consulted: PDF of full paper retrieved from scientiasalut.gencat.cat (institutional repository); pdftotext extraction; verified Tables 1, 4 directly.
- Finding on first author: the first author is **Jiwon Oh**, not Montalban. Author order: Oh J, Capezzuto L, Kriara L, Schjodt-Eriksen J, van Beek J, Bernasconi C, Montalban X (7th of 15), Butzkueven H, Kappos L, Giovannoni G, Bove R, Julian L, Baker M, Gossens C, Lindemann M.
- Finding on cohort sizes: confirmed 1,350 self-declared MS and 1,133 self-declared non MS from 17 countries across four continents (full text page 1).
- Finding on 4 of 7 active tests: confirmed via Table 4 (page 10 of PDF). After adjustment for age and sex, the 4 significant tests were IPS Test (p<0.001), IPS Digit-Digit Test (p<0.001), Pinching Test (p<0.001), and U-Turn Test (p=0.002). The 3 non significant tests were Draw a Shape Test (p=0.159), Static Balance Test (p=0.054), and 2MWT (p=0.066). The text on page 9 reports the U-Turn Test result as "(p < 0.05; adjusted for age and sex)" which is consistent with the Table 4 value of p=0.002. The clinical-references.md summary that "U Turn Test (p < 0.05)" is technically true; the more precise value from Table 4 is p=0.002.
- Full citation verified: Oh J, Capezzuto L, Kriara L, Schjodt-Eriksen J, van Beek J, Bernasconi C, Montalban X, Butzkueven H, Kappos L, Giovannoni G, Bove R, Julian L, Baker M, Gossens C, Lindemann M. Use of smartphone-based remote assessments of multiple sclerosis in Floodlight Open, a global, prospective, open-access study. Scientific Reports. 2024;14:122. doi:10.1038/s41598-023-49299-4.
- Verdict: `confirmed with revision`. The DOI, year, journal, cohort sizes, and the 4 of 7 finding are all correct. Author attribution is wrong: this is **Oh et al. 2024**, not Montalban et al. 2024. Every place this paper is cited (clinical-references.md, SPEC.md Section 7.1.1, SPEC.md Section 9, the Clinical Validator's notes) must change "Montalban et al. 2024" to "Oh et al. 2024" with Montalban listed in the et al.

### A.5. Galati et al. 2024, JMIR Human Factors 11:e57033

- Cited claim: day 30 retention 30.8% with reminders, 9.7% without; activation conversion 53.9% to 74.6% after onboarding redesign.
- Source consulted: humanfactors.jmir.org/2024/1/e57033 (open access full text).
- Full citation verified: Galati A, Kriara L, Lindemann M, Lehner R, Jones J. User Experience of a Large-Scale Smartphone-Based Observational Study in Multiple Sclerosis: Global, Open-Access, Digital-Only Study. JMIR Human Factors. 2024;11:e57033. doi:10.2196/57033.
- Source quote on retention: "53/172, 30.8% vs 34/350, 9.7%" for day 30, with reminders versus without reminders (US MS daily activities cohort).
- Source quote on activation conversion: redesigned onboarding journey improved conversion "from 53.9% (328/608) to 74.6% (359/481)".
- Verdict: `confirmed`. Both load bearing numbers verified exactly as written in SPEC.md Sections 9 and 16, in clinical-references.md, and in docs/plan.md Phase 0.

### A.6. Mathiowetz et al. 1985, American Journal of Occupational Therapy (9HPT)

- Cited claim: original 9 Hole Peg Test paper, the standard EDSS upper limb measure.
- Source consulted: PubMed search and SAGE Journals listing.
- Finding: the paper exists, by the cited authors, in the cited year. **The journal is wrong.** The paper is Mathiowetz V, Weber K, Kashman N, Volland G. Adult Norms for the Nine Hole Peg Test of Finger Dexterity. Occupational Therapy Journal of Research. 1985;5(1):24-38. doi:10.1177/153944928500500102. It was published in *Occupational Therapy Journal of Research* (a SAGE journal), not *American Journal of Occupational Therapy* (the journal of AOTA). These are two different journals.
- A separate Mathiowetz et al. paper does exist in *American Journal of Occupational Therapy* (Mathiowetz et al. 2003, on commercially available nine hole peg test norms, AJOT 57(5):570-573) but that is a 2003 paper, not 1985.
- Verdict: `confirmed with revision`. Correct the journal name to *Occupational Therapy Journal of Research*, add volume 5, issue 1, pages 24-38, DOI 10.1177/153944928500500102.

### A.7. Bays et al. 2015 (digital tap based proxies for 9HPT in MS)

- Cited claim: alternating bilateral tap rates correlate with 9HPT completion times in MS.
- Source consulted: PubMed search for "Bays 2015 multiple sclerosis tap"; Google Scholar and broader web search for "Bays" + 9HPT + MS in 2014, 2015, 2016. Searched for plausible spelling variants (Bays, Boyer, Bayer).
- Finding: no paper matching "Bays et al. 2015" on digital tap based proxies for 9HPT in MS could be located. The Clinical Validator's flag is corroborated. There are MS smartphone tap papers that do exist (for example, Tanigawa et al. 2017 on finger and foot tapping in MS, *Multiple Sclerosis Journal: Experimental, Translational and Clinical*), but none matches "Bays 2015".
- Verdict: `not found`. **Veto exercised.** This citation must not be retained in the bibliography or used to support the Tap Test design in SPEC Section 6.1 until either (a) the actual paper is located by the Clinical Validator and a corrected citation produced, or (b) the project replaces it with a verified MS tap proxy paper. As of this audit, the Tap Test clinical anchor for the 9HPT smartphone proxy claim is unsupported. Recommend the PM dispatch the Clinical Validator to identify a real digital tap test in MS reference (Tanigawa 2017 may be a candidate but I have not verified it supports the specific correlation strength claim and will not assert it from memory).

### A.8. Buckley et al. 2020, Sensors (smartphone gait review; 4 to 7 percent stride length error)

- Cited claim: smartphone gait analysis stride length error in the 4 to 7 percent range across published methods.
- Source consulted: PubMed search "Buckley gait smartphone sensors review 2020"; MDPI Sensors search; broader Google Scholar searches for Buckley as first or last author on smartphone gait review in 2019-2020.
- Finding: PubMed returned zero results for the search. No paper by Buckley as first author published in *Sensors* in 2019 or 2020 reviewing smartphone gait analysis with a 4-7 percent stride length error finding could be located. There are systematic reviews of smartphone gait analysis from this period (a 2025 review reports SEM 2.1 to 7.8 percent, which roughly brackets the cited range) but those are not by Buckley and not in 2020.
- A different "Buckley et al. 2019" review exists ("The Role of Movement Analysis in Diagnosing and Monitoring Neurodegenerative Conditions: Insights from Gait and Posture Studies") in *Behavioral Sciences*, but I have not directly verified it contains the 4-7 percent stride length error claim and will not assert it.
- Verdict: `not found`. **Veto exercised.** The 4 to 7 percent stride length error range is the foundational justification for SPEC Section 7.2's "stride length within 5 percent" target. Until either (a) the Buckley 2020 Sensors paper is located, (b) the citation is corrected to the actual Buckley paper, or (c) the project anchors on a different verified review, the 5 percent target in SPEC Section 7.2 has no verified literature anchor. This is a load bearing citation. Recommend the PM dispatch the Signal Processing Engineer or Clinical Validator to identify a verified smartphone or hip mounted IMU gait analysis review with the relevant accuracy range.

### A.9. Madgwick 2010 (orientation filter)

- Cited claim: Madgwick orientation filter, used in the Phase 3 gait pipeline.
- Source consulted: Semantic Scholar and the freely available Madgwick internal report PDF.
- Full citation verified: Madgwick SOH. An efficient orientation filter for inertial and inertial/magnetic sensor arrays. Internal Report, University of Bristol. 2010. (Conference paper version: Madgwick SOH, Harrison AJL, Vaidyanathan R. Estimation of IMU and MARG orientation using a gradient descent algorithm. IEEE International Conference on Rehabilitation Robotics. 2011, doi:10.1109/ICORR.2011.5975346.)
- Verdict: `confirmed`. The 2010 internal report is the foundational work; the 2011 IEEE conference paper is the most commonly cited peer reviewed version. Recommend the bibliography cite both, with the 2011 IEEE paper as the formally citable peer reviewed reference and the 2010 internal report as the original technical specification.

### A.10. Skog et al. 2010, IEEE Transactions on Biomedical Engineering (ZUPT)

- Cited claim: Zero velocity update method for stride length integration.
- Source consulted: Web search confirmed publication.
- Full citation verified: Skog I, Handel P, Nilsson J-O, Rantakokko J. Zero-velocity detection: an algorithm evaluation. IEEE Transactions on Biomedical Engineering. 2010;57(11):2657-2666. doi:10.1109/TBME.2010.2060723.
- Caveat from the Clinical Validator that the cited paper may be foot mount specific while the project uses a hip mounted (front pocket) phone: confirmed concern. The Skog et al. 2010 evaluation is grounded in foot mounted IMU navigation. The phone in the front pocket position is closer to a hip mount; ZUPT is still applicable in principle but the specific algorithm thresholds and detection windows may need adjustment. This is a Phase 3 implementation concern, not a citation defect.
- Verdict: `confirmed`. Add volume, issue, pages, and DOI to the bibliography.

### A.11. Cutter et al. 1999, Brain (T25FW / MSFC)

- Cited claim: standard EDSS lower limb measure.
- Source consulted: PubMed PMID 10355672.
- Full citation verified: Cutter GR, Baier ML, Rudick RA, Cookfair DL, Fischer JS, Petkau J, Syndulko K, Weinshenker BG, Antel JP, Confavreux C, Ellison GW, Lublin F, Miller AE, Rao SM, Reingold S, Thompson A, Willoughby E. Development of a multiple sclerosis functional composite as a clinical trial outcome measure. Brain. 1999;122(Pt 5):871-882. doi:10.1093/brain/122.5.871.
- Note: this is the MSFC paper, which includes the T25FW as one of three components. The bibliography summary "the standard EDSS lower limb measure" is imprecise: the T25FW is the lower limb component of the MSFC, not part of the EDSS itself. Recommend revising the summary to "Cutter et al. 1999 introduced the Multiple Sclerosis Functional Composite (MSFC), of which the Timed 25 Foot Walk (T25FW) is the lower limb component." The MSFC is complementary to, not part of, the EDSS.
- Verdict: `confirmed with revision`. Add volume, issue, pages, DOI; revise the relevance summary to clarify that T25FW is part of MSFC rather than part of EDSS.

### A.12. Balcer et al. 2003, Neurology (Sloan low contrast acuity)

- Cited claim: validation of low contrast acuity at 100, 25, 5, and 1.25 percent for tracking MS optic neuritis recovery and progression. Source of the contrast levels in SPEC Section 6.3.
- Source consulted: web search confirmed (publisher full text was paywalled).
- Full citation: Balcer LJ, Baier ML, Cohen JA, Kooijmans MF, Sandrock AW, Nano-Schiavi ML, Pfohl DC, Mills M, Bowen J, Ford C, Heidenreich FR, Jacobs DA, Markowitz CE, Stuart WH, Ying GS, Galetta SL, Maguire MG, Cutter GR. Contrast letter acuity as a visual component for the Multiple Sclerosis Functional Composite. Neurology. 2003;61(10):1367-1373. doi:10.1212/01.WNL.0000094315.19931.90.
- Caveat: I confirmed the citation exists and that the paper used Sloan low contrast charts; I have not directly verified from the full text that all four specific contrast levels (100, 25, 5, 1.25 percent) were validated in this specific paper. The Sloan letter acuity literature cumulatively validates these levels, but if this 2003 paper is the sole anchor for the four level choice, the Phase 6 design note should reference it together with subsequent Sloan literature for the specific contrast levels.
- Verdict: `confirmed with revision`. Add volume, issue, pages, DOI. Phase 6 design note should re-verify the four contrast levels are explicitly named in this paper or supplement with additional Sloan citations (the Regan and Neima 1983 Sloan chart development papers, or the more recent low contrast literature, may be the better anchor for the specific levels).

### A.13. Smith 1968 (original SDMT manual)

- Cited claim: original paper SDMT, Western Psychological Services.
- Source consulted: Western Psychological Services publisher page (wpspublish.com), ePROVIDE listing.
- Finding: the original SDMT manual was published by Aaron Smith via Western Psychological Services in **1973**, not 1968. A 1982 revised manual also exists. There is a 1968 work by Smith ("The symbol-digit modalities test: a neuropsychologic test of learning and other cerebral disorders") which appeared as a chapter in J. Helmuth (Ed.), Learning Disorders, vol. 3 (Special Child Publications, Seattle, 1968), pp. 83-91. This is an earlier paper but is not the SDMT manual sold by WPS.
- Verdict: `confirmed with revision`. The cited year 1968 is technically correct for the earliest published Smith chapter on the SDMT, but the WPS manual that is the standard source for the test materials is 1973 (revised 1982). The bibliography should cite either Smith 1968 (Helmuth ed., book chapter) for the conceptual original, OR Smith 1973 (WPS manual) for the test materials, depending on which is intended. The current entry conflates the two by saying "Smith 1968, Western Psychological Services." Western Psychological Services published the 1973 manual, not the 1968 chapter.

### A.14. Benedict et al. 2017, Multiple Sclerosis Journal (digital SDMT)

- Cited claim: validation of digital SDMT adaptations in MS, justifying the 90 second duration and digital format in SPEC Section 6.4.
- Source consulted: PubMed PMID 28206827.
- Full citation verified: Benedict RH, DeLuca J, Phillips G, LaRocca N, Hudson LD, Rudick R; Multiple Sclerosis Outcome Assessments Consortium. Validity of the Symbol Digit Modalities Test as a cognition performance outcome measure for multiple sclerosis. Multiple Sclerosis. 2017;23(5):721-733. doi:10.1177/1352458517690821.
- Finding: the paper validates the SDMT broadly for MS (oral and written formats; the paper does not specifically validate a digital tap based smartphone adaptation). The bibliography summary "validation of digital SDMT adaptations in MS" overstates what the paper supports.
- Verdict: `confirmed with revision`. Correct the relevance summary to "validation of the SDMT (in its standard oral and written formats) as a cognition performance outcome measure for MS, with strong support for the 90 second duration." For the specifically digital tap based adaptation, cite Pham et al. 2021 npj Digital Medicine (DOI 10.1038/s41746-021-00401-y) instead.

### A.15. Rusz et al. 2018, Sleep Medicine (voice biomarkers)

- Cited claim: acoustic features (jitter, shimmer, HNR) correlate with bulbar function and global fatigue in MS.
- Source consulted: PubMed search for Rusz 2018 in *Sleep Medicine*.
- Finding: Jan Rusz has published extensively on **Parkinson's disease** voice biomarkers, and on REM sleep behaviour disorder (iRBD) as a Parkinson's prodrome. No 2018 *Sleep Medicine* paper by Rusz on MS voice biomarkers could be located. The Clinical Validator's flag is corroborated.
- Verdict: `replace`. The Rusz body of work is Parkinson's specific, not MS specific. For MS voice biomarkers in v1, cite the new MS specific systematic review by Boz et al. 2025 in *Journal of Voice* (see Section B.4 below). The clinical-references.md entry "Rusz et al. 2018, *Sleep Medicine*" should be removed and replaced with an MS specific anchor.

### A.16. Koo and Li 2016, Journal of Chiropractic Medicine (ICC)

- Cited claim: guidelines for selecting and reporting ICC; the 0.75 threshold for primary feature test retest reliability.
- Source consulted: PubMed PMID 27330520.
- Full citation verified: Koo TK, Li MY. A guideline of selecting and reporting intraclass correlation coefficients for reliability research. Journal of Chiropractic Medicine. 2016;15(2):155-163. doi:10.1016/j.jcm.2016.02.012.
- Source quote on thresholds: "values less than 0.5, between 0.5 and 0.75, between 0.75 and 0.9, and greater than 0.90 are indicative of poor, moderate, good, and excellent reliability."
- Verdict: `confirmed`. The 0.75 threshold cited in SPEC Sections 7.2 and 9 maps directly to the Koo and Li good reliability boundary. Recommend the Biostatistics Reviewer (Phase 5) confirm that the project is using the appropriate ICC variant (1,1 vs 2,1 vs 3,1; absolute agreement vs consistency) before applying the 0.75 threshold.

## Section B. Unverified candidates flagged by the Clinical Validator

### B.1. Feys et al. 2017, Multiple Sclerosis Journal (9HPT consensus)

- Status: confirmed exists.
- Full citation: Feys P, Lamers I, Francis G, Benedict R, Phillips G, LaRocca N, Hudson LD, Rudick R; Multiple Sclerosis Outcome Assessments Consortium. The Nine-Hole Peg Test as a manual dexterity performance measure for multiple sclerosis. Multiple Sclerosis. 2017;23(5):711-720. doi:10.1177/1352458517690824.
- Note: this is one paper in the MSOAC consensus series in the same Mult Scler issue. The companion Benedict et al. 2017 (SDMT) is at doi 10.1177/1352458517690821 and the Motl et al. 2017 (T25FW) is at doi 10.1177/1352458517690823.
- Verdict: `confirmed for inclusion`. Add to bibliography as the canonical MS specific 9HPT consensus reference. This anchor should sit alongside Mathiowetz et al. 1985 (the original 9HPT methodology paper) for the Tap Test design rationale.

### B.2. Balcer et al. 2017, Multiple Sclerosis Journal (low contrast acuity review)

- Status: not found in 2017 *Multiple Sclerosis Journal* as written. There is a 2014 review (Balcer et al. Vision and vision-related outcome measures in multiple sclerosis. Brain. 2014, also indexed in PubMed). I did not locate a Balcer 2017 review in *Multiple Sclerosis Journal* on this topic.
- Verdict: `not found` as cited. The 2014 *Brain* paper (Balcer LJ et al., PMID 25433914) may be the intended replacement; I have not verified its content directly and will not assert what it says without fetching the full text. Recommend the Clinical Validator decide whether the 2014 Brain paper or the existing 2003 Neurology paper is the better Phase 6 anchor.

### B.3. Strober et al. 2019 (digital SDMT regression based norms; the Clinical Validator brief mentioned DOI 10.1093/arclin/acz046)

- Status: the cited DOI 10.1093/arclin/acz046 resolves to Martins-Rodrigues et al. 2021, "Clinical Utility of Two- and Three-Dimensional Visuoconstructional Tasks in Mild Cognitive Impairment and Early Alzheimer's Disease," in *Archives of Clinical Neuropsychology* 36(2):177-185. This is **not** a Strober paper, not a 2019 paper, and not about SDMT. The DOI in the project research notes is wrong.
- A real **Strober et al. 2019** paper does exist on SDMT in MS: Strober L, DeLuca J, Benedict RH, Jacobs A, Cohen JA, Chiaravalloti N, Hudson LD, Rudick RA, LaRocca NG; Multiple Sclerosis Outcome Assessments Consortium (MSOAC). Symbol Digit Modalities Test: A valid clinical trial endpoint for measuring cognition in multiple sclerosis. Multiple Sclerosis. 2019;25(13):1781-1790. doi:10.1177/1352458518808204.
- Note: this real Strober 2019 paper is a clinical trial endpoint validation paper for the SDMT in MS, not a regression based normative paper. The "regression based norms for SDMT" claim from the project research notes is not what this paper provides. The regression based normative paper for SDMT is Fellows RP, Schmitter-Edgecombe M, Symbol Digit Modalities Test: Regression-Based Normative Data and Clinical Utility, Archives of Clinical Neuropsychology, 2019, 35(1):105-115, doi 10.1093/arclin/acz020 (note the different DOI suffix: acz020, not acz046). Fellows and Schmitter-Edgecombe is a healthy population normative paper, not MS specific.
- Verdict: `confirmed for inclusion` if the project intends the MS SDMT clinical trial endpoint anchor (Strober et al. 2019, Mult Scler). `replace` for the "regression based norms" claim, with Fellows and Schmitter-Edgecombe 2019 (Arch Clin Neuropsychol, doi 10.1093/arclin/acz020) as the corrected citation. The PM should ask the Clinical Validator which Phase 7 SDMT use case needs which paper before the bibliography is updated.

### B.4. Boz et al. 2025, Journal of Voice (systematic review on MS voice biomarkers)

- Status: confirmed exists. The paper is titled "Voice Alterations in Multiple Sclerosis: A Systematic Review and Meta-analysis of Acoustic Parameters", DOI accessible through ScienceDirect at the article PII S0892199725004370 in *Journal of Voice*, 2025. Online publication date and full author list could not be fully confirmed because the publisher full text was paywalled and the abstract page returned 403 in this session. The acknowledgements list contributors as TRA, JDH, MB, GGG (not "Boz"), so the first author identity is uncertain.
- Verdict: `confirmed for inclusion` with caveat. A 2025 *J Voice* systematic review on MS voice biomarkers does exist and is the strongest available anchor for the project's voice feature selection. **Veto on assertion of the first author as Boz** until the publisher full text is consulted. The Citation Auditor was unable to confirm the first author of the paper PII S0892199725004370 is Boz; this is the candidate the Clinical Validator brief named, but the abstract page in this session listed acknowledged contributors with different initials, raising uncertainty about whether the *J Voice* 2025 review the project research notes refer to is the same paper. Recommend the PM either obtain institutional access to the full text or treat the citation as "a 2025 *Journal of Voice* MS voice biomarkers systematic review (publisher details to confirm)" until verified.

### B.5. Fairbanks 1960 Rainbow Passage

- Status: confirmed exists.
- Full citation: Fairbanks G. The Rainbow Passage. In: Voice and Articulation Drillbook (2nd ed.), New York: Harper and Row; 1960. pp. 124-139.
- Note: the Rainbow Passage is the standard reading passage in clinical voice analysis, named by ASHA's expert panel as the standard for adult instrumental voice evaluation. Public domain status: the Rainbow Passage as included in Fairbanks 1960 is widely treated as freely usable in clinical practice; I did not directly verify the public domain or copyright status of the underlying text and will not assert it. Recommend the Phase 8 design note address copyright posture before using the passage in the application.
- Verdict: `confirmed for inclusion`.

### B.6. MSOAC consensus citation for SDMT in MS

- Status: confirmed. The MSOAC consensus citation is Strober et al. 2019 (DOI 10.1177/1352458518808204; see B.3 above) for SDMT, with companion papers in the same Mult Scler 23(5) issue (Feys et al. 2017 for 9HPT, Benedict et al. 2017 for SDMT methodology, Motl et al. 2017 for T25FW). The Strober 2019 paper is the most direct MSOAC SDMT clinical trial endpoint paper.
- Verdict: `confirmed for inclusion`. See B.3 for the full citation.

### B.7. Praat acoustic analysis citation (Boersma and Weenink)

- Status: confirmed. Per the official Praat citation guidance (fon.hum.uva.nl/praat):
  - For the software: "Praat: doing phonetics by computer (Computer program), Version X.Y.ZZ, retrieved DD MMM YYYY from https://praat.org" by Paul Boersma and David Weenink.
  - For the peer reviewed reference: Boersma P. Praat, a system for doing phonetics by computer. Glot International. 2001;5(9/10):341-345.
- Verdict: `confirmed for inclusion`. Cite Boersma 2001 (Glot Int) as the peer reviewed reference; cite the Praat manual or website for software version specific implementation details. The Citation Auditor will re-audit the specific Praat algorithmic citations once the Signal Processing Engineer commits to specific jitter, shimmer, HNR definitions in Phase 8.

### B.8. Sosnoff et al. 2014, PubMed 25117855 (gait during 6MWT)

- Status: paper exists at the cited PubMed ID, but the **first author is not Sosnoff**.
- Full citation: Socie MJ, Motl RW, Sosnoff JJ. Examination of spatiotemporal gait parameters during the 6-min walk in individuals with multiple sclerosis. International Journal of Rehabilitation Research. 2014;37(4):311-316. doi:10.1097/MRR.0000000000000074.
- Note: Jacob Sosnoff is the third author; Michael Socie is the first author. The paper is from the Sosnoff lab at Illinois.
- Verdict: `confirmed for inclusion as Socie et al. 2014`. The PM and Clinical Validator must update every reference to "Sosnoff et al. 2014" to "Socie et al. 2014". The paper does support the project's gait test design in the sense that it characterizes spatiotemporal gait parameters in MS during the 6MWT; it does not specifically validate a 30 second window as adequate. The Clinical Validator's caveat about whether 30 seconds is sufficient remains open and will need a Phase 5 power analysis (no published paper specifically addresses the 30 second smartphone walking window in MS, to my knowledge).

### B.9. Comber et al. 2017, Gait Posture meta analysis (DOI 10.1016/j.gaitpost.2016.09.026; step length 45.3 vs 72.1 cm; cadence 94.4 vs 115.2)

- Status: the Comber et al. 2017 paper exists; the cited values are from a different paper.
- Full citation for Comber 2017: Comber L, Galvin R, Coote S. Gait deficits in people with multiple sclerosis: A systematic review and meta-analysis. Gait Posture. 2017;51:25-35. doi:10.1016/j.gaitpost.2016.09.026.
- Comber 2017 reports **standardized mean differences (SMDs)**, not raw step length and cadence values. Comber 2017 SMDs are: step length SMD=1.15 (large effect), stride length SMD=1.27 (large), cadence SMD=0.43 (small). 41 studies included, of which 32 contributed to the meta-analysis; participants had EDSS 1.8 to 4.5.
- The **45.3 cm vs 72.1 cm step length and 94.4 vs 115.2 cadence values** are from a different paper: Givon U, Zeilig G, Achiron A. Gait analysis in multiple sclerosis: characterization of temporal-spatial parameters using GAITRite functional ambulation system. Gait Posture. 2009;29(1):138-142. doi:10.1016/j.gaitpost.2008.07.011. This is a single center GAITRite walkway study, not a meta-analysis.
- Verdict: `confirmed with revision`, and **veto exercised on the misattribution**. SPEC Section 7.1.1 currently asserts these values come from "the Comber 2017 meta analysis" verbatim; clinical-references.md and the Clinical Validator's notes both repeat this. **This is a load bearing misattribution.** SPEC Section 7.1.1, clinical-references.md, and the Clinical Validator's notes must be corrected: the 45.3 vs 72.1 cm step length and 94.4 vs 115.2 cadence numbers come from **Givon, Zeilig, Achiron 2009** (Gait Posture 29(1):138-142), not from Comber 2017. The Comber 2017 paper supports the qualitative claim that MS gait shows large effects on step length and stride length and a small but significant effect on cadence relative to controls, expressed as standardized mean differences; it does not provide raw cm/min values. The PM should dispatch the Documentation Engineer to revise SPEC Section 7.1.1 with the corrected attribution. The cited Givon 2009 study is methodologically narrow (single center, GAITRite, EDSS 1-6.5) and the Phase 5 reference range overlay design should account for that narrowness rather than treating the values as broadly representative.

## Section C. PRO instruments recommended by the Patient Advocate

The Patient Advocate recommended 8 PRO instruments by name only. Each below is verified for the canonical original validation citation.

### C.1. MSIS-29

- Hobart J, Lamping D, Fitzpatrick R, Riazi A, Thompson A. The Multiple Sclerosis Impact Scale (MSIS-29): a new patient-based outcome measure. Brain. 2001;124(Pt 5):962-973. doi:10.1093/brain/124.5.962.
- Verdict: `confirmed for inclusion`.

### C.2. MSQOL-54

- Vickrey BG, Hays RD, Harooni R, Myers LW, Ellison GW. A health-related quality of life measure for multiple sclerosis. Quality of Life Research. 1995;4(3):187-206. doi:10.1007/BF02260859.
- Verdict: `confirmed for inclusion`.

### C.3. FAMS

- Cella DF, Dineen K, Arnason B, Reder A, Webster KA, Karabatsos G, Chang C, Lloyd S, Mo F, Stewart J, Stefoski D. Validation of the Functional Assessment of Multiple Sclerosis quality of life instrument. Neurology. 1996;47(1):129-139. doi:10.1212/WNL.47.1.129.
- Verdict: `confirmed for inclusion`.

### C.4. Neuro-QoL

- Cella D, Lai JS, Nowinski CJ, Victorson D, Peterman A, Miller D, Bethoux F, Heinemann A, Rubin S, Cavazos JE, Reder AT, Sufit R, Simuni T, Holmes GL, Siderowf A, Wojna V, Bode R, McKinney N, Podrabsky T, Wortman K, Choi S, Gershon R, Rothrock N, Moy C. Neuro-QOL: brief measures of health-related quality of life for clinical research in neurology. Neurology. 2012;78(23):1860-1867. doi:10.1212/WNL.0b013e318258f744.
- For the most relevant MS validation paper, the Patient Advocate brief asks for both the underlying NIH PRO bank work (above) and an MS validation. A widely cited MS specific Neuro-QoL validation is Miller DM, Bethoux F, Victorson D, Nowinski CJ, Buono S, Lai JS, Wortman K, Burns JL, Moy C, Cella D. Validating Neuro-QoL short forms and targeted scales with people who have multiple sclerosis. Multiple Sclerosis Journal. 2016;22(6):830-841. doi:10.1177/1352458515599450. Note: I did not directly verify the Miller 2016 paper in this session via web fetch and am flagging it as a strong candidate for the MS validation citation pending direct verification.
- Verdict: `confirmed for inclusion` for Cella 2012 (Neurology). `confirmed for inclusion pending verification` for Miller 2016 (Mult Scler J), to be re-audited before the Phase 0 retention design or any patient facing artifact references it.

### C.5. MFIS (Modified Fatigue Impact Scale)

- The MFIS evolved from the FIS during MSQLI development. The most commonly cited original FIS reference: Fisk JD, Pontefract A, Ritvo PG, Archibald CJ, Murray TJ. The impact of fatigue on patients with multiple sclerosis. Canadian Journal of Neurological Sciences. 1994;21(1):9-14. doi:10.1017/S0317167100048691.
- The 21 item MFIS itself was published as part of the MSQLI; the canonical reference is Multiple Sclerosis Council for Clinical Practice Guidelines. Fatigue and Multiple Sclerosis: Evidence-Based Management Strategies for Fatigue in Multiple Sclerosis. Washington, DC: Paralyzed Veterans of America; 1998. (This is not peer reviewed in the journal sense; it is the clinical practice guideline that codified the MFIS.) For peer reviewed psychometric validation in MS, cite Larson RD. Psychometric properties of the Modified Fatigue Impact Scale. International Journal of MS Care. 2013;15(1):15-20. doi:10.7224/1537-2073.2012-019.
- Verdict: `confirmed for inclusion` for Fisk 1994 (FIS original) and Larson 2013 (MFIS psychometrics in MS). The Clinical Validator (per the Patient Advocate's flag) should make the explicit MFIS vs FSS choice and document the rationale in Phase 0 or before any patient facing artifact uses either.

### C.6. PDDS (Patient Determined Disease Steps)

- The original Disease Steps scale: Hohol MJ, Orav EJ, Weiner HL. Disease Steps in multiple sclerosis: A simple approach to evaluate disease progression. Neurology. 1995;45(2):251-255. doi:10.1212/WNL.45.2.251.
- The PDDS validation paper most commonly cited: Learmonth YC, Motl RW, Sandroff BM, Pula JH, Cadavid D. Validation of patient determined disease steps (PDDS) scale scores in persons with multiple sclerosis. BMC Neurology. 2013;13:37. doi:10.1186/1471-2377-13-37.
- Verdict: `confirmed for inclusion` for both the Hohol 1995 original Disease Steps and the Learmonth 2013 PDDS validation.

### C.7. SymptoMScreen

- Green R, Kalina J, Ford R, Pandey K, Kister I. SymptoMScreen: A Tool for Rapid Assessment of Symptom Severity in MS Across Multiple Domains. Applied Neuropsychology: Adult. 2017;24(2):183-189. doi:10.1080/23279095.2015.1125905.
- Verdict: `confirmed for inclusion`.

### C.8. MS specific cognitive function PRO (MSNQ vs PROMIS Cognitive Function MS-validated short forms)

- MSNQ original validation: Benedict RHB, Munschauer F, Linn R, Miller C, Murphy E, Foley F, Jacobs L. Screening for multiple sclerosis cognitive impairment using a self-administered 15-item questionnaire. Multiple Sclerosis Journal. 2003;9(1):95-101. doi:10.1191/1352458503ms861oa.
- PROMIS Cognitive Function in MS: the most commonly cited MS validation of the PROMIS cognitive function short forms is Becker H, Stuifbergen A, Lee H, Kullberg V. Reliability and Validity of PROMIS Cognitive Abilities and Cognitive Concerns Scales among People with Multiple Sclerosis. International Journal of MS Care. 2014;16(1):1-8. doi:10.7224/1537-2073.2012-047. Note: I did not directly verify this PROMIS paper in this session and will not assert its findings; the Clinical Validator should re-audit it before the project commits to PROMIS as the cognitive PRO over MSNQ. The Neuro-QoL Cognitive Function short forms are also commonly used in MS (Cella 2012 above and Miller 2016 short form validation).
- Verdict: `confirmed for inclusion` for MSNQ (Benedict 2003). `confirmed for inclusion pending verification` for the PROMIS cognitive abilities MS validation (Becker 2014). The PM should dispatch the Clinical Validator to make the MSNQ vs PROMIS Cognitive Function vs Neuro-QoL Cognitive Function decision before any patient facing artifact uses either.

## Section D. Cited numbers in SPEC.md

### D.1. "approximately 2.8 million" globally (SPEC Section 2; Walton 2020)

- Verified against Walton et al. 2020 Atlas of MS third edition. The number is exactly correct.
- Verdict: `confirmed`.

### D.2. "stride length within 5 percent" target (SPEC Section 7.2)

- The 5 percent target is stated in SPEC Section 7.2 as the project's stride length accuracy goal, with the bibliography summary citing Buckley 2020 Sensors with a 4 to 7 percent stride length error range across published methods.
- Verdict: **veto exercised**, see Section A.8. The Buckley 2020 Sensors paper as cited could not be located. The 5 percent target itself is internally defensible as a project target, but the literature anchor for the 4 to 7 percent range needs to be replaced with a verified review. Until then, SPEC Section 7.2 should either remove the literature anchor or replace it with a verified citation.

### D.3. "ICC above 0.75" threshold (SPEC Section 9; Koo and Li 2016)

- Verified. Koo and Li 2016 explicitly define the 0.75 boundary as the lower edge of "good" reliability.
- Verdict: `confirmed`.

### D.4. "30.8 percent day 30 retention" floor (SPEC Section 9, Section 16; Galati 2024)

- Verified. Galati et al. 2024 Table reports 53/172 = 30.8 percent at day 30 in the US MS daily activities cohort with reminders enabled.
- Verdict: `confirmed`.

### D.5. "9.7 percent day 30 retention" without reminders (SPEC Section 9; Galati 2024)

- Verified. Galati et al. 2024 reports 34/350 = 9.7 percent at day 30 in the US MS daily activities cohort without reminders.
- Verdict: `confirmed`.

### D.6. "step length 45.3 vs 72.1 cm; cadence 94.4 vs 115.2" (SPEC Section 7.1.1)

- **Misattributed.** SPEC Section 7.1.1 attributes these values to "Comber et al. 2017 *Scientific Reports*" (which is wrong on two counts: Comber 2017 is in *Gait Posture*, not *Scientific Reports*, and Comber 2017 reports SMDs not raw cm/min values). The actual source for these specific raw values is Givon U, Zeilig G, Achiron A. Gait analysis in multiple sclerosis. Gait Posture. 2009;29(1):138-142. doi:10.1016/j.gaitpost.2008.07.011.
- The clinical-references.md note also says "Comber 2017 (Gait Posture meta analysis)" which has the journal right but still attributes the wrong numbers.
- Verdict: **veto exercised** on the SPEC Section 7.1.1 attribution. The PM must revise SPEC Section 7.1.1 to attribute these values to Givon et al. 2009. The accompanying caveat that this is a single center GAITRite walkway study (not a meta-analysis) should be added so the values are not misread as broadly representative of the MS population.

### D.7. "53.9 to 74.6 percent activation conversion" (Galati 2024)

- Verified. Galati et al. 2024 Table reports 53.9% (328/608) to 74.6% (359/481) registration to activation conversion change after the onboarding redesign.
- Verdict: `confirmed`.

### D.8. "4 of 7 active tests" finding (SPEC Section 7.1.1)

- Verified, with author attribution correction. SPEC Section 7.1.1 attributes this to "Montalban et al. 2024" (DOI 10.1038/s41598-023-49299-4); the first author is **Oh**, not Montalban. The 4 of 7 finding is directly supported by Table 4 of the paper: IPS, IPS Digit Digit, Pinching (all p < 0.001), and U Turn (p = 0.002 after age and sex adjustment). The 3 non significant tests after adjustment: Draw a Shape (p = 0.159), Static Balance (p = 0.054), 2 Minute Walk Test (p = 0.066).
- Verdict: `confirmed with revision`. The substantive claim is correct. Change every "Montalban et al. 2024" attribution to "Oh et al. 2024" (with Montalban listed in the et al.) across SPEC.md, clinical-references.md, the Clinical Validator's notes, and any other artifact that cites this paper.

## Summary

### Verdict counts

For Section A (existing bibliography entries), Section B (unverified candidates), and Section D (SPEC numbers) combined:

- `confirmed`: 6 (A.1 Walton, A.2 Kurtzke, A.5 Galati, A.9 Madgwick original, A.10 Skog, A.16 Koo and Li, plus D.1, D.3, D.4, D.5, D.7 which all map onto the same underlying confirmed papers)
- `confirmed with revision`: 7 (A.4 Oh et al. attribution, A.6 Mathiowetz journal name, A.11 Cutter MSFC vs EDSS framing, A.12 Balcer 2003 add details, A.13 Smith 1973 vs 1968 SDMT manual, A.14 Benedict 2017 scope, D.8 Oh et al. attribution)
- `replace`: 2 (A.3 Montalban 2021 npj Digital Medicine to Montalban 2022 Mult Scler, A.15 Rusz 2018 Sleep Medicine to Boz 2025 J Voice)
- `remove`: 0
- `not found`: 3 (A.7 Bays 2015, A.8 Buckley 2020 Sensors, B.2 Balcer 2017 Mult Scler J as cited)
- `confirmed for inclusion` (Section B and C unverified candidates that did locate as plausible bibliography additions): 11 (B.1 Feys 2017, B.5 Fairbanks 1960, B.6 MSOAC consensus = Strober 2019, B.7 Praat / Boersma 2001, B.8 Socie 2014 not Sosnoff, plus C.1 through C.7 PRO instruments and the MSNQ 2003 from C.8)
- `confirmed for inclusion pending verification`: 3 (B.3 Strober 2019 Mult Scler J vs Fellows and Schmitter-Edgecombe 2019 Arch Clin Neuropsychol, B.4 Boz 2025 J Voice first author confirmation, the Miller 2016 Neuro-QoL MS validation in C.4, the Becker 2014 PROMIS in C.8)

### Most consequential corrections (load bearing)

1. **The Comber 2017 misattribution in SPEC Section 7.1.1.** The MS step length 45.3 cm vs 72.1 cm and MS cadence 94.4 vs 115.2 steps per minute values are from Givon et al. 2009 (Gait Posture), not from the Comber 2017 meta-analysis as currently asserted. Comber 2017 reports SMDs, not raw cm/min values. SPEC Section 7.1.1 also says "Comber et al. 2017 *Scientific Reports*" which compounds the error: Comber 2017 is in *Gait Posture*. This is the single most important correction in this audit because the values are explicitly load bearing for the Phase 5 validation strategy, and the project's reference range overlay rationale rests on what these numbers represent (a meta analytic average vs a single center walkway study).

2. **The Oh et al. 2024 attribution.** Every place that currently cites "Montalban et al. 2024 *Scientific Reports* DOI 10.1038/s41598-023-49299-4" must change to "Oh et al. 2024" (Montalban is the seventh author of fifteen). The substantive claims (1,350 MS / 1,133 non MS cohort, 4 of 7 active tests discriminating after adjustment, 2MWT not discriminating) are all correct.

3. **The Buckley 2020 Sensors stride length error citation cannot be located.** The 4 to 7 percent stride length error range across published methods (which underwrites SPEC Section 7.2's "stride length within 5 percent" target) has no verified literature anchor as currently cited. The PM must dispatch the Signal Processing Engineer or Clinical Validator to find a verified review or remove the literature anchor and treat the 5 percent target as a project goal supported by the project's own validation experiment in Phase 5.

4. **The Bays 2015 tap test citation cannot be located.** The Tap Test design in SPEC Section 6.1 claims the alternating bilateral tap rate correlates with 9HPT completion times in MS, anchored on "Bays et al. 2015 and follow ups." No paper matching this citation could be located. Until a real MS specific digital tap proxy paper is identified (Tanigawa 2017 may be a candidate but has not been verified), the Tap Test design has a clinical rationale gap.

5. **The Montalban 2021 npj Digital Medicine citation conflates two distinct papers.** The "original Floodlight precedent" paper is most likely Montalban et al. 2022 *Multiple Sclerosis Journal* (online July 2021, doi 10.1177/13524585211028561). The npj Digital Medicine 2021 smartphone SDMT paper is by Pham et al., not Montalban. Both should be in the bibliography; neither matches the cited entry as written.

6. **The Sosnoff 2014 citation has the wrong first author.** PMID 25117855 is Socie et al. 2014, not Sosnoff et al. 2014.

7. **The Mathiowetz 1985 journal name is wrong.** *Occupational Therapy Journal of Research*, not *American Journal of Occupational Therapy*.

8. **The Strober 2019 DOI in the Clinical Validator brief notes is wrong.** DOI 10.1093/arclin/acz046 resolves to a Martins-Rodrigues 2021 paper on visuoconstructional tasks in Alzheimer's, not a Strober paper on SDMT. The real Strober et al. 2019 SDMT MS paper is at doi 10.1177/1352458518808204 in *Multiple Sclerosis Journal*. The Fellows and Schmitter-Edgecombe 2019 SDMT regression based normative paper (DOI 10.1093/arclin/acz020, note acz020 not acz046) is a separate paper in healthy populations.

9. **The Rusz 2018 Sleep Medicine citation is not MS specific.** Rusz's published voice biomarkers work is on Parkinson's disease and REM sleep behaviour disorder, not MS. The MS specific anchor should be the 2025 *Journal of Voice* MS voice biomarkers systematic review (PII S0892199725004370), with the first author identity to be re-confirmed when the publisher full text is accessible.

### Items the Citation Auditor was uncertain about and did not assert

1. The exact first author of the 2025 *Journal of Voice* MS voice biomarkers systematic review (PII S0892199725004370). The publisher abstract page returned 403 in this session and the search excerpts showed acknowledged contributors with initials TRA/JDH/MB/GGG, which do not obviously include "Boz." The candidate flagged by the Clinical Validator may be the same paper but the first author could not be positively confirmed.
2. The full text of Balcer et al. 2003 *Neurology* on the four specific contrast levels (100, 25, 5, 1.25 percent). The publisher full text was paywalled. The citation exists; the abstract supports Sloan low contrast acuity in MS; the four specific level validation may need a supplementary citation.
3. The full text of Comber et al. 2017 *Gait Posture*. The publisher abstract was retrievable; the full text was paywalled. The SMD values are from the abstract; the absence of raw cm/min values is from the abstract, which is a strong signal but not a direct full text confirmation.
4. The Miller et al. 2016 *Multiple Sclerosis Journal* Neuro-QoL MS validation (suggested as the MS specific Neuro-QoL anchor in C.4). The citation is plausible from web search but was not directly verified by full text fetch.
5. The Becker et al. 2014 *International Journal of MS Care* PROMIS Cognitive Abilities MS validation (suggested as the PROMIS option in C.8). Not directly verified.
6. The Tanigawa et al. 2017 finger and foot tapping in MS paper, which may be a candidate replacement for the missing Bays 2015 reference. Not directly verified.

### Vetoes exercised

The following citations are blocked from project artifacts (SPEC.md, README.md, on screen text, PDF report text) until corrected. The veto can only be overridden by the user.

1. **A.3 Montalban 2021 npj Digital Medicine.** Either the journal or the first author is wrong. The bibliography must not use this citation as written.
2. **A.7 Bays 2015 (digital tap proxies for 9HPT in MS).** Citation cannot be located. The Tap Test SPEC Section 6.1 anchor is unsupported until replaced.
3. **A.8 Buckley 2020 Sensors (4 to 7 percent stride length error).** Citation cannot be located. SPEC Section 7.2's "stride length within 5 percent" target loses its literature anchor until replaced.
4. **A.15 Rusz 2018 Sleep Medicine (MS voice biomarkers).** The cited paper is Parkinson's specific, not MS specific. Replace with the 2025 *Journal of Voice* MS systematic review (pending first author confirmation per B.4).
5. **D.6 Comber 2017 attribution of step length 45.3 vs 72.1 cm and cadence 94.4 vs 115.2 in SPEC Section 7.1.1.** These values are from Givon et al. 2009 *Gait Posture*, not Comber 2017 *Gait Posture*. SPEC Section 7.1.1 must be revised before any artifact (README, PDF report, on screen text) repeats the misattribution. The misattribution to "Comber 2017 *Scientific Reports*" in SPEC Section 7.1.1 also has the journal wrong (Comber 2017 is *Gait Posture*).

End of Phase 0 Citation Auditor audit. Citation Auditor, 2026-05-07.

## Phase 0 to Phase 1 cleanup re audit, 2026-05-07

This section is appended after the Clinical Validator's cleanup pass replaced the five Phase 0 vetoed citations. The Validator explicitly flagged two items as needing Citation Auditor re verification before the Phase 0 cleanup commits. All web fetches and searches in this re audit were performed in this session.

### R.1. Romero-Arias et al. 2025, Journal of Voice (replacement for the vetoed Rusz 2018 Sleep Medicine entry; verdict A.15 / B.4 follow up)

- Cited claim: MS specific systematic review and meta analysis comparing acoustic voice parameters between people with MS and healthy controls. Reports statistically significant differences in six of seven acoustic indicators, including increased jitter and shimmer (two of the three voice quality features in the project's feature set), increased F0 standard deviation, and decreased maximum phonation time. Article PII S0892199725004370 in Journal of Voice 2025; first author Romero-Arias confirmed by the Clinical Validator via Google Scholar plus the University of La Laguna researcher portal; volume, issue, pages, DOI, and full author list were pending Citation Auditor re verification.
- Sources consulted in this re audit:
  - Crossref API record for DOI 10.1016/j.jvoice.2025.10.017 (api.crossref.org/works/10.1016/j.jvoice.2025.10.017).
  - OpenAlex API record for the same DOI (api.openalex.org/works/doi:10.1016/j.jvoice.2025.10.017).
  - PubMed ESummary for PMID 41206297 (eutils.ncbi.nlm.nih.gov ESearch returned PMID 41206297 for the DOI; ESummary returned the full bibliographic record).
  - The publisher abstract page at sciencedirect.com/science/article/abs/pii/S0892199725004370 returned HTTP 403 in this session, consistent with the Clinical Validator's earlier experience. Bibliographic detail was completed entirely from indexing services that do not require publisher access.
- Bibliographic detail completed:
  - Title: Voice Alterations in Multiple Sclerosis: A Systematic Review and Meta-analysis of Acoustic Parameters.
  - Authors in order: Romero-Arias T, Delgado Hernández J, Betancort M, Gálvez García G. Four authors total; the Clinical Validator's tentative coauthor list (Delgado-Hernandez J, Betancort M, Hernandez-Perez MA) was partially correct (Delgado Hernández and Betancort confirmed) and partially incorrect (the fourth author is Gálvez García G, not Hernandez-Perez MA; affiliation Universidad de Salamanca and Universidad de La Frontera, Chile). Author affiliations from OpenAlex confirm the first three authors are at Universidad Europea de Canarias and Universidad de La Laguna in Tenerife, Spain; the fourth author is at Universidad de Salamanca and Universidad de La Frontera in Chile.
  - Journal: Journal of Voice (the official journal of the Voice Foundation; ISSN 0892-1997 print, 1873-4588 electronic; published by Elsevier).
  - Year: 2025.
  - Publication date: 2025 November 7 (epub ahead of print; PubMed Pubdate and Epubdate both 2025/11/07; OpenAlex publication date 2025-11-01; Crossref published-print 2025/11). Indexed in Crossref May 4, 2026.
  - Volume, issue, page numbers: not yet assigned. Crossref, OpenAlex, and PubMed all return null or empty volume / issue / page fields. The article is listed as a journal article (review type) without volume / issue / pages. This is consistent with epub ahead of print: the article has a DOI and is citable, but has not been assigned to a print issue yet.
  - DOI: 10.1016/j.jvoice.2025.10.017 (confirmed by all three indexing sources and consistent with the article PII S0892199725004370 cited by the Clinical Validator).
  - PMID: 41206297.
  - Article type: systematic review (OpenAlex `type: review`; PubMed publication type Journal Article; Review).
- Verdict: `confirmed for inclusion` with the Romero-Arias 2025 Journal of Voice paper as the primary MS specific anchor for the voice feature set. Bibliographic detail is now sufficient to cite the paper formally:
  - Romero-Arias T, Delgado Hernández J, Betancort M, Gálvez García G. Voice Alterations in Multiple Sclerosis: A Systematic Review and Meta-analysis of Acoustic Parameters. Journal of Voice. Published online November 7, 2025. doi:10.1016/j.jvoice.2025.10.017. PMID: 41206297.
  - Once the article is assigned to a print issue, the volume, issue, and page numbers should be added; until then "Published online November 7, 2025" with the DOI is the standard citation form for an article in press.
- Recommended action for clinical-references.md: update the Romero-Arias entry to the form above. The entry's current "publisher full text was inaccessible during verification; volume, issue, pages, and DOI are pending direct confirmation, and the full author list past the first author is also pending direct confirmation" caveat can be replaced with: DOI confirmed via Crossref / OpenAlex / PubMed; full author list confirmed via the same indexing services as Romero-Arias T, Delgado Hernández J, Betancort M, Gálvez García G; volume / issue / pages not yet assigned because the article is epub ahead of print as of November 7, 2025. The Clinical Validator's tentative speculation that Hernandez-Perez MA is a coauthor is incorrect for this paper and should be removed; Hernandez-Perez MA is a coauthor on a separate Romero-Arias group paper (the in press 2026 machine learning classification paper at sciencedirect.com/science/article/pii/S2211034826001458, which is a different work by the same lab).
- Items the Citation Auditor was uncertain about and did not assert:
  - The exact substantive content claims (six of seven acoustic indicators significantly differ; specifically increased jitter and shimmer, increased F0 SD, decreased maximum phonation time, plus increased FCR and HNR per the search excerpt obtained in this session) come from a public summary of the abstract, not the full text. The publisher full text remained inaccessible (HTTP 403 on ScienceDirect direct fetch). The substantive claims appear consistent with the search excerpt that summarised the abstract, which lists "reduced F0 and MPT, together with increased F0 SD, jitter, shimmer, FCR, and HNR" as the six of seven indicators with significant differences. The Phase 8 design note that ultimately cites this paper should re verify the specific findings against the full text once institutional access is available; until then, the substantive feature support claim should be qualified as "per the published abstract."
- This re audit lifts the Phase 0 veto (verdict A.15) on the Rusz 2018 replacement: the Romero-Arias 2025 paper is verified to exist, is MS specific, and is bibliographically complete enough to cite. Substantive content (specific feature findings) remains to be confirmed against the full text in Phase 8 before it appears in any user facing artifact.

### R.2. Bea et al. 2025, Journal of Functional Morphology and Kinesiology (replacement for the vetoed Buckley 2020 Sensors entry; verdict A.8 follow up)

- Cited claim in the bibliography entry as currently written by the Clinical Validator: "Standard error of measurement (SEM) for stride length, where reported, ranged from 2.1 percent to 7.8 percent of the walking distance, which approximately brackets the project's 5 percent stride length target."
- Sources consulted in this re audit:
  - PMC open access full text at pmc.ncbi.nlm.nih.gov/articles/PMC12015829/ (open access, no paywall; full text directly retrieved in this session).
- Bibliographic detail confirmed: Bea T, Chaabene H, Freitag CW, Schega L. Psychometric Characteristics of Smartphone-Based Gait Analyses in Chronic Health Conditions: A Systematic Review. Journal of Functional Morphology and Kinesiology. 2025;10(2):133. doi:10.3390/jfmk10020133. PMID: 40566429. All bibliographic elements as cited by the Clinical Validator are correct.
- Direct quote from the open access full text, Section 3.3 "Validation, Reliability, and Feasibility Outcomes":

  "Reliability was analysed in 27 studies (50%), with test–retest reliability examined in 25 studies (93%) and inter- and intra-rater reliability assessed in 2 studies (7%). ICC values for test–retest reliability ranged from 0.53 to 0.95, with higher reliability observed for temporal gait parameters (gait speed, cadence: ICC = 0.80–0.95) than for spatial parameters (step length, stride variability: ICC = 0.53–0.88), and higher values observed in clinical settings (ICC = 0.75–0.95) than in home environments (ICC = 0.53–0.90). Standard error of measurement (SEM) was provided in only two studies (4%) and ranged from 2.1% to 7.8% of the walking distance."

- Methodological context of the SEM number, verified directly against the full text:
  - The SEM sentence appears in the test retest reliability paragraph for spatiotemporal gait parameters considered together (the same paragraph that reports ICC 0.53 to 0.95 across spatiotemporal parameters). The SEM range 2.1 percent to 7.8 percent is reported for the SEM metric across the two studies (out of 54) that reported it, expressed as a percentage of "the walking distance," which from context refers to the total distance walked during a test trial (the standard denominator for expressing SEM as a percentage in published gait reliability studies).
  - The SEM range is therefore not specifically attributed to stride length in the paper. The Citation Auditor performed an exhaustive search of the full text for any other occurrence of "SEM" or "standard error of measurement" and for any sentence that simultaneously mentions stride length and a SEM value or error percentage; none was found. The Bea 2025 paper does not provide a stride length specific SEM number.
- Verification of the other numerical claims the Clinical Validator placed in the bibliography entry (verbatim against the PMC full text):
  - "Validity for spatial parameters such as stride length and step length against gold standard systems reported as r = 0.42 to 0.90." Confirmed (paper reports r = 0.42–0.90 for step length).
  - "Temporal parameters (gait speed, cadence) showing higher r = 0.74 to 0.97." Confirmed for cadence (r = 0.74–0.95) and gait speed (r = 0.78–0.97); the union range 0.74 to 0.97 across both temporal parameters as cited is correct.
  - "Test retest reliability for spatiotemporal parameters ICC = 0.53 to 0.95." Confirmed (paper reports ICC values for test retest reliability ranged from 0.53 to 0.95).
  - "Hip and pocket placements ICC = 0.85 to 0.95." Confirmed (paper reports ICC = 0.85–0.95 for hip and pocket placements in the sensor placement section).
  - 54 studies included. Confirmed (paper reports 54 studies eligible for inclusion).
  - Chronic conditions covered include Parkinson's (46 percent), MS (19 percent), stroke (6 percent), rheumatoid arthritis (4 percent), pulmonary diseases (4 percent), chronic low back pain (2 percent), per the full text. The bibliography entry's framing "54 studies, including MS, Parkinson's, stroke, and other neurological populations" is accurate, although the precise share by condition could be added if the Clinical Validator wants more specificity.
- Verdict: `confirmed with revision`. The Bea 2025 bibliographic detail and the four numerical claims (r ranges for spatial and temporal validity, ICC range for test retest reliability, ICC range for hip and pocket placements, 54 studies) all check out against the open access PMC full text. **However, the Clinical Validator's framing that the 2.1 percent to 7.8 percent SEM range is "for stride length, where reported" overspecifies what the paper actually says.** The paper reports SEM was provided in only two of the 54 studies, and the range 2.1 percent to 7.8 percent is expressed as a percentage of the walking distance in the test retest reliability paragraph for spatiotemporal parameters considered together, not stride length specifically. The walking distance referent is the total distance walked during a test trial, not stride length per stride.
- Recommended action for clinical-references.md and the Phase 0 to Phase 1 transition note in the same file: the bibliography entry should be revised to drop the "for stride length" qualifier on the SEM range. A faithful rewrite is:
  - "Standard error of measurement (SEM), reported in only two of the 54 included studies, ranged from 2.1 percent to 7.8 percent of the walking distance for spatiotemporal gait parameters; the paper does not isolate a stride length specific SEM. The 2.1 to 7.8 percent range approximately brackets the project's 5 percent stride length target as a thin literature anchor for the order of magnitude of error reported in smartphone IMU gait analyses, but the SEM range is not stride length specific and the project's 5 percent target remains primarily defended by its own Phase 5 validation experiment."
- The misattribution does not invalidate the citation. The Bea 2025 paper is still the best available 2025 systematic review of psychometric characteristics of smartphone IMU gait analyses, it does include MS specific studies, and the order of magnitude bracket (single digit percentage SEM in the published smartphone gait literature) is the load bearing claim that anchors the project's 5 percent target. The revision is a matter of accurately characterising what the source says rather than a veto of the source itself.

### Summary of the Phase 0 to Phase 1 cleanup re audit

- **Item R.1 verdict: `confirmed for inclusion`.** The Romero-Arias et al. 2025 Journal of Voice systematic review is verified to exist; bibliographic detail (DOI 10.1016/j.jvoice.2025.10.017, PMID 41206297, full author list Romero-Arias T, Delgado Hernández J, Betancort M, Gálvez García G, published online November 7, 2025) is now complete via Crossref, OpenAlex, and PubMed; volume, issue, and pages are not yet assigned because the article is epub ahead of print, and that is appropriate to record in the citation as "Published online November 7, 2025." The Phase 0 veto on the Rusz 2018 replacement is lifted. The Clinical Validator's tentative coauthor speculation (Hernandez-Perez MA) is incorrect for this paper and should be removed. Substantive content claims (six of seven acoustic indicators significantly differing) come from the published abstract and should be re verified against the full text in Phase 8 before the paper is referenced in any user facing artifact.
- **Item R.2 verdict: `confirmed with revision`.** The Bea et al. 2025 bibliographic detail is correct as cited. The verbatim SEM quote ("Standard error of measurement (SEM) was provided in only two studies (4%) and ranged from 2.1% to 7.8% of the walking distance.") is faithfully reproduced. However, the Clinical Validator's bibliography entry frames the SEM range as "for stride length, where reported," which is an overspecification: the paper reports SEM in the test retest reliability paragraph for spatiotemporal parameters considered together, expressed as a percentage of the walking distance (total distance walked), not stride length specifically. The Bea 2025 paper does not report a stride length specific SEM. The bibliography entry should be revised to characterise the SEM range as a general spatiotemporal SEM range rather than a stride length specific number.
- **Bibliography ready for commit?** Not quite; one revision is recommended before the Phase 0 cleanup commits:
  1. The Bea 2025 entry in clinical-references.md (lines around 34) and the Phase 0 to Phase 1 transition resolution paragraph (lines around 191 to 196) should drop the "for stride length, where reported" qualifier on the SEM range. A faithful replacement wording is given in R.2 above. After this revision, the bibliography is ready for commit.
  2. Optionally, the Romero-Arias entry in clinical-references.md (lines around 50 to 52) and the matching Phase 0 to Phase 1 transition resolution paragraph (lines around 198 to 202) should be updated with the now confirmed bibliographic detail (DOI, PMID, full author list as Romero-Arias T, Delgado Hernández J, Betancort M, Gálvez García G), removing the "pending verification" caveat for those bibliographic elements while retaining the "substantive content claims should be re verified against the full text in Phase 8" caveat. The reference to Hernandez-Perez MA as a possible coauthor of the Journal of Voice 2025 paper should be removed; he is a coauthor on a different Romero-Arias group paper (the 2026 ML classification paper).
- **Items the Citation Auditor was uncertain about and did not assert in this re audit:**
  1. The substantive content of the Romero-Arias 2025 Journal of Voice paper. The DOI, authors, and publication date are confirmed via three independent indexing services (Crossref, OpenAlex, PubMed). The substantive findings (six of seven acoustic indicators with significant differences; specifically reduced F0 and MPT, increased F0 SD, jitter, shimmer, FCR, HNR) come from a published summary of the abstract. The full text remained inaccessible (HTTP 403 on ScienceDirect direct fetch). Phase 8 should re verify the specific findings against the full text once institutional access is available.
  2. Whether "FCR" in the abstract summary refers to formant centralisation ratio (a specific acoustic parameter widely used in dysarthria literature) or to a different abbreviation. This is a minor specificity concern that Phase 8 will resolve when the Signal Processing Engineer commits to specific feature definitions.

Citation Auditor, Phase 0 to Phase 1 cleanup re audit, 2026-05-07.
