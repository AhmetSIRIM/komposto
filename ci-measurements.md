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
| 2 | [28943194194](https://github.com/AhmetSIRIM/komposto/actions/runs/28943194194) | miss (+1m36s create) | miss | 13m47s | ~20s | 6m54s | 6m33s | 1m06s (gradle 46s) | 17m22s |
| 3 | [28944433950](https://github.com/AhmetSIRIM/komposto/actions/runs/28944433950) | hit (32s restore) | miss | 13m46s | ~58s | 6m28s | 6m20s | 1m04s (gradle 45s) | 15m54s |

Baseline summary (runs 2-3, AVD-cached case = run 3): total ~16m, Record
~13m46s of which Gradle build ~6m30-55s and Shot execution ~6m20-35s.
Record step variance across runs is under 10 seconds; the pipeline is
remarkably stable.

*Run 1 failed at the final instrumentation line (multi-line script incompatible
with android-emulator-runner's per-line shell execution); both Gradle
invocations completed successfully, so the Record breakdown is valid. Caches
were cold (first run on fork), so build time is an upper bound.

## Findings so far

1. Emulator boot from snapshot is cheap (~16-20s inside the Record step).
   The Record step cost is almost entirely Gradle build (~6m45s) plus Shot
   test execution (~6m35s).
2. The Gradle cache is never populated, on this fork and on upstream alike:
   `gradle/actions/setup-gradle` only writes cache entries from jobs on the
   default branch, and this workflow triggers exclusively on `pull_request`.
   Both repos show a 0s "Post Gradle cache" step (nothing saved) and a 0-1s
   restore (nothing to restore). Every PR run therefore compiles from
   scratch, paying roughly 7 minutes per run. Seeding the cache from a
   default-branch job (or setting `cache-read-only: false` for this
   workflow) is the single biggest optimization candidate.
3. AVD cache saved successfully after run 2 (17s post step), so run 3+
   should skip the ~1m35s AVD creation step.
4. Screenshot recording is deterministic on the runner image: the commit
   step found zero pixel diffs against LFS-tracked goldens.

## Variant A: writable Gradle cache from PR jobs (`cache-read-only: false`)

Hypothesis: setup-gradle never persists cache because this workflow only
runs on `pull_request` and the action defaults to read-only off the default
branch. Allowing PR jobs to write should cut the ~6m40s build to the
incremental cost of the PR's actual diff. Requires two runs: one to seed,
one to measure warm.

| Run | Link | Gradle cache | Record step | Gradle build | Shot record | Total job |
|---|---|---|---|---|---|---|
| 4 (seed) | [28945575409](https://github.com/AhmetSIRIM/komposto/actions/runs/28945575409) | miss on restore, saved on post (28s) | 14m42s | 7m07s | 6m20s | 16m49s |

## Variant B: (planned) single emulator boot + ATD image

Pending variant A completion.
