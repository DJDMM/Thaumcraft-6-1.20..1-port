# Focus Bolt: released CoreGLE geometry audit — 0.41.0

The reference is the released TC6 6.1.BETA26 JAR, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Recovered Java is used for navigation; the geometry comparison executes the
unchanged released `CoreGLE`, `matrix` and `intersect` class bytes.

## Confirmed differences and repair

`FXBolt` uses five contour sides and join style1042 (`ANGLE | CAP | NORM_PATH`);
`gen_polycone` additionally closes the contour. Its first and last path points
are tangent guides. They are not endpoints of visible cylinder segments.

Auromancy final3 showed a large foreground pink polygon. The first repair
omitted those guide segments. Final4 completed33 native scenes and one
auxiliary Bolt capture with native exit0, but the remaining large pink coil
was still not accepted as proof of original rendering. Its34 full PNG, sheet
and compact preview were independently viewed and remain rejected historical
evidence in `visual-041-auromancy-final4-independent-review.json`.

The [independent final4 rejection](../../validation/visual-041-auromancy-final4-independent-review.json)
binds the executed log, metadata, source snapshot and every actual image SHA.

Further released-bytecode inspection confirmed that the previous port used
independently oriented straight cylinders, omitted both caps, and stretched
texture V once over the whole path. Released `extrusion_angle_join` instead
uses bisecting planes, line/plane intersections and a reflected up vector;
`draw_segment_color` advances V by one complete repeat for every segment.
The current pure-Java `OriginalBoltGeometry` restores these semantics.
`FocusBoltClient` submits the resulting vertices through the existing
texture/shader/render-type path. Each original strip triangle becomes one
degenerate modern quad, preserving the original diagonal on nonplanar faces.

The original near-degenerate-point quirk is preserved: the helper returns
the last tested adjacent difference for segment length, even after skipping
a point. The frame still follows the aggregate visible segment direction.
An initial comparison exposed this distinction; the failed comparison is
retained instead of relaxing the fixed tolerance.

The seeded path, phase, age, alpha, source, texture and blend are unchanged.
Outer radius uses the released float `pointWidth / 10`; inner radii divide
that double by3. There is no muzzle displacement, first-person suppression,
extra fade or radius reduction.

## Executed CPU comparison

The comparison harness loads the original classes from the pinned JAR and
intercepts their immediate GL matrix, texture-coordinate and vertex calls.
It separately compiles the exact production helper bytes with Java17.
All166 fixtures pass at a fixed absolute position/UV tolerance of1e-9;
the maximum actual difference is `1.1524808884999516e-13`. Fixtures cover
straight, vertical, bent, reflected, translated, duplicate and near-coincident
paths, both original lighting branches, and24 seeds with three ages and
two tube radii. Side triangle topology and UV are compared; convex cap
boundary and coverage are checked. `javac` and the CPU Java process exit0.

The [SHA-bound CPU proof](../../validation/visual-041-bolt-original-geometry-proof-r2.json)
references the [verified compressed raw capture and harness](../../validation/visual-041-bolt-original-geometry-cpu-proof-r4.zip).
The original failed comparison and earlier proof remain historical evidence.

The original cap callbacks emit no texture coordinate. Integer coordinates
inherited from a preceding segment repeat the same seam texel, so the modern
helper explicitly uses `(0,0)` for its cap vertices. The first original cap
may inherit arbitrary fractional caller state; that state is not established
by this comparison. This is an explicit modern vertex-API adaptation, not
a claim of every original GL state or pixel being identical.

CPU triangle/UV agreement does not establish native shader, camera,
framebuffer, random-history or partial-tick parity. Generated fixtures give
both implementations the same geometric inputs. No original Minecraft
screenshot or live OpenGL context is involved in the CPU proof.

## Native perspective evidence

The original outer radius is approximately.066 at default width.66. The first
drawable point is only about `1 / pi` blocks ahead of a first-person caster,
and the original age-dependent wiggle plus jitter can bring it closer to the
near plane. A large foreground projection and low texture contrast alone
therefore do not prove a wrong radius or missing texture.

The opt-in Auromancy driver now logs one parseable JSON snapshot at its
existing auxiliary Bolt PNG request. Data comes from the actual draw:
source/target/eye, phase/seed/age/partial, generated points, outer radii,
color/alpha/width, pose/model-view/projection matrices, submitted vertex count
and render sequence. It changes no scene, payment, capture gate, clock or
random state. The acceptance condition is a fresh native execution, a replay
of that exact geometry through the released class, and a binding between the
replay and actual PNG before interpreting the first-person silhouette as
original geometry. The CPU fixtures alone are not a native acceptance result.

The final4 Cloud frame is inside a red damage-tinted cow and does not isolate
the cloud silhouette. Its SpellBat is partly clipped at the right edge while
the trail is visible. These are stated capture limitations, not new production
defects. Gallery ritual/completion frames do not prove world-rendered Focal
particles. Raw prerequisites, materials, actors, initial aura and gifted spell
packages remain explicit fixtures; no full survival, multiplayer, audio,
shader-pack or all-angle claim is made.

## Frozen source bytes used for the final native runs

| File | SHA256 |
| --- | --- |
| `FocusBoltClient.java` | `2639f3abcd1e7346525b886d6cf06e581a4a210cfadcaf699c8c3bb16e375769` |
| `OriginalBoltGeometry.java` | `1b3023386c557dd8e984f6b1f770bf1594dc721b76e903dcd3555a3f66e34827` |
| `AuromancyClientSmokeTest.java` | `ba1d168c3d573b6e761fbd78a9b1f85986c5ac770f26ca428e1356486fe4614f` |

The new pure helper adds one source fingerprint: the expected source/resource/
script/build total becomes4580. This document records source and CPU evidence;
the fresh physical client result is recorded below.

## Accepted actual Auromancy final5 capture

`visual-041-auromancy-final5` completed33 main native scenes plus its one
auxiliary Bolt PNG with native JVM exit0, PID3492,16 owned hidden windows and
input desktop `Default` unchanged. All4580 source/resource/script/build
fingerprints match the executed snapshot and final build8 snapshot
`53b9fe8bc00bccf88222d851700dc3fe63336f508c3f234d4c57e2b3a0708e3c`.
The actual native log retains43/83/93vis table payments and the corresponding
cast assertions; its success counter is not the visual acceptance criterion.

Both root and the independent reviewer personally viewed all34 complete
1600x1000 native PNG, the full existing contact sheet, compact preview, and
the full reference boundary overlay and mask. Their separate
[root review](../../validation/visual-041-auromancy-final5-root-review.json)
and [independent review](../../validation/visual-041-auromancy-final5-independent-review.json)
accept this run with the stated fixture/capture limits. The earlier final3
and final4 rejections and their actual images remain historical evidence.

The [actual native replay](../../validation/visual-041-auromancy-final5-bolt-original-replay-r1.json)
binds the native log, PNG, source snapshot and geometry marker. Seed315,
phase78.53982, age1 and partial.17996931 independently reproduce the captured
38-point path and outer radii with zero difference. Both actual layers sent
through the unchanged released `CoreGLE` produce712 triangles in total,
matching the modern2848 submitted vertices with maximum absolute position/UV
difference `1.7763568394002505e-15` at the unchanged1e-9 tolerance.

Projecting all712 released triangles through the actual source-eye and
captured pose/model-view/projection matrices produces bounds
`x766.9172766415171..1594.1196908450925`,
`y299.7732244450624..706.4218747768749`. The cyan reference boundary visibly
tracks the entire broad pink first-person silhouette. This resolves the
foreground-size ambiguity in favor of the original near-eye geometry.
It does not establish an intended narrow beam. The full-viewport reference
mask has178222 pixels; descriptive pink counts are174492 inside and396
outside, without a hand-selected narrow region or an acceptance threshold.

The selected SpellBat final5 frame now contains its complete small model and
purple trail; the final4 right-edge clipping limit is not carried into this
new image. The Cloud camera-inside-cow and Gallery ritual/completion limits
remain. Original first-cap fractional caller UV is still not established.
The replay is CPU geometry and observed native-outline evidence, not an
original Minecraft1.12 GPU framebuffer, complete world-RNG/constructor
history, or bit-identical shader/fog/lightmap/blend output. Tiny retained
phase-conversion rounding and statistical native particle RNG are not claimed
to reproduce the original world's entire random history.

At this Auromancy review, the other seven profiles were still awaiting
review. Final0.41 acceptance is now complete: server4 passed1678/1678,
all eight selected clients and196 full native PNG passed the recorded
root and independent reviews with4580 identical final-build fingerprints.
See the [final report](../../validation/artifact-report-0.41.json) and
[validation limits](../../VALIDATION.md); this does not extend the geometry
replay into a claim of original GPU framebuffer or complete mod parity.
