# Исследования, рецепты и реальные scan-факты зеркал — 0.32

Основание — выпущенный Thaumcraft6.1.BETA26:
SHA-256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Исходные JSON и `ConfigRecipes/ConfigResearch` сверены в
[аудите зеркал](MIRRORS-BETA26-AUDIT.md); данный файл фиксирует интеграционные
проверки. Ни TC4, ни addon не определяют эту прогрессию.

| Запись | Строгие завершённые родители | Реальный платёж первой стадии |
| --- | --- | --- |
| ARCANELAMP | BASEARTIFICE | 16 raw Observation Artifice; обнаруженный Lux (`!lux`) |
| MIRROR | ARCANELAMP, INFUSION | 32 raw Theory Artifice +16 raw Observation Artifice; ender pearl×1; mirrored glass×1; scan-факт f_TELEPORT |
| MIRRORHAND | MIRROR | 32 raw Theory Artifice |
| MIRRORESSENTIA | MIRROR | 32 raw Theory Artifice |

Все четыре исходные записи имеют две стадии и не задают warp. Обработчик
порта сохраняет общий порядок: старт5XP, оплаченный переход5XP, завершённая
стадия3. Платёж ищет obtain в main inventory, не в offhand/броне.
Нехватка любого raw knowledge, предмета или факта отвергает весь платёж;
expected current-stage защищает от replay. Условия typed `!lux` означают
известный аспект, а не отсутствие света. Scan даёт собственные оригинальные
Observation rewards; проверки стоимости измеряют именно delta16 после scan,
не стирают законную награду игрока.

`MirrorProgressionScans` добавляет f_TELEPORT только для оригинального набора:
сам ender pearl ItemStack/ItemEntity, native Nether portal, End portal,
End portal frame BlockState и EnderMan class. Просмотр HUD не пишет знание.
Сам Thaumometer реконструирует target из серверной руки, ray и world entity;
scan-факт не приходит от клиента как готовая строка. Eye of Ender, chorus,
obsidian, End stone, Endermite, spawn egg и предмет зеркала не подменяют
указанный набор. Сам scan не открывает и не завершает платные записи.

## Обычные ингредиенты и оплаченный верстак

Для precursor chain добавлены исходные arcane definitions:

| Результат | Gate | Физические материалы | Платёж |
| --- | --- | --- | --- |
| mirrored_glass×1 | начатый BASEARTIFICE | quicksilver×1 + glass pane×1, shapeless | 50vis, Aqua×1, Ordo×1 |
| lamp_arcane×1 | начатый ARCANELAMP | четыре iron plates вокруг amber block | 50vis, Aer×1, Ignis×1 |

Bare recipe gates повторяют released `isResearchKnown` и разрешают вошедшую
стадию1; это независимо от strict parent requirement книги. Неизвестное
исследование и отсутствие одного кристалла не платят часть vis/materials.
Результат реального `ArcaneWorkbenchBlockEntity.craft` записывает craft proof;
пустой grid не повторяет оплату. Обычный shaped рецепт упаковки четырёх amber
gems даёт1amber block; распаковка возвращает4. Это recipe route, а не новый
скрытый research unlock или заявление о полном добывающем прохождении.
Glass-pane tag принимает исходное семейство paneGlass, включая цветные панели.

## Три точных рецепта наполнения

| Gate / результат | Central | Компоненты | Исходная эссенция / instability |
| --- | --- | --- | --- |
| MIRROR / mirror×1 | mirrored_glass | gold ingot×3, ender pearl×1 | Motus/Tenebrae/Permutatio по25;1 |
| MIRRORESSENTIA / mirror_essentia×1 | mirrored_glass | iron ingot×3, ender pearl×1 | Motus/Aqua/Permutatio по25;2 |
| MIRRORHAND / hand_mirror×1 | plain unbound item mirror | wooden stick, compass, **empty map** | Instrumentum/Motus по50;5 |

`Items.MAP` оригинального `ConfigRecipes` — `ItemEmptyMap`, подтверждённый
JAR bytecode. Поэтому native ingredient — `minecraft:map`, а filled_map не
подходит. Original plain mirror metadata0 сопоставляется только с не связанным
item mirror: полный namespaced либо читаемый legacy link отображается в
legacy damage1 и не может оплатить этот central. У unrelated NBT без полного
endpoint нет силы link. Essentia mirror тоже не заменяет central item mirror.

Проверки требуют каждый peripheral и exact multiplicity/count; plan/network
сохраняют costs, map identity и bound predicate. Каждый recipe mode — replace,
не mutation; итоговая единица не наследует central link/посторонний NBT.

## Определённые native проверки и границы

`MirrorProgressionGameTests` содержит **20** определений:
strict parents/census94; lamp Lux/raw shortage; пять отдельных Mirror shortages;
exact32/16/physical obtain/replay/save; offhand/book; оба32Theory children;
реальные held/dropped pearl,3 portal block rays и Enderman; отрицательные
specimens/readonly hover;3 exact started infusion plans; peripheral/central
requirements; empty map/bound central; network/detached output; обычная amber
route;2 реально оплаченных arcane crafts;3 native world-ticked altar payments.

Алтарные проверки запускают настоящий caster block callback, ждут native idle
stability, затем обычные matrix world ticks. Проверяются 75/75/100 физической
эссенции, central и4/4/3 reagents, один fresh output, настоящий craft proof,
исследовательский платёж10XP и отсутствие повторной оплаты после100native ticks.
Использованы разные высоты временных арен220/250/280; исходные состояния и
tile NBT восстанавливаются после финального assertion. Проверенные precursor
research, raw balances, инструменты, материалы, свечи и заполненные банки —
подготовленные условия. Эти сценарии не доказывают полное survival-прохождение,
самостоятельную добычу каждого материала или независимый multiplayer.

Definitions не являются passed checks. Итоги свежего native server, final
hidden client, builds, source fingerprints и настоящие screenshots публикует
ведущий агент в VALIDATION.md только после проверки. Старые доказательства
0.31 не являются доказательствами реализации этих четырёх новых записей.
