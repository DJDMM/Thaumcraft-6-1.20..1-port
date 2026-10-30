# BETA26 remaining supported progression — 0.42 preparation

Authority: released `Thaumcraft-1.12.2-6.1.BETA26.jar`,
SHA256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
The original research JSON was compared byte-for-byte with every copied
`legacy_research` file. Released `ConfigRecipes` and
`PacketSyncProgressToServer.checkRequisites` bytecode was inspected with
Java17 javap; the pinned recovered source is a navigation aid.

This slice connects nine original records to already native systems. It does
not implement nine entirely new gameplay systems, and does not make the
remaining natural world or Eldritch mechanics complete.

| Key | Original acquisition / stage payment |
| --- | --- |
| WARP | Actual normal/permanent Warp after FIRSTSTEPS through the existing event-specific acquisition; an arbitrary empty book request cannot grant this hidden root. |
| CrimsonRites | Original `!CrimsonCultist` parent; stage1 consumes one physical metadata6 curio from main inventory. The original two-stage book route is supported alongside the existing physical-use route. Only the separate item-use route has the actual-Warp >20 predicate and its knowledge/Warp rewards. |
| PLANTS | Actual scan of Greatwood/Silverwood logs or saplings, Cinderpearl, Shimmerleaf or Vishroom, including ScanBlock's item registration. Leaves/planks/ordinary flowers are excluded. |
| !Wisp | Actual native Wisp class, not a catalogue ID imitation. |
| !TaintSeed | Actual native seed and Prime subclass, matching original ScanEntity subclasses=true. |
| BATHSALTS | Completed HEDGEALCHEMY and the original event fact !BATHSALTS; Observation Eldritch16 + Theory Alchemy32. |
| SANESOAP | Completed BATHSALTS; Observation Eldritch16 + Theory Alchemy32. |
| IMPROVEDSMELTING | Completed TUBES and BELLOWS; Observation Alchemy16 + Observation Artifice16. |
| IMPROVEDSMELTING2 | Completed IMPROVEDSMELTING; the same16+16 Observation payment. |

PLANTS also acquires the original independent `!PLANTWOOD`,
`!PLANTCINDERPEARL`, `!PLANTSHIMMERLEAF` and `!PLANTVISHROOM` proofs.
The one canonical no-cost stage pays5XP once; later independent addenda do not
pay another canonical reward. HUD matching remains read-only. The full
server scan commit owns completion, ordinary knowledge and persistence.

The paid two-stage studies retain5XP on start, atomic stage payment and5XP
on completion. Their last empty stages are skipped as in the original.
Neither an incomplete parent, missing category/type/raw unit nor an offhand
cost item can partially pay a study. Stale stage requests do not pay twice.

## Physical recipes

Three previously absent recipe resources are supplied:

- Bath Salts: Salis Mundus plus Cognitio40/Aer40/Ordo40/Victus40.
- Sanity Soap: one Flesh Block plus Cognitio75/Alienis50/Ordo75/Victus50.
- Sanity Checker: the original BN / M N / BN shaped grid; brass nuggets,
  two brains and mirrored glass;20vis, Ordo1/Perditio1, WARP gate.

The existing Aux/Vent recipes already produce native smelter attachments:
Aux100vis, Aer1/Terra1; Vent150vis, Aer1. Their native smelting mechanics are
not reimplemented in this slice. The original bare recipe gate is
`isResearchKnown` (started); strict completion remains the canonical parent
predicate. The common Crucible gate correction is owned by root and is
required for a started Bath/Soap recipe to behave like the release.

## Executed-validation status

`RemainingProgressionGameTests` defines ten native tests covering exact
parents, item/knowledge shortage atomicity, paid completion/replay/reload,
the legitimate Crimson book route, read-only hover and physical held scan,
all seven plant families/addenda, native Wisp/seed/Prime scan inheritance,
physical Crucible outputs and three real Arcane Workbench crafts.

Raw parents, knowledge, materials, prepared hot Crucible state, initial aura,
native actors and a controlled test arena are explicitly supplied fixtures.
The tests do not establish natural Crimson Rites acquisition, full survival,
multiplayer, audiovisual parity or a completed release. This implementation
agent did not run Gradle, the native server or a client. Root must record
actual execution and shared census changes after integration.
