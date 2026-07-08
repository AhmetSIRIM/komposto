# CI Measurements for Upstream Issue #16

Evidence log for optimizing the screenshot test workflow runtime.
All runs execute on this fork (`AhmetSIRIM/komposto`), GitHub-hosted `ubuntu-latest`.
Step durations come from the GitHub Actions API; intra-step phases come from
`ci-measure.sh` timing written to `GITHUB_STEP_SUMMARY`.

Methodology notes:

- App token step is replaced with the default `GITHUB_TOKEN` on this fork
  (no CI app secrets here). Its cost is treated as a constant (~1s upstream).
- Runs on shared runners vary by roughly 10-20 percent; each variant needs
  2-3 runs, medians are compared.
- Upstream reference run for comparison: Trendyol/komposto run 26880871087
  (2026-06-03, total 16m46s, Record step 14m14s, interaction step 1m18s).

## Variant: baseline (unmodified pipeline)

| Run | Link | AVD cache | Gradle cache | Record step | Boot within record | Gradle build | Shot record | Interaction step | Total job |
|---|---|---|---|---|---|---|---|---|---|
| 1 | [28942031506](https://github.com/AhmetSIRIM/komposto/actions/runs/28942031506) | miss (+1m31s create) | miss | 13m40s | ~16s | 6m39s | 6m42s | did not run | failed* |

*Run 1 failed at the final instrumentation line (multi-line script incompatible
with android-emulator-runner's per-line shell execution); both Gradle
invocations completed successfully, so the Record breakdown is valid. Caches
were cold (first run on fork), so build time is an upper bound.

## Variant: (planned) ATD image + single emulator boot

Pending baseline completion.
