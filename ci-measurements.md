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

| Run | Link                                                                           | AVD cache            | Gradle cache | Record step | Boot within record | Gradle build | Shot record | Interaction step   | Total job |
|-----|--------------------------------------------------------------------------------|----------------------|--------------|-------------|--------------------|--------------|-------------|--------------------|-----------|
| 1   | [28942031506](https://github.com/AhmetSIRIM/komposto/actions/runs/28942031506) | miss (+1m31s create) | miss         | 13m40s      | ~16s               | 6m39s        | 6m42s       | did not run        | failed*   |
| 2   | [28943194194](https://github.com/AhmetSIRIM/komposto/actions/runs/28943194194) | miss (+1m36s create) | miss         | 13m47s      | ~20s               | 6m54s        | 6m33s       | 1m06s (gradle 46s) | 17m22s    |
| 3   | [28944433950](https://github.com/AhmetSIRIM/komposto/actions/runs/28944433950) | hit (32s restore)    | miss         | 13m46s      | ~58s               | 6m28s        | 6m20s       | 1m04s (gradle 45s) | 15m54s    |

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

| Run      | Link                                                                           | Gradle cache                         | Record step | Gradle build | Shot record | Total job |
|----------|--------------------------------------------------------------------------------|--------------------------------------|-------------|--------------|-------------|-----------|
| 4 (seed) | [28945575409](https://github.com/AhmetSIRIM/komposto/actions/runs/28945575409) | miss on restore, saved on post (28s) | 14m42s      | 7m07s        | 6m20s       | 16m49s    |
| 5 (warm) | [28957197414](https://github.com/AhmetSIRIM/komposto/actions/runs/28957197414) | hit (9s restore)                     | 9m39s       | 56s          | 7m39s       | 12m09s    |

| 6 (warm) | [28958105723](https://github.com/AhmetSIRIM/komposto/actions/runs/28958105723) | hit (9s restore) | 9m27s | 52s | 7m37s | 12m05s |

Variant A verdict: CONFIRMED. Warm Gradle cache cuts the build from ~6m40s
to ~54s and the total job from ~15m54s to ~12m07s (about 24 percent, ~4min
per run). Runs 5 and 6 agree within 4 seconds.

Note: Shot execution is systematically ~75s slower in warm runs (7m38s vs
6m20-33s cold). Likely daemon-warmth composition: in cold runs the 7min
build leaves a hot Gradle daemon and the Shot invocation is almost pure
test execution; in warm runs the Shot invocation carries more of its own
configuration/packaging cost. Net total still improves by ~4 minutes.

With the build cached, Shot test execution (~7.5min) is now the dominant
cost. Structural follow-ups (out of scope for this change): sharding
across emulators, or migrating to JVM screenshot testing (Roborazzi).

## Variant B: single emulator session (interaction tests merged into Record)

Hypothesis: the separate interaction step pays a second emulator
boot/setup (~20-40s of its ~65s total). Running both Gradle commands in
one android-emulator-runner session removes that overhead.

| Run | Link                                                                           | Record+interaction step | Gradle build | Shot record | Interaction | Total job |
|-----|--------------------------------------------------------------------------------|-------------------------|--------------|-------------|-------------|-----------|
| 7   | [28959113270](https://github.com/AhmetSIRIM/komposto/actions/runs/28959113270) | 10m36s                  | 48s          | 8m15s       | 23s         | 12m04s    |
| 8   | [28960006229](https://github.com/AhmetSIRIM/komposto/actions/runs/28960006229) | 10m36s                  | 54s          | 8m06s       | 24s         | 12m36s    |
| 9   | [28993031771](https://github.com/AhmetSIRIM/komposto/actions/runs/28993031771) | 9m32s                   | 54s          | 7m14s       | 22s         | 11m38s    |

Variant B verdict: CONFIRMED, small. Interaction cost reliably drops from
~65s (separate step, own emulator boot) to ~23s in the shared session, a
~40s structural saving. Shot execution varies 7m14s-8m15s across warm
runs (shared-runner noise), which can mask the saving in single-run
totals.

## Confirmation loop (A+B variant, repeated runs)

To make sure runs 5-9 were not lucky samples, a scripted loop re-triggered
the full pipeline repeatedly on 2026-07-09 (empty commits, one run at a
time, raw per-run data in `ci-loop-runs.csv`). 13 successful runs; one run
was cancelled mid-flight and is excluded.

Per-run data:

| #  | Run                                                                                | Gradle build | Shot record | Interaction | Record step | Total job |
|----|------------------------------------------------------------------------------------|--------------|-------------|-------------|-------------|-----------|
| 1  | [28994443316](https://github.com/AhmetSIRIM/komposto/actions/runs/28994443316)     | 0m53s        | 7m33s       | 0m21s       | 10m02s      | 11m32s    |
| 2  | [28995055730](https://github.com/AhmetSIRIM/komposto/actions/runs/28995055730)     | 0m48s        | 8m05s       | 0m23s       | 10m19s      | 11m44s    |
| 3  | [28997552785](https://github.com/AhmetSIRIM/komposto/actions/runs/28997552785)     | 1m04s        | 8m11s       | 0m22s       | 10m58s      | 13m03s    |
| 4  | [28998332276](https://github.com/AhmetSIRIM/komposto/actions/runs/28998332276)     | 0m48s        | 7m44s       | 0m28s       | 10m05s      | 11m51s    |
| 5  | [28998864854](https://github.com/AhmetSIRIM/komposto/actions/runs/28998864854)     | 0m54s        | 6m03s       | 0m19s       | 8m29s       | 10m35s    |
| 6  | [28999360771](https://github.com/AhmetSIRIM/komposto/actions/runs/28999360771)     | 1m09s        | 7m14s       | 0m21s       | 9m47s       | 11m53s    |
| 7  | [29000060156](https://github.com/AhmetSIRIM/komposto/actions/runs/29000060156)     | 0m48s        | 8m03s       | 0m22s       | 10m11s      | 11m50s    |
| 8  | [29000710973](https://github.com/AhmetSIRIM/komposto/actions/runs/29000710973)     | 1m14s        | 6m51s       | 0m21s       | 9m30s       | 11m50s    |
| 9  | [29001588527](https://github.com/AhmetSIRIM/komposto/actions/runs/29001588527)     | 1m06s        | 7m53s       | 0m27s       | 10m26s      | 12m03s    |
| 10 | [29002272573](https://github.com/AhmetSIRIM/komposto/actions/runs/29002272573)     | 1m11s        | 7m04s       | 0m21s       | 9m48s       | 11m26s    |
| -  | [29002914577](https://github.com/AhmetSIRIM/komposto/actions/runs/29002914577)     | cancelled mid-run, excluded | | | | |
| 11 | [29003745963](https://github.com/AhmetSIRIM/komposto/actions/runs/29003745963)     | 0m56s        | 7m18s       | 0m21s       | 9m37s       | 11m08s    |
| 12 | [29004689651](https://github.com/AhmetSIRIM/komposto/actions/runs/29004689651)     | 0m54s        | 7m39s       | 0m22s       | 9m57s       | 12m14s    |
| 13 | [29005727827](https://github.com/AhmetSIRIM/komposto/actions/runs/29005727827)     | 1m09s        | 7m20s       | 0m23s       | 9m51s       | 11m36s    |

Aggregates:

| Phase               | n  | Median | Min    | Max    | p90    |
|---------------------|----|--------|--------|--------|--------|
| Gradle build        | 13 | 56s    | 48s    | 1m14s  | 1m11s  |
| Shot record         | 13 | 7m33s  | 6m03s  | 8m11s  | 8m05s  |
| Interaction tests   | 13 | 22s    | 19s    | 28s    | 27s    |
| Record step (whole) | 13 | 9m57s  | 8m29s  | 10m58s | 10m26s |
| Total job           | 13 | 11m50s | 10m35s | 13m03s | 12m14s |

The medians confirm the earlier small-sample numbers: warm build stays
under ~1m, interaction stays ~22s, and the total job lands at ~11m50s
median against the ~15m54s baseline.

## Final summary

| Variant                                  | Total job (median) | Record step               | Gradle build           |
|------------------------------------------|--------------------|---------------------------|------------------------|
| Baseline (AVD cached)                    | 15m54s             | 13m46s                    | ~6m40s cold, every run |
| A: writable Gradle cache                 | 12m07s             | 9m33s                     | ~54s                   |
| A+B: plus single emulator session (n=13) | 11m50s             | 9m57s (incl. interaction) | ~56s                   |

Net effect of the proposed changes: roughly 15m54s to ~11m50s median,
about 4 minutes (~25 percent) per PR run, dominated by variant A.
Remaining dominant cost is Shot test execution (~7m30s median);
structural follow-ups (sharding, JVM screenshot testing) are out of
scope here.

## Additional finding: flaky CountdownTimer goldens

Warm runs 6 and 7 each produced pixel diffs in CountdownTimer screenshot
tests (run 6: backgroundAlphaTest and sizeTest, run 7: backgroundAlphaTest
again), which the workflow auto-committed. The rendered frame appears to
depend on wall-clock timing, so goldens are nondeterministic. Separate
upstream issue candidate: inject a fixed/controllable clock into
CountdownTimer screenshot tests.

## Variant: ATD system image (evaluated, deferred)

`aosp_atd` would speed AVD creation (cache-miss only) and boot, but boot
from snapshot is already just ~20-60s AND a different system image can
change rendered pixels, which would force regenerating all 227 LFS golden
screenshots. Weak gain, real migration cost; not pursued.
