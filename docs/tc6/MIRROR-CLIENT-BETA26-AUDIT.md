# Native mirror client contract, 0.32.0

This records the implementation and opt-in QA definition. It does not claim a
passed client/server run or reviewed screenshots. Final native evidence belongs
to VALIDATION.md and the versioned artifact report after QA actually completes.

The released BETA26 JAR and pinned source in MIRRORS-BETA26-AUDIT.md are the
authority. TileMirrorRenderer uses mirrorframe for physical items, mirrorframe2
for essentia, a sixteen-layer tunnel/particlefield for linked mirrors within
distance squared <1024 and mirrorpanetrans over that tunnel. The original
UtilsFX swapped quad UV plus90-degree plane rotation maps pane UV to1-x,1-y;
both manual panes retain this orientation. Unlinked and distant
mirrors use mirrorpane. This is a portal surface, not a camera into a remote
world. All six facing transformations retain the source rear-slab mounting.

MirrorRenderer renders the original alpha sprite frames as native generated
models registered as additional block-model resources, with a one-sixteenth-deep
extrusion separately from the pane. No dummy inventory item is registered. No static
opaque catalogue pane is rendered over a linked tunnel. Original sixteen seeded
31100 color layers, source texture scale/rotation/time animation, additive field
layers and .1875..8125 inset are retained. OpenGL 1.12 EYE/OBJECT texgen is adapted
to explicit camera-projected vertex UVs in the modern render pipeline; this is
not a pixel-identical replay of the old fixed-function projection. Culling is
disabled for native mounted planes; depth testing remains active, with no remote
world render, chunk access, link mutation or payment in a renderer.

Native item-mirror block event 1 produces the source wispy mote using the original
particles grid frames 512..527. Position jitter .33+.33*random, facing/2 offset,
facing/50 velocity plus Gaussian .01, random base lifetime10..30 expanded by up to
half its integer base, dark red/blue colors and source fade/scale are retained.
The modern independent client list advances on ordinary unpaused client ticks;
it uses the source random horizontal wander and gravity with .98 drag. The old
generic wind engine is not reproduced. World changes clear particles; malformed
event floods cap only live visual particles at4096. Actual emission/draw counters
are observational and never move a paid item.

HandMirrorScreen has the original 176x166 gui_handmirror texture, one input at
80,24, inventory at8,84 and hotbar at8,142. Labels remain absent. The source
selected main-hand hotbar slot overlay is retained; the supported modern offhand
menu has no incorrectly protected main-hand overlay. All hotbar-key actions
remain disabled. Native inventory/tooltips remain ordinary gameplay; no QA text,
button, fuel, remote storage catalogue or buffer is added.

The eight images below were verified byte-for-byte against the original JAR.
The four new imported media are the hand GUI, linked transparent pane, tunnel and particle field;
all existing original media remain unchanged.

| Media | Bytes | SHA-256 |
| --- | ---: | --- |
| gui_handmirror.png | 8443 | 3c521f4aeb9bb260c1a090c56cc810f22fa3c9eccfcb5f6aebf78f2e827c04d1 |
| mirrorframe.png | 569 | 0512b6a79b2af3b22174273b24da2836bd4f1cad1ffbc6d8356591693a2dd331 |
| mirrorframe2.png | 436 | 55573361752c58b751ad2e3badebec80b524eb0f760f49ca1d9d2cc0ee39387b |
| mirrorpane.png | 434 | e40cbf5daedfa1bdf7a3d9a4d09bb5306f5fb9f9f0d98e2a2ec3abc8548016a4 |
| mirrorpanetrans.png | 445 | b27c20b4fec3ffd1b9950d36a2137a9bc56b64b023f380511be3a313ac91079a |
| tunnel.png | 10020 | e003b525a039e51d19dc8542994ad102363637363a7d1ab9f899ce42235b3182 |
| particlefield.png | 7709 | 25f7450853dd771eeafc5765f8c42ab9b5321d1e5cb88d9fa3d5bfee1c0d115a |
| particles.png | 408277 | 3c31a3bed7a007ef4d671745bd627112c79d1ac70049e23194a1454929f17000 |

## Native hidden-client scenario definition

Property thaumcraft.mirrorSmokeTest, run/mirror-smoke, owned timestamped survival
world,20 fresh screenshot paths prefixed tc6-mirror- (the initial18-scene plan was extended with two native closeups). Root alone may start the
verified non-input hidden Windows desktop launcher. No visible launch, desktop
activation/switch, global input or manual-save mutation is used by this driver.

The driver scans physical torch and ender pearl samples through real Thaumometer
shift/offhand C2S to discover Lux and f_TELEPORT. It grants only preceding studies
and raw knowledge, then pays ARCANELAMP/MIRROR/MIRRORHAND/MIRRORESSENTIA through
native research packets. Its expected payment is96 raw Artifice theory,32 raw
observation,40 earned study XP and MIRROR's ender pearl plus one actually crafted glass.

Actual native bench result clicks craft one lamp and six mirrored glass with350
vis and14 primal crystals. Original raw components/crystals/aura are explicit
fixtures. Native altar world ticks craft three item mirrors, one hand mirror
consuming one of those plain paid mirrors, and two essentia mirrors. The five
typed raw jars start with490 total and must retain exactly15 after475 essence
payment. Physical raw peripherals, precursor studies, loaded arena, floor and
symmetrical candle pairs are explicit fixtures. Target studies/facts/aspects,
finished mirrors/glass/lamp and teleported outputs are never supplied.

The remaining four mirrors are actually survival placed/bound through native
BlockItem packets. A raw8-emerald entity at the source block must disappear via
ordinary collision and appear from the partner's native one-item output queue.
The actual paid hand mirror binds to the source mirror and opens the native
one-input GUI;8 raw diamonds pay through its C2S input and eject from that selected
source, not the partner. The native scene shows those physical loose outputs.

Two paid essentia mirrors route a previously implemented native output transfuser
to a remote native source jar. The raw24 Ignis source, empty labeled receiver and
transfuser are explicit precursor fixtures. Ordinary fifth-tick transfers must
conserve source+receiver=24, ending0/24 without a mirror buffer, direct manager
transfer call or scripted target amount. The paid lamp is actually placed and
must produce original glimmer blocks with observed native block light15.

Six-face observations reuse the same four paid mirrors: eight actual survival
client harvests, physical drop/pickup and native re-placement change N/S to E/W
to floor/ceiling. Reciprocal restoration must succeed after every pair settles.
The final observer includes incidental collection of prior emerald/diamond
outputs into the player's real inventory, avoiding a false duplicate/loss claim.

No direct runtime ticker, queue setter, link/restoration setter, transport call,
target fact award or final-output constructor is used. Server/client task futures
serialize server mutations; render counters only observe native draw calls.
Screenshots are ordinary book, inventory/altar and world scenes, never a gallery
or developer-caption screen. The scenario does not prove full manual survival,
independent multiplayer, all front-view camera angles, audible sound or a native
client cross-dimension visit. Native server tests must establish dimension and
transaction coverage separately. Only actual final logs and reviewed PNGs may
justify success counts, versioned previews or delivery.

## Final executed native evidence

The final mirror-032-client4 completed20/20 scenes with native JVM exit0,
PID38828, 16 owned hidden windows
and input desktop Default unchanged. Both root and mirror_essentia_finish_032
actually viewed every full1600x1000 PNG and both the full sheet and six-frame
delivery preview. Their exact SHA-bound findings and limitations are in
[artifact-report-0.32.json](../../validation/artifact-report-0.32.json).

The actual final observation contains8 emeralds and8 hand-mirror diamonds in
the real inventory,0 loose output, remote source0/receiver24 Ignis,
11 glimmers/native light15 and8 survival relocations of the same four mirrors.
Actual native rendering observed33664 tunnel-layer draws,2129 pane draws,
18 emitted item-mirror motes and630 mote draws. Accepted remote transport
published312 packets on each dimension-qualified leg; all four endpoints
match24 native observations. The remote jar atY112 is below UP pumpY113:
readonly native candidate enumeration excludes direct16-range access and
includes the remote-facing8-range source. No observer publishes its own FX.

Night DayTime18000, FOV80 and fixed overview20.5,112,-9.5 are explicit capture
fixtures. F1 is enabled only for the two closeups and then restored. The ceiling
view uses actual native CROUCHING/eye1.27; an opt-in MovementInputUpdateEvent
retains shift after KeyboardInput reads the hidden desktop, with owned motion
and jump zeroed. No global input, visible activation or manual player pose is
used. Client3 stalled at its19th capture because an END-tick shift was reset;
only its verified owned QA JVM45876 was stopped. Client4 reran all20 scenes.

Server5 passed1211/1211. All4342 server-relevant fingerprints still match that
run; the sole later change is this opt-in Dist.CLIENT QA driver. Final client
and build match all4343 fingerprints, with the driver exception and both
hashes recorded. Build22s, 2852 JSON and original-media byte
parity passed. Prepared scenes do not establish full manual survival,
independent multiplayer, audio playback or every animation frame/optical angle.
