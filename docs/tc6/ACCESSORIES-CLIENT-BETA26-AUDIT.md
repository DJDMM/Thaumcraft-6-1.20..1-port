# BETA26 accessories: client appearance and native verification contract

Scope 0.39.0, 2026-10-16. This records original appearance, the implemented
modern rendering bridge and the required prepared client route. It does not
claim an executed client pass or an actual screenshot review. Final logs,
native exits, exact frame hashes, two actual reviewers and earlier failures
belong in `VALIDATION.md` and the final artifact report after execution.

## Primary appearance evidence

The released `Thaumcraft-1.12.2-6.1.BETA26.jar` is authority, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
`ItemGoggles.onPlayerBaubleRender` was read in Java17 `javap -c -p` output
`work/accessories-039-tc6-primary-javap.txt`, SHA-256
`b2058e63381b73bb2b00c239db8944dd6271ad2231f0b9559dd3c0519dd7047e`.
`UtilsFX` and `GuiFocusPouch` were also disassembled into
`work/accessories-039-visual-primary-javap.txt`, SHA-256
`296cd8b03acb8674052c3fbd01d52e4296a07f8e39c4a2e0675b63f75c3d9718`.
Source commit `954022bb777b7546281fb36df8522f0ba6b43f81` is a readable
companion. The Baubles rendering helpers are verified in official Baubles
1.12-1.5.2 source commit
`5df3d468afed5ebba1d5df983221e90ff2a9a9b3`. No TC4/5 accessory appearance
or menu is imported.

The following archive PNG bytes remain unchanged in the port:

| Texture | Original SHA-256 |
| --- | --- |
| `textures/items/goggles_bauble.png` | `9cd01a5c65168d6d96eabaa53e36722e23430b0ed948dbe0f8c1690488786aef` |
| `textures/gui/gui_focuspouch.png` | `d5002659e6972d7c982c3a3667fad6a6310a10ccfb935db9c82cdf695870137a` |
| `textures/misc/radial.png` | `07d379a3e754ed351b633bcd70649a465bbc9e1864c001d9886540d4a89c0d2d` |
| `textures/misc/radial2.png` | `931d88b76ff06276740628c956ac58e7f6d229e529b4f0862d474ecc98fadc61` |

The goggles PNG is **16×16**. The released extrusion nevertheless passes a
literal **16×26** strip count to `UtilsFX.renderTextureIn3D`, not the PNG height;
that literal geometry is retained.

## Goggles as actual head accessories

Released goggles render only in Baubles `RenderType.HEAD`. They bind
`goggles_bauble.png`, call `translateToHeadLevel`, `translateToFace`,
`defaultTransforms`, then rotate180 around Y, translate(-.5,-.5,z) and extrude
the full texture at thickness .1. The source's armor branch tests `helmet !=
null`, not nonempty. The nonnull empty stack returned by player armor storage
therefore still takes the literal z=0.11999999731779099 branch.

`GogglesCurioRenderer` registers through the native Curios renderer registry.
It consumes the parent HumanoidModel's real `head.translateAndRotate` pose.
The neutral-head helper composition is represented by translate
(-.275,.005,-.336) and scale(.55,-.55,-.55); parent head animation supplies the
modern pose. It emits the original front/back quads,16 horizontal strips,
26 vertical strips, half-texel UV offsets and .1 thickness into the native
`entityCutoutNoCull` buffer with normal entity packed light. Render-hidden
ordinary Curios items are still functional; normal Curios rendering visibility
determines whether the renderer is called. Cosmetics do not become functional
head gear.

This bridge keeps original texture/neutral geometry, and follows the actual
modern head model. It is not an assertion of byte-identical legacy OpenGL state,
dynamic sneaking/head-pose parity, every armor combination, shader behavior or
all camera angles. A positive native draw counter establishes execution of the
renderer, not visual quality by itself. Actual captured pixels must be reviewed.

## Pouch and focus selector

The pouch screen uses original `gui_focuspouch.png`,175×232 background,6×3 focus
slots at(37+x18,51+y18), native player inventory/hotbar and the original locked
hotbar marker at y209/UV240,0. Number-key hotbar swapping is disabled as in the
released GUI. Ordinary labels use the original localization; no development
status labels or reference catalog are added.

Both hands can own the live menu. Correct offhand ownership and stale source
protection are deliberate modern fixes to the released container's hardcoded
main-hand/current-slot assumption. The source bag is never a copied detached
inventory that can save over a moved or replaced physical bag. Detailed
container ownership/close/drop/swap checks are in
[FOCUS-POUCH-BETA26-AUDIT.md](FOCUS-POUCH-BETA26-AUDIT.md).

The F selector uses the original radial textures and focus icons. Its modern
non-pausing Screen is the cursor/input bridge for the original hold-F radial
HUD. It reads actual inventory/equipped pouch sources and sends the typed
server request; blank foci remain valid pouch contents but do not become
compiled castable choices. Source/target physical ownership is checked by the
common menu/selector implementation, not entrusted to the displayed icon.

## Required isolated native client route

The intended route has **14 declared scenes**, derived in order from the final
driver's literal `IMAGES`. Captures use `tc6-accessories-*.png`,1600×1000, and
the `THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE` marker. Successful completion
must have matching native `CLIENT_SMOKE_OK` and `RENDER_AUDIT_OK` counts; the
driver declaring frames is not completion evidence.

The required sequence establishes:

- Both original studies through actual C2S requests:16 AUROMANCY plus16 ARTIFICE
  raw observation,64 raw AUROMANCY theory and20 earned XP in total.
- All six original ordinary jewelry crafts through native crafting-menu result
  slots, retaining the paid mundane belt and mundane amulet for later consumers.
- One paid25vis/no-primal-crystal pouch craft consuming that actual paid belt.
- One native-world-ticked infusion consuming the actual paid mundane amulet,
  Vis Resonator and five original typed crystals, and exactly200 typed essentia.
  Initial prepared essentia is203, leaving3 total after payment.
- Native Curios button/menu actions physically equip the paid pouch, paid crafted
  Vis Amulet, raw supplied goggles and raw supplied apprentice ring; native
  synchronization must expose actual worn slots to the client.
- Actual main-hand and offhand pouch screens store three supplied compiled foci
  plus one supplied blank focus. The equipped-pouch radial selector performs one
  C2S install and one return without copying or losing a physical focus.
- A fresh raw gun begins at0 charge and gains at least20 units through the worn
  crafted amulet's actual five-tick native callback and paid ambient aura.
  Equipped goggles must render through their real Curios layer.

Parent studies, raw knowledge/materials/crystals, supplied goggles/apprentice
ring/gun/compiled and blank foci, arena/workbench/altar/pedestal/jars/candles,
stable initial aura and player setup are explicit fixtures. Loot-only apprentice
ring acquisition is not demonstrated by directly supplying it. No active
amulet target-charge, aura, source item, effect clock or RNG reset may substitute
for the claimed native consumer route. Recipe/knowledge payments must be bound
to the exact native facts log; screenshots alone do not prove those debits.

The client launches only through the verified Win32 hidden non-input desktop
launcher. It must exit its owned JVM with native0, retain the user's input
desktop, and never activate/switch desktops, synthesize global input or touch
manual saves. Root alone launches and packages it. No visible launch is an
allowed fallback.

Before delivery, two reviewers must each actually view all14 original full
captures, the complete review sheet and the compact delivery preview from the
same final native run. Reviews record image/log hashes. The build ZIP contains
the port, TerraBlender and Curios; the screenshot ZIP and reviewed preview are
delivered beside it. These prepared trials do not establish a complete survival
playthrough, independent multiplayer/client reload, audio or dynamic/all-angle
render parity. Any earlier stopped/failed attempts remain separate truthful
evidence and do not become a pass by changing the driver contract.

## Фактически завершённый QA0.39

`accessories-039-server2` прошёл1628/1628; `accessories-039-client2`14/14, оба native exit0.
Два проверяющих просмотрели все14полных1600×1000PNG и оба листа.
Настоящие study/recipe payments, Curios equip, pouch/source ownership,
radial C2S, worn recharge/render и точные native observations/SHA
содержатся в [artifact-report0.39](../../validation/artifact-report-0.39.json).
Server altar209→9 и client203→3 — отдельные подготовленные маршруты.
Ранние отказы, фикстуры, современные адаптации и границы —
[VALIDATION.md](../../VALIDATION.md); полного survival не заявляется.
