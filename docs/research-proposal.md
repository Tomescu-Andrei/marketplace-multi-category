# Research proposal: second-hand price estimation

Research Industry Projects — M1 proposal, 1 November 2026.

**Status:** proposed methodology. No dataset has been selected or downloaded, no
model has been trained, and no experimental scores are claimed in this document.
M1 delivers the proposal and marketplace foundation; experiments belong to M4.

## Objective and questions

Investigate whether product attributes can improve second-hand price estimates
over a simple market baseline, while keeping inference practical inside the
Spring Boot backend.

The initial experimental category is proposed to be used laptops, subject to
finding suitable data. Starting with one product family makes attribute meanings
and error analysis manageable before attempting multiple categories.

Questions:

1. How much do structured attributes improve MAE and RMSE relative to a training-set median price?
2. Does listing text add useful accuracy beyond structured attributes?
3. How much accuracy, model size and latency change when the selected model is exported for Java inference?

Success means a reproducible, leakage-controlled comparison and a documented
integration feasibility result. Improvement over the baseline is a hypothesis,
not a guaranteed outcome. If a model fails to improve, that result will be reported.

## Data acquisition and target

Before collecting data, select a source with documented permission or a suitable
license and record its URL, access date, license, provenance and field definitions.
Prefer a downloadable dataset or an authorized API. External product APIs may
enrich specifications, but must not be treated as second-hand transaction labels.
Data acquisition is a pending research task, not an existing integration.

Required fields: source listing identifier, product family, target price and
currency. Useful optional fields: brand/model, CPU, RAM, storage, condition,
release year, title, description, observation time and a pseudonymous seller ID.
Record missing-field rates and exclusions; do not infer that unavailable fields exist.

Use completed sale price only if its definition and provenance can be verified.
If only asking prices are available, explicitly study **asking-price estimation**;
do not describe the output as realized sale value or a guaranteed fair price.
Exclude auction starting prices, deposits, accessories and bundles unless they
are separately identifiable and the target definition is adjusted accordingly.

Use one currency and geographic market initially. Cross-currency data should be
excluded until a dated conversion policy is defined. Remove direct identifiers
and contact information from features and retained text. Keep raw data outside
Git unless redistribution is explicitly permitted.

## Preparation and leakage controls

1. Audit duplicates and repeated observations using source IDs and normalized
   product/text fingerprints. Group observations of the same item before splitting.
2. Reject invalid/nonpositive labels using documented rules fixed before evaluation.
   Record outlier handling; do not drop expensive held-out items merely because
   they produce large errors.
3. If reliable observation times exist, reserve the latest approximately 20% as
   the final test set. Remove cross-boundary duplicate-item groups from training.
   Use rolling validation on earlier data; fit no preprocessing on future rows.
4. If reliable times do not exist, use a reproducible group-based 80/20 split by
   item group, grouping by seller too when metadata permits. Tune on grouped
   cross-validation within the training portion. Report this weaker temporal claim.
5. Fit imputation, categorical vocabularies, scaling and any text vocabulary on
   training folds only, using a scikit-learn Pipeline. Keep the final test set
   untouched until model selection is complete.

Record the split manifest, random seed, software versions and dataset checksum.
If seller grouping cannot be supported, state that limitation and avoid claiming
generalization to unseen sellers. For temporal evaluation, report seller overlap.

## Candidate experiments

| Experiment | Inputs | Purpose |
|---|---|---|
| Training median | Training price distribution only | Required naive baseline |
| Ridge regression | Imputed numeric attributes and encoded categories | Simple interpretable model |
| Random forest regression | Structured attributes | Nonlinear comparison |
| Ridge with text | Structured attributes plus title TF-IDF, if text is available | Text ablation |

Use the same split and target for every candidate. Tune a small, recorded search
space within training validation. Consider a log-price target only as a separate
experiment; invert predictions before evaluating in currency units. Check Java
export feasibility early for every candidate rather than after final selection.

## Evaluation

Report MAE and RMSE in the chosen currency and R² on the same held-out rows.
MAE is the primary selection metric on validation; RMSE highlights large errors.
Report sample counts, target distribution and baseline comparison alongside scores.
R² may be negative and should not be silently clipped. If the target variance is
zero or a slice is too small, report the metric as undefined rather than misleading.

Analyze residuals by price band, brand, condition and missingness where sample
sizes permit. Use paired, group-aware bootstrap intervals for MAE differences
when enough independent groups exist; otherwise report the uncertainty limitation.
Avoid repeated tuning against the held-out test results.

## Java integration feasibility

Prefer ONNX export where the chosen model and preprocessing operators are supported.
Verify converter/runtime compatibility for the actual pipeline before choosing it.
Package the feature order, dtypes, categories, missing-value rules, units and
target transformation with the model.

Compare Python and Java predictions on a frozen set of representative inputs,
including missing values, unknown categories and boundary values. Establish a
numerical tolerance before this comparison (initial proposal: relative 1e-4 plus
absolute 0.01 currency units). Report observed deviations, model size, cold-start
time and warmed-up median/p95 latency on named hardware. Numerical tolerance is
separate from prediction accuracy against real prices.

If the best model cannot be exported reliably, compare the strongest compatible
candidate and document the accuracy trade-off. Do not introduce a FastAPI service.
No inference endpoint is implemented during M1.

## Deliverables and schedule

- M1: proposal, target definition, data requirements and reproducible evaluation plan.
- Before M4: source/license decision, data audit and initial export feasibility check.
- M4: data preparation, versioned experiment settings, validation/test results,
  error analysis, exported model and Python/Java equivalence evidence.
- M5: integration evaluation and limitations in the final report.

Planned research folders: `research/datasets/`, `research/notebooks/` and
`research/experiments/`. Create executable research code when data work begins;
this proposal does not require empty placeholder notebooks or fabricated datasets.

## Risks and limits

Data access or license restrictions may require changing the source or product
family. Asking prices can differ from transaction prices. Missing condition data,
selection bias, geographic scope and changing markets limit generalization.
Sparse categories and unsupported export operators may reduce usable scope.
Document these outcomes rather than extending M1 into premature model development.

Method references: [scikit-learn regression metrics](https://scikit-learn.org/stable/modules/model_evaluation.html#regression-metrics),
[scikit-learn data leakage guidance](https://scikit-learn.org/stable/common_pitfalls.html#data-leakage),
and [ONNX Runtime Java](https://onnxruntime.ai/docs/get-started/with-java.html).
