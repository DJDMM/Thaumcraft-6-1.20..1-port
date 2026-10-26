# Research table GUI — TC6 6.1.BETA26 audit

Baseline: pinned official `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and the matching `GuiResearchTable.java`/`ModelResearchTable.java`. The local
`work/theory-gui-javap.txt` contains the official GUI bytecode inspected for this port.

## Confirmed original behavior

- The 255×255 wooden desk, ordinary/gilded parchment, inspiration bulbs,
  bonus sheets and aid-selection highlight reuse original resources.
- Cards leave the paper stack with a seeded tilt, the last offer moves first,
  hovering enlarges/lifts an offer, and the chosen offer moves to the right-hand
  stack while rejected offers disappear. The original writes the selected card
  after that motion and displays the last resolved card between draws.
- Aid objects are shown as block/item icons in rows of at most six. Selection
  requires `selected.size() + 1 < availableInspiration`, leaving at least one
  inspiration. The port supports all 14 registered definitions; only 13 have
  detectable aid objects in BETA26. The null Eldritch object remains dormant.
- Category percentages approach their new values one point per player tick,
  with golden sparkles on increases. Rejected categories and reduced rewards
  use different colors. Percentages may exceed 100; displayed reward units
  are taken from the authoritative session's original reward calculation.
- Required items display their exact initialized templates and original atlas
  marker for consumed items. Celestial notes, aspect crystals and filled
  phials retain their metadata/NBT requirements; inventory counts use the
  same `TheoryCard.matchesRequirement` predicate as server payment.
- Enchantment requires five levels. Spellbinding spends up to five available
  levels; Dark Whispers consumes all levels and clears remaining XP. Scripting
  performs extra table callbacks: its second ink callback clamps at exhaustion,
  and the original ignored failure to consume extra paper is preserved.
- Eight original OGG files back `page`, `pageturn`, `clack` and `write`.
  The source sound clips are copied byte-for-byte from the pinned JAR.

## Visual repairs in 0.41.0

- Restored full original sheet geometry: seeded whole-sheet flips/tilt,
  6-to-8 zoom, 110px offer spacing, original text/item scales, inspiration
  cells, paper stack and completed-card stack. Sheets explicitly disable
  culling during their seeded Y mirror, matching the released renderer.
- Deal, hover and resolution use the original partial-render-frame formulas.
  Page sounds follow each staggered sheet leaving the deck. Previous actual
  completed-card seeds are persisted when paid selection succeeds; old saves
  retain their actual last card without inventing missing history.
- Removed the extra progress strip and substitute portal icons. Original
  Nether/End portal blocks have no item representation; Crimson aid is an
  entity class. Their original empty icon behavior is retained.
- Percentage increases use original atlas frames320/512, 16-frame loops,
  age32..39, scale24-to-48, alpha keys and native tick motion. A dedicated
  GUI shader declares SRC_ALPHA/ONE and the original1/255 alpha cutoff;
  a prior blendFunc call followed by a vanilla GUI blit is insufficient.
- Native screenshots and actual appearance review for these repairs remain
  pending until recorded by the root-run0.41 validation. Source/bytecode
  parity alone is not a visual pass.

## Explicit modern adaptations

### Overlapping offer tooltip found during the 0.41 native rerun

`visual-041-theory-final3` completed all22 native scenes, but actual full-frame
review rejected `tc6-theory-complete-offers-hover.png`: the port emitted its
generic drawing-cost hint and the hovered Sculpting card tooltip at the same
pointer position. Both text streams and their backgrounds overlapped. Native
completion does not establish visual acceptance; the rejected frame and the
unaltered run remain historical evidence.

The released `GuiResearchTable` has no generic drawing-cost hover tooltip.
The retained official bytecode in
`validation/theory-041-original-gui-click-javap.txt`
(SHA256 `240954d7dceea445ee501804be1cfabe0c76af87f062e1a304a8b2fa87a6ab4f`)
confirms that `drawScreen` calls the offered-sheet overlays at offset440,
then its missing-ink/paper messages at584/672 and native inventory tooltip
at678. `drawSheetOverlay` contains only the required-item tooltip branches.
The draw pile's background branch at891–925 merely tests empty choices and
the75×90 hover rectangle to highlight the question mark; it emits no text.

The repair removes only the port's `draw_cost` tooltip emission. It adds no
replacement hint and changes no card/item tooltip, layout, animation, input,
revision or payment behavior. The22-scene driver and all its assertions remain
unchanged. Fresh native execution and actual image review are required before
claiming this repair passed.

### Container click routing found during the 0.41 native rerun

`visual-041-theory-final1` stopped at scene2 after START had succeeded:
revision1, paper16 and no offers. Only its two idle-aid PNGs were captured.
Its explicit smoke failure remains historical evidence; Java exit0 only
records the orderly shutdown after failure. The desktop helper completed
with16 hidden windows and Default unchanged, while the interrupted outer
wrapper's original `run.json` still says `running`. The separate
`validation/visual-041-theory-final1-helper-recovery-archive.json` preserves
both facts and the hashes of the original metadata and two archived PNGs;
it does not rewrite that metadata or assert a pass.

The repaired original-style parchment hitboxes were unreachable because
the port returned immediately when modern `AbstractContainerScreen.mouseClicked`
returned true. Its mapped Forge47.4.10 bytecode returns true even for an
empty desk click (offsets584–585). Released BETA26 `GuiResearchTable`
instead invokes the void `GuiContainer` handler at offset4 and then
continues into its own aid/card/paper hitboxes; its `drawCards` call is
still reached at offset511. Exact full bytecode outputs are retained as
`validation/theory-041-modern-container-click-javap.txt` and
`validation/theory-041-original-gui-click-javap.txt`, with hashes in
`validation/theory-041-click-routing-analysis.json`.

The port now retains the modern handler's result, runs the existing guarded
parchment handling afterward, and returns that result if no custom action
applies. This preserves original native-container-first order and ordinary
slot/widget handling. It changes no payment, revision, availability or
animation guard, and none of the22-scene driver's assertions were changed.
Idle aids correctly become hidden once START creates a session; their
reported count0 in the failure is not a missing-icon defect. A fresh native
22-scene run and actual image review are still required after this fix.

- The full original desk and external category column scale as one layout
  on small Minecraft GUI dimensions, preserving slot and aid hitboxes.
  Full descriptions and exact item/resource details remain in tooltips.
- There is no client "animation complete" payment packet. Selection is paid
  atomically and persisted on the server first; only an authoritative changed
  `lastCard`/`placedCards` snapshot starts cosmetic resolution of detached old
  offers. Closing the GUI, lag, rejected/stale requests and interrupted
  animation cannot refund, repeat or postpone a server transaction.
- Paper/ink/XP/item shortages and request status are explicit, translated UI
  states. Local guards improve feedback; server ownership/revision/payment
  checks remain authoritative. Original empty-extra-paper/final-ink Scripting
  quirks do not become invented local resource gates.
- Discovered aid icons form compact rows, without reserving a gap for the
  original dormant Eldritch definition. Choice/completion actions wait until
  cosmetic motion finishes, while server payment remains immediate.
  Available controls render above the offered papers, preserving readable
  labels and matching their unchanged click regions.

## Isolated integrated client fixture

`runClient -PtheoryCompleteSmokeTest` enables the property
`thaumcraft.theoryCompleteSmokeTest` and uses `run/theory-complete-smoke`.
`TheoryCompleteClientSmokeTest` creates a fresh survival world. Late research
stages and exact initialized offers are explicit fixtures, never manual saves
or evidence of a full survival playthrough.

The fixture captures 22 scenes. Actual menus and C2S/S2C packets exercise
START with all 13 detected aids, DRAW/paper, SCRAP, paid SELECT and FINISH.
It checks deal/hover/resolution motion, GUI scales 2/4, missing XP/notes/ink/paper,
foreign ownership, stale revisions, unchanged rejected transactions and exact
completion rewards. New-deck scenes cover wrong/paid Synthesis aspect templates
and compound output; wrong/paid Infuse phial NBT and ingredient payment;
Dark Whispers XP reset and normal Warp; and Scripting extra callbacks at final
ink. Paid captures await the current request's accepted revision/card, a new
resolution animation, exact server/client inventory and vanilla XP equality,
and a completed GUI/HUD render. They never assign client payment states to
produce a screenshot. A world scene verifies the registered BER uses actual synced active-table
state. Only a successful marker in the root-run client log establishes a pass;
the fixture definition itself does not claim runtime success.
