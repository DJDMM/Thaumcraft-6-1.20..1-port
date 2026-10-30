# Закрытый исходный список работ0.42

Исходные40 канонических пробелов ниже перечислены относительно0.41.
В0.42 реализованы все148 путей и56 наполнений; сервер4 прошёл1786/1786,
пять клиентов дали87 PNG с двумя фактическими просмотрами. Оставшиеся
границы — длительное выживание, multiplayer, звук/все ракурсы и сторонняя
совместимость; исходная dormant Outer Lands не расширяется механиками TC4.
Точные условия/исключения — VALIDATION.md и artifact-report-0.42.json.
Ниже сохранён исторический план до выполнения, без повторного открытия
выполненных систем только из-за старых формулировок.

---

# Remaining TC6 BETA26 mechanics after accepted 0.41

This is an implementation ledger, not a test pass or a claim of full completion.
Baseline: 108/148 canonical entries, 44/56 operational infusion research/craft
paths, accepted commit 8a821fa. Display-only registrations are not runtime.
Original released BETA26 is authoritative; do not import TC4 dungeon mechanics.

## Exact forty canonical gaps in the baseline

| Category | Keys |
| --- | --- |
| Alchemy | METALPURIFICATION, LIQUIDDEATH, BOTTLETAINT, BATHSALTS, SANESOAP, ARCANESPA, ESSENTIASMELTERVOID, IMPROVEDSMELTING, IMPROVEDSMELTING2, EVERFULLURN, POTIONSPRAYER |
| Auromancy | WORKBENCHCHARGER |
| Basics | WARP, PLANTS, CrimsonRites, UNLOCKELDRITCH |
| Eldritch | BASEELDRITCH, VOIDSIPHON, VOIDSEERPEARL, VOIDROBEARMOR, PRIMALCRUSHER |
| Golemancy | MATSTUDVOID |
| Infusion | INFUSIONANCIENT, INFUSIONELDRITCH, PAVINGSTONES, CLOUDRING, CURIOSITYBAND, VERDANTCHARMS, CHARMUNDYING |
| Entity scans | !Wisp, !Pech, !ThaumSlime, !TaintSeed, !TaintCrawler, !Taintacle, !TaintSwarm, !CrimsonCultist, !BrainyZombie, !EldritchGuardian, !EldritchCrab |

Nine keys already have all or most native runtime and need their original
progression wiring: WARP, CrimsonRites, PLANTS, BATHSALTS, SANESOAP,
IMPROVEDSMELTING, IMPROVEDSMELTING2, !Wisp, !TaintSeed. These are not nine
missing systems. CrimsonRites permits its original parent/item stage as well
as its existing curio-consumption acquisition. WARP remains event-acquired.

## Twelve remaining registered infusion paths

charmundying, cloudring, curiosityband, primalcrusher, verdantheart,
verdantheartlife, verdantheartsustain, voidrobechest, voidrobehelm,
voidrobelegs, voidseerpearl, voidsiphon.

## Runtime work, in dependency order

1. Complete the started native Crimson lesser portal/cultists, exact natural
   surface portal/mound generation and player-kill Crimson Rites acquisition.
2. Complete Warp's original event scheduler/counter/severity, afflictions,
   Guardian/MindSpider/Orb and original private cosmetic messages/effects.
3. Complete finite Flux Goo, Bottle of Taint, slime/crawler/swarm/tentacles
   and genuine champion mechanics; preserve the already native seeds and terrain.
4. Connect Void/Eldritch study costs, metallurgy, native Void Siphon and
   existing native late armor/tools through actual acquisition prerequisites.
5. Complete Pech trading, Firebat, Brainy variants and remaining registered
   native combat entities, while respecting original natural accessibility.
6. Complete the remaining seven devices/material systems and late accessories
   represented by the table above. Do not duplicate already native mirrors,
   Bore, lamps, generator, pattern table, dioptra, furnace, cleansing or focus AI.
7. Reconcile the original inactive Ancient/Eldritch altar/dungeon-dependent
   entries explicitly; no invented ingredients or TC4 portal/dimension chain.
8. Verify paid gameplay, persistence/protection/automation, original visuals,
   survival acquisition and independent-server/client compatibility. Fixtures
   and representative scenes must remain distinct from a full survival pass.

No native 0.42 QA has been declared in this ledger. Final evidence and remaining
limits must be recorded only after the implementation and actual checks.
