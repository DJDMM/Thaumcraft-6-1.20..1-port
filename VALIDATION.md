# Исправления визуального переноса0.41.0

Дата упаковки: **2026-10-26**. TC6.1.BETA26 / Minecraft1.20.1 / Forge47.4.10 / Java17.
Охват108/148 исследований и44/56 наполнений не изменён.

`visual-041-server4` прошёл1678/1678, native/Gradle exit0.
Его исходники, все восемь клиентов и итоговая сборка совпадают по4580
отпечаткам файлов. Исключений для различающихся исходников нет.
После отклонённых кадров Bolt восстановлены направляющие точки,
стыки по биссектрисам, торцы и повторение текстуры исходного GLE.
Прямое сравнение геометрии с выпущенным JAR прошло166сценариев; широкая
проекция луча возле камеры сохраняется как свойство оригинала. Повторные клиентские проверки используют настоящий
протокол закрытия контейнера и подготовленные начальные условия ауры.
Таумометр проверяется с исходной опцией wuss на отдельной копии
конфигурации в памяти; файл не меняется, опция восстанавливается.

| Клиент | Фактический запуск | Сценарии | PNG |
| --- | --- | ---: | ---: |
| WorldVisual | `visual-041-world-final9` | 23 | 23 |
| VisualFx | `visual-041-fx-final7` | 53 | 53 |
| ArmorVisual | `visual-041-armor-final7` | 19 | 19 |
| TheoryComplete | `visual-041-theory-final5` | 22 | 22 |
| Infusion | `visual-041-infusion-final5` | 15 | 15 |
| Auromancy | `visual-041-auromancy-final5` | 33 | 34 |
| Thaumometer | `visual-041-thaumometer-final3` | 22 | 22 |
| Sounding | `visual-041-sounding-final2` | 8 | 8 |

Каждый выбранный JVM завершился с exit0; активный входной рабочий стол
не менялся. Все196полных1600×1000PNG, каждый лист и выбранное превью
фактически просмотрены корневым и отдельным проверяющим. Дополнительный
кадр молнии Auromancy объясняет разницу195сценариев/196PNG.
Совпадение ресурсов и счётчики FX не заменяли просмотр.

Добавлены мировые кадры настоящего второго оплаченного наполнения:
эссенция, руны, фрагменты ингредиентов и завершение. Отдельный начальный
нестабильный алтарь проверяет естественные искры. События5/11 на пустом
пьедестале явно подготовлены для проверки визуального callback; они не
доказывают естественное подавление нестабильности, расход стабилизатора
или выброс оплаченного ингредиента.

Ранние ошибки и прерванные запуски сохранены. FX5 был отклонён несмотря
на53native-сценария: пену скрывал бортик. Theory3 был отклонён из-за двух
наложенных подсказок. FX6 r2 отклонил слабую дугу щита на синем небе;
исходный r3 сохранил тот отказ и проверил настоящий additive-сигнал
на прежней области с прежними порогами, оба контрольных кадра пусты.
Кадры и параметры эффекта ради этого не изменялись.
Полные PNG ранних попыток сохранены локально; итоговый архив снимков
содержит выбранные текущие кадры и связанные доказательства. Он не
дублирует все исторические изображения.

Collector частично закрыт штатным death-poof/добычей, часть ранних
эффектов мала или закрыта сущностями; подробные ограничения отдельных
кадров сохранены в обоих отзывах. QA использует подготовленные материалы,
знания, актёров, ауру и арены. Это не полный survival, независимый
multiplayer, звук, все ракурсы или произвольные шейдерпаки.

Проверено2893JSON/3779ресурсных
совпадений JAR. Оригинальные TC6 media и отдельные vanilla1.12.2 вода/метаданные
связаны SHA и CRC. [Полный отчёт](validation/artifact-report-0.41.json).

---

# Исправление отрисовки Сонара0.40.1

Дата: **2026-10-19**. TC6.1.BETA26 / Minecraft1.20.1 / Forge47.4.10 / Java17.

Пользователь обнаружил верную проблему на снимке0.40: сигнала за камнем
не было. Прежний `visibleRipples()` проверял только возраст/задержку
объектов эффекта. Визуальный просмотр не выявил дефект, поэтому его вывод
о корректной видимости Сонара был ошибочным. Сами старые журналы/снимки
сохранены; успешные оплаты и остальные действия остаются в своём исходном
объёме проверки. Новый отчёт не объявляет повторное полное прохождение0.40.

Причина установлена по точному Forge47.4.10 bytecode: `NO_DEPTH_TEST`
приGL_ALWAYS519 ничего не меняет, а ParticleEngine включает depth перед
AFTER_PARTICLES. Теперь оба исходных слоя имеют собственный shard с явными
disable/enable; атлас, группировка, цвета, дальность и время жизни прежние.
[Технический аудит](docs/tc6/SOUNDING-DEPTH-BETA26-AUDIT.md).

Свежий `sounding-0401-server2` прошёл1673/1673, native0/Gradle0. Целевой клиент
`sounding-0401-client2` прошёл8/8, native0, PID28020,
16 собственных скрытых окон,
входной столDefault→Default.
Все8полных1600×1000PNG и оба листа просмотрены двумя проверяющими.

В каждом режиме Fancy/Fabulous настоящий Shift+ПКМ по сплошной стене
создаёт три S2C-группы скрытой угольной, алмазной и железной руды. Два
действия оплачивают ровно10прочности. Подготовлены сырой зачарованный
инструмент, неподвижная камера и стена; это не повтор платной прогрессии.
Кадры до/после не содержат эффекта; активные кадры дают реальное изменение
пикселей в проекциях всех трёх скрытых групп:

- fancy-coal: 2913 изменённых пикселей; контроль до/после 0.
- fancy-diamond: 3419 изменённых пикселей; контроль до/после 0.
- fancy-iron: 4486 изменённых пикселей; контроль до/после 0.
- fabulous-coal: 3216 изменённых пикселей; контроль до/после 0.
- fabulous-diamond: 3419 изменённых пикселей; контроль до/после 0.
- fabulous-iron: 4486 изменённых пикселей; контроль до/после 0.

Это сравнение настоящих PNG, без перерисовки игровых пикселей. Уголь имеет
исходный тёмный alpha-сигнал, две другие группы — additive. Контрольная
изумрудная руда вне кубаrank1 исключена по фактическому числу групп;
она вне кадра, поэтому её исключение не выдаётся за пиксельную проверку.
Произвольные шейдерпаки, независимый multiplayer, звук, production launch
и полный survival не проверялись. Полные события искажения ещё не перенесены.

Native markers:

- `[17:03:06] [Server thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_FIXTURES: isolatedSurvivalWorld/rawRank1DiamondPick/opaqueStoneWallX-12to12Y112to120Z-6to0/cameraPlayer0.5,112,4.5Yaw180Pitch0Fov80/click0,113,0/rank1Cube8/coalCenter-4,114.5,-1.5/diamondCenter0.5,115,-1.5/ironCenter5,114.5,-1.5/excludedEmerald9,114,-2/noActiveEffectClockOrRenderFlagInjection/notPaidProgression`
- `[17:03:07] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_BEFORE: graphics=fancy camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 fov=80 solidWall=true nativeRipples0 damage=0`
- `[17:03:07] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fancy-before.png graphics=fancy camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=0 ripples=[]`
- `[17:03:07] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fancy-before.png`
- `[17:03:09] [Server thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_USE: mode=FANCY/rawRank1VanillaDiamondPickaxe/actualSneakUseC2S/one5Durability/nativeS2C3Groups/coalAlpha1/additive2/noTerrainMutation/noInjectedRipples`
- `[17:03:09] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fancy-active-early.png graphics=fancy camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=5 ripples=[ReadOnlyRipple[position=(-4.0, 114.5, -1.5), color=1052688, dark=true, delay=0, age=14], ReadOnlyRipple[position=(0.5, 115.0, -1.5), color=6155509, dark=false, delay=0, age=22], ReadOnlyRipple[position=(5.0, 114.5, -1.5), color=14200723, dark=false, delay=0, age=14]]`
- `[17:03:09] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fancy-active-early.png`
- `[17:03:09] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fancy-active-late.png graphics=fancy camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=5 ripples=[ReadOnlyRipple[position=(-4.0, 114.5, -1.5), color=1052688, dark=true, delay=0, age=19], ReadOnlyRipple[position=(0.5, 115.0, -1.5), color=6155509, dark=false, delay=0, age=27], ReadOnlyRipple[position=(5.0, 114.5, -1.5), color=14200723, dark=false, delay=0, age=19]]`
- `[17:03:09] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fancy-active-late.png`
- `[17:03:11] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fancy-after.png graphics=fancy camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=5 ripples=[]`
- `[17:03:11] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fancy-after.png`
- `[17:03:13] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_BEFORE: graphics=fabulous camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 fov=80 solidWall=true nativeRipples0 damage=5`
- `[17:03:13] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fabulous-before.png graphics=fabulous camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=5 ripples=[]`
- `[17:03:13] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fabulous-before.png`
- `[17:03:14] [Server thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_USE: mode=FABULOUS/rawRank1VanillaDiamondPickaxe/actualSneakUseC2S/one5Durability/nativeS2C3Groups/coalAlpha1/additive2/noTerrainMutation/noInjectedRipples`
- `[17:03:14] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fabulous-active-early.png graphics=fabulous camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=10 ripples=[ReadOnlyRipple[position=(-4.0, 114.5, -1.5), color=1052688, dark=true, delay=0, age=14], ReadOnlyRipple[position=(0.5, 115.0, -1.5), color=6155509, dark=false, delay=0, age=22], ReadOnlyRipple[position=(5.0, 114.5, -1.5), color=14200723, dark=false, delay=0, age=14]]`
- `[17:03:14] [IO-Worker-17/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fabulous-active-early.png`
- `[17:03:14] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fabulous-active-late.png graphics=fabulous camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=10 ripples=[ReadOnlyRipple[position=(-4.0, 114.5, -1.5), color=1052688, dark=true, delay=0, age=19], ReadOnlyRipple[position=(0.5, 115.0, -1.5), color=6155509, dark=false, delay=0, age=27], ReadOnlyRipple[position=(5.0, 114.5, -1.5), color=14200723, dark=false, delay=0, age=19]]`
- `[17:03:15] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fabulous-active-late.png`
- `[17:03:16] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_HEARTBEAT: scene=7 phase=0 uses=2 ripples=[ReadOnlyRipple[position=(-4.0, 114.5, -1.5), color=1052688, dark=true, delay=0, age=39], ReadOnlyRipple[position=(5.0, 114.5, -1.5), color=14200723, dark=false, delay=0, age=39]]`
- `[17:03:16] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_NATIVE_FRAME: name=tc6-sounding-fabulous-after.png graphics=fabulous camera=(0.5, 113.26999998092651, 4.5) yaw=180.0 pitch=0.0 damage=10 ripples=[]`
- `[17:03:16] [IO-Worker-16/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\sounding-smoke\.\screenshots\tc6-sounding-fabulous-after.png`
- `[17:03:16] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_RENDER_AUDIT_OK: 8 captures/FancyAndFabulous/threeNativeHiddenOreGroups/oneCoalAlphaTwoAdditive/closedWallBeforeActiveEarlyActiveLateAfter/readOnlyNativeFacts/actualPixelsRequireIndependentReview`
- `[17:03:16] [Render thread/INFO] [th.eq.to.cl.SoundingClientSmokeTest/]: THAUMCRAFT_SOUNDING_CLIENT_SMOKE_OK: 8 captures/2actualSneakUseC2S/nativeS2C3GroupsEach/exact10Durability/noTerrainMutation/noInjectedRipples/notPaidProgression,world=thaumcraft-sounding-smoke-1792414949135`

Сборка: 2882JSON, 3720ресурсных
проверок; оригинальные674PNG/7JPG/60OGG сохранены. Версия, CRC, три JAR
с зависимостями, SHA исходников/кадров и ранний успешный демонстрационный
прогон1 связаны с [artifact-report0.40.1](validation/artifact-report-0.40.1.json).

---

# Проверка улучшений наполнением0.40.0

Дата: **2026-10-18**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Охват108/148 исследований,
44/56 наполнений; исходных физических рецептов56.
Контракт — [UPGRADES.md](UPGRADES.md), точные файлы/SHA —
[artifact-report0.40](validation/artifact-report-0.40.json).

Свежий полный `infusion-upgrades-040-server3` прошёл **1673/1673**, native exit0 и BUILD SUCCESSFUL.
Сохранены1628 предыдущих проверок; новые определения: `{'equipment/enchantment/InfusionEnchantmentEffectsGameTests.java': 32, 'research/InfusionUpgradesProgressionGameTests.java': 13}`.
Проверены исходные строгие родители, теории, янтарь, XP, Warp и replay,
настоящие оплаченные наполнения, обычные infused-инструменты и Forge hooks,
руны на настоящих Curios/броне, типизированные маски/очки и их потребители.
Серверные прямые block-entity callbacks устанавливают физическую оплату,
а не истечение мирового времени. Реальный маршрут клиента проверен отдельно.

Клиент `infusion-upgrades-040-client4` прошёл **24/24**, JVM exit0, PID15728,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`; входной стол
`Default→Default`. Переключений/global input нет.
Два проверяющих просмотрели все24полных1600×1000PNG, полный обзорный
лист и компактное превью одного финального native прогона.

Фактические native markers:

- `[18:44:25] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_FIXTURES: ownedSurvivalArena/fourRawStableAltars+24Pedestals+128Candles+8stabilizers/initialTypedJarsFor14originalInfusions+1eachAspect/rawstrictParents128InfusionTheoryOneAmber/rawVanillaUnenchantedTools+FortressChestLegsHelmDamage17OwnerMemory+MundaneRingCaster/rawComponents+Initial5000vis0fluxPer49Chunks/raw1collectorLog9stone3burrowingLogs32ironOreSoundingOreAndDarkCave/controlledNoAIVanillaActors; targetstudies/enchants/runic/masksAbsent/noActiveAuraChargeClockRngSetters/notFullSurvivalMultiplayerReloadAudio`
- `[18:44:25] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_STUDY_PAID: INFUSIONENCHANTMENT/actualC2S/rawInfusionTheory64/earnedXP10/runicAmber1orMaskWarpPermanent1Normal1`
- `[18:44:26] [IO-Worker-18/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-enchantment-study.png`
- `[18:44:26] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_STUDY_PAID: RUNICSHIELDING/actualC2S/rawInfusionTheory32/earnedXP10/runicAmber1orMaskWarpPermanent1Normal1`
- `[18:44:27] [IO-Worker-16/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-runic-study.png`
- `[18:44:27] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_STUDY_PAID: FORTRESSMASK/actualC2S/rawInfusionTheory32/earnedXP10/runicAmber1orMaskWarpPermanent1Normal1`
- `[18:44:28] [IO-Worker-16/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-fortress-mask-study.png`
- `[18:44:28] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: iecollector/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/180rawExpectedEssentia/noPaidOutputSupplied`
- `[18:44:28] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: iedestructive/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/450rawExpectedEssentia/noPaidOutputSupplied`
- `[18:44:28] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: ieburrowing/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/230rawExpectedEssentia/noPaidOutputSupplied`
- `[18:44:28] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: iesounding/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/100rawExpectedEssentia/noPaidOutputSupplied`
- `[18:44:38] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=3 phase=0 batch=0 launch=2 infusions=0 paidEss=0 shield=0`
- `[18:44:48] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=3 phase=1 batch=1 launch=0 infusions=0 paidEss=0 shield=0`
- `[18:44:58] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=3 phase=1 batch=1 launch=0 infusions=0 paidEss=0 shield=0`
- `[18:45:08] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=3 phase=1 batch=1 launch=0 infusions=0 paidEss=0 shield=0`
- `[18:45:18] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=3 phase=1 batch=1 launch=0 infusions=0 paidEss=0 shield=0`
- `[18:45:21] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: iecollector/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/180essentia/2reagents/physicalRemainderConserved/totalInfusions1`
- `[18:45:21] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-collector.png`
- `[18:45:31] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:45:41] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:45:51] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:46:01] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:46:11] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:46:21] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:46:31] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=4 phase=1 batch=1 launch=0 infusions=1 paidEss=180 shield=0`
- `[18:46:32] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: iedestructive/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/450essentia/2reagents/physicalRemainderConserved/totalInfusions2`
- `[18:46:33] [IO-Worker-18/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-destructive.png`
- `[18:46:33] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: ieburrowing/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/230essentia/2reagents/physicalRemainderConserved/totalInfusions3`
- `[18:46:34] [IO-Worker-18/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-burrowing.png`
- `[18:46:34] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: iesounding/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/100essentia/2reagents/physicalRemainderConserved/totalInfusions4`
- `[18:46:35] [IO-Worker-18/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-sounding.png`
- `[18:46:35] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: ierefining/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/140rawExpectedEssentia/noPaidOutputSupplied`
- `[18:46:35] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: iearcing/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/100rawExpectedEssentia/noPaidOutputSupplied`
- `[18:46:35] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: ieessence/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/100rawExpectedEssentia/noPaidOutputSupplied`
- `[18:46:35] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: ielamplight/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/100rawExpectedEssentia/noPaidOutputSupplied`
- `[18:46:45] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=7 phase=1 batch=2 launch=0 infusions=4 paidEss=960 shield=0`
- `[18:46:55] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=7 phase=1 batch=2 launch=0 infusions=4 paidEss=960 shield=0`
- `[18:47:05] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=7 phase=1 batch=2 launch=0 infusions=4 paidEss=960 shield=0`
- `[18:47:14] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: ierefining/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/140essentia/2reagents/physicalRemainderConserved/totalInfusions5`
- `[18:47:14] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-refining.png`
- `[18:47:15] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: iearcing/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/100essentia/2reagents/physicalRemainderConserved/totalInfusions6`
- `[18:47:15] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-arcing.png`
- `[18:47:16] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: ieessence/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/100essentia/2reagents/physicalRemainderConserved/totalInfusions7`
- `[18:47:16] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-essence.png`
- `[18:47:17] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: ielamplight/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/100essentia/2reagents/physicalRemainderConserved/totalInfusions8`
- `[18:47:18] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-lamplight.png`
- `[18:47:18] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: runicarmor/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/80rawExpectedEssentia/noPaidOutputSupplied`
- `[18:47:28] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=11 phase=2 batch=2 launch=0 infusions=8 paidEss=1400 shield=0`
- `[18:47:38] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=11 phase=2 batch=2 launch=0 infusions=8 paidEss=1400 shield=0`
- `[18:47:41] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: runicarmor/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/80essentia/2reagents/physicalRemainderConserved/totalInfusions9`
- `[18:47:42] [IO-Worker-18/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-runic-armor.png`
- `[18:47:42] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: runicarmor/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/80rawExpectedEssentia/noPaidOutputSupplied`
- `[18:47:52] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=12 phase=2 batch=2 launch=0 infusions=9 paidEss=1480 shield=0`
- `[18:48:02] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=12 phase=2 batch=2 launch=0 infusions=9 paidEss=1480 shield=0`
- `[18:48:05] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: runicarmor/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/80essentia/2reagents/physicalRemainderConserved/totalInfusions10`
- `[18:48:06] [IO-Worker-18/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-runic-accessory.png`
- `[18:48:06] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: helmgoggles/physicalUnenchantedTargetOrPreservedPaidInput/2rawReagents/80rawExpectedEssentia/noPaidOutputSupplied`
- `[18:48:16] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=13 phase=2 batch=2 launch=0 infusions=10 paidEss=1560 shield=0`
- `[18:48:26] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=13 phase=2 batch=2 launch=0 infusions=10 paidEss=1560 shield=0`
- `[18:48:30] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: helmgoggles/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/80essentia/2reagents/physicalRemainderConserved/totalInfusions11`
- `[18:48:30] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HELM_WORN: helmgoggles/actualArmorQuickMoveC2S/samePhysicalHelm/gogglesRetained/damageAndUnrelatedNBTConserved`
- `[18:48:30] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-fortress-goggles.png`
- `[18:48:31] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: maskgrinningdevil/physicalUnenchantedTargetOrPreservedPaidInput/6rawReagents/180rawExpectedEssentia/noPaidOutputSupplied`
- `[18:48:40] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=14 phase=2 batch=2 launch=0 infusions=11 paidEss=1640 shield=0`
- `[18:48:50] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=14 phase=2 batch=2 launch=0 infusions=11 paidEss=1640 shield=0`
- `[18:49:00] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=14 phase=2 batch=2 launch=0 infusions=11 paidEss=1640 shield=0`
- `[18:49:10] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=14 phase=2 batch=2 launch=0 infusions=11 paidEss=1640 shield=0`
- `[18:49:20] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=14 phase=2 batch=2 launch=0 infusions=11 paidEss=1640 shield=0`
- `[18:49:25] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: maskgrinningdevil/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/180essentia/6reagents/physicalRemainderConserved/totalInfusions12`
- `[18:49:25] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HELM_WORN: maskgrinningdevil/actualArmorQuickMoveC2S/samePhysicalHelm/gogglesRetained/damageAndUnrelatedNBTConserved`
- `[18:49:26] [IO-Worker-20/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-grinning-devil.png`
- `[18:49:26] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: maskangryghost/physicalUnenchantedTargetOrPreservedPaidInput/6rawReagents/180rawExpectedEssentia/noPaidOutputSupplied`
- `[18:49:36] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=15 phase=2 batch=2 launch=0 infusions=12 paidEss=1820 shield=0`
- `[18:49:46] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=15 phase=2 batch=2 launch=0 infusions=12 paidEss=1820 shield=0`
- `[18:49:56] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=15 phase=2 batch=2 launch=0 infusions=12 paidEss=1820 shield=0`
- `[18:50:06] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=15 phase=2 batch=2 launch=0 infusions=12 paidEss=1820 shield=0`
- `[18:50:16] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=15 phase=2 batch=2 launch=0 infusions=12 paidEss=1820 shield=0`
- `[18:50:21] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: maskangryghost/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/180essentia/6reagents/physicalRemainderConserved/totalInfusions13`
- `[18:50:21] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_MASK_WITHER: nativePaidWornMask1/controlledVanillaZombie12attack/nativeLivingHurtEvent/Wither80/noRNGReset/noHealthSet`
- `[18:50:21] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HELM_WORN: maskangryghost/actualArmorQuickMoveC2S/samePhysicalHelm/gogglesRetained/damageAndUnrelatedNBTConserved`
- `[18:50:21] [IO-Worker-20/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-angry-ghost.png`
- `[18:50:22] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RAW_COMPONENTS: masksippingfiend/physicalUnenchantedTargetOrPreservedPaidInput/6rawReagents/180rawExpectedEssentia/noPaidOutputSupplied`
- `[18:50:32] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=16 phase=2 batch=2 launch=0 infusions=13 paidEss=2000 shield=0`
- `[18:50:42] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=16 phase=2 batch=2 launch=0 infusions=13 paidEss=2000 shield=0`
- `[18:50:52] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=16 phase=2 batch=2 launch=0 infusions=13 paidEss=2000 shield=0`
- `[18:51:02] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=16 phase=2 batch=2 launch=0 infusions=13 paidEss=2000 shield=0`
- `[18:51:12] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HEARTBEAT: scene=16 phase=2 batch=2 launch=0 infusions=13 paidEss=2000 shield=0`
- `[18:51:16] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_ALTAR_PAID: masksippingfiend/actualCasterC2S+nativeWorldTicks/fullInputNBTPreserved/180essentia/6reagents/physicalRemainderConserved/totalInfusions14`
- `[18:51:16] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_HELM_WORN: masksippingfiend/actualArmorQuickMoveC2S/samePhysicalHelm/gogglesRetained/damageAndUnrelatedNBTConserved`
- `[18:51:17] [IO-Worker-20/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-paid-sipping-fiend.png`
- `[18:51:20] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_RUNIC: actualArmorQuickMoveC2S+CuriosButtonRingC2S/paidChest1+paidRing1/nativeWallClock2credits2vis/fullFortressSet/sameFourTimesMutatedHelmMask2Goggles`
- `[18:51:21] [IO-Worker-20/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-curios-runic-shield.png`
- `[18:51:22] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_COLLECTOR: paidVanillaDiamondAxe/nativeSurvivalMiningC2S/oneOakLogHomedThenPhysicalPickup/durability1`
- `[18:51:23] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-collector.png`
- `[18:51:24] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_DESTRUCTIVE: paidVanillaDiamondPickaxe/nativeSurvivalC2S/nineRealStoneBlocks/nineDurability`
- `[18:51:25] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_BURROWING: paidVanillaDiamondAxe/nativeSurvivalC2S/furthestTopLogOnly/baseTwoConserved/oneDurability`
- `[18:51:25] [IO-Worker-20/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-destructive-burrowing.png`
- `[18:51:30] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_REFINING: paidVanillaDiamondPickaxe/nativeSurvivalMiningC2S/actualRawIronToCluster/successAfter4OrdinaryOres/noRNGReset`
- `[18:51:31] [IO-Worker-20/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-refining.png`
- `[18:51:32] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_ARCING: paidVanillaDiamondSword/nativeAttackC2S/actualPrimaryAndSecondaryCowDamage/noRNGReset`
- `[18:51:39] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_ESSENCE: paidVanillaDiamondSword/nativeAttackC2S/nativeCowDeathAndTypedAspectCrystals/4Attacks/noRNGReset`
- `[18:51:39] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_SIPPING: actualPaidMask2/nativeLivingHurtEventObservedHeals2/controlledNativeWounds1/noHealthSet/noRNGReset`
- `[18:51:40] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-arcing-essence.png`
- `[18:51:40] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_SOUNDING: paidVanillaDiamondPickaxe/nativeSneakUseC2S/oneFiveDurabilityPayment/nativeS2COreRipples/noScanOrTerrainMutation`
- `[18:51:41] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-sounding.png`
- `[18:51:43] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_NATIVE_LAMPLIGHT: paidVanillaDiamondPickaxe/nativeSurvivalMiningC2S/combinedLightBelow10/afterNativeWorldTickENDGlimmer15/oneOrdinaryDurability/noExtraVis`
- `[18:51:43] [IO-Worker-17/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\infusion-upgrades-smoke\.\screenshots\tc6-infusion-upgrades-native-lamplight.png`
- `[18:51:43] [Server thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_FACTS: scenes24/studies3/theoryInfusion128/researchAmber1/earnedXP30/maskPermanentWarp1NormalWarp1/paidInfusions14/enchants8/runicArmor1Curio1/onePhysicalHelmFourMutations/paidEssentia2180/shieldCredits4/maskWither1/maskNativeHeals2/refiningOreAttempts4/essenceAttackAttempts4/allEightNativeEffects/noActiveAuraChargeRngClockSetters/notFullSurvivalMultiplayerReloadAudio`
- `[18:51:43] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_RENDER_AUDIT_OK: 24 scenes; actualBook/nativeAltars/physicalPaidTools/nativeCurios/fortressNBT/nativeEffects/noDeveloperLabels`
- `[18:51:43] [Render thread/INFO] [th.eq.up.cl.InfusionUpgradesClientSmokeTest/]: THAUMCRAFT_INFUSION_UPGRADES_CLIENT_SMOKE_OK: 24 scenes; originalThreeStudies/14NativeInfusions/physicalPaidVanillaToolsCurioArmorHelm/nativeEffects,world=thaumcraft-infusion-upgrades-smoke-1792334625359`

Оплаты разобраны из конкретных native markers и связаны с файлом/хешем
через `clientFactContractEvidence`; числа не извлечены из картинок:

```json
{
  "studies": 3,
  "theoryInfusion": 128,
  "researchAmber": 1,
  "earnedXP": 30,
  "paidInfusions": 14,
  "enchants": 8,
  "paidEssentia": 2180,
  "runicArmor": 1,
  "runicCurio": 1
}
```

Прочие численно проверенные наблюдения:

```json
{
  "scenes": 24,
  "nativeShieldCredits": 4,
  "nativeMaskWither": 1,
  "nativeSippingHeals": 2,
  "actualRefiningOreAttempts": 4,
  "actualEssenceAttackAttempts": 4,
  "refiningMarkerOreAttempts": 4,
  "essenceMarkerAttackAttempts": 4,
  "sippingMarkerObservedHeals": 2,
  "controlledNativeWounds": 1
}
```

Подготовленные условия:

- Owned Survival arena, four stable40block-spaced altars,24pedestals,128candles,eightstabilizers and initial typed jars are prepared fixtures.
- Strict precursor studies,128raw Infusion theory and one research amber are supplied; three actual target-study C2S payments award30XP and the Mask transition awards permanent1/normal1 Warp.
- Eight unenchanted vanilla tools, raw component materials, prior-stage Fortress pieces and a mundane ring are fixtures. No target enchantments, runic charge, goggles or mask are supplied.
- All14 actual native altar payments consume2180essentia and40reagents; one physical milk-bucket remainder is retained. Initial jars hold original aggregate costs plus one surplus per distinct altar/aspect pair.
- Initial5000Vis/zeroFlux in49loaded arena chunks precedes active consumers. Runic shielding pays native aura; no active aura/charge/health/absorption/clock/RNG reset substitutes for gameplay.
- Mining targets,32iron ores, buried sounding ores, a physically enclosed dark room and controlled no-AI vanilla actors are fixtures. Controlled zombie attacks12 and60 are actor attributes, not ordinary zombie statistics. The60attack may create at most seven actual native wounds total, only after native iframe expiry/full health, until an actual Sipping heal is observed; no health, hunger, RNG or timer setters are used.
- Native pedestal pickup, armor Quick Move C2S and actual Curios inventory-button/ring-slot C2S conserve the paid physical items; camera/player teleports are positioning fixtures.

Границы утверждений:

- Prepared routes are not complete Survival acquisition, natural resource exploration or a proof of earlier-stage item crafting.
- Random Refining and Essence results come from bounded ordinary native actions with no forced RNG. Exact observed attempt counts are bound to the selected log.
- Client checks one runic level on one armor piece and one Curios ring; dynamic higher levels, malformed data and wider compatibility are separate server contracts.
- Grinning Devil is worn and its paid mutation is checked; no full WarpEvent is manufactured. Source severity-consumer contracts require separately selected native server evidence.
- Screenshots supplement exact native payments and synchronization assertions; they do not alone prove all transactions or detailed visual/audio parity.
- No independent multiplayer, restart/respawn, arbitrary addon/death/Curios configuration matrix, every pose/view angle or audio playback proof.
- This helper performs no pixel review. Both reviewers must actually inspect all24full1600x1000frames and both complete sheets before recording pass.

Ранние попытки сохранены отдельно без присвоения pass:

- `infusion-upgrades-040-gametest1.log`: SHA256 `8ad95ce090d4f85f11a3967655efbd6b9db9b74242f6c8fef07bfe979745b345`.
- `infusion-upgrades-040-gametest2.log`: SHA256 `a75b1344e8d55e6e7a7461e506d92f33f47d392711e82550697c0d34bfdcc696`.
- `infusion-upgrades-040-server1-run.json`: SHA256 `250c7968371984896fd589a28062d9a0c5c3a6dd82f2cc10a79e75b1f1b37f73`.
- `infusion-upgrades-040-server2-run.json`: SHA256 `5ade57a809b4e0947bd8a0ebdbeccaa9e82c8c75ad468508c4ca52f18b46f7bf`.
- `infusion-upgrades-040-compile1.log`: SHA256 `47ba9d1da3e0785aba491565d8d6867d8b0340ed60c565702849416e845bd205`.
- `infusion-upgrades-040-compile2.log`: SHA256 `fb1c3c284f81d05a54d913f1e9f993db6071a4a729f48dd0327837e9b32a208d`.
- `infusion-upgrades-040-compile3.log`: SHA256 `f2ddfa38a7a4a439d6c496e458e3819066be5c48576f8a8a63237dc73f8201d9`.
- `infusion-upgrades-client-infusion-upgrades-040-client1-client.log`: SHA256 `a1fb96783265156a8dcc59fe4acd0042ebdf4a0869eb22288cd7405a96c8b347`.
- `infusion-upgrades-client-infusion-upgrades-040-client2-client.log`: SHA256 `9c1d7e2362d03127195b4f57d46d9a5e3463661176386852fa56e43bd415f8b5`.
- `infusion-upgrades-client-infusion-upgrades-040-client3-client.log`: SHA256 `85e85e1d68caa7eae90ca093912aa2e805322683cea39531dcfe328896838d02`.
- `infusion-upgrades-040-client1-state.json`: SHA256 `79a679fcead5d66f0abd2bfd02d99a8720a47eaf8a42f817f9f5cca095bf5b0d`.
- `infusion-upgrades-040-client2-state.json`: SHA256 `10751b072b56ee7a13a1a263bff254746440a96993a3b11a6a1716201e3f3ff2`.
- `infusion-upgrades-040-client3-state.json`: SHA256 `704f2c47a6fa45a07e4630741ccb7f7097a7d2c3a59941bfe453cafce30ae322`.
- `infusion-upgrades-040-client2-owned-stop.json`: SHA256 `2af3b8f12620ff80bf552d1ff5ffcda2d0c181f5a710e17f45fe4a05679352c4`.
- `infusion-upgrades-040-client3-owned-stop.json`: SHA256 `51bfbf81b48625ebff4cb3d44b91629b57dd9dd8cc6aa3a4f4394e0a5090a423`.
- `infusion-upgrades-client-infusion-upgrades-040-client1-desktop.json`: SHA256 `66b7bc65f4cf001fc0ef17d947d1d0a69a1f1c398e2b19f2a97cc7cc1604b01c`.
- `infusion-upgrades-client-infusion-upgrades-040-client2-desktop.json`: SHA256 `974b8a84c2e2953d9bc1f7568430b232043309b38b162a9dad62dd036c9cb472`.
- `infusion-upgrades-client-infusion-upgrades-040-client3-desktop.json`: SHA256 `8c5cef752536b7b2365b8a205417750ca1bfc42635c8562a717a18c6b8d6bf71`.
- `infusion-upgrades-client-infusion-upgrades-040-client1-run.json`: SHA256 `19531534c47b2c1ed05b69957f8e3579052370cfe6c22dac36303bc4cfb5218f`.
- `infusion-upgrades-client-infusion-upgrades-040-client2-run.json`: SHA256 `ff131492c41a69d74c6fddd6ad415a402b422f1ab2d8fb8d3cc80353ca4658b8`.
- `infusion-upgrades-client-infusion-upgrades-040-client3-run.json`: SHA256 `77db02e9acf7405dfe72d674df7b43720c3e26a3142660860b2fb39377f200c2`.

Все4491 финальных client/build fingerprints совпадают; все server-релевантные
совпадают. Возможное исключение относится только к opt-in Dist.CLIENT QA driver:

```json
[
  {
    "file": "src/main/java/thaumcraft/equipment/upgrades/client/InfusionUpgradesClientSmokeTest.java",
    "serverExecutedSha256": "9856a7691c005c4b29f41ebeea4cf3c2e1960a29f3ed85b25940000144f787c2",
    "finalClientBuildSha256": "1bfa287cd28531114dec46aa8ab0b0cd0804fb4b4fd56ecaf8719f185d283d1a",
    "reason": "Only opt-in Dist.CLIENT QA driver changed; core/resources/helpers/tests identical"
  }
]
```

Финальная сборка успешна: **2882JSON**, **3720resource-byte**,
исходные **674PNG/7JPG/60OGG** проверены. Проверены версия, CRC, классы,
ресурсные байты, точные public методы/дескрипторы узкого Forge bridge
в итоговом reobfuscated JAR и обоих Forge47.4.10 binaries (mapped/SRG).
Это структурная проверка; отдельный запуск production-установки не заявлен.
В архиве три JAR:
порт, TerraBlender3.0.1.10, Curios5.14.1+1.20.1.
JAR SHA256: `7a9b2c5d72e5417cd4d55bb7144a80713996c2aa03a2e0e0393b0d4f36c6a93d`. Скриншоты настоящие;
подписи обзорных листов находятся вне игровых пикселей.

---

# Проверка аксессуаров0.39.0

Дата: **2026-10-17**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Канонические исследования105/148,
доступные наполнения31/56; исходных зарегистрированных наполнений56.
Контракт — [ACCESSORIES.md](ACCESSORIES.md), точные файлы/SHA —
[artifact-report0.39](validation/artifact-report-0.39.json).

Свежий полный `accessories-039-server2` прошёл **1628/1628**, native exit0 и BUILD SUCCESSFUL.
Сохранены1559предыдущих проверок; новые определения: `{'equipment/accessories/AccessoriesGameTests.java': 29, 'equipment/pouch/FocusPouchGameTests.java': 25, 'research/AccessoriesProgressionGameTests.java': 15}`.
Проверены реальные Curios capability/слоты/экипировка/выпадение и копирование,
два исходных исследования, точные оплаты обычных/магического/наполнения
рецептов, обе частоты амулета, выбор цели и дробный расход, общие потребители,
владение сумкой/фокусами и отсутствие повторной/неполной оплаты.
Локальные native block-entity callbacks серверного алтаря устанавливают
семантику оплаты, а не истечение мирового времени. Его подготовлено209ess,
осталось9; клиентский алтарь отдельно получил203ess и оставил3.

Клиент `accessories-039-client2` прошёл **14/14**, JVM exit0, PID13976,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`. Входной стол
`Default→Default`; переключений/global input нет.
Root и независимый проверяющий просмотрели все14полных1600×1000PNG,
полный обзорный лист и компактное превью одного финального native прогона.

Нативные наблюдения финального лога:

- `[14:22:51] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_FIXTURES: ownedSurvivalarena/rawCraftingTable+ArcaneBench+stableAltar+6pedestals+32candles+2stabilizers/203typedEssentia51Auram101Potentia51Vacuos/initial5000vis0fluxper30loadedChunks/rawstrictparents/rawknowledge+ordinarymaterials+crystals/rawgoggles+apprenticeRing+foundAmulet+fresh0chargeGrapple+emptyCaster+3compiledFocusPackages+1blank; targetstudies/paidBaubles/Pouch/craftedAmulet/chargeAbsent; noactiveChargeAuraClockRngSetters; notfullsurvival/multiplayer/thirdpartyCuriosMatrix/audio`
- `[14:22:52] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_STUDY_PAID: FOCUSPOUCH actualC2S/originalrawknowledge/+10XP,total1`
- `[14:22:53] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-paid-pouch-study.png`
- `[14:22:53] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_STUDY_PAID: VISAMULET actualC2S/originalrawknowledge/+10XP,total2`
- `[14:22:54] [IO-Worker-17/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-paid-visamulet-study.png`
- `[14:22:54] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ORDINARY_PAID: baubles_amulet_mundane actualnativeCraftingMenuC2S/oneoriginaloutput/physicalingredients,total1`
- `[14:22:54] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ORDINARY_PAID: baubles_ring_mundane actualnativeCraftingMenuC2S/oneoriginaloutput/physicalingredients,total2`
- `[14:22:54] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ORDINARY_PAID: baubles_girdle_mundane actualnativeCraftingMenuC2S/oneoriginaloutput/physicalingredients,total3`
- `[14:22:55] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ORDINARY_PAID: baubles_amulet_fancy actualnativeCraftingMenuC2S/oneoriginaloutput/physicalingredients,total4`
- `[14:22:55] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ORDINARY_PAID: baubles_ring_fancy actualnativeCraftingMenuC2S/oneoriginaloutput/physicalingredients,total5`
- `[14:22:55] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ORDINARY_PAID: baubles_girdle_fancy actualnativeCraftingMenuC2S/oneoriginaloutput/physicalingredients,total6`
- `[14:22:55] [IO-Worker-17/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-six-paid-bauble-crafts.png`
- `[14:22:56] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_ARCANE_PAID: onePouch/25vis/zeroCrystals/actualPaidMundaneBelt+7leather+goldConsumed`
- `[14:22:56] [IO-Worker-17/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-paid-pouch-bench.png`
- `[14:23:02] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=4 phase=2 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:02] [IO-Worker-16/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-visamulet-altar-active.png`
- `[14:23:07] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:12] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:17] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:22] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:27] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:32] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:37] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:42] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:47] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:52] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:23:57] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_HEARTBEAT: scene=5 phase=0 studies=2 ordinary=6 stored=0 wear=0 charged=0 gun=0 goggles=0`
- `[14:24:00] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_INFUSION_PAID: actualPaidMundaneAmulet+6originalReagents/200typedEssentia/remaining3/oneCraftedVisAmulet/nativeCasterC2S+worldTicks`
- `[14:24:01] [IO-Worker-17/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-visamulet-altar-complete.png`
- `[14:24:02] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_POUCH_STORED: nativeMainHandC2S/3compiledFixtureFoci+1blank/4physicalQuickMoves/fullNBTPreserved`
- `[14:24:02] [IO-Worker-17/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-original-pouch-mainhand-storage.png`
- `[14:24:02] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_POUCH_OFFHAND: nativeRequestedHandC2S/samePaidBag/fourStoredStacks/noForeignMainReplacement`
- `[14:24:03] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-original-pouch-offhand-reopen.png`
- `[14:24:04] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CURIO_EQUIPPED: actualVanillaInventoryButton/nativeCuriosC2SMenu/fourPhysicalItems/paidPouchBelt+paidCraftedVisNecklace+rawGogglesHead+rawApprenticeRing/sevenOriginalLogicalSlots`
- `[14:24:05] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-curios-seven-slots.png`
- `[14:24:06] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-original-equipped-pouch-radial.png`
- `[14:24:06] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_FOCUS_SWAP: originalRadialNativeMouseClick/actualC2S/equippedPhysicalPaidBagSource/compiledFocusInstalled/remainingBag3`
- `[14:24:07] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-pouch-focus-swap.png`
- `[14:24:07] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_FOCUS_RETURN: actualRemoveC2S/equippedPouchFirst/sameThreeCompiled+OneBlank/conservedNBT/noLooseCopy`
- `[14:24:07] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-pouch-focus-return.png`
- `[14:24:09] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_NATIVE_RECHARGE: actualPaidCraftedVisAmulet/CuriosLivingTickEvent/nativeInterval5/20oneUnitCredits/freshPhysicalGun/auraBefore5000.0/auraNow4980.0/noTargetChargeAuraClockSetters`
- `[14:24:10] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-visamulet-recharge.png`
- `[14:24:14] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_UNEQUIP_PAUSE: actualCuriosC2S/80worldTicks/nativeCharge24Unchanged/paidAmuletPreserved`
- `[14:24:15] [IO-Worker-18/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_IMAGE: C:\Users\user\Documents\Codex\2026-09-29\thaumcraft-1-12-2\outputs\thaumcraft-1.20.1\run\accessories-smoke\.\screenshots\tc6-accessories-native-worn-goggles.png`
- `[14:24:15] [Server thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_FACTS: scenes14/paidStudies2/observationAuromancy16/observationArtifice16/theoryAuromancy64/earnedXP20/ordinaryCrafts6/arcaneCrafts1/arcaneVis25/arcaneCrystals0/infusionCrafts1/infusionEssentia200/infusionReagents6/rawEssentia203/altarRemaining3/storedCompiled3/storedBlank1/curiosEquips4/selectorSwaps1/selectorReturns1/nativeChargedUnits24/chargeAtValidation24/amuletUnequipPause80/gogglesDraws93/onePaidPouch1/onePaidCraftedAmulet1; explicitrawparents/knowledge/materials/stableAltar/essentia/aura/previousstageGunCaster/lootGogglesApprenticeFoundAmulet/compiledFocusPackages,notfullsurvival/multiplayer/thirdpartyMatrix/audio`
- `[14:24:15] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_RENDER_AUDIT_OK: 14 scenes; originalPouch175x232/18slots/redX/actualOffhand/originalCounterRotatingRadial/CuriosNativeSevenSlots/actualWornGoggles/noInventedLabels`
- `[14:24:15] [Render thread/INFO] [th.eq.ac.cl.AccessoriesClientSmokeTest/]: THAUMCRAFT_ACCESSORIES_CLIENT_SMOKE_OK: 14 scenes; actualPaidStudies+6ordinaryCrafts+Pouch25vis+Amulet200ess/nativeCuriosEquip/storage+RadialC2S/nativeAmuletCharge+80tickUnequipPause,world=thaumcraft-accessories-smoke-1792232532205`

Оплаты отдельно разобраны из конкретных native markers и связаны с их
файлом/хешем через `clientFactContractEvidence`; это не числа из кадров:

```json
{
  "paidStudies": 2,
  "observationAuromancy": 16,
  "observationArtifice": 16,
  "theoryAuromancy": 64,
  "earnedXP": 20,
  "ordinaryCrafts": 6,
  "arcaneCrafts": 1,
  "arcaneVis": 25,
  "arcaneCrystals": 0,
  "infusionCrafts": 1,
  "infusionEssentia": 200,
  "infusionReagents": 6,
  "altarRemaining": 3,
  "onePaidPouch": 1,
  "onePaidCraftedAmulet": 1
}
```

Прочие численно проверенные наблюдения финального native прогона:

```json
{
  "scenes": 14,
  "rawEssentia": 203,
  "storedCompiled": 3,
  "storedBlank": 1,
  "curiosEquips": 4,
  "selectorSwaps": 1,
  "selectorReturns": 1,
  "amuletUnequipPause": 80,
  "nativeChargedUnits": 24,
  "chargeAtValidation": 24,
  "wornGogglesDrawsAtValidation": 93,
  "rechargeCreditsAtScene12": 20,
  "rechargeAuraBefore": 5000.0,
  "rechargeAuraAtScene12": 4980.0,
  "actualUnequipPauseWorldTicks": 80,
  "unequippedGunCharge": 24
}
```

Подготовленные условия:

- Owned Survival arena/player setup; native crafting table, Arcane Workbench and stable altar with six pedestals,32 candles and two stabilizers are prepared fixtures.
- Strict precursor studies and raw16 AUROMANCY+16 ARTIFICE observations/64 AUROMANCY theory are supplied; both target studies are then paid through actual C2S advances and award20 XP.
- Raw ordinary crafting ingredients and six infusion reagents are supplied. Six native ordinary result-slot payments create the jewelry; the resulting actual mundane belt/amulet are consumed in the target recipes.
- Prepared203 typed essentia consists of51 Auram,101 Potentia and51 Vacuos; actual native infusion consumes200 and leaves one in each jar (three total).
- Initial5000 Vis/zero Flux per30 loaded chunks is supplied before active consumers; no active target charge, aura, source item, clock or RNG reset substitutes for recharge.
- Raw goggles, apprentice ring, found Vis Amulet, fresh zero-charge prior-stage Grapple Gun, empty caster, three compiled named focus packages and one blank focus are supplied.
- The paid pouch/paid crafted amulet and raw goggles/apprentice ring are physically equipped through native Curios inventory-button/menu C2S actions; Curios owns native storage and synchronization.

Границы фактических утверждений:

- Prepared routes are not a complete survival acquisition playthrough. Supplying apprentice ring/found amulet does not prove natural loot or Pech trading.
- Client covers crafted five-tick Vis Amulet dispatch and one charged gun. Found forty-tick cadence, broader ordering/guards and native lifecycle cases require the separately selected actual full server run.
- Ownership-safe offhand pouch opening, Curios storage/lifecycle/GUI, expanded configured slots and modern cursor/input bridge are explicit modern adaptations rather than Baubles packet/UI byte parity.
- Native screenshots supplement source assertions and exact logged transaction debits; they alone do not prove conservation, synchronization or every mechanical case.
- No independent multiplayer, persisted world restart/respawn, arbitrary addon/canceled-death matrix, audio playback or every pose/view-angle render proof.
- Contract generation does not perform visual review. Both reviewers must actually view every final full1600x1000 PNG and both sheets before recording pass.

Ранние ошибки и остановленные попытки сохранены отдельно без pass:

- `accessories-039-gametest1.log`: SHA256 `eba55386638ff8c2c3c2fe324fb35158c03b17ea4819e51541bec920bdaaf353`.
- `accessories-039-server1-run.json`: SHA256 `1e608d0693bcc12fdd6d3e27165637e022e1c55362f576e992868f788a4b3a52`.
- `accessories-039-compile1.log`: SHA256 `c23d41970a1497f0aca4837b20d04bb5ffee613bdb05f2aecb41ab0332dc282c`.
- `accessories-039-compile2.log`: SHA256 `da42f02fc4fbbf5e97f6aa31cbda93417138cfe9c76e9a159644cae4b3cb4a23`.
- `accessories-client-accessories-039-client1-client.log`: SHA256 `83a607042f28d5e03908cc2e1ed110007cda43daba4f061c4b625ebc324e2905`.
- `accessories-039-client1-state.json`: SHA256 `803db2141aa83413c7e030574a3d0db735867dbb0adeff38c711ac7a844bfd9e`.
- `accessories-039-client1-owned-stop.json`: SHA256 `5281a28dd43c99ea48af54bf7cfb2480803787cc00d72b425fea418635ca0f6d`.
- `accessories-client-accessories-039-client1-desktop.json`: SHA256 `82fa082e26c8d4834ae1e52f77d39250493f0906822acf2bb0a16345f3aa43ee`.
- `accessories-client-accessories-039-client1-run.json`: SHA256 `b261efbaf32a090971f88d4e91c36689c0b372c6bc7b250093cbf19367848a5d`.

В первом серверном прогоне было3ошибки/native3/Gradle1: привязка алтарной
фикстуры к переданному вместо фактически клонированного предмета,
проверка старого объекта после native quick-move и устаревший список
доступных наполнений. Исправлены проверки физического владения и ожидание
амулета без открытия остальных поздних ветвей. Файлы и результаты каждого
раннего прогона сохранены.

Все4485финальных client/build fingerprints совпадают; server-релевантные
совпадают. Единственные разрешённые post-server изменения касаются opt-in
Dist.CLIENT QA driver и связаны обоими SHA, если они были:

```json
[
  {
    "file": "src/main/java/thaumcraft/equipment/accessories/client/AccessoriesClientSmokeTest.java",
    "serverExecutedSha256": "39fc88dc9437a0557b4d39ef2c3a6f1d3ecf40b3780256e2d9530a4c44423275",
    "finalClientBuildSha256": "785e27518f2ec06c93cde87e8eb18aaa390480c61c2dc20c40d1999ed632ef81",
    "reason": "Only opt-in Dist.CLIENT QA driver changed; core/resources/helpers/tests identical"
  }
]
```

Финальная сборка успешна: **2881JSON**, **3718resource-byte**,
исходные **674PNG/7JPG/60OGG** проверены. Проверены версии, CRC, классы,
ресурсные байты и три JAR в установочном архиве: порт, TerraBlender3.0.1.10,
Curios5.14.1+1.20.1. JAR SHA256: `1ddaa352e77419af7a8f5a9e198386250ccb09d0e0813264b23c86ed868f9fa4`.
Снимки настоящие; подписи листов находятся вне игровых пикселей.

---

# Проверка крюка0.38.0

Дата: **2026-10-16**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Охват103/148, наполнение30/56;
зарегистрированных исходных наполнений56. Контракт — [GRAPPLE-GUN.md](GRAPPLE-GUN.md),
точные SHA/наблюдения — [artifact-report0.38](validation/artifact-report-0.38.json).

Свежий полный `grapple-038-server2` прошёл **1559/1559**, native exit0 и BUILD SUCCESSFUL.
Сохранены1534 предыдущих проверки; новые определения: {'artifice/grapple/GrappleGameTests.java': 19, 'research/GrappleGunProgressionGameTests.java': 6}.
Проверены строгие родители BASEARTIFICE+RECHARGEPEDESTAL,64rawTheory/валидный
singular nugget meta10/10XP, три исходных arcane-рецепта, фактическое расходование
оплаченных промежуточных частей, отсутствие повторной/неполной оплаты.
Ядро проверяет100charge/стоимость выстрела включая Creative/обе руки,
исходную траекторию/время жизни/попадания/формулу подтягивания/первый толчок,
приседание/замену/сохранение transient-состояния и современные callback guards.
Локальные native tickNonPassenger callbacks проверяют семантику сущности;
они сами по себе не доказывают прошедшие мировые тики.

Клиент `grapple-038-client2` прошёл **11/11**, JVM exit0, PID28860,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`; входной рабочий стол
`Default→Default`. Нет переключения/global input.
Root и независимый проверяющий просмотрели все11полных1600×1000PNG,
полный лист и шестикадровое превью одного финального native прогона.

Нативные наблюдения финального лога:

- `[15:59:05] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_FIXTURES: ownedSurvivalarena/staticstoneanchorwallZ24/rawbench+rechargepedestal/BASEARTIFICE+RECHARGEPEDESTALprecursors/rawtheory/nugget/ordinarycraftmaterials/primalcrystals/initial5000vis0fluxper30loadedchunks; actualpaidtip+spoolusedforgun; targetgun/research/charge/hooks absent; noactivehookteleports/noactivehookorplayermotion/charge/aura/RNGsetters; notfullsurvival/multiplayer/reload/audio/allangles`
- `[15:59:06] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_STUDY_PAID: actualnativeC2S/32rawTheoryARTIFICE+32BASICS/originalsingularnuggetmeta10one/+10earnedXP`
- `[15:59:07] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_BENCH_PAID: grapple_gun_tip actualnativeC2S/25vis/originalprimalprices/physicalingredients/actualpaidTipSpoolUsedForGun,total=1`
- `[15:59:08] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_BENCH_PAID: grapple_gun_spool actualnativeC2S/25vis/originalprimalprices/physicalingredients/actualpaidTipSpoolUsedForGun,total=2`
- `[15:59:09] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_BENCH_PAID: grapple_gun actualnativeC2S/75vis/originalprimalprices/physicalingredients/actualpaidTipSpoolUsedForGun,total=3`
- `[15:59:11] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_EMPTY_USE: actualnativeC2S/unchargedpaidgun/nohook/nochargecost/originaliceSound`
- `[15:59:20] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_PEDESTAL_PAID: actualC2Sinsert/nativeWorldTicks/20observedChargeSteps/100wholeChargeFromNative100visRoute/auraBefore4875.0/auraAfter4775.0/no directchargesetter; aggregateAuraAlsoHasNativeDiffusion`
- `[15:59:21] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_PEDESTAL_RETRIEVED: actualC2Semptyhand/nativephysicalItemEntitypickup/samepaidgun100charge`
- `[15:59:22] [Render thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_MAIN_FLIGHT: actualnativeC2S/mainhand/newhook207/originalflight/nosynthetichook`
- `[15:59:23] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_MAIN_PULL: actualblockimpact/status6/nativeowneracceleration/move0.9100767530704659/rise0.7593570881920044/charge99/fallDistance0.0`
- `[15:59:24] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_NATIVE_RELEASE: main actualC2Sshift/ownerhookremoved/nativeinventoryloaded0/charge99`
- `[15:59:26] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_OFFHAND_PULL: actualnativeC2S/handOFF/nativeblockimpact/move0.9100849517241488/rise0.7593678247293809/charge98/hook209`
- `[15:59:26] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_NATIVE_REPLACEMENT: actualnativeC2Srefirewhileloaded/old209removed/new211/oneactiveownerhook/charge97/noactiveownerteleport`
- `[15:59:27] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_NATIVE_RELEASE: final actualC2Sshift/ownerhookremoved/nativeinventoryloaded0/charge97`
- `[15:59:28] [Server thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_CLIENT_FACTS: scenes11 paidStudy1/TheoryARTIFICE32BASICS32/originalNugget1/earnedXP10/paidBench3/vis125/Terra1Aqua1Aer1Ignis1/actualPaidTipSpoolConsumed/oneFreshGun/emptyUse1/nativePedestalCharge100/nativeRechargeVis100/nativeShots3/charge97/mainMove20.890752976225453/mainRise3.175930142459407/offMove19.30924479798958/offRise3.175562874088925/replacementtrue/shiftReleases2/hookDraws121/ropeSegments7614/glowDraws121; explicitprecursors/rawknowledge/materials/arena/initialaura/rawpedestal,notfullsurvival/multiplayer/reload/audio/allangles`
- `[15:59:28] [Render thread/INFO] [th.ar.gr.cl.GrappleClientSmokeTest/]: THAUMCRAFT_GRAPPLE_RENDER_AUDIT_OK: 11 scenes; originalhookcuboids/sixframeadditiveglow/sourcewavecenterline/foursiderope/mainoffhandloadedmodels/noinventedGUI`

Фактическая цепь:64rawTheory/rareearth1/10XP; три bench125vis,
Terra1/Aqua1/Aer1/Ignis1; оплаченные tip/spool вошли в единственный свежий
незаряженный крюк. C2S пустое использование не создало снаряд. Native пьедестал
настоящими мировыми тиками зарядил100единиц через100vis API-route, затем
тот же предмет возвращён физическим ItemEntity/native pickup. Совокупная
аура может меняться от естественной диффузии, поэтому100vis — стоимость
изолированного recharge-route, а не заявление о чистой разнице всей мировой ауры.
Три реальных C2S-выстрела оставили97; основная/вторая рука дали настоящее
движение/подъём, повторный выстрел заменил старый снаряд, оба освобождения
выполнены приседанием. Ни одного active-hook/player-motion/charge/aura/RNG
сброса ради кадров; телепорты только до активного зацепа/между освобождёнными.

Фикстуры: completed родители, сырые знания/обычные study/bench материалы,
кристаллы, простой механизм, верстак, пьедестал, bedrock-арена/каменная стена
и начальная аура. Сам механизм в этой клиентской цепи выдан как материал,
его отдельный прежний10vis-рецепт не включён в125vis. Target study/gun/charge
не выданы. Материалы и исходные исследования не подтверждают полный survival.
Модель/текстуры/центрline верёвки исходные; современная трубка заменяет GLE,
точная эквивалентность его tessellation/UV/miter не заявляется.

Ранний `grapple-038-server1` завершился четырьмя ошибками новых фикстур,
native exit4/Gradle1: две проверки обходили native рост tickCount,
две перепутали индексы Aqua/Terra. Исправлены фикстуры, исходные цены и
правила оригинала сохранены. Ранние доказательства сохранены отдельно:

Попытка `grapple-038-client1` остановлена **без pass**, PID29876,
native exit4294967295, Default input desktop не изменён.
Сохранены5полных кадров в
`validation/grapple-038-client1-screenshots` и точный owned-stop receipt.
Наблюдение: Study and threebench125vis passed, pedestal charged100 in20native steps/100vis actual aura4875→4775. Native END observer sees real held gun100charge; driver stalls waiting Inventory.contains fresh untagged sample. Five actual frames retained.
Граница диагноза: Mapped native Inventory.contains(ItemStack) compares isSameItemSameTags; charged tc.charge100 differs from fresh sample. This is an opt-in QA predicate failure, not an established native pickup/gameplay defect.
Исправление: Only opt-in Dist.CLIENT driver uses items.stream item identity for pickup readiness; existing exact server onepaidgun100charge and pedestalempty assertions retained.
Единственное изменение source после этой попытки — opt-in Dist.CLIENT driver;
оба SHA связаны в отчёте.


- `grapple-038-gametest1.log`: SHA256 `fce705b231f7f72d651777e7f73c30ce4cbcf8ddcbe75bc6a03e5e4eb0a79b99`.
- `grapple-038-server1-run.json`: SHA256 `87f5adf734a402ca5e1276126a4f290fc093352e98c1b696e16241cf1b0f23f6`.
- `grapple-038-compile1.log`: SHA256 `459b43e7b1d4d7e1b9e5ef807af83fac93117cf82226c1233485a9ed694d176a`.
- `grapple-038-compile2.log`: SHA256 `0ed9772555e18f62c7dab564297bdecfbe7473e912316e471b4d8a1f9ce5af54`.
- `grapple-038-compile3.log`: SHA256 `00fa245023d9b5072617f274975121c4761fcd83571484f3b269eae33a573b13`.
- `grapple-038-compile4.log`: SHA256 `68b3fb29ffa3765917f1fc9529d11987ed29bc561ba8a2f424c0a1f6e4870992`.
- `grapple-client-grapple-038-client1-client.log`: SHA256 `b1219c50aceca5d1e4e7a630796ab86634fdfa3ea71747fade87fe794b754bc4`.
- `grapple-038-client1-state.json`: SHA256 `b3d1a8084cef3d342fc0edcbad9c9a107d4b53135ac79b58c2b714a3e4293e0d`.
- `grapple-038-client1-owned-stop.json`: SHA256 `46d140ee8e74a5f5fafa3d987e042992aa32b4a8b03cfb9eb69bd4fe35be9669`.

Все 4449финальных client/build fingerprints совпадают; server-релевантные
совпадают. Любое единственное исключение opt-in Dist.CLIENT QA driver после
server связано обоими SHA в отчёте. Сборка успешна, **2863JSON**,
**3696resource-byte**, оригинальные **670PNG/7JPG/60OGG** проверены.
Версии, классы, CRC и архивы проверены. JAR SHA256: `10442d6f5b244ddf18adeda22dc1b261951551f4450eb0635f03f0aaec693a57`.

Границы: подготовленный QA не доказывает полное обычное survival,
independent multiplayer/reload, каждый угол/случайный кадр или слышимый звук.
Bench кадры могут быть после оплаты; её точные значения устанавливают
native logs/серверные проверки. Кадры настоящие, подписи листов вне игровых пикселей.

---

# Проверка вис-генератора0.37.0

Дата: **2026-10-16**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Охват102/148, наполнение30/56;
зарегистрированных исходных наполнений по-прежнему56.
Правила — [VIS-GENERATOR.md](VIS-GENERATOR.md), точные SHA/наблюдения —
[artifact-report0.37](validation/artifact-report-0.37.json).

Свежий полный `generator-037-server1` прошёл **1534/1534**, native exit0 и BUILD SUCCESSFUL.
Сохранены1488 прежних проверок; новые определения: {'artifice/generator/VisGeneratorGameTests.java': 28, 'artifice/generator/VisGeneratorOwnershipGameTests.java': 4, 'research/VisGeneratorProgressionGameTests.java': 14}.
Прогрессия проверяет strict completed REDSTONERELAY,32rawTheoryBASICS и32ARTIFICE,
!potentia, оплату физического redstone_block/piston из main inventory,
5+5XP/noWarp, книгу/повторы/сохранение и настоящий arcane recipe25vis/Ignis1/Ordo1.
Два разных поршня оплачиваются отдельно: один исследованием, другой рецептом.
Устройство проверяет оригинальное1vis→1000FE, фактическую дробную конверсию,
нулевой/положительный запас, предел20FE, шесть сторон, redstone/capability,
loaded-only, physicalplacement/drop/save/callback ownership. Конкретные
проверки, оригинальные quirks и современные адаптации описаны в аудитах.

Клиент `generator-037-client2` прошёл **12/12**, JVM exit0, PID20244,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`; входной рабочий стол
`Default→Default`. Нет переключения/global input.
Root и независимый проверяющий просмотрели все12полных1600×1000PNG,
полный лист и шестикадровое превью одного финального native прогона.

Нативные наблюдения финального лога:

- `[09:34:38] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_FIXTURES: ownedSurvivalarena/rawbench/REDSTONERELAYprecursor/rawknowledge/studymaterials/typedPotentiacrystal/originalbenchingredients/initial500visper16loadedchunks/sixnativeChestwithOPTINQAForgeFEreceiverscapacity200000; vanillaChesthasnoFEconsumer/gameplaydummyregistrationsnone; onepaidtargetdevice reused, no activeenergy/tick/aura/RNG reset`
- `[09:34:39] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_ASPECT_SCANNED: actualThaumometerC2S/rawtypedPotentiaCrystal/no targetaspectsetter`
- `[09:34:40] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_STUDY_PAID: actualnativeC2S/32rawTheoryBASICS+32ARTIFICE/redstoneBlock1+piston1/+10earnedXP`
- `[09:34:42] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_BENCH_PAID: actualnativeC2S/25vis/Ignis1Ordo1/fourPlanks+RedstoneDust+Piston+twoRareEarthNuggets+VisResonator->onephysicalVisGenerator`
- `[09:34:43] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facingdown actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState`
- `[09:34:49] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingdown actualnativeC2S/onephysicalpaidBlockItem; originaldropdoesnotpersiststoredFE`
- `[09:34:49] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_PICKUP_APPROACH: actualSettledItemEntity=277cea1d-d9db-4cff-aa9b-5a32d00c09ce at-11.914841495078822/113.875/-7.632657172568726/movePlayerOnly/no itemmovement/directpickup`
- `[09:34:49] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facingup actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState`
- `[09:34:55] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingup actualnativeC2S/onephysicalpaidBlockItem; originaldropdoesnotpersiststoredFE`
- `[09:34:55] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_PICKUP_APPROACH: actualSettledItemEntity=19010d06-697e-43a8-9835-d4c4dedb284d at1.7173954906036613/112.0/-8.716675768879636/movePlayerOnly/no itemmovement/directpickup`
- `[09:34:56] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facingnorth actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState`
- `[09:35:01] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingnorth actualnativeC2S/onephysicalpaidBlockItem; originaldropdoesnotpersiststoredFE`
- `[09:35:02] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_PICKUP_APPROACH: actualSettledItemEntity=d87aaa49-c91f-4a04-963a-add65aaa633d at11.450979996089197/112.0/-7.536537536272094/movePlayerOnly/no itemmovement/directpickup`
- `[09:35:02] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facingsouth actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState`
- `[09:35:07] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingsouth actualnativeC2S/onephysicalpaidBlockItem; originaldropdoesnotpersiststoredFE`
- `[09:35:08] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_PICKUP_APPROACH: actualSettledItemEntity=ddf90110-71f8-4abb-9f10-30eedd8762c5 at-11.313048567341648/114.0/6.653479624237246/movePlayerOnly/no itemmovement/directpickup`
- `[09:35:08] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facingwest actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState`
- `[09:35:14] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingwest actualnativeC2S/onephysicalpaidBlockItem; originaldropdoesnotpersiststoredFE`
- `[09:35:15] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_PICKUP_APPROACH: actualSettledItemEntity=80d5653b-4231-4579-a25b-69396b01e2b8 at0.0625/114.0/6.070691423083777/movePlayerOnly/no itemmovement/directpickup`
- `[09:35:15] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facingeast actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState`
- `[09:35:22] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_PAUSE: actualC2Sredstone/81worldticks/energy880unchanged/receiverFE1120unchanged/originalpoweredchargedSparks4`
- `[09:35:25] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_NATIVE_RESUME: actualSurvivalRedstoneharvest/native20FEpush/receiver1120->1320FE/no directenergy/tick/aura reset`
- `[09:35:30] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingEAST actualnativeC2S/onephysicalpaidBlockItem/finalteardown`
- `[09:35:31] [Server thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_CLIENT_FACTS: scenes12 nativeAspectPotentia/study1/paidTheoryBASICS32ARTIFICE32/paidStudyRedstoneBlock1Piston1/earnedXP10/paidBench1/vis25/Ignis1Ordo1/onePaidDevice/SurvivalPlacements6/nativeDropPickups5/SurvivalHarvests6/nativeFacingMask63/acceptedFE13820/receiverCalls691/maxOffer20/redstonePause80ticks/nativePoweredChargedSparks15/nativeSparkEmitted81/drawn854/sparkFaces63/positiveEnergySamples83/poweredPositiveSamples15; explicitREDSTONERELAY/rawknowledge/materials/arena/initialaura/OPTINownedChestFEcapability,notvanillaChestFE/notfullsurvival/multiplayer/reload/audio`
- `[09:35:31] [Render thread/INFO] [th.ar.ge.cl.GeneratorClientSmokeTest/]: THAUMCRAFT_GENERATOR_RENDER_AUDIT_OK: 12 scenes; originalsixfacingbakedmodels/sameOriginalTextureForEnabledAndPoweredStates/actualstoredenergyrandomDisplayTickSpark/FXGeneric16grid8frames/originalpoweredchargedquirk/noGUI`

Фактическая оплата:64rawTheory, redstoneblock1/piston1,10earnedXP;
один bench25vis/Ignis1/Ordo1 и реальные4доски/redstoneDust/дваrareearthnuggets/
piston/VisResonator. Один оплаченный генератор проходит6Survival-размещений,
6разборок и5обычных native ItemEntity pickup между ними. Сервер наблюдал
все6выходных направлений и13820принятыхFE; кадры содержат исходные
6направлений корпуса и реальные заряженные искры. 81мировых тиков
красного камня сохраняют запас/вывод; искры при заряде продолжаются как
в оригинале. После удаления красного камня настоящий FE-приём возобновляется.

Финальный native log содержит 5 наблюдаемых
`NATIVE_PICKUP_APPROACH`: игрок QA подходит к уникальному настоящему ItemEntity,
только когда тот уже на земле; UUID и реальные координаты связаны в отчёте.
Положение/стек предмета не переписываются, прямого pickup или кредита инвентаря
нет. Количество подходов не подменяет пять реально завершённых подборов.

Важная граница: шесть ChestBlockEntity имеют явно подготовленную opt-in
Forge ENERGY capability только в этом именном QA-мире. Обычный сундук
Minecraft не является потребителем FE, игровой фиктивный блок/предмет для
этой проверки не регистрируется. Этот route доказывает настоящий Forge
capability-контракт с наблюдаемым приёмом, но не полную совместимость со
всеми сторонними энергетическими модами.

Фикстуры: completed REDSTONERELAY, сырые знания/typed Potentia crystal,
обычные study/bench материалы, магический верстак, bedrock-арена и начальные
500vis в16чанках до размещения потребителей. Potentia получен настоящим
Thaumometer C2S. Target study/готовый генератор не выданы. После размещения
нет прямого вызова consumer tick, сброса energy/count/RNG или ауры ради фото.
Точный shader/atlas/spark source, те же original textures для обоих состояний,
state/facing и собственные persistence/callback guards закреплены в аудитах.

Ранние попытки сохранены отдельно. Перечень связывает фактически найденные
файлы SHA256 и не переименовывает отказ в успех или непроверенную причину:

Попытка `generator-037-client1` остановлена **без pass**:
сохранены 6 исходных полных кадров,
native exit `4294967295`, проверенный собственный PID5264,
входной рабочий стол `Default→Default`.
Последнее фактическое состояние:

`THAUMCRAFT_GENERATOR_HEARTBEAT: scene=6 phase=3 placed=3 harvested=3 pickedUp=2 energy=940 acceptedFE=6840 calls=342 nativeFaces=7 sparkFaces=7 sparks=29 poweredSparks=0 drawn=290`

Наблюдение: Native NORTH teardown asserted one physical paid generator drop; next scene waited for third native pickup at fixed player position. No continued FE output after source removal.

Граница диагноза: Exact live item coordinates were not captured. Adjacent elevated receiver geometry makes the fixed y112 pickup approach fragile; this is a fixture diagnosis, not proof of a generator drop defect.

Остановка: Native smoke stalled at scene6 phase3 waiting for physical pickup after NORTH teardown; no forced pickup/item copy. Stop only this verified owned QA JVM before fixing driver approach.

Исправление только opt-in клиента: Move only owned QA player to the unique real dropped ItemEntity native position; never copy/move item, set inventory credit or call direct pickup.
Остальные source/resource/helper fingerprint этого запуска совпадают
с финальными; изменённые SHA привязаны в отчёте. Сохранены
[generator-037-client1-state.json](validation/generator-037-client1-state.json),
[generator-037-client1-owned-stop.json](validation/generator-037-client1-owned-stop.json),
снимки в `validation/generator-037-client1-screenshots` и исходный log.
Это не успешная проверка и не доказательство дефекта выпадения генератора.


- `generator-client-generator-037-client1-client.log`: SHA256 `138e5377e159f5ba0bff75ec09504120d1baf10e83af96d57b3747928aef3900`.
- `generator-037-compile1.log`: SHA256 `41af0ae3ff5e7fb0d953ecfabecb9b963513371521b066f034ad0334c6a38d49`.
- `generator-037-compile2.log`: SHA256 `29f08024c62379b981a12eac1fc975ce87d5a44e2ec1bc340dcb5027f949c317`.
- `generator-037-client1-owned-stop.json`: SHA256 `7a81abc80b3dbfa29771136cc445e0f539f0734c6c29ab58e7b55ec28bd27475`.
- `generator-037-client1-state.json`: SHA256 `8a8be5354ce36d2114d8abef9b8b4966483ea94092095742f76701e3b13743ab`.

Все 4436 отпечатков исходников, ресурсов и скриптов совпадают между
финальными серверным прогоном, клиентским прогоном и сборкой. Исключений нет.
Сборка успешна, **2860JSON**, **3690**resource-byte, оригинальные
**670PNG/7JPG/57OGG** прошли. Версии, классы, CRC и архивы проверены.
JAR SHA256: `5a76376fe228f029460560ae0a7f2ccce52ba2579e2d13b08c1fbbce0870eae8`.

Ограничения: подготовленный QA не доказывает полное обычное survival,
естественное получение всех precursor материалов, стороннюю FE compatibility
matrix, отдельный multiplayer, client reload, каждый угол/случайный кадр
частиц или слышимый звук. Снимки после scan/bench callback не показывают
весь платёж; точные цены и действия связаны с native logs/серверными тестами.
Кадры реальные, подписи листов находятся вне игровых пикселей.

---

## История: Проверка шаблонного верстака и диоптры0.36.0

Дата: **2026-10-16**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Охват101/148, наполнение30/56;
зарегистрированных исходных наполнений по-прежнему56.
Правила — [PATTERN-CRAFTER.md](PATTERN-CRAFTER.md), точные SHA/наблюдения —
[artifact-report0.36](validation/artifact-report-0.36.json).

Свежий полный `pattern-036-server4` прошёл **1488/1488**, native exit0 и BUILD SUCCESSFUL.
Сохранены1422 прежних проверки; новые определения: {'artifice/pattern/PatternCrafterGameTests.java': 30, 'artifice/pattern/PatternOwnershipGameTests.java': 6, 'artifice/dioptra/DioptraGameTests.java': 14, 'research/PatternCrafterProgressionGameTests.java': 16}.
Проверены исходные маски/одинаковый материал/порядок источников/стороны
верхнего и нижнего контейнеров, native recipes, idle5vis/literal1power,
redstone, физическая оплата/выход, повторы/сохранения/loaded-only migration.
Диоптра проверяет настоящий legacyTRUEdefault/placementTRUE,20tick clock,
169сэмплов/центр84/ориентацию, read-only Vis/Flux, click-only mode,
redstone-ignored/comparator,169byteNBT/разные load-update clocks,
bounded malformed saves, missing loaded chunks и сохранение чужих BE.
Есть настоящий world-ticked тест диоптры с native readiness/region ticket;
синхронные проверки clock/NBT используют явные подготовленные состояния.
Исследования проходят исходную BASEARTIFICE→DIOPTRA→ARCANEPATTERNCRAFTER
цепь,16rawObservationAUROMANCY, по32rawTheory трёх категорий,
!sensus/!auram/!fabrico, по5+5XP и два настоящих50vis arcane-рецепта.

Клиент `pattern-036-client1` прошёл **23/23**, JVM exit0, PID26160,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`; входной рабочий стол
`Default→Default`. Нет переключения/global input.
Root и независимый проверяющий просмотрели все23полных1600×1000PNG,
полный лист и шестикадровое превью одного финального native прогона.

Нативные наблюдения финального лога:

- `[03:17:56] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_FIXTURES: ownedSurvivalarena/rawbench/30loadedchunks variedinitialVisFlux/BASEARTIFICEprecursor/rawknowledgeandtypedcrystals/materialsdeferred/rawchestsandvanillarecipeinputs; targetstudies/paiddevices/aspects absent; no directactiveconsumer tick/count/power/type/grid/RNG/aura reset; notfullsurvival/multiplayer/reload/audio/allangles`
- `[03:17:57] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/sensus/no targetaspectsetter`
- `[03:17:59] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/auram/no targetaspectsetter`
- `[03:18:01] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/fabrico/no targetaspectsetter`
- `[03:18:02] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_STUDY_PAID: DIOPTRA actualnativeC2S/16rawknowledge/+10earnedXP/actualdiscoveredaspects/noitemcost`
- `[03:18:04] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_BENCH_PAID: dioptra actualnativeC2S/50vis/2crystals/physicaloriginalingredients->oneactualoutput,total=1`
- `[03:18:05] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_SURVIVAL_PLACED: dioptra nativeC2S/paidBlockItem/no forcedtargetstate`
- `[03:18:08] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_DIOPTRA_FLUX: actualnativeC2Stoggle/169nativecachedaurasamples/centerbyte2/no directgridsetter`
- `[03:18:09] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_STUDY_PAID: ARCANEPATTERNCRAFTER actualnativeC2S/96rawknowledge/+10earnedXP/actualdiscoveredaspects/noitemcost`
- `[03:18:11] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_BENCH_PAID: pattern_crafter actualnativeC2S/50vis/3crystals/physicaloriginalingredients->oneactualoutput,total=2`
- `[03:18:12] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_SURVIVAL_PLACED: pattern_crafter nativeC2S/paidBlockItem/no forcedtargetstate`
- `[03:18:15] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode0/actualnativeworldticks/physical9wheat->1hay_block /onepower/total1`
- `[03:18:17] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode1/actualnativeworldticks/physical1oak_log->4oak_planks /onepower/total2`
- `[03:18:19] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode2/actualnativeworldticks/physical2oak_planks->1oak_pressure_plate /onepower/total3`
- `[03:18:21] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode3/actualnativeworldticks/physical2oak_planks->4stick /onepower/total4`
- `[03:18:23] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode4/actualnativeworldticks/physical4oak_planks->1crafting_table /onepower/total5`
- `[03:18:25] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode5/actualnativeworldticks/physical3oak_planks->6oak_slab /onepower/total6`
- `[03:18:30] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_UNMATCHED: mode6/threeverticalplanks/no vanilla recipe/80actualworldticks/physicalinput3conserved/power4.0 unchanged`
- `[03:18:33] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode7/actualnativeworldticks/physical6oak_planks->2oak_trapdoor /onepower/total7`
- `[03:18:35] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode8/actualnativeworldticks/physical6oak_planks->3oak_door /onepower/total8`
- `[03:18:37] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode9/actualnativeworldticks/physical8oak_planks->1chest /onepower/total9`
- `[03:18:42] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_PAUSE: actualC2Sredstone/80worldticks/input8/output23 /power1.0 unchanged`
- `[03:18:46] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_NATIVE_RESUME: actualSurvivalRedstoneharvest/native20tickworldcraft/8physicalplanks->secondchest/onepower,total10`
- `[03:18:48] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_SURVIVAL_HARVESTED: pattern_crafter actualnativeC2S/onephysicalpaiditem/no syntheticrefund`
- `[03:18:48] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_SURVIVAL_HARVESTED: dioptra actualnativeC2S/onephysicalpaiditem/no syntheticrefund`
- `[03:18:50] [Server thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_CLIENT_FACTS: scenes23 nativeAspects3/studies2/paidTheoryGOLEMANCY32ARTIFICE32AUROMANCY32/paidObservationAUROMANCY16/earnedXP20/paidBench2/vis100/Aer1Aqua2Terra1Ordo1/SurvivalPlacement2/nativeDioptraVisFlux169samples/knobCycles9/dies10/nativeVanillaCrafts10/oneUnmatchedDieConserved/physicalOutputs24/chest2/redstonePause80ticks/SurvivalTeardown2/renderFrames1053/movingFrames989/renderModes1023/dioptraVisFrames73/FluxFrames110; explicitBASEARTIFICE/rawknowledge/crystals/materials/chests/arena/initialaura,notfullsurvival/multiplayer/reload/audio/allangles`
- `[03:18:50] [Render thread/INFO] [th.ar.pa.cl.PatternClientSmokeTest/]: THAUMCRAFT_PATTERN_RENDER_AUDIT_OK: 23 scenes; originaltenframeDie/nativeopposedextrudedgears/actualnativeblockevent/Dioptra144cells48edges4sides/nativeVisFluxcolors/lightmapfogadditive/noinventedGUI`

Arcane-оплата:100vis, Aer1/Aqua2/Terra1/Ordo1, точные реальные компоненты
двух устройств. Это отдельная оплата от последующего native расхода ауры
автокрафтером. Девять подходящих схем и повтор после паузы дают10операций,
24реальных выходных предмета, включая два сундука. Схема6 не имеет vanilla
рецепта для трёх вертикальных досок:80native world ticks сохраняют ввод
и power, затем физический остаток участвует в следующей подходящей схеме.
Оба изготовленных устройства поставлены и разобраны через Survival C2S.
Диоптра использует настоящий native cache169сэмплов в обоих режимах.

Фикстуры: завершён только BASEARTIFICE; сырые знания, typed scan crystals,
обычные материалы/5первичных кристаллов, workbench, сундуки/vanilla ввод,
арена/загруженные чанки и начальные Vis/Flux заданы явно. Sensus/Auram/
Fabrico получены actual Thaumometer C2S; открытия и готовые исследованные
устройства не выданы. После размещения нет прямого вызова consumer tick,
сброса его счётчиков/типа/power/grid/RNG или ауры ради результата.
Нативные сканы сохраняют заработанные Observation rewards, не обнуляют их.

Исходные quirks и современные ограничения указаны в четырёх аудитах:
uniform-material recipes,20tick cadence, idle5vis refill/literal1craftcost,
fractional stall, данные BE/геометрия/анимации; loaded-only границы,
наблюдаемое физическое владение и callback conservation являются явными
адаптациями. Неизвестные сторонние handlers без наблюдаемого владения
не заявлены как поддерживаемый оплачиваемый контракт.

Перед финальным прогоном исправлялась только подготовка тестов. Server1
завершился четырьмя сбоями: аудит сохранённого мира подтвердил пересечение
широкой арены турели с алтарём зеркала и печью; отдельно исправлено создание
двойного сундука настоящим BlockItem. Первая мешавшая сущность не установлена.
После изоляции арен Server2 выявил восемь проверок, которые начинали поиск
сущностей до готовности новых чанков. Добавлено однократное ожидание
настоящей загрузки и обработки чанков. Server3 прошёл 1487/1488:
одна подготовка не уложилась в старые 100 тиков; подробных состояний чанков
тот прогон не записывал, поэтому точная причина задержки не установлена.
Финальная подготовка ограничена 300 тиками, с очисткой на 298-м и диагностикой
недоступных чанков. Все 81 условие двадцати проверок сохранены; отдельная
проверка AI по-прежнему использует ровно 140 настоящих тиков и прежний лимит.
Аудиты трёх ранних прогонов привязаны к исходным логам и SHA в отчёте.
Код игровых механик между этими прогонами не менялся.

Клиент1 выполнен перед финальным Server4. Все 4425 отпечатков исходников,
ресурсов и помощников совпадают между этим клиентом, Server4 и финальной
сборкой; исключений нет.

Ранние попытки сохранены отдельно. Список ниже привязывает фактически
найденные файлы SHA256 и не переименовывает отказ в успех или причину:

- `pattern-036-gametest1.log`: SHA256 `4a5c3198ee70baf8360276ced5e067c80dfda2187a0720770ae9238ed5c211e8`.
- `pattern-036-gametest2.log`: SHA256 `7e2a7ea5da0f440e7deaf0d977114ce9468127347c620ed7d96e297048784d2e`.
- `pattern-036-gametest3.log`: SHA256 `f6974a1539ae2f3c2886abf7019b969980804da16e1e6175bc1afe858fd5b40a`.
- `pattern-036-compile1.log`: SHA256 `cc453a114643490232c3d736c1612dd78b2b36b65a66abe8d5eaac84249e6ff7`.
- `pattern-036-server1-run.json`: SHA256 `d3bafaeea516e9ebcb79b08570139c19247c35d4109310452f8372cf42456ccc`.
- `pattern-036-server2-run.json`: SHA256 `a4120119b3c2c5c6e4091c308e41de4f9fbc02b5f637e14240dd5ed3cd6cba82`.
- `pattern-036-server3-run.json`: SHA256 `0efbd968ed8bcef433f9718b6da2c8e0b22533e2338f167d2f43ee32d79588a2`.
- `pattern-036-server1-overlap-audit.json`: SHA256 `391bd75e33767aa76899bd1a9061b41eb500b2e89844e86e9d45459e31c23ae7`.
- `pattern-036-server2-readiness-audit.json`: SHA256 `dc97fd021f8509b7d2364b713e1bc80983a34a1e2d9818df18a60a0bf434f5f1`.
- `pattern-036-server3-readiness-budget-audit.json`: SHA256 `6f8b5d6134630682a70b97a503209facda9b907f7090deabe2428a0d8e35a140`.

Все 4425 финальных client/build source/resource/helper fingerprint совпадают;
server-релевантные совпадают. Единственное допустимое исключение после
server — opt-in Dist.CLIENT QA driver; если оно было, оба SHA связаны в report.
Сборка успешна, **2859JSON**, **3689**resource-byte, оригинальные
**670PNG/7JPG/57OGG** прошли. Версии, классы, CRC и архивы проверены.
JAR SHA256: `a0e07e889bba4cb31870b03115f9752701fe510579232052541817cdc0397cd2`.

Ограничения: подготовленный QA не доказывает полное обычное survival,
естественное получение всех precursor материалов, отдельный multiplayer,
client reload, все углы/кадры анимации или слышимый звук. Изображения —
реальные кадры; подписи листа находятся вне игровых пикселей.

---

## История: Проверка ламп роста и плодородия0.35.0

Дата: **2026-10-15**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Охват99/148, наполнение30/56;
зарегистрированных исходных рецептов наполнения по-прежнему56.
Контракты — [LAMPS.md](LAMPS.md), SHA/наблюдения/фикстуры —
[artifact-report0.35](validation/artifact-report-0.35.json).

Свежий полный `lamps-035-server3` прошёл **1422/1422**, native exit0 и BUILD SUCCESSFUL.
Сохранены1369 прежних проверок; фактические новые определения: {'lampCore': 32, 'placementOwnership': 6, 'paidResearchAndCrafting': 15}.
Проверены исходная геометрия/шесть сторон/свет15/0, оплата и отменённые,
reentrant/foreign placement callbacks, транспорт/строгая тяга/резерв,
сохранение отрицательных зарядов и исходных счётчиков, поиск растений,
мature/stacked исключения, отложенный native рост, сохранение принятых tick,
точные классы/возраст/любовь/население животных, redstone, физические
выпадения, повторы/сохранения и loaded-only миграция старых блоков.
Прогрессия проверяет оба исходных родителя,32rawTheory на запись,
Herba/Desiderium, физическую костную муку/пшеницу/морковь и5+5XP.
Два настоящих алтаря используют оплаченные предшествующие лампы,
по шесть реальных реагентов и65эссенции, без ручного продвижения ритуала.

Клиент `lamps-035-client2` прошёл **15/15**, JVM exit0, PID30284,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`; входной рабочий стол
`Default→Default`. Нет переключения/global input.
Root и независимый проверяющий просмотрели все15полных1600×1000PNG,
полный лист и шестикадровое превью. Снимки дымки и сердец выбираются
по реально наблюдаемым эффектам; QA не создаёт их ради фотографии.

Нативные наблюдения финального лога:

- `[17:02:55] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_FIXTURES: ownedSurvivalarena/rawbench+stablealtar+sixpedestals+32candles+148typedaltarEssentia/2500visperloadedchunk/predecessorARCANELAMP+INFUSION/80rawAGE0Wheat/randomTickSpeed0/fence; rawknowledge/studymaterials/crystals/recipeingredients deferred; targetaspects/studies/paidArcaneLamps/GrowthFertility/babies absent; no directruntime tick/grow/charge/reserve/queue/RNG/love/breedcall`
- `[17:02:56] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/herba; no targetaspectsetter`
- `[17:02:58] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/desiderium; no targetaspectsetter`
- `[17:02:59] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_STUDY_PAID: LAMPGROWTH actualnativeC2S/32rawTheoryARTIFICE/+10earnedXP/originalphysicalitems`
- `[17:03:01] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_STUDY_PAID: LAMPFERTILITY actualnativeC2S/32rawTheoryARTIFICE/+10earnedXP/originalphysicalitems`
- `[17:03:02] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_BENCH_PAID: 50vis/Aer1Ignis1/fourIronPlates+AmberBlock->oneArcaneLamp,total=1`
- `[17:03:02] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_BENCH_PAID: 50vis/Aer1Ignis1/fourIronPlates+AmberBlock->oneArcaneLamp,total=2`
- `[17:03:32] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_ALTAR_PAID: lamp_growth physicalpaidArcaneLamp+sixoriginalperipherals+65nativeTypedEssentia->oneactualoutput,total=1`
- `[17:04:00] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_ALTAR_PAID: lamp_fertility physicalpaidArcaneLamp+sixoriginalperipherals+65nativeTypedEssentia->oneactualoutput,total=2`
- `[17:04:01] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_SURVIVAL_PLACED: lamp_growth actualnativeC2S/onepaidBlockItem/supportfacingDOWN/rawtyped40source+verticaltube,chargedonlybynativeworlddraw`
- `[17:04:03] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_NATIVE_GROWTH: scheduledactualworldcropAGEsum=1 grown=1 rawHerbaRemaining=38 charges=12 reserve=true packets=1 emitted=8 drawn=104,randomTickSpeed0/no directdrivergrow/queue/tickcall`
- `[17:04:04] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_RAW_ANIMAL_FIXTURE: twoadultnativeCow entities/fullnativeAI/no age/love/babysetter afterspawn`
- `[17:04:04] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_SURVIVAL_PLACED: lamp_fertility actualnativeC2S/onepaidBlockItem/supportfacingDOWN/rawtyped40source+verticaltube,chargedonlybynativeworlddraw`
- `[17:04:05] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_NATIVE_LOVE: originalDesiderium5payment/nativeCowsetInLove/events18=2 adultLove=2 charges=0 source=35; nofeeding/directdriverlove/birthcall`
- `[17:04:08] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_NATIVE_BIRTH: actualvanillaBreedGoal/onephysicalbaby/twoadultcooldowns/DesideriumsourcePlusTube=27 charges=8 exactPairPayment5/heartEvents=3; no directbreed/spawnchilddrivercall`
- `[17:04:29] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_NATIVE_PAUSE: actualtwoC2SRedstoneBlocks/320worldticks cropAGEsum=14 fertilityActiveTicks=71 poweredrefillallowed/growthcharges=11 reserve=true fertilitycharges=10`
- `[17:04:32] [Render thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_NATIVE_RESUME: actualSurvivalRedstoneharvest/resumedcropAGEsum14->15 freshoriginalMistPackets6->7`
- `[17:04:34] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_SURVIVAL_HARVESTED: lamp_growth actualnativeC2S/onephysicalpaiditem/lampremoved/nochargeforcedspill`
- `[17:04:34] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_SURVIVAL_HARVESTED: lamp_fertility actualnativeC2S/onephysicalpaiditem/lampremoved/nochargeforcedspill`
- `[17:04:36] [Server thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_CLIENT_FACTS: scenes=15 nativeAspects2/studies2/paidTheoryARTIFICE64/earnedXP20/paidArcaneLampBench2/vis100/Aer2Ignis2/paidInfusions2/typedEssentia130/altarRemainder18/nativeSurvivalPlacement2/herbaSourceRemaining34/desireSourceRemaining25/cropAGEsum22/nativeLoveObservedtrue/nativeCowBaby1/nativeHeartEvents3/nativeMistPackets12/nativeMistEmitted96/nativeMistDraws8278/redstonePause320ticks/refillallowed/nativeSurvivalTeardown2; explicitprecursors/rawknowledge/materials/crystals/arenacrops/adults/jars/tubes/aura,notfullsurvival/multiplayer/reload/audio/allangles`
- `[17:04:36] [Render thread/INFO] [th.ar.la.cl.LampsClientSmokeTest/]: THAUMCRAFT_LAMPS_RENDER_AUDIT_OK: 15 scenes; originalstaticGrowthFertilitymodels/enabledlight15/nativegreenframe56grid16additivemist/nativeAnimalhearts/actualAIbaby/noinventedbeam/nooverlay`

Фактические оплаты:64rawTheoryARTIFICE,20полученногоXP, два precursor bench
по50vis/Aer1Ignis1, два наполнения по65эссенции/6реагентов/центральной лампе.
Всего100vis,4первичных кристалла и130эссенции;18исходных единиц остаются
в банках алтаря. Две изготовленные лампы поставлены в survival; источник
Herba даёт native попытки роста при **randomTickSpeed0**, Desiderium —
одну пару с ценой5, native event18 сердца и детёныша через настоящий BreedGoal.
Подтверждены 320мировых тиков redstone-паузы с разрешённым исходным
пополнением, продолжение роста и две физические survival-разборки.

Подготовленные условия: завершённые ARCANELAMP/INFUSION, сырые знания,
материалы/кристаллы/предметы исследования, bedrock-арена,80растений wheatAGE0,
забор, workbench, стабильный алтарь/6пьедесталов/32свечи/148эссенции,
начальные2500vis в загруженных чанках, два взрослых nativeCow и по40Herba/
Desiderium в банках с трубами. Целевые открытия аспектов получены настоящим
ThaumometerC2S. Целевые исследования, крафты, дети и заряды ламп не выданы
как готовые результаты. После размещения потребителей QA не сбрасывает
заряды/резерв/поиск/очередь/RNG и не вызывает напрямую рост/любовь/размножение.

Исходные quirks сохранены: рост считает попытки забора, включая две при
пустом устройстве, хранит20зарядов и отдельный резерв; применяет строгую
сферу6,169перемешанных колонок, проход сверху вниз и задержанное сравнение
последнего растения. Плодородие работает при charges>1, списывает ровно5
и может получить−3 с тягой158; счётчик идёт только при разрешённой работе,
с первым импульсом0 и следующими через300. Население считается по точному
классу, включая молодых/уже влюблённых; при количестве>9 новых пар нет.

Современная адаптация: native scheduled callback растений1.12 разделился
в1.20 на scheduled tick и randomTick. Сохранён однократный принятый отложенный
вызов, реальная chunk readiness, дедупликация/персистентность; используется
нативный рост с его вероятностями и Forge событиями, не bonemeal.
Loaded-only/неприсвоение чужих транзакций/наблюдаемое владение физическими
выпадениями являются явными современными ограничениями безопасности ресурсов.
Сторонние оплачиваемые handlers без наблюдаемого владения не заявлены.

Ранние попытки этого этапа сохраняются отдельно и не заменяют финальный
native прогон. Следующие реально найденные файлы привязаны SHA256;
сам список не переименовывает отказ в успех и не приписывает ему причину:

- `lamps-035-gametest1.log`: SHA256 `44bccac7a824b96395b85403d53812d8a38fb5d0361f202144a89c6c05415a3c`.
- `lamps-035-gametest2.log`: SHA256 `afabc8e2214fcb94604cf0cb0a69703951e50239d357b771fdc1acadc4434270`.
- `lamps-client-lamps-035-client1-client.log`: SHA256 `522d47d6d2d7fc10aa6d95126fa4af4e5e42ea9c2f26837629554f7108863591`.
- `lamps-035-compile1.log`: SHA256 `5bb7c12d268b93510440f2d71519bdaefc8e049d2e17037345fe341df66f2740`.
- `lamps-035-server1-run.json`: SHA256 `244eaad37918ec3fd7fce4ce7400d220a1b700dbfb4e5f4311b74aa8df666a92`.
- `lamps-035-server2-run.json`: SHA256 `568ba91e414591db132ae2ac79574f03a294a12e851f5e4e9901616ec54e8112`.
- `lamps-035-client1-audit.json`: SHA256 `be8d8ad9ad265953ae47de18e042ec7f991aa34a7bc0bb5290b247d186004625`.
- `lamps-035-server1-falling-state.json`: SHA256 `f918e272dfc51bbd3ded4875ed635001392aef0eba8df82fc820a8af41de553d`.
- `lamps-035-server1-source-drift.json`: SHA256 `0001c564f2f2c9b3ad4bc029563630258b296c99db613445336b056e240b2d8e`.
- `lamps-035-server2-source-drift.json`: SHA256 `fff48fdc55b65e447c765bd9795b654310261e25f2ccc884f49bfb0d7d8899f1`.

Все 4400 финальных client/build source/resource/helper fingerprint совпадают;
server-релевантные совпадают. Допустимое исключение только для opt-in
Dist.CLIENT QA driver, если оно было, связано обоими SHA в artifact report.
Сборка успешна, **2856JSON**, **3684**resource-byte и оригинальные
**668PNG/7JPG/57OGG** прошли; CRC/классы/версии/архивы проверены.
JAR SHA256: `9118cbae541267fd46774b19d366d597c4f684bb874b2ad8d65c1667b281218e`.

Ограничения: подготовленный QA не доказывает полное обычное survival-прохождение,
естественное получение всех материалов, отдельную multiplayer-сессию,
clientreload, все углы/кадры анимации или слышимый звук. Шесть направлений
имеют серверную проверку; клиент использует фактическое крепление DOWN.
Остальные устройства и поздние ветви требуют дальнейшего переноса.

---

## История: Проверка конденсатора загрязнения0.34.0

Дата: **2026-10-14**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26. Охват97/148, наполнение28/56.
Правила — [CONDENSER.md](CONDENSER.md), SHA/полные измерения/подготовленные
условия — [artifact-report0.34](validation/artifact-report-0.34.json).

Свежий полный `condenser-034-server7` прошёл **1369/1369**, native exit0 и BUILD SUCCESSFUL.
Сохранены1283 прежних проверки; добавлены {'condenserCore': 56, 'placementOwnership': 8, 'paidResearchAndCrafting': 17, 'resonatorReadout': 5}.
Проверены оригинальные источники/соседние стороны/сuction/ранний return,
100-единичные резервуары, пятый тик, взвешенный обход/грязь/радиус74/двойнаябаза,
расход эссенции и Flux/выход снизу, nativeTube/Buffer/Transfuser,
redstone/счётчик, реальная оплата рецептов/исследования/резонатор,
отменённые/reentrant/foreign callbacks, физические выпадения/сохранения
и миграция старых отсутствующих catalogue tiles без принудительной загрузки.

Клиент `condenser-034-client5` прошёл **14/14**, JVM exit0, PID25544,
максимум16 собственных окон скрытого
`WinSta0\ThaumcraftQA_*`; входной рабочий стол
`Default→Default`. Нет переключения/global input.
Root и независимый проверяющий просмотрели все14полных1600×1000PNG,
полный лист и шестикадровое превью.

Нативные наблюдения финального лога:

- `[16:52:10] [Render thread/INFO] [th.es.co.cl.CondenserClientSmokeTest/]: THAUMCRAFT_CONDENSER_LIVE_SPARKS: actualclienttileessentia=1 emitted=1 drawn=2 live=1 source=BlockPos{x=0, y=117, z=0},originalgrid16frames8/24/40/layer0,no syntheticemit`
- `[16:52:34] [Server thread/INFO] [th.es.co.cl.CondenserClientSmokeTest/]: THAUMCRAFT_CONDENSER_OUTPUT_PAID: realticks=497 consumedAer=6 drainedAuraFlux=1.0 actualoutputVitium=1 coreEssentia=74 bottomJar=1,no queue/ticker/direct output call; explicitRAWGoggles equipped ONLYafter9paidcrafts960vis/no visdiscount,noGogglescraftclaim,reusedphysicalScanner read-onlyhover`
- `[16:53:33] [Server thread/INFO] [th.es.co.cl.CondenserClientSmokeTest/]: THAUMCRAFT_CONDENSER_CLIENT_FACTS: scenes=14 nativeRiftScan/paidFLUXCLEANUP Observation64Theory64phialVitium10/+10XP/9benchcrafts960vis43crystals/1core4lattice8filters/filtersUsedInRecipes4 cleanupFilters=1 unforced50percentCrystals=0 observedNativeVitium=3 rawAerConsumed=18 nativeAuraFluxDrained=3.0 actualSourceVitiumJar=10+Tube=0 conserved10/casterarmclose/nativebackflow/redstone620ticks/resume/survivalteardown; explicitfixtures/no fullsurvival/multiplayer/reload/audio/allanglesclaim`
- `[16:53:33] [Render thread/INFO] [th.es.co.cl.CondenserClientSmokeTest/]: THAUMCRAFT_CONDENSER_RENDER_AUDIT_OK: 14 actualRiftscan/book/paidbench/nativeconnectedclean-dirtylattice/sparks/output/power/teardown PNG,originalstaticmodels/byteidenticalmedia/spark8framesgrid16layer0 emissions=82 draws=977; no gallery/no TESRinvented`

Подготовленные условия — предшественники/rawknowledge/10Vitiumфиал,
материалы/кристаллы, bedrock-арена/bench, Aer80/Vitium10банки/трубы/пустая
выходная банка. Настоящий Rift seed0x54433634/size10/stability100 — цель-фикстура;
реальный ThaumometerC2S даёт факты, естественная генерация в клиенте не заявляется.
Vis2500 задан до9крафтов; оплаченные960vis сохраняются. Flux10 в25чанках
восстановлен после удаления Rift и всех крафтов **до размещения конденсатора**;
после начала производства аура/резервуары/счётчик не сбрасываются.
RawGoggles надеты после9крафтов для настоящего popup банки и не дают скидку.
Фильтры изготавливаются оплаченно; случайные50%награды не форсируются.

Ранние попытки сохранены в отчёте и validation: Server1 не запустил native
тесты из-за отсутствующего импорта GameRules; compile1 имел две Java syntax
ошибки, compile2 прошёл. Server2 завершился с13ошибками/nativeexit13:
неверные ожидания вертикальных решёток, отсутствующие primaryпластины,
лишний NBT фиала и отсутствие native ticking ticket у дальнего теста.
Нативная comparator-проверка также выявила реальное отличие основания:
modern fullcollision делал его redstone-conductor; теперь явный false
соответствует released nonopaque/nonfull блоку. Изменён только этот
productionфлаг и подготовленные условия; исходный DFS не переписывался.
Эти попытки не заменяют финальный native прогон.
Server3 прошёл все86новых проверок, но упал в старом rate0 Taint тесте.
Rawdebug фиксирует Forge ConfigWatcher reload непосредственно перед отказом;
старое фактическое значение rate не было захвачено. Подготовленный
TaintTerrain fixture теперь использует scoped native SPEC/in-memory config,
сохраняет исходный fileConfig и восстанавливает его в finally. Проверка
принудительного native afterReload и явные rate/base/Flux/target checkpoints
исключают гонку disk-autosave/reload без изменения production распространения.
Server4 прошёл конденсатор и исправленный Taint тест, но старый async
алтарь причинного коллапсера не завершился. Сохранённые MCA/NBT показывают
затопленную площадку Y−35, заменённую водой банку Vitium, выпавшую банку
с полными53Vitium и потерянные свечи; точный исходный приток воды не наблюдался.
В прежнем Server3 сохранились обе банки, и ритуал завершился.
Только этот QA алтарь перенесён наY250 с ограниченной готовностью чанков,
проверкой сухой площадки/физических источников и подробными наблюдениями.
Сроки90/1100/1200тиков,100эссенции/8реагентов/TNT/реальный выход сохранены;
production ритуал, RNG и баланс после начала не менялись.
Каждый новый server-world находится только в проверенном QA каталоге;
предыдущий мир архивируется лишь если он действительно существовал.

Client1 завершился bounded-отказом на искрах. DiagnosticClient2 подтвердил:
реальные четыре решётки и запас19 уже пришли клиенту, счётчик162 продвинулся,
но cachedweight/interval/cost оставались0 и cleanlist пустым. Остановлен только
точно проверенный принадлежащий QA hidden JVM; logs/partialPNG сохранены.
Modern LevelChunk пропускает прежний onPlace-путь инвалидизации на клиенте.
Production Dist.CLIENT observer теперь обновляет исходный loaded-only DFS
на существующем20tick discovery; кэш ограничен текущими3чанками вокруг игрока.
Это явная современная визуальная адаптация с задержкой до20clientticks;
счётчик, эссенция, Flux, серверная оплата и исходный sparkcadence не меняются.
Финальный server/client прогон выполнен заново после production исправления.
Client3 подтвердил actualclient4/3.10/554/6, настоящие искры, первый Vitium
и совпадение грязной решётки. Root просмотр полногоPNG выявил заслонённую
временной bedrock-площадкой машину; прежняя высота этой площадки также мешала
нативной позиции для фильтра. Остановлен только проверенный QA PID13884,
сохранены10частичныхPNG и logs. Исправлен исключительно opt-in Dist.CLIENT
driver: очищает ещё принадлежащие fixture BEDROCK ячейки3,112..115,3 до
новой высоты/возврата камеры. Целевые блоки, оплата и счётчик не менялись.
После исправления клиент прошёл заново; server6 production sources остались
точно прежними, единственное разрешённое Driver-исключение связано SHA.
Client4 остановился сам на слишком узком QA ожидании9Vitium в банке+1в трубе.
Сохранённые MCA/NBT и released bytecode подтверждают native возврат:
после закрытия EAST suction трубы31 меньше32банки, поэтому реальное
состояние стало10+0 при отключённом основании и грязной решётке. Потери нет.
Driver теперь проверяет сумму10, точные допустимые9+1/10+0 и реальную
единицу Vitium до закрытия. Финальные логи сообщают фактические количества.
Для снимка искры выбирается наблюдением естественный непрозрачный кадр
исходного спрайта; исходные кадры30/31 полностью прозрачны. QA не создаёт
частиц и не изменяет RNG/частоту/срок их жизни. Ранние9PNG/logs сохранены.
Независимый released/render-аудит также выявил production отличие прежнего
стандартного шейдера: cutoff0.1 и обычное alpha-смешивание вместо1/255 и
SRC_ALPHA,ONE исходного ParticleEngine. Добавлен отдельный зарегистрированный
нативный шейдер с исходным порогом, additive смешиванием и lightmap.
PNG/спрайты/эмиссия/цвет/срок жизни/частота не менялись. Полный финальный
сервер и клиент заново проверяют именно эту production версию.

Все 4385 финальных client/build source/resource/helper fingerprint совпадают;
server-релевантные совпадают. Разрешённые исключительно opt-in клиентские
исключения, если они понадобились, указаны с обоими SHA в artifact report.
Сборка успешна, **2856JSON**, **3684**resource-byte и оригинальные
**668PNG/7JPG/57OGG** прошли. JAR/ZIP CRC, классы и версии проверены.
JAR SHA256: `50088444b811360a5232cec60f7f25d3bd6b26e615a0a4f526a4c43f0d373ef9`.

Ограничения: подготовленный survival QA, не полное ручное прохождение,
не отдельная multiplayer-сессия/clientreload/allangles/audio. Прямой push
конденсатора отклонён вместо исходного ложного приёма без хранения;
горизонтальный pull и исходные quirks сохранены. Loaded-only/физические
transactions — явные современные адаптации. Произвольные сторонние paid
handlers без наблюдаемого владения не имеют native-контракта отката.
Остальные устройства и поздние ветви требуют дальнейшего переноса.

---

## История: проверка адской печи и мехов0.33.0

Дата: **2026-10-12**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Оригинал — выпущенный TC6.1.BETA26; JAR/source SHA закреплены в аудитах.
Канонический охват **96/148**, наполнение **28/56**.
Правила — [FURNACE.md](FURNACE.md), точные доказательства и SHA —
[artifact-report0.33](validation/artifact-report-0.33.json).

Полный свежий Server4 `infernal-033-gametest4.log` прошёл **1283/1283**,
native exit0/Gradle exit0/BUILD SUCCESSFUL **4m 3s**.
К прежним1211 добавлены42 проверки печи/конструкции,5 владения оплатой,
16 прогрессии/рецептов,7 мехов и2 pollution-пакета. Проверены исходная
ориентация isOpp, все вращения/уровни лавы/top wildcard, отменённые/вложенные
callbacks, физическая рука/оплата,32 UP-only/no-extraction слота,
cookedflag/idle20vis/countdown/bonus/XP quirks, реальные handlers/ItemEntity/XP,
сохранение принятой выдачи, разбор/возврат, loaded-only миграция,
ванильное ускорение и typed dimension-qualified FX в строгом радиусе32.

Финальный скрытый клиент `infernal-033-client4` прошёл **15/15**, JVM exit0,
PID49864, максимум16 принадлежащих
ему окон на `WinSta0\ThaumcraftQA_*`; входной рабочий стол
`Default→Default`. Никакого переключения
рабочего стола или global input. Root и независимый проверяющий просмотрели
все15 полных1600×1000PNG, полный лист и шестикадровое превью.

Нативные измерения из финального лога:

- `THAUMCRAFT_INFERNAL_NETHER_FIXTURE: actual ownedNetherbiome/7x7safeplatform/nativechangeDimension transport,no fact setter; awaitoriginal200Livingticks`
- `THAUMCRAFT_INFERNAL_VANILLA_NATIVE: actualelapsed=35 cookingProgress=52 originalpaidfuel/outputunfinished`
- `THAUMCRAFT_INFERNAL_LIVE_POLLUTION_OBSERVED: actual32radiusS2C/sourceBlockPos{x=1, y=113, z=2}/received=1 emitted=1 live=1 drawn=2 goldSoFar=2 queued=30 pending=0; captureordinaryworld DURINGnativeproduction,sourceframe56/RGB1,.3,.9/no syntheticpollution`
- `THAUMCRAFT_INFERNAL_SMELTED: realworldticks=873 raw32RawGold->32gold +43bonusnuggets +2rareearth/32XP;3bellows/cook26/speedy8/visPaid40/Fluxdelta=1.0/originalevent3=31/actualPurplePackets=1 puffs=1 draws=210,preproductionSeed=1413690931`
- `THAUMCRAFT_INFERNAL_PHYSICAL_PICKUP_PAID: nativeplayerinventory32gold/43nuggets/2rare +32XP,actualplayer=(3.2379243301282887, 112.0, 5.16677451278989),elapsedWorldTicks=3`
- `THAUMCRAFT_INFERNAL_CLIENT_FACTS: scenes=15,studies2,observationsRawPaid32/ARTIFICE16+GOLEMANCY16,studyXPEarned20,paidBenchCrafts4/vis100/Aer4,paidSalis1/redstone1+3crystals/bowlFlintReturned,survivalBellows4,formedFurnace1/dust1,physicalGold32,nativeGoldBonus=43,nativeRareEarthBonus=2,nativeXP32,visPaid=40.0,FluxDelta=1.0,nativeLavaEvents=32,nativeLavaParticles=160,bellowsRender=faces=[0, 0, 0, 1740, 1666, 3394],bufferExtensions=0,inflation=0.30607173..1.0216956,actualSurvivalTeardown1; explicitrawfixtures,no fullsurvival/portalbuilding/multiplayer/audio/allanglesclaim`
- `THAUMCRAFT_INFERNAL_POLLUTION_FINAL_OBSERVED: source=BlockPos{x=1, y=113, z=2} nativeReceivedPackets=1 emittedOriginalPuffs=1 actualPuffDraws=210 preproductionSeed=1413690931 no postproductionFlux/RNG/send/FX/queue/output replacement`

Факты получены реальными200 Living ticks в адском биоме и Thaumometer C2S.
Два исследования расходуют32 raw Observation и дают20XP; четыре меха
изготовлены за100vis/Aer4 и настоящие материалы. Salis Mundus изготовлен
в vanilla CraftingMenu из redstone/трёх различных кристаллов с возвратом
кремня/миски; один оплаченный порошок формирует26 частей печи.
32RawGold вставлены реальной ItemEntity-коллизией и обработаны настоящими
мировыми тиками; бонусы/XP подобраны и золото передано в обычный сундук.
Показаны работа vanilla Furnace, реальные лавовые и purple pollution FX,
40 тиков остановки мехов redstone и survival harvest с обратной структурой.

Подготовленные условия: родительские исследования/raw observations,
материалы/кристаллы, bedrock-арена, исходные26 блоков, bench/vanilla furnace/
chest. Транспорт в настоящий Nether и безопасная площадка7×7 — fixture;
строительство портала не проверялось. Аура80/highFlux9chunks задана **до**
формирования ядра:20vis заряжается в простое, второй20vis после20 выплавок;
итоговый40vis/8speedy измерен без промежуточного сброса. Однократный
pre-input RNG seed для покрытия визуального pollution-события указан в логе.
Финальные факты/исследования/мехи/порошок/печь/выходы/XP не выданы fixtures.
Driver не подменяет ticks, очереди, выдачу, Flux или частицы.

Ранние прогоны сохранены отдельно и не считаются финальным доказательством.
Server1 завершил1281 тест с тремя ошибками fixtures: canceled entity observer,
BlockItem сравнение через неподходящий catalogue helper, миграция второго
чанка. Исправлены только наблюдения/fixtures. Server2 завершил1281 тест
с одной старой Taint fixture ошибкой; её историческая причина **не установлена**.
Добавлена bounded readiness actual AABB-chunks и строгая диагностика исходного
75HP/0Flux перед одним20tick pulse: Server3 получил74.5HP/.1Flux.
Taint production/границы/ожидания не ослаблены и pulse не повторяется после отказа.
Client1 подтвердил Nether/Ignis, но завис на phase3, проверявшей уже убранный
в инвентарь стержень; остановлен только проверенный QA PID47552.
Client2 проверил выплавку/выдачу, но его pickup-сцена ждала предметы вне
достижимого расстояния; остановлен только проверенный QA PID21564.
После Server3 изменён только opt-in Dist.CLIENT driver: phase3 earlyreturn
и маршрут физического игрока к реально выброшенным предметам/XP с ограниченным
ожиданием и строгими исходными подсчётами; выдача/производство не изменены.
Client3 прошёл15 native сцен, но кадры были854×480 из-за отсутствующих
width/height аргументов в Infernal launch profile. После исправления выполнены
свежие полный Server4 и Client4. Все**4366** source/resource/helper fingerprints
финального сервера/клиента/сборки совпадают, исключений больше нет.

Сборка успешна; **2853 JSON**, **3679** JAR resource-byte проверок,
оригинальные**668PNG/7JPG/57OGG** без изменений прошли. CRC/версии/классы,
единственный AT cookingProgress и архивы проверены.
SHA256 JAR: `2b296cdcac639dcce433475a57cb9eac09761ad609493915ea7fb54be4ea500d`.

Границы: не полное manual survival, не независимый multiplayer и не аудиопроверка
всех звуков/углов. Произвольный opaque сторонний IItemHandler, принимающий и
уничтожающий предмет до наблюдения/return, не покрывается native ownership-proof.
Сохранены исходные quirks countdown/idlevis/XP>=1 и vanilla literal199;
modern raw-metal tags/typed dimensions/loaded-only защита документированы.
Остальные устройства, Warp/Crimson/Eldritch и незавершённая экология остаются.

---

## История: проверка зеркал и магической лампы0.32.0

Дата: **2026-10-12**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Исходный TC6.1.BETA26, pinned JAR/source и современные адаптации описаны
в [MIRRORS.md](MIRRORS.md) и отдельных аудитах поведения, прогрессии,
клиента и лампы. Канонический охват **94/148**, наполнение **28/56**.
Зеркало эссенции реализует IAspectSource, не трубу и не накопитель:
одна операция оплачивает одну единицу в физической удалённой банке.

Свежий полный сервер `mirror-032-gametest5.log` прошёл **1211/1211** обязательных проверок,
native exit0/BUILD SUCCESSFUL **3m 52s**. Сохранены прежние1109;
добавлены51 item/hand,22 remote airborne,20 progression и8 lamp/migration, плюс одна отдельная native initialization проверка Flyer.
Проверяются связи, сохранение, тип/направление, исходная random-entry очередь,
один предмет после count>20, instability/Flux, loaded-only межпространственные
endpoints, отмена/вложенные Forge callbacks и возвраты. Native EntityPlaceEvent
проверяет итоговую отмену, включая позднюю отмену/снятие отмены, физическую
руку/списание, survival/creative, раннее удаление и последующее движение слота.
Удалённые операции сохраняют один hop, shared cache/cooldown, оригинальный
cold acceptance quirk, реальные leaf jars и две dimension-qualified FX legs.
Настоящие matrix/transfuser routes и отсутствие force chunk tickets проверены.

Исследования сохраняют исходных строгих родителей, !lux, f_TELEPORT,
физические ender pearl/mirrored glass, знания32/16 и общие5+5XP.
Glass/lamp bench проверяет50vis и точные Aqua/Ordo либо Aer/Ignis кристаллы.
Три настоящих world-ticked алтаря проверяют75/100/75 эссенции, central,
все reagents, fresh output/craft proof и100последующих native ticks без replay.
HandMirror требует empty map и plain unbound metadata0 mirror; filled map
и bound typed link отвергаются, включая сетевое восстановление рецепта.
Лампа проверяет native support/редстоун/границы очистки, LOS, Y/clamp,
invisible persistent light15 и миграцию, сохраняющую оплаченные native очереди.

Первый запуск завершился до тестов из-за дублированного jar sound registry;
зеркала теперь используют существующий BrainJarModule.JAR. Server2
завершился native exit6 с шестью ошибками; исходные журналы сохранены.
Ошибочные primal indices заменены символическими именами и exact assertions.
Отрицательный zero-aspect hover не обязан иметь HUD, но геометрия и read-only
знания проверены. Старое ожидание empty Enderman scan заменено exact f_TELEPORT,
с сохранением запрета f_golem для этого существа. Межпространственный тест
проверяет принятый физический ItemEntity через native Forge join, а не
visibility query без игрока; queue2+output1=3 и remote level/cooldown проверены.
Сбой lamp signal выявил настоящую ошибку: .air() glimmer терялся в пустой
секции native LevelChunk. Теперь он хранится как non-air невидимый заменяемый
блок света (как vanilla LightBlock), остаётся без коллизии/loot/clone и явно
не служит опорой лампы. Новый восьмой lamp test проверяет реальное размещение
в пустой секции, чтение, native chunk palette serialization и удаление.
Это документированная modern storage adaptation; прежние неудачные прогоны
не являются passed QA.

Финальный скрытый клиент `mirror-032-client4`, профиль Mirror: **20/20**,
native JVM exit0, PID**38828**, максимум
**16** собственных окон.
Input desktop **Default → Default**.
Каждый PID/desktop/path/маркер связан с одним финальным запуском.
Desktop не активировался, глобальный ввод и обычные сохранения не менялись.

Native C2S оплатил четыре исследования:96raw Artifice Theory,
32raw Artifice Observation; за стадии получены исходные40XP. Lux/teleport получены настоящим
Thaumometer scan. Семь bench crafts оплатили350vis/14кристаллов;
шесть наполнений оплатили475typed essence, оставив15 из490raw.
Созданы3item mirrors (один потреблён hand recipe),1hand и2essentia mirrors.
Четыре paid mirror instances реально размещены и связаны в две пары.
Проверены8physical emerald output,8hand C2S diamonds и24Ignis remote jar→
receiver, свет15 оплаченной лампы, восемь survival harvest/pickup/replacements
тех же четырёх зеркал для всех шести направлений крепления.
Точные фактические counters сохранены в nativeClientFacts машинного отчёта.
Клиент не выполнял прямые runtime tick/queue/setLink/transport вызовы.

root и mirror_essentia_finish_032 просмотрели все20полных1600×1000 PNG
и фактический лист. Имена/хэши, выводы и ограничения каждого просмотра
связаны с окончательным client log SHA256 в visualReview отчёта.
Книга, реальные bench/altar, исходное окно176×166 с одним входом,
разные native frame textures, pane/tunnel и крепления проверены.
Нет dev/server-check/F3/gallery overlay. Captions листа находятся вне игры.

Дополнительное ревью выявило nested addFreshEntity: внутреннее добавление
может принять предмет, а внешнее вернуть false. Monotonic native onAddedToWorld
proof предотвращает повторное владение, включая accepted-then-consumed callbacks.
Отдельные проверки сохраняют/reload очередь и ledger внутри такого callback:
уже принятая физическая выдача не попадает в сохранённый оплаченный остаток.
Не принятые reservation snapshots продолжают сохранять оплату.

Старый Flyer test в server3 сообщил скачок без координат/причины. В server4
неизменённый строгий порог прошёл с диагностикой; это не доказательство причины3.
Проверка теперь разделяет native firstRun initialization и последующий flight:
actual world tick завершает firstRun до установки высокого потолка/печати,
порог<1 сохраняется на всей задаче. Отдельный deterministic native test
проверяет исходную firstRun/recoverHome ветку при пересечении home boundary.
Production Flyer AI не менялся; исходный сбой3 не приписывается этой причине
как установленному факту.

Client1 сохранил18кадров, но финальная проверка сохранности не прошла:
QA stow удалял изумруды, случайно подобранные в выбранную руку.
Сохранение содержало Emerald0/Diamond8, пустые очереди и source0/receiver24.
Исправлен только opt-in driver: физическое сохранение каждой занятой руки,
при прежних строгих8+8. Client2 прошёл18/18/nativeexit0.
Client3 добавлял широкие планы и две closeups, но после19кадров остановился
на потолочном ракурсе: KeyboardInput сбрасывал shift, выставленный в END tick.
Завершён только проверенный собственный QA JVM45876; это не passed QA.
Client4 использует opt-in MovementInputUpdateEvent после чтения клавиатуры
и проверяет настоящую CROUCHING pose/eye1.27; все20сцен прошли/nativeexit0.
Исходные журналы и ранние кадры сохранены отдельно и не выдаются
как окончательное доказательство0.32.

Подготовленные условия: собственный survival QA-мир, предшествующие studies,
raw balances/materials/crystals/aura, bedrock arena, физические bench/altar,
симметричные candle pairs/490typedessentia, raw scanner/caster и прежний
output transfuser. Ночь18000 — явная fixture для настоящего света лампы;
две closeups используют обычные crouch/F1 и затем восстанавливают HUD.
Целевые исследования, факты, лампа/стекло/зеркала и
телепортированные предметы не выдавались как fixtures.
Это не полное survival-прохождение или независимый multiplayer; клиентские
сцены не доказывают все dimensions/audio/animation frames. Cross-dimension
серверные испытания и реальные client PNG имеют отдельные границы.
Native tunnel использует исходную проекцию камеры, не remote-world camera.

Финальная сборка **22s**, **2852 JSON**.
**4343** src/resources/scripts/build.gradle
fingerprints совпадают полностью между окончательными client/build snapshots.
Все4342server-relevant файла совпадают с server5. Единственная последующая
правка — opt-in Dist.CLIENT MirrorClientSmokeTest: сохранение подобранных
предметов, ракурсы двух closeups и собственный QA crouch input event.
Core/resources/helpers/tests не менялись;
старый и финальный SHA драйвера, полный список snapshots сохранены в отчёте.
Server5 не объявляется испытанием последующего клиентского драйвера.
JAR CRC/duplicates/version/nativeclasses и **3677**
resource byte checks прошли; **668PNG/7JPG/57OGG** совпадают с released JAR.
Mods ZIP содержит этот JAR и pinned TerraBlender3.0.1.10;
screenshots ZIP содержит ровно20финальных кадров, preview — просмотренный лист.
SHA256 JAR: `e9f69ed7f2de38ed6b28331ea55631cc625d07d6c6d4308bd288709cfceae69a`. [Машинный отчёт0.32](validation/artifact-report-0.32.json).

Остаются другие устройства, Warp/Crimson/Eldritch и незавершённая экология.
Эта сборка не объявляет все148исследований и весь мод завершёнными.

## История: проверка магического бура 0.31.0

Дата: **2026-10-11**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Магический бур реализован по выпущенному TC6.1.BETA26: один слот кирки,
без топлива, реальные Forge harvest/drop/XP, спираль, чары, владение,
красный камень/особый рельс, исходные модель/GUI/луч/частицы/сохранение.
ARCANEBORE сохраняет INFUSION+BASICTURRET+FOCUSBREAK, исходные32/16/16
сырых знаний и наполнение basic turret с восьмью компонентами и200эссенции.
Канонический охват **90/148**, наполнение **25/56**.
Контракты и современные защитные адаптации — в [BORE.md](BORE.md) и
трёх аудитах [поведения](docs/tc6/ARCANE-BORE-BETA26-AUDIT.md),
[прогрессии](docs/tc6/ARCANE-BORE-PROGRESSION-BETA26-AUDIT.md),
[клиента](docs/tc6/ARCANE-BORE-CLIENT-BETA26-AUDIT.md).

Свежий полный сервер **bore-031-gametest2** прошёл **1109/1109**
обязательных тестов, native exit **0**, BUILD SUCCESSFUL **3m50s**.
Лог `validation/bore-031-gametest2.log`; прежний проверенный QA-мир
сохранён в `run/gametest-server/archive-bore-031-server2`.
Добавлены35проверок бура и8прогрессии/наполнения; прежние1066сохранены.
Высокие подготовленные площадки находятся на абсолютной Y284/300.
Синхронные тесты используют native entity callbacks, отдельные события
и физические инвентари. Отдельный сценарий выполняет50добыч на настоящих
world/server/entity ticks, проверяет равенство трёх счётчиков, ровно50
булыжников в сундуке, один износ кирки и исходный неизрасходованный заряд10.
Другие проверки охватывают реальный дроп/XP/содержимое блока, зачарования,
замену кирки даже идентичной копией, размещение/снятие/NBT, отменённые
и вложенные callbacks, смерть внутри выдачи предмета и повторные выдачи.

Первый полный прогон сохранён:1108тестов/native exit6/6ошибок fixtures.
В новых тестах исправлены native hurt cooldown, veto только окончательного
выброса вместо перехвата добычи, срок чистого спирального50-block опыта.
Добавлен35-й тест exact-copy BreakEvent. Старые fixtures исправлены через
реальную доступность всех AABB-чанков Seed, настоящий skylight вместо5тиков
и правильный earChunk/native END очередь миграции вместо200ручных callbacks;
физический EAST redstone source сохраняет питание реле. Производственные
границы64/200, Taint pulse и CelestialScanner не менялись ради этих сбоев.
Перед первым прогоном независимое ревью выявило и исправило настоящие
потенциальные дубли/потери через callback-подмену placer/tool, смерть внутри
выброса и отменённую/исцелённую смерть. Финальный свежий сервер проверяет
окончательные guards; неудачный тестовый опыт не повторялся до случайного успеха.

Скрытый клиент **bore-031-client1**, профиль Bore: **10/10**, native JVM
exit **0**, PID**34588**, максимум**16**собственных окон на non-input desktop.
Input desktop **Default → Default**; переключения/активации, глобального
ввода и изменений обычных сохранений не было.
Логи `validation/bore-client-bore-031-client1-client.log`, `-run.json`,
`-desktop.json`; каждый путь/десктоп/PID/маркер связан с одним запуском.

Native C2S оплатил ARCANEBORE32GolemancyTheory+16ArtificeObservation+
16AuromancyObservation и дал исходные10XP(start5+completion5).
Физический алтарь потребил200typed essentia,8компонентов и1basic turret,
оставил3единицы в каждой из5банок и создал один свежий Bore без inputNBT.
Этот предмет извлечён, размещён настоящим survival placer и потреблён.
Один owned Bore, ID235/HP50/NORTH/NoAI=false, получил plain/enchant кирки
через реальные GUI transfers. Plain свойства7/24/+4; enchanted11/56/+7,
Fortune4(maxSoundingIV)/Refining4/SilkTouch. Пауза без питания40native ticks;
после настоящего redstone block работает обычная спираль/Forge harvest.
В финале снятие питания и20native ticks подтвердили13air блоков из252=
**7cobble+6Silkstone**,0loose duplicate drops,1Bore/charge10/toolDamage0.
Наблюдались115отрисовок луча и789эмиссий настоящих втягиваемых частиц.
Сундуковый PNG снят до зачарованной добычи и показывает7cobble; финальные
7+6доказаны последующим native журналом, а не подменённым кадром.

Root и независимый bore_audit_review_031 просмотрели все10полных1600×1000
PNG и лист. Книга, рецепт, GUI/здоровье/свойства читаемы; модель/стекло,
зелёный луч, частицы и реальные отверстия видны. Missing textures и
dev/server-check/F3/gallery надписей в игровом кадре нет. Малый текст
соответствует original0.5scale; captions листа находятся вне игры.
SHA256 каждого кадра/листа привязан к финальному клиентскому журналу.

Подготовленные условия: новый собственный survival мир, предшествующие
исследования/сырые знания, загруженная bedrock-площадка/252stonewall/chest,
физический алтарь/свечи, raw basic central/eight ingredients/215typedessentia,
250vis и две raw кирки. Enchanted tool подан как fixture, не получен
зачарованием в этом опыте. Готовый Bore/целевое исследование/добыча не
выдавались. Нет полного survival/enchanting прохождения или независимого
multiplayer; один ракурс/GUI scale/English locale. Статичные PNG не проверяют
все углы прозрачности, каждый кадр непрерывной анимации или воспроизведение звука.

Финальная сборка `validation/build-0.31-final.log`: exit0/BUILD SUCCESSFUL20s.
`validation/resources-0.31-final.log`: **2846JSON**, модели/текстуры/локали.
**4307** fingerprints всехsrc/scripts/build.gradle одинаковы в финальных
server/client/build snapshots. JAR CRC/дубликаты/версия/classes прошли,
**3667** упакованных resource byte checks; исходные **664PNG/7JPG/57OGG**
побайтно совпадают с закреплённым BETA26. Mods ZIP содержит этот JAR и
закреплённый TerraBlender3.0.1.10; screenshot ZIP ровно10финальных кадров,
preview — реально просмотренный лист. SHA256 JAR: `e45c402975b3293cd6120a931ab30677d4c5c677f5be230368ee19d31683dccb`.
Машинный отчёт — [artifact-report0.31](validation/artifact-report-0.31.json).

Зеркала ещё не включены, охват ими не увеличен. Следующий этап подготовлен
в [MIRRORS-BETA26-AUDIT.md](docs/tc6/MIRRORS-BETA26-AUDIT.md), включая
обязательные lamp/glass/f_TELEPORT, empty-map correction и remote essentia API.

## История: проверка устройств 0.30.0

Дата: **2026-10-11**. Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
Добавлены исходные REDSTONERELAY, ARCANEEAR, BASICTURRET и ADVANCEDTURRET,
обычный рецепт переключаемого уха и магический рецепт активирующего рельса.
Канонический охват — **89/148**, наполнение остаётся **24/56**:
обе турели изготовляются на магическом верстаке. Правила —
[ARTIFICE-DEVICES.md](ARTIFICE-DEVICES.md), исходные контракты —
[реле и ухо](docs/tc6/REDSTONE-EAR-BETA26-AUDIT.md) и
[турели](docs/tc6/CONSTRUCT-TURRETS-BETA26-AUDIT.md).

Финальный свежий сервер **turret-030-gametest6** прошёл **1066/1066**
обязательных тестов, native exit **0**, BUILD SUCCESSFUL **3m 16s**.
Журнал: `validation/turret-030-gametest6.log`.
Прежний QA-мир сохранён в проверенном соседнем
`run/gametest-server/archive-turret-030-server6`.
Добавлено **56** проверок: 21 конструкций, 24 реле/уха и 11 исходной
прогрессии/оплаченных рецептов. Прежние 1010 проверок сохранены.
Проверены владение, реальные стрелы/дропы, размещение и отмены callbacks,
скорость/характеристики/восстановление, NBT/старые сохранения, GUI/дистанция,
AI/цели/команды/PvP, физическое питание рельса, нотные события/импульсы,
переключаемое ухо, исходные требования и точные платежи.

Поиск врага отдельно проверен на **140 настоящих мировых, серверных и
entity ticks**, с выбором монстра вместо более близкого животного,
физическими стрелами и расходом боеприпасов. Новые чанки и entity sections
сначала должны стать доступны; затем проходит один фиксированный опыт.
Неудачный опыт не повторяется до случайного успеха.
Синхронные arenas находятся на абсолютной Y250, асинхронная — Y280;
области обзора загружаются явно и восстанавливаются после проверки.

Финальный скрытый клиент **turret-030-client2**, профиль Turret:
**10/10** сцен, native JVM exit **0**, PID **31388**, максимум **16**
принадлежащих JVM окон на отдельном non-input рабочем столе,
input desktop **Default → Default**. Журнал и данные запуска:
`validation/turret-client-turret-030-client2-client.log`,
`validation/turret-client-turret-030-client2-run.json` и
`validation/turret-client-turret-030-client2-desktop.json`.
Переключения/активации рабочего стола, глобального ввода и обращения
к обычным сохранениям не было.

Клиент оплатил BASICTURRET/ADVANCEDTURRET настоящими C2S-запросами,
изготовил **два обычных и один улучшенный арбалет** за **350 vis и четыре Aer**.
Улучшение потребило второй действительно оплаченный обычный арбалет.
Два предмета размещены через настоящие placers; обе стопки по64 стрел
внесены через живое окно, с удалением из инвентаря игрока.
Обычная AI самостоятельно нашла Husk: ammo64→63, здоровье цели1000→994.096;
улучшенная после выбора животных нашла Cow: ammo64→63, здоровье1000→995.
Наблюдались физические Arrow с правильным владельцем и реальные попадания.
Все четыре кнопки исходного меню прошли C2S/синхронизацию:
flags2→3→1→5→13→5→1. Остаток стрел обычной турели вынут через её GUI;
настоящий UP-раздатчик под ней передал64 стрел в физический main-hand слот.
Клиент наблюдал шеститиковый выстрел и десятитиковое заряжание.
Все **10** полных PNG1600×1000 и контактный лист просмотрены root и
независимым проверяющим. GUI читается, модели видны, пропавших текстур
и служебных надписей нет. Раннее сообщение о высоком Flux — исходный
`tc.fluxevent.3`, а не dev-подсказка. Кадры верстака показывают живой
оплачиваемый рецепт до извлечения; следующий C2S-журнал доказывает платёж.
Раздатчик расположен заподлицо с площадкой и сам по себе не различим
на статическом кадре; расход его стопки проверен native-журналом.
SHA каждого кадра и листа привязан к финальному клиентскому журналу.

Подготовленные условия указаны явно: собственный свежий survival QA-мир,
предшествующие исследования и f_DISPENSER, сырые знания и материалы,
250vis/fullmoon и соседние1000flux для изоляции платежа,
загруженная площадка, неуязвимость игрока, NoAI/1000HP только на целях,
две исходные стопки по64 стрел и UP-раздатчик с дополнительной стопкой64.
Готовые турели и целевые исследования не выдавались fixtures.
Клиент не фотографирует физическую работу реле/уха: эти цепи проверены
серверными native circuit-тестами. Сценарий не является полным survival
прохождением или независимым multiplayer-тестом.

Финальная сборка: `validation/build-0.30-final.log`, native exit **0**,
BUILD SUCCESSFUL **19s**. Ресурсная проверка:
`validation/resources-0.30-final.log`, **2846 JSON**.
Оригинальные два GUI извлечены из закреплённого JAR; игроку не добавлены
служебные надписи, новые dev-локализации или подменённые тексты исследований.
JAR проверен по CRC, отсутствию дубликатов, версии0.30.0, классам native
устройств и **3664** точным сравнениям упакованных resource bytes.
Оригинальные **662 PNG / 7 JPG / 56 OGG** побайтно совпадают с BETA26.
Mods ZIP содержит этот JAR и закреплённый TerraBlender3.0.1.10;
скриншотный ZIP — ровно10 финальных кадров, preview — просмотренный лист.
SHA256 JAR: `70ef93ace82899b4a92b1275dcd103d533dc9f76a4c70d5d2b5ad89d4181a7f8`.

История неисправных прогонов сохранена. Server1 остановился на повторной
регистрации `thaumcraft:key`; теперь используется существующий SoundEvent.
Server2 выявил пропуск тега `minecraft:rails` и неверные условия
muzzle/death/wire/migration fixtures. При сверке интеграции также исправлен
registry lookup пластин, которые зарегистрированы основным модулем.
Server3 выявил пересечение низких arenas и ожидание после случайного
распада падающей корки. Последний тест теперь наблюдает native посадку
сразу и по-прежнему требует точный блок/источник/отсутствие item drop.
Server4/5 сохраняют неудачу теста acquisition:140 обновлений внутри одного
server callback замораживали глобальную чётность современного Mob scheduler.
Производственный AI ради этих проверок не менялся; server6 использует
настоящие последовательные тики.
Client1 остановлен только по проверенной принадлежности его JVM после
зависания сценария на hotbar slot2. В opt-in TurretClientSmokeTest добавлена
обычная ClientboundSetCarriedItemPacket0 для синхронизации выбранного слота.
Client2 полностью перезапущен после этого исправления. Остальные **4294**
исходных/resource/helper/test файла побайтно совпадают с пройденным server6;
клиент и финальная сборка имеют один полный fingerprint из **4295** файлов.
Исключение касается только opt-in client driver и записано в отчёте.

Arcane Bore, зеркала и остальные устройства ещё требуют переноса.
Итоговые артефакты, хеши, просмотр кадров и границы приемки —
в [artifact-report-0.30.json](validation/artifact-report-0.30.json).
Исторические результаты 0.29 и предыдущих этапов сохранены ниже.

# Проверка очагов заражения 0.29.0

Дата: **2026-10-11**. Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
Добавлены живые Seed/Prime/Tiny, исходное событие Prime разлома,
распространение и распад заражённых блоков, падающая корка,
исходные звуки и реакции модели. Охват исследований **85/148**
и путей наполнения **24/56** сохранён. Правила — в
[TAINT-ECOLOGY.md](TAINT-ECOLOGY.md), сверка с закреплённым оригиналом —
в [аудите BETA26](docs/tc6/TAINT-SEED-ECOLOGY-BETA26-AUDIT.md).

Финальный полный сервер **taint-029-gametest3**: **1010/1010**,
native exit **0**, BUILD SUCCESSFUL **2m 42s**.
Журнал: `validation/taint-029-gametest3.log`.
Перед запуском предыдущий QA-мир сохранён в проверенном соседнем
`run/gametest-server/archive-taint-029-server3`.
Добавлено 58 проверок: 27 сущностей, 25 земли/волокон, четыре падающей
корки и две дополнительные проверки события разлома.
Проверены точные характеристики, boost/20/1200 тиков,
кубы заражения, геометрия и оплата конверсий, поддержка и распад,
смерть/выгрузка/перезагрузка, настоящее препятствие сну и смерть в пустоте.
Падающая корка проверена на владение исходным блоком, перенос при падении,
NBT и удаление вне высоты мира; отдельного клиентского кадра падения нет.

Изолированный клиент **taint-029-client3**, профиль Taint:
**6/6** сцен, JVM exit **0**, PID **3444**, максимум **16** принадлежащих
JVM окон на отдельном non-input рабочем столе; input desktop
**Default → Default**. Журнал и данные запуска:
`validation/taint-client-taint-029-client3-client.log`,
`validation/taint-client-taint-029-client3-run.json` и
`validation/taint-client-taint-029-client3-desktop.json`.
Переключения/активации стола, глобального ввода и обращения
к обычным сохранениям не было.

В собственном новом мире настоящее событие1 создало Prime с boost25,
добавило Flux30→42 и удалило разлом. Реальные тики вырастили **28 волокон**,
вызвали **одно малое щупальце**, смерть семени оставила **один физический
кристалл**; после смерти источника случайные тики уменьшили волокна
**28→1**. Шесть фактических PNG просмотрены полностью root и независимым
агентом: исправлены чёрные плоскости, пустой значок заражения и уменьшенная
модель Prime. В обычном интерфейсе новых служебных надписей нет;
EN/RU переводы и канонические тексты исследований не менялись.

Подготовленные условия указаны явно: выбранное событие1, size25/Flux30,
загруженная dirt/bedrock площадка, дополнительный boost400, цель на порче,
survival invulnerability/flying, выкладка материалов и randomTickSpeed100.
Они не доказывают полный survival, вероятность случайного события
или все естественные превращения. Статичные кадры не подтверждают
весь цикл анимации; стекло таумометра, блоки и частицы ограничивают
детализацию Tiny и кристалла. Их числа подтверждает отдельный native log.

Сохранён первый неуспешный серверный прогон: 8 падений из1010.
Семь новых тестов вызывали entity.tick напрямую, хотя tickCount увеличивает
внешний ServerLevel; исправлены на настоящий tickNonPassenger с точными
проверками счётчика. Один прежний тест каталога ожидал декоративный AI
у теперь живых семян/щупалец; исходные native политики проверяются отдельно.
Server2 прошёл1010, после чего сверка модели обнаружила два пустых root
элемента, сдвигавших pulse q0→q2. Финальные server3/client3 выполнены
после пропуска этих неиспользуемых элементов. Первые снимки/журналы
сохранены локально; итоговый отчёт привязан к последним исходникам.

Финальная сборка: BUILD SUCCESSFUL **17s**.
Проверены **2839 JSON**, **3656** побайтовых соответствий ресурсов JAR,
CRC и отсутствие дубликатов. Исходные медиа: **660 PNG / 7 JPG / 56 OGG**
совпадают с закреплённым BETA26 JAR; две18×18 иконки эффектов совпадают
с оригинальными crop атласа y216. Версия и название без dev-суффикса.
Хеши JAR, комплекта с TerraBlender, шести кадров, журналов и изменённых
исходников — в [artifact-report-0.29.json](validation/artifact-report-0.29.json).

Полные Crawler/Swarm, большие и гигантские щупальца, Bottle of Taint,
конечный Flux Goo/Thaumic Slime и Champion13 ещё не перенесены.
Независимого multiplayer-клиента и полного ручного survival-прогона не было.

# Историческая проверка игрового интерфейса 0.28.0

Дата: **2026-10-11**. Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
Охват **85/148** канонических исследований и **24/56** путей наполнения
сохранён: очистка интерфейса не добавляет исследований или механик.
Правила и сверка с закреплённым BETA26 — в
[PLAYER-INTERFACE.md](PLAYER-INTERFACE.md).

Свежий полный сервер **product-028-gametest1**: **952/952**, native exit 0,
BUILD SUCCESSFUL **3m 1s**. Журнал:
`validation/product-028-gametest1.log`. Предыдущий QA-мир сохранён
в проверенном соседнем архиве `run/gametest-server/archive-product-028-server1`.

Изолированный клиент **product-028-client1**, профиль ThaumonomiconComplete:
**37/37** сцен, JVM exit 0, PID 29748, максимум 16 принадлежащих JVM окон
на отдельном non-input рабочем столе; input desktop **Default → Default**.
Журнал и сведения о запуске:
`validation/thaumonomicon-client-product-028-client1-client.log`,
`validation/thaumonomicon-client-product-028-client1-run.json` и
`validation/thaumonomicon-client-product-028-client1-desktop.json`.
Видимый Minecraft не запускался; переключения/активации рабочего стола,
глобального ввода и обращения к обычным сохранениям не было.

Маркер NORMAL_PRODUCT подтверждает семь подготовленных ранних состояний:
начальную карту, отсутствие обхода через Tab/нижнюю границу и закрытые
категории, отдельные стадии FIRSTSTEPS, скрытую ORE и строго открываемое
дополнение. Обычные страницы, поиск и переходы по предметам исключают
справочные рецепты. Проверено отображение пяти видов зарегистрированных
игровых рецептов и трёх доступных конструкций, история переходов,
два масштаба GUI, английские
и русские страницы, уведомления и настоящие C2S/S2C подтверждения чтения.
Игровые знания и инвентарь до/после просмотра совпадают;
отдельные отметки чтения меняются настоящими пакетами.

Отдельный явно включённый внутренний layout audit охватывает **148 записей,
287 глав и 375 изображений рецептов**: **930** различных запросов разворотов
и **1369** фактических посещений рендера. Это предоставленные данные
для проверки верстки; они не являются режимом игрока, прохождением
исследований или доказательством реализации оставшегося содержимого.

Root просмотрел все 37 текущих PNG на семи контактных листах и отдельно
полные кадры normal-first-stage, requirements-costs, ru-infusion и
ru-knowledge. Независимый визуальный аудит просмотрел все 37 PNG
на семи листах и восемь кадров в полном размере: блокирующих проблем
и служебных надписей не обнаружено, хеши совпали с манифестом.
Привязка снимков к исходникам/запуску сохранена в
[artifact-report-0.28.json](validation/artifact-report-0.28.json).
Состояния исследований, открытые аспекты, дополнение BELLOWS, исходные
запасы знаний, физическая книга/предметы и каменная площадка — явные
fixtures нового изолированного мира. Платное survival-прохождение
и независимая multiplayer-сессия этим прогоном не устанавливаются.

Независимое сравнение переводов: изменены **137** EN и **148** RU служебных
строк; канонические `research.*` тексты не изменены. Настоящие показатели
предметов, стоимость исследований и исходные настройки печатей,
включая Use NBT Data, сохранены. Творческий раздел использует обычные
исходные варианты вместо расширенного QA-набора NBT.

Финальная сборка `validation/build-0.28-final.log`: BUILD SUCCESSFUL **18s**.
Проверка `validation/resources-0.28-final.log`: **2839 JSON**,
167 значков, 192 размерные текстуры, 12 GUI и 10 IMG.
Название мода — Thaumcraft 6, версия **0.28.0** без суффикса dev.
Упаковка проверила все **3651** ресурс JAR побайтно и паритет исходных
**660 PNG / 7 JPG / 51 OGG**. Удалённый `ContentPreviewScreen.class`
в окончательный JAR не входит. Точные хеши и проверки JAR, mods.zip
и архива 37 снимков сохранены в
[отчёту 0.28](validation/artifact-report-0.28.json).
Результаты и ограничения прежних этапов сохранены ниже.

---

# Проверка хвата таумометра0.27.1-dev

Дата: **2026-10-11**. Minecraft1.20.1 / Forge47.4.10 / Java17.
Канонический набор **85/148** и пути наполнения **24/56** сохранены.
Хват по пользовательскому изображению — адаптация порта;
отдельного двуручного callback в закреплённом BETA26 нет.
[Контракт](docs/tc6/THAUMOMETER-GRIP-PORT.md).

Свежий полный сервер **grip-0271-gametest1**: **952/952**, native exit0,
BUILD SUCCESSFUL **2m36s**. Предыдущий QA-мир сохранён в проверенном
соседнем архиве `run/gametest-server/archive-grip-0271-server1`.
Новых исследований, рецептов и серверных механик этот патч не добавляет.

Клиент **grip-0271-client1**, профиль Thaumometer: **22/22**, JVMexit0,
PID16852,16 owned windows на отдельном non-input рабочем столе;
input desktop **Default → Default**. Видимый Minecraft не запускался,
переключения/активации рабочего стола и глобального ввода не было.
Обычные сохранения не использовались.

Сохранены11 исходных сцен: настоящие C2S-сканы блока/существа/контейнера,
наведение без награды,5XP/FLUX stage1, неизменность всех27 слотов сундука,
повторный скан, аура/переполнение, Shift/offhand, GUI scales2/3,
F1 и удаление прибора. Добавлены11 сцен с физическими серверными
перестановками предметов и native client-settings packets:
основной/второй слот × пустая/занятая другая рука × правая/левая ведущая
рука, два таумометра, настоящий C2S-взмах и возврат к мечу/факелу.
Счётчики фиксируют реальные вызовы PlayerRenderer/ItemRenderer;
маски LEFT1/RIGHT2/BOTH3 и число рук проверяются после equip-перехода.
Знания/XP и содержимое сундука во время сцен хвата не изменяются.

Root просмотрел все22 текущих PNG на четырёх контактных листах и
отдельно полный кадр блока, одноручного хвата и взмаха.
Независимый агент просмотрел все11 сцен хвата в полном размере.
Ладони касаются нижних сторон оправы; посторонний предмет остаётся
видимым, зеркальные режимы совпадают с физической рукой, третья
ванильная рука не появляется, взмах сохраняет контакт.
Фактический скин клиента — slim/Efe. Геометрия default проверена по
native коду; отдельного клиентского прогона default-скина не было.
Прибор, предметы сравнения, арена, аура и цели — подготовленные fixtures;
полное ручное survival-прохождение и отдельный multiplayer не заявляются.

Ресурсы: **2839 JSON**,167 значков,192 размерные текстуры,12 GUI и10 IMG.
Исходные модель/текстуры прибора и GUI/third-person transforms сохранены.
Финальная сборка: BUILD SUCCESSFUL **17s**. Проверены CRC/отсутствие
дубликатов JAR, byte parity всех3651 ресурсов и исходных660PNG/7JPG/51OGG.
JAR, mods.zip с закреплённым TerraBlender и архив22 снимков собраны;
SHA-256 — в [отчёте0.27.1](validation/artifact-report-0.27.1.json).
Результаты и ограничения прежних этапов сохранены ниже.

## Историческая проверка разломов0.27.0-dev

Дата: **2026-10-10**. Minecraft1.20.1 / Forge47.4.10 / Java17;
эталон — закреплённые исходники и JAR Thaumcraft **6.1.BETA26**.
Текущий поддержанный набор **85/148**, пути наполнения **24/56**.
Правила — [FLUX-RIFTS.md](FLUX-RIFTS.md), исходные контракты —
[разлом](docs/tc6/FLUX-RIFT-BETA26-AUDIT.md),
[события/висп](docs/tc6/RIFT-EVENTS-WISP-BETA26-AUDIT.md),
[исследования/коллапсер](docs/tc6/FLUX-RIFT-PROGRESSION-COLLAPSER-BETA26-AUDIT.md).

Свежий финальный сервер **rift-027-gametest4**: **952/952**, native exit0,
BUILD SUCCESSFUL **2m58s**. К888 добавлены64 проверки:19 физического
разлома/ауры/стабилизатора/наград,26 событий/виспа/инфекции/Flux Taint,
19 фактов/платежей/коллапсера/настоящего наполнения. `wussMode=false`;
предыдущие QA-миры сохранены в проверенных соседних архивах.
Проверены fractional sqrt(Flux*3) debit, strict75%/Flux5000 probability,
общая seeded spine/bbox,120/600 cadence, erosion без harvest loot,
реальные OUTLINE-пересечения цветка/кристалла без collision и native ticks,
жидкость/bedrock negatives, контактный урон/creative исключение,
схлопывание с1auraunit/тик и отдельной жемчужиной damage4..7,
оба Warp/эффекты/distanceSquared32 и quirks NBT/reward snapshot.
Стабилизатор расходует1energy на.125 с исходной задержкой.

Висп проверен с настоящей стеной/удалением стены/20-тиковым зарядом,
stationary4damage, negative reload, flight/retaliation/type/crystal loot.
Проверены веса/успех/провал без reroll, infectious3000@2 и6000 каскад,
исходный vis penalty, free cloud и отсутствие придуманной стабилизации.
Flux Taint проверен через native ticking/Forge damage event/registry tags:
40>>amplifier,1damage/armor bypass/обычную Resistance, undead immunity,
лечение восьми исходных tainted-классов, milk cure, lethal damage и saves.
Champion13 не подменяется NBT. Проверены настоящие scan facts,
hover/possession negatives, строгие предшественники и точные платежи;
отдельный native infusion batch расходует TNT/8components/100essentia,
после чего бросает изготовленный результат.

Финальный скрытый клиент **rift-027-client3**, профиль Rift: **9/9**,
JVMexit0, PID31516,16 принадлежащих JVM окон, input desktop
**Default->Default**. Проверена цепочка естественного появления через
обычный aura END tick: Flux5001 (с1unit diffusion) ->4878.5254,
size122 при цене122.47449; скан C2S и FLUX/FLUXRIFT/RIFTCLOSER через
настоящие book requests. Расход RIFTCLOSER:16Observation Auromancy,
64Theory Alchemy/32Theory Auromancy и10XP; FLUX отдельно5XP.
Обычные matrix cycles оплатили50Alienis+50Vitium (две банки70->20),
израсходовали TNT+8ингредиентов и создали один коллапсер. Реальный
стабилизатор лечил разлом за энергию; настоящий survival C2S бросок
израсходовал изготовленный предмет, collision/explosion начал collapse,
11семян подобраны обычными ItemEntity collisions без выдачи в инвентарь.
Все9 текущих PNG просмотрены root на3листах; natural/infusion-output/
closer-paid также полноразмерно. Платежи подтверждает журнал;
частицы частично закрывают altar output, луч стабилизатора кадром не доказан.

Ранние проверки сохранены: server1 **936/940**, server2 **939/940** —
пустой registry specimen, старый23infusion census и visibility fixture виспа.
Production matcher не ослаблен: реальные VoidSeed берутся из реестра,
в census24 и Causality Collapser, Wisp имеет свой9x5x9 arena и wall control.
Client1 остановлен после4сцен: тестовый nitor_yellow не входил в точный
рецепт. Исправлен fixture на настоящий AlchemyModule.NITOR_ITEM; client2
прошёл9/9, server3 —950/950. Независимая сверка обнаружила реальную ошибку
COLLIDER: цветы/кристаллы сохранялись вместо разрушения по выделяемому контуру.
OUTLINE исправляет gameplay; добавлены2 native positive/negative tests,
после чего заново выполнены финальные server4/client3.

Fixtures предоставляют загруженную bedrock arena, Flux5001,
INFUSION+VISBATTERY, raw knowledge, сырьё/эссенцию/свечи и энергию стабилизатора.
После настоящего появления stability100/Flux30 явно защищают длинный
платёжный аудит от посторонних событий. Finished rift/collapser/rewards,
crafting plan и вызовы runtime tickers не подставляются. Это не полный
survival playthrough и не отдельная multiplayer-сессия.
Prime Seed/полная taint ecology, Champion13, Crimson/Eldritch и Void Siphon
остаются неподдержанными. Prime weightedslot10 — no-op без повторного
выбора/декоративного спавна/удаления разлома/загрязнения. Семена и f_VOIDSEED
не завершают поздние исследования. Современные адаптации явно перечислены
в FLUX-RIFTS.md. Итоговая сборка,2839JSON,CRC/ресурсные byte checks и хеши —
в [artifact-report-0.27.json](validation/artifact-report-0.27.json).
Финальный build прошёл за18s. Проверены3651 ресурсных файлов JAR побайтно,
CRC/duplicate entries, version0.27.0-dev и исходные660PNG/7JPG/51OGG.
JAR:10,556,087байт; SHA256
`09db4cbbde65252a60f2879ffd4acba518e2533e392aa89ecac63851fb44e986`.
Mods ZIP содержит этот же JAR и закреплённый TerraBlender; screenshots ZIP —9кадров.


---

# Проверка исправления 0.26.1-dev

Дата: **2026-10-09**. Эталон — закреплённый Thaumcraft **6.1.BETA26**,
Minecraft1.20.1 / Forge47.4.10 / Java17. Полный канонический набор остаётся
**82/148**; вводная FLUX stage1 не входит в число завершённых веток.
Контракт: [сканирование BETA26](docs/tc6/THAUMOMETER-SCANNING-BETA26-AUDIT.md),
игровые правила и адаптации: [THAUMOMETER.md](THAUMOMETER.md).

Финальный свежий сервер **scanner-0261-gametest3**: **888/888**, native exit0,
BUILD SUCCESSFUL **2m41s**. К сохранённым842 добавлено46 проверок:
13 контейнера/сканирования,11 эффектов,10 независимых аспектов,5 миграции,
5 ауры/мгновенного ввода,2 настоящего pick-item/жидкостей.
Проверены отсутствие предварительных аспектных/Alchemy gates, точные
взвешенные награды и три раздельных first-aspect бонуса, неизменность
при повторах/наведении, NBT и зачарования без повторной общей награды,
UP инвентари/обе половины сундука/fallback,100-й и101-й непустые слоты,
сохранность содержимого и знаний после загрузки, отдельные эффекты,
20-тиковый FLUX trigger/строгие пороги/5XP/уведомление/незавершённость.

Первый полный прогон887/888 выявил взаимодействие тестовых миров:
исходный приоритет непустых банок отправил одну Aer в соседний тест
(22 вместо его исходных21). Исправлена только подготовка ближайшей
банки suction-теста: начальная1Aer, затем source1->0/destination1->2;
отрицательные условия сохраняют1/1. Production transport не менялся.
Повторный полный прогон888/888 прошёл. После исправления локализации
он запущен заново, finalserver3 также888/888.

Финальный клиент **scanner-0261-client3**, скрытый профиль Thaumometer:
**11/11** сцен, JVMexit0, PID10752,16 принадлежащих JVM окон,
input desktop **Default->Default**. Реальные C2S сканы блока, коровы,
сундука и повтор сундука; пять общих записей, новый аспект/Observation
только при сканировании, точное сохранение всех27 слотов с четырьмя
непустыми, FLUX stage1 и ровно5XP один раз. HUD/F1/убирание прибора,
две руки/Shift/GUI scales2/3, raw overflow и фактические HUD/world render
счётчики проверены. Все11 финальных PNG просмотрены на двух листах;
overflow и repeat-container дополнительно в полном размере.
Client2 показал ключ вместо предупреждения; finalclient3 повторён после
копирования исходных en/ru строк FLUX и лимита инвентаря. Финальные кадры
показывают настоящий текст. Старые неудачные свидетельства сохранены отдельно.

Финальная сборка BUILD SUCCESSFUL **17s**; ресурсный валидатор прошёл
**2835 JSON**,167bookicons/192размера/12GUI/10IMG. Хеши, байтовая проверка
JAR/исходных media и финальные артефакты —
в [artifact-report-0.26.1.json](validation/artifact-report-0.26.1.json).
Клиент использует подготовленные прибор, объекты, содержимое и raw ауру,
а не полное survival-прохождение; отдельный multiplayer не заявляется.
Общая дальность9, HUD snapshots5ticks и offhand convenience остаются
явными современными адаптациями. Поздняя FLUX/растения/оставшиеся особые
существа и их канонические ветки не объявляются завершёнными.

## Историческая проверка сборки 0.26.0-dev

Дата: **2026-10-09**. Эталон — Thaumcraft **6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.

Работают исходные ORE, CRYSTALFARMER и VISBATTERY: **82/148** полных
канонических исследований. Семь кластеров получают рост, усыхание,
распространение, оригинальную процедурную модель и точные дропы.
Батарея получает исходные десять единиц заряда, пороги, задержки,
редстоун, компаратор, свечение и минимум яркости модели180.
Контракты и игровой путь: [CRYSTAL-FARMING.md](CRYSTAL-FARMING.md).

Финальные проверки:

- **842/842** обязательных GameTests, exit0, BUILD SUCCESSFUL за2m43s:
  validation/crystal-026-gametest4.log. Свежий QA-мир; прежние сохранены
  в проверенных соседних архивах. Сохранены783 прежних сценария,
  добавлены32 роста/опор/добычи,16 батареи и11 сканов/платежей/рецептов.
  Проверены все семь видов, точные NBT/knowledge/XP, отрицательные случаи,
  повторные запросы, сохранения, отказ при неподходящей грани и расход
  native BlockItem. Старый cloud continuation тест изолирует только
  естественное изменение ауры своего чанка во время ручных END событий
  и восстанавливает членство в finally; прежние точные проверки не ослаблены.
- **14/14** CrystalFarming, crystal-026-client4, JVM exit0. Настоящие C2S
  сканы открывают ORE и три дополнения. Два исследования расходуют
  16+16 raw Observation и дают по 10 XP; Farmer дополнительно расходует
  шесть точных первичных кристаллов основного инвентаря. Реальный Salis
  формирует алтарь; настоящее наполнение расходует центральный Aer,
  семена, Salis и **25 эссенции**, создавая один посадочный кластер.
  Результат извлечён из настоящего пьедестала, подобран и установлен
  native BlockItem; обычные random ticks выращивают размер0→3 за10vis
  на шаг. Ускорение512 явно подготовлено; дополнительное распространение
  отдельно платит10vis и не выдаётся за расход только30vis.
  Настоящий верстак расходует **50vis,12 кристаллов**, восемь плит и
  резонатор. Оплаченная батарея установлена обычным BlockItem, затем
  обычные обновления забирают ровно10vis; физический редстоун возвращает
  ровно10. Готовые новые результаты не подставлены, прямых вызовов
  growth/battery tick нет.
- Проверены **1355** состояний baked models, оригинальный mesh8/96,
  настоящий rock ModelData оплаченного выросшего кластера; **11** моделей
  батареи, **132** quad checks и **2660** вызовов настоящего Forge lighting
  merge. Геометрия/inventory/более яркий свет/небо сохраняются. Исходный
  crystal.ogg добавлен; в финальном клиенте предупреждения о пропущенном
  звуке кристалла нет. Проверка не заявляет прослушивание через устройство.
- **10/10** EssentiaTransfuser, crystal-026-regression1, JVM exit0.
  Книга реально оплачивает16+16 Observation/10XP. Два верстака платят
  по100vis/Aer1/Aqua1; оба результата физически установлены. Обычные
  серверные тики переносят **24Ignis**: источник→средняя банка→приёмник;
  итог source0/middle0/collector24. Исходные шесть ориентаций сохранены.
- Оба клиента работали на собственных скрытых Win32 desktops, по**16**
  принадлежащих им окон; input desktop **Default→Default**. Переключения,
  активации окна и глобального ввода не было.
- Root просмотрел все **24PNG** на шести контактных листах и восемь
  кадров в полном размере. Настоящий мир показан для выросшего кристалла,
  полной батареи и двух активных переносов. Остальные live-state галереи
  и обзор всех форм честно являются рендер-превью реальных состояний.
  SHA-256 и замечания: validation/visual-review-0.26.json;
  точные bytes: validation/screenshots-0.26/.
- Финальная сборка с reobfuscation — BUILD SUCCESSFUL за**23s**:
  validation/build-0.26-final.log. Ресурсы — **2835JSON / 3642файлов**:
  validation/resources-0.26-final.log. Упаковка проверяет все bytes ресурсов
  JAR, исходные **660PNG /7JPG /46OGG**, CRC и полное содержимое архивов.
  Итоговые хеши: [artifact-report-0.26.json](validation/artifact-report-0.26.json).

INFUSION/RECHARGEPEDESTAL, raw knowledge, исходные ингредиенты/руды,
эссенция банок, арена, загруженная область и высокая изолированная аура
с соседним flux1000 — явные условия нового клиента. Для независимой
проверки батареи выросшие кластеры QA-арены удалены без дропа.
Это проверка настоящих платежей и механик в подготовленных условиях;
полное ручное survival-прохождение и независимый multiplayer не заявляются.

Наполнение: **23/56** research/craft, **21** установленный обычный путь
ингредиентов, **33** поздних gate. Vitium доступен из адского нароста
через прежнюю BASEALCHEMY, а Vitreus/Vinculum из стекла/деревянной двери.
Мировые Flux Rift, Crimson ecology, Firebat/Pech/Primordial Pearl,
остальные исследования и поздний Элдритч ещё предстоят. В BETA26 нет
активной цепи лабиринта/измерения Outer Lands из TC4.
Все прежние отчёты ниже сохранены как история.

## Историческая проверка сборки 0.25.0-dev

Дата итогового отчёта: **2026-10-08**. Прогоны и сборка ниже выполнены
**2026-10-07**. Эталон — Thaumcraft **6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.

Срез поддерживает **79/148** полных канонических исследований:
78 прежних плюс ESSENTIATRANSPORT. Оба трансфузера получают исходные
исследование/рецепты, физическое размещение и работу раз в пять тиков.
Матрица наполнения сохраняет прежнее воздушное извлечение.
Точные правила: [устройства](docs/tc6/ESSENTIA-TRANSFUSERS-BETA26-AUDIT.md)
и [общий обработчик](docs/tc6/AIRBORNE-TRANSFUSER-HANDLER-BETA26-AUDIT.md).

Финальные проверки:

- **783/783** обязательных серверных GameTests, `BUILD SUCCESSFUL`, exit 0,
  за 3m32s: `validation/air-025-gametest2.log`. Свежий QA-мир;
  прежний сохранён в проверенном соседнем архиве. Сохранены все 746 прежних
  тестов, добавлены 19 shared-handler, 15 физических устройств и 3 платежей/
  рецептов/книги. Проверены приоритеты/границы/кэш, точная единица,
  отказ native endpoints, owned one-shot confirmations, конкуренция,
  повторный вход, сохранения, шесть сторон и миграция старых блоков.
- **10/10** новых клиентских сцен, `air-025-client3`, JVM **exit 0**.
  Реальные C2S-запросы книги расходуют по 16 raw Observation Alchemy/Artifice
  и дают 10 XP: 5 за начало и 5 за завершение. Настоящий магический верстак
  дважды расходует 100 vis, Aer ×1, Aqua ×1 и исходные ингредиенты.
  Оба оплаченных результата устанавливаются через native BlockItem C2S
  по верхней стороне физического устройства. Обычные серверные тики
  переносят **24 Ignis**: перегонный куб → промежуточная банка → конечная
  банка с меткой. Итог source0/mid0/collector24; дублирующих предметов нет.
  Готовые приборы не подставлялись, прямые tick/transfer вызовы не использованы.
  Есть два кадра активного переноса в настоящем мире. Все шесть направлений
  обоих моделей показаны в отдельной справочной галерее; это рендер-превью,
  а не клиентская установка двенадцати дополнительных приборов.
- **15/15** сцен регрессии LateGolem, `air-025-regression2`, JVM **exit 0**.
  Подтверждены настоящие сканы/четыре исходных исследования, Hungry Chest,
  два Levitator, Brain Jar через наполнение за **75 эссенции** с остатком
  ведра, захват **20 XP** и случайная выдача **14 XP** (в банке остаётся 6).
  Пресс оплачивает компоненты и **17 Machina**, выпускает единственный Flyer
  `props=12884901888`. Его AI поднимается к реальной Collect-печати,
  забирает три алмаза и доставляет вниз в ранее изготовленный Hungry Chest.
  Нулевая начальная скорость подготовленного ItemEntity устраняет случайный
  бросок за край одноблочной площадки; гравитация и настоящий AI сохраняются.
- Оба клиента использовали собственные скрытые Win32 desktops, по **16**
  принадлежащих JVM окон. Input desktop **Default → Default** в обоих
  отчётах; переключения, активации окна или глобального ввода не было.
- Root просмотрел все **25 PNG** на пяти контактных листах и восемь
  выбранных кадров в полном размере: исследование/верстак транспорта,
  два активных native-world переноса, шесть ориентаций, Brain Jar,
  Flyer с грузом и доставленный сундук. Отображение моделей/текстур/меню
  подтверждено этими кадрами; исходный custom stream ribbon не заявляется.
  Замечания и SHA-256: `validation/visual-review-0.25.json`;
  точные текущие bytes: `validation/screenshots-0.25/`.
- Финальная сборка с reobfuscation прошла за **29s**:
  `validation/build-0.25-final.log`. Ресурсы прошли:
  `validation/resources-0.25-final.log`, **2832 JSON / 3637 файлов**.
  Отчёт упаковки: [artifact-report-0.25.json](validation/artifact-report-0.25.json).
  Проверяется совпадение всех ресурсов JAR, исходных **660 PNG / 7 JPG /
  45 OGG**, CRC и полных bytes JAR/mods/source/screenshots архивов.

Ранние THAUMATORIUM/INFUSION, сырые наблюдения, обычные ингредиенты/кристаллы,
аура250/full moon/flux1000 в соседних чанках, арена и пустые физические
источники/банки — явные подготовленные условия нового клиента. Raw24Ignis
добавлены в перегонный куб только после установки оплаченного Filling;
метка Ignis дана потребителю после установки оплаченного Emptying.
В регрессии предшествующие исследования, знания/ингредиенты, аура,
пьедесталы/источники и загруженная область навигации подготовлены явно.
Это проверка реальных платежей и механик в этих условиях, без утверждения
о полном ручном survival-прохождении или независимом multiplayer.

Наполнение остаётся **16/56** research/craft, **14** обычных ingredient paths,
**40** поздних gates. Зеркала, MATSTUDVOID/BASEELDRITCH, естественные
источники Firebat/Pech/Primordial Pearl и остальная мировая экология ещё нужны.
Исходная поздняя цепь разобрана в [Void/Eldritch roadmap](docs/tc6/VOID-ELDRITCH-BETA26-ROADMAP.md).
Все прежние отчёты и проверенные результаты ниже сохранены как история.

## Историческая проверка сборки 0.24.0-dev

Дата: 2026-10-07. Эталон — **Thaumcraft 6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.

Работают Hungry Chest, Levitator, летающие големы FLYER и Brain in a Jar:
исходные исследования, сканы, платежи, рецепты, размещение, серверная логика
и модели. Канонический набор — **78 из 148**: прежние 74 плюс HUNGRYCHEST,
LEVITATOR, GOLEMFLYER и JARBRAIN. Сохраняются пять материалов, все 16 печатей,
владение/дом/следование, логистика и остальные ранее реализованные механики.
MATSTUDVOID, BASEELDRITCH, ESSENTIATRANSPORT и оставшиеся поздние мировые
ветки сохраняют ограничения; полный Thaumcraft 6 ещё не перенесён.

Контракты: [применение](GOLEMANCY.md),
[четыре исследования](docs/tc6/LATE-GOLEM-PROGRESSION-BETA26-AUDIT.md),
[Hungry Chest](docs/tc6/HUNGRY-CHEST-BETA26-AUDIT.md),
[Levitator](docs/tc6/LEVITATOR-BETA26-AUDIT.md),
[FLYER](docs/tc6/GOLEM-FLYER-BETA26-AUDIT.md),
[Brain in a Jar](docs/tc6/BRAIN-JAR-BETA26-AUDIT.md).

Финальные проверки прошли:

- **746/746** обязательных серверных GameTests, `BUILD SUCCESSFUL`:
  `validation/late-golem-024-gametest4.log`, свежий собственный QA-мир.
  Сохранены прежние 700; добавлены 9 проверок Hungry Chest, 14 Levitator,
  11 Brain Jar, 6 настоящего полёта и 6 прогрессии/платежей/сканов.
  Проверены физические инвентари, остатки и точная оплата, повторные запросы,
  направления/диапазоны/redstone/аура, живые перемещения, NBT/перезагрузка,
  comparator/зачарование/XP и совместимость прежних механизмов.
- **15/15** сцен LateGolem, `late-golem-024-client6`, JVM **exit 0**.
  Настоящие C2S-сканы мозга, Volatus-кристалла и отслеживаемой летучей мыши
  дают нужные факты. Четыре исследования оплачиваются через книгу:
  32 raw Observation Golemancy + 16 Artifice + 64 raw Theory Golemancy,
  сундук/воронка/мозг и исходные 2 permanent + 1 normal warp.
  Hungry Chest изготовлен за 15 vis + Terra/Aqua, размещён и захватывает
  три обычных падающих яблока. Два левитатора изготовлены за 35 vis + Aer
  каждый: один размещён и поднимает перо штатными тиками, второй входит
  в настоящий платёж компонентов FLYER. Размещённый прибор забирает 1 vis,
  пополняет буфер 1200 и расходует его на подъём; в конце aura=99.
  Brain Jar создан настоящим наполнением: Cognitio/Sensus/Exanimis по 25,
  мозг, два глаза и ведро воды; пустое ведро сохранено.
  Размещённый выход собирает 20 XP и после обычного клика выдаёт **4 XP**;
  проверены точное сохранение суммы world+player и задержка захвата 40 тиков.
  Пресс оплачивает компоненты и 17 Machina, выдаёт единственный
  `props=12884901888`; этот результат размещается как действующий Flyer.
  Настоящий AI поднимается к физической печати, забирает три алмаза и
  доставляет их вниз в ранее изготовленный Hungry Chest, очищая руку.
  Готовые сундук/левитатор/банка/голем в сценарий не подставлялись.
- **18/18** сцен регрессии Golemancy, `golem-024-regression2`, JVM **exit 0**.
  Повторно проверены реальная оплата Mind 32 + 32, пятиблочный Salis 2 → 1,
  компоненты/12 Machina/выход/размещение, настройки физических печатей,
  Collect/Store трёх алмазов, Provide двух яблок (источник 6 → 4),
  колокольчик/следование/подбор с сохранением исходных props.
- Оба клиента использовали собственные скрытые Win32 desktops: по **16**
  принадлежащих JVM окон, input desktop **Default → Default**.
  Переключения/активации окна и глобального ввода не было.
- Все **33 PNG** фактически просмотрены на шести контактных листах.
  Фильтр печати, логистика, галерея големов, меню Hungry Chest, левитатор,
  наполненная Brain Jar, размещённый Flyer и доставленный груз также
  просмотрены в полном размере. Оригинальные текстуры/модели и меню
  отображаются; визуальные утверждения ограничены этими кадрами.
  Замечания и SHA-256: `validation/visual-review-0.24.json`;
  снимки: `validation/screenshots-0.24/`, архив в `outputs`.
- Финальная сборка с reobfuscation: `validation/build-0.24-final.log`,
  `BUILD SUCCESSFUL`. Ресурсы: `validation/resources-0.24-final.log`,
  **2830 JSON / 3635 ресурсных файлов**, ссылки моделей/текстур и переводы.
  Проверены CRC и каждый ресурс JAR, байтовое совпадение **660 PNG,
  7 JPG и 45 OGG** с закреплённым BETA26 JAR, два извлечённых значка эффектов,
  состав/байты каждого файла в ZIP исходников, модов и снимков.

Границы проверки: предшествующие исследования, сырые теории/наблюдения,
компоненты/кристаллы, арена и загруженная область навигации, aura, алтарь,
симметричные свечи/источники эссенции и предметы печатей подготовлены явно.
Для измерения расхода левитатора fixture задаёт фазу луны 4 и равные значения
ауры соседних чанков: природная регенерация/диффузия не скрывают платёж.
В регрессии Golemancy поздние Direct/Logistics и advanced/gallery конструкции
также подготовлены; показ Void в галерее не подтверждает его изготовление.
Эти сценарии не являются полным ручным survival-прохождением или проверкой
двух независимых клиентов. Все сочетания невидимости/X-ray не проверены.
Современная навигация, native частицы, render buffers, dictionary-to-tags,
ограничения загрузки чанков/NBT и защиты транзакций отмечены как адаптации.
Сохранены исходные особенности: последний подъём при частичном буфере,
пополнение под redstone, 40 тиков Brain Jar, props=0 анимация пресса и
зависание заблокированного выхода при нулевой оставшейся стоимости.

Наполнение: **16/56** исследовательских/крафтовых путей, **14** с установленными
обычными источниками ингредиентов, **40** за поздними research gates.
JarBrain — единственный новый доступный рецепт этого среза. Естественные
Firebat/Pech/Primordial Pearl и полный поздний мир остаются в работе.
Все прежние 21 узел фокусов и производственные механики входят в серверную
регрессию; отдельные прежние клиентские прогоны ниже являются историческими.

Отчёт и хеши: [artifact-report-0.24.json](validation/artifact-report-0.24.json).
Локальные сырые логи/миры/review PNG исключены из Git. Архив исходников
содержит код, ресурсы, документацию и отчёты без build/run/рабочих архивов
оригинала. Исторические результаты 0.23 и более ранних версий сохранены ниже.


## Историческая проверка 0.23

Дата: 2026-10-05. Эталон — **Thaumcraft 6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.

Реализованы размещение изготовленных големов, владелец/дом/следование,
подбор с сохранением свойств и опыта, рабочие traits/бой/ранги и все **16**
исходных печатей с настройками, серверными заданиями и логистикой.
Изготовление доступно для дерева, железа, глины, латуни и таумия с
исследованными деталями. Поддержаны **74 из 148** канонических записей:
51 предыдущая и 23 новых. Оплаченный Mind сохраняет исходный переход
32 + 32 Theory и рекурсию пустых CONTROLSEALS → SEALCOLLECT → SEALSTORE.
Пустотный материал, Flyer, левитация, Brain in a Jar и поздние мировые
механики остаются неперенесёнными.

Контракты: [управление и доступность](GOLEMANCY.md),
[сущность](docs/tc6/GOLEM-ENTITY-BETA26-AUDIT.md),
[система печатей](docs/tc6/SEAL-SYSTEM-BETA26-AUDIT.md),
[действия/рецепты](docs/tc6/SEAL-BEHAVIORS-BETA26-AUDIT.md).

Финальные проверки прошли:

- **700/700** обязательных серверных GameTests, `BUILD SUCCESSFUL`:
  `validation/golem-023-gametest2.log`, свежий собственный QA-мир.
  Сохранён предыдущий набор 645 проверок; добавлены 13 проверок сущности,
  11 системы печатей, 15 действий и 16 прогрессии/сканов.
  Проверены настоящая навигация вокруг препятствия и возврат домой,
  владельцы/подбор, лечение, ранги, сохранение рук/props/XP, server menu
  revisions, физические Forge inventory endpoints, claims/приоритеты,
  отсутствие повторной оплаты/потери предметов и пересадка с оплатой семян.
- **18/18** сцен Golemancy, `golem-023-client4`, JVM **exit 0**.
  Через настоящий C2S оплачены Mind 32 + 32 и компоненты пресса;
  Salis расходуется 2 → 1, 12 пятых тиков потребляют Machina.
  Единственный выход с `props=4294967296` извлечён из физического слота
  и размещён как действующий голем владельца. Предмет голема не подставлялся.
  Печати установлены с расходом предметов; их область, ghost-фильтр,
  приоритет и цвет изменены через рабочие меню. Реальный AI собрал три
  выброшенных алмаза и положил их в сундук. Provide доставил игроку
  два яблока по заказу: источник 6 → 4, игрок 0 → 2; конкурирующий Store
  не перехватил адресованный груз. Колокольчик переключил следование;
  подбор вернул ровно один предмет исходных props и удалил сущность/задачи.
- Клиент работал на собственном скрытом Win32 desktop: **16 окон**,
  input desktop **Default → Default**. Переключения, активации окна
  или глобального клавиатурного/мышиного ввода не было.
- Все **18 PNG** просмотрены на трёх контактных листах. Размещённый голем,
  фильтр печати, логистический экран и галерея дополнительно просмотрены
  в полном размере. Исправлена обрезка верхнего ряда моделей в галерее.
  SHA-256 и отдельные замечания: `validation/visual-review-0.23.json`;
  текущие снимки: `validation/screenshots-0.23/`, архив в `outputs`.
- Финальная сборка с reobfuscation: `validation/build-0.23-final.log`,
  `BUILD SUCCESSFUL` за **18 секунд**.
- Валидатор ресурсов: `validation/resources-0.23-final.log`,
  **2828 JSON / 3625 ресурсных файлов**, ссылки моделей/текстур и переводы.
  Упаковщик проверяет CRC, каждый ресурс JAR и байтовое совпадение
  оригинальных PNG/JPG/OGG с закреплённым BETA26 JAR.

Границы проверки: предшествующие исследования, теории, компоненты,
источник Machina, предметы печатей/фильтров и содержимое Provide подготовлены
явно. Поздние GOLEMDIRECT/GOLEMLOGISTICS и advanced/gallery модели тоже
являются fixtures. Галерея показывает шесть материалов, включая закрытый
Void; это проверка отображения, а не естественного изготовления Void.
Следование в клиенте проверяет переключение режима, движение вокруг стены
проверяет сервер. Невидимость/отображение сквозь стены реализованы по
оригинальному контракту, но эти 18 кадров не подтверждают все их сочетания.
Плоские мировые значки печатей, native частицы и современные RenderType/
навигация/Forge protection callbacks описаны как адаптации. Эти результаты
не подтверждают полное ручное прохождение или два независимых клиента.

Наполнение: **15/56** путей исследования/крафта, **13** с установленными
обычными survival-источниками, **41** за поздними research gates.
Добавлены MindBiothaumic и Harvest/Butcher/Break seals; Zombie Brain
получил исходный естественный дроп. Firebat/Pech/Primordial Pearl world
sources и экология поздних существ ещё нужны. Все прежние 21 типа узлов
фокусов и производственные механики входят в серверную регрессию;
их отдельные клиентские прогоны ниже сохраняются как исторические.

Отчёт и хеши: [artifact-report-0.23.json](validation/artifact-report-0.23.json).
Локальные сырые логи, миры и review PNG исключены из Git; хеши и границы
включены в отчёт. Архив исходников содержит код, ресурсы, документацию
и отчёты, без build/run/рабочих архивов оригинала.

## Историческая проверка 0.22


Дата: 2026-10-04. Эталон — **Thaumcraft 6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
Полный канонический набор — **51 ключ**: прежние 50 плюс MINDCLOCKWORK.
Стадия 2 оплачивает 32 raw Theory Artifice + 32 raw Theory Golemancy и
завершается на стадии 4 после пустого заключения. Прежний предел @2 снят.
CONTROLSEALS и поздние ветки остаются закрыты.

Рабочий пресс формируется из пяти настоящих блоков через Salis Mundus,
берёт материалы и Machina, сохраняет оплаченный заказ и выдаёт предмет
голема. Доступны четыре деревянные сборки за **8/12/12/16 Machina**.
Размещение предмета как сущности, AI, печати, колокольчик и поздние детали
ещё не реализованы. Все 148 архивных записей не означают готовность всего мода.

Контракты: [пресс](docs/tc6/GOLEM-PRESS-BETA26-AUDIT.md),
[данные конструкции](docs/tc6/GOLEM-DESIGN-BETA26-AUDIT.md),
[прогрессия](docs/tc6/GOLEMANCY-COMPONENT-PROGRESSION-BETA26-AUDIT.md).

Финальные проверки прошли:

- **645/645** обязательных серверных GameTests, `BUILD SUCCESSFUL`:
  `validation/golem-022-gametest2.log`, свежий собственный QA-мир.
  Добавлены 6 проверок данных конструкции, 15 пресса и 4 формирования.
  Перебраны все 2400 сочетаний данных; изготовление разрешено только для
  четырёх доступных деревянных сборок с завершёнными исследованиями.
- **6/6** сцен GolemPress: `golem-022-client4`, JVM exit 0.
  Проверены реальная оплата исследования через C2S, ритуал Salis,
  дебет физических компонентов, 12 пятых тиков с Machina, один предмет
  с `props=4294967296`, извлечение из настоящего выходного слота и схема книги.
- **25/25** сцен EssentiaProduction: `press-022-regression1`, JVM exit 0.
  Повторно проверены производство/трубы/центрифуга/Тауматорий, оплаченные
  mind_clockwork → Brain Box, установка полученного Brain Box и capacity3,
  прежние рецепты и схемы. 1084 baked states для 17 прежних рабочих блоков
  проверены отдельно от живого рендера нового пресса.
- Обе JVM работали на собственных скрытых Win32 desktops: **16 окон** каждая,
  input desktop **Default → Default**. Переключения или глобального ввода не было.
- Финальная сборка с reobfuscation: `validation/build-0.22-final.log`,
  `BUILD SUCCESSFUL` за 18 секунд.
- Ресурсы: `validation/resources-0.22-final.log`, **2810 JSON / 3602 файла**.
  Упаковка проверяет байты каждого ресурса в JAR, CRC архивов и совпадение
  с оригиналом для **657 PNG / 7 JPG / 35 OGG**, отдельно два производных значка.
- Все **31 PNG** просмотрены в шести контактных листах. Модель/меню/схема
  пресса и рецепт механического разума дополнительно просмотрены в полном
  размере. SHA-256, профили и границы —
  [artifact-report-0.22.json](validation/artifact-report-0.22.json).

Серверные проверки покрывают строгие исследования, неверные ID/rank,
стоимость/traits, атомарные отказы и rollback, busy/replay/reentry,
запросы вне физического меню/дальности, соседние capability-инвентари,
пятый тик и тип Machina, save/load и одноразовую выдачу/разборку.
Исходная схема сохраняет три wildcard-клетки, четыре поворота и обычный
поршень вверх. Проверены отмена Forge placement, отказ без исследования,
водяной котёл/повреждённая наковальня и восстановление обычных исходных блоков.
Показатели builder — hardness2/effective resistance12; placeholders — 2.5/2.5.

Исходные особенности сохранены: props0 производится без клиентской анимации;
после последней оплаченной единицы заблокированный выход оставляет
cost0/golem>=0 и не повторяет выдачу после освобождения/загрузки.
Излишний дебет при частичных запасах в соседнем и личном инвентаре исправлен
резервированием с точным rollback. Цена каждого компонента по-прежнему
целиком должна находиться у игрока либо в соседних инвентарях.

Первый серверный прогон обнаружил неправильный тестовый блокер:
UP второго пресса не подключаем. Стенд заменён настоящим верхним входом
Тауматория над физическим тиглем; предпосылки подключаемости/output проверяются
явно, исходные точные assertions расхода сохранены. Runtime draw не менялся.
Первый клиентский прогон оплатил и изготовил голема, затем нашёл сбой
заголовка прямой схемы: multiblock не является обычным рецептом.
Заголовок теперь разрешает MultiblockCatalog и безопасно обрабатывает пустой
список рецептов. Просмотр снимков выявил отменяемый оригиналом V-flip и
неверное предположение о центрированном quad лавы. Отдельная модель пресса
учитывает исходную текстурную матрицу, а лава находится внутри чаши;
финальный client4 подтвердил исправления. Старые модели существ используют
прежнее преобразование. Full-bright лава и native vent particles — адаптации.

Предыдущие исследования, теории, стартовые компоненты и источник Machina
в клиентском сценарии — явные фикстуры. Выходной голем не выдаётся стенду:
его производство и извлечение оплачены настоящим сетевым путём.
Книжная сцена использует прямой тестовый переход к схеме; обычную видимость
заключительной главы проверяют серверные тесты прогрессии.
Регрессионный Mind@2 имеет неоплаченные теории, а не глобальное ограничение
стадии. Эти проверки не доказывают полное ручное survival-прохождение
или independent-client multiplayer. ItemGolemPlacer/AI/печати — следующая работа.

Созданы `thaumcraft-1.20.1-0.22.0-dev.jar`, `-mods.zip` с TerraBlender и
`-source.zip` в родительском outputs. Проверки CRC, точных байтов всех
3602 ресурсов JAR, исходного media и состава обоих ZIP прошли.
SHA-256 JAR: `7d7be31485779bbc7a69dcb56c5d144c788590599328a9cf41aeb5b15a2aed71`.
Исходный ZIP содержит 4185 файлов проекта; raw QA-миры, журналы, снимки,
bytecode scratch и локальные build/dependency caches в него не входят.

---

# Историческая проверка сборки 0.21.0-dev

Дата: 2026-10-04. Эталон — **Thaumcraft 6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
Теперь полностью поддерживаются **50 исходных ключей исследования**:
к предыдущим 46 добавлены HEDGEALCHEMY, UNLOCKGOLEMANCY, BASEGOLEMANCY
и MATSTUDWOOD. **MINDCLOCKWORK поддержан отдельно до входа в стадию 2**;
завершение этого исследования, пресс, AI големов и печати пока недоступны.
Все 148 архивных записей Таумономикона не означают готовность всех механик.

Контракты: [прогрессия големостроения](docs/tc6/GOLEMANCY-COMPONENT-PROGRESSION-BETA26-AUDIT.md),
[компонентные рецепты](docs/tc6/CLOCKWORK-COMPONENTS-BETA26-AUDIT.md),
[Hedge Alchemy и свечи](docs/tc6/HEDGE-ALCHEMY-BETA26-AUDIT.md),
[общая прогрессия](RESEARCH-PROGRESSION.md).

Финальные проверки прошли:

- **620/620** обязательных серверных GameTests, `BUILD SUCCESSFUL`:
  `validation/golemancy-021-gametest2.log`, свежий собственный QA-мир.
  Новые 17 сценариев: девять для Hedge Alchemy/свечей, три для компонентных
  рецептов и пять для сканов/оплаты/стадий исследования/видимости книги.
- **25/25** клиентских сцен EssentiaProduction,
  `clockwork-021-client3`, JVM exit 0. Скрытый Win32 desktop;
  input desktop **Default → Default**, 16 принадлежащих JVM окон.
  Проверены 1084 baked states для 17 рабочих машин и реальные BER.
- Финальная сборка и reobfuscation: `validation/build-0.21-final.log`,
  `BUILD SUCCESSFUL` за 18 секунд.
- Ресурсы: `validation/resources-0.21-final.log`, **2810 JSON / 3569 файлов**.
- Все **25 PNG** просмотрены в пяти контактных листах. Рецепты механического
  разума, дублирования светокамня и создания лавы, а также передняя сторона
  живого Тауматория дополнительно просмотрены в полном размере.
  SHA-256 и результаты — [artifact-report-0.21.json](validation/artifact-report-0.21.json).

Проверен реальный клиентский путь: меню арканного верстака через C2S оплачивает
механический разум, полученный предмет переносится в рецепт Brain Box,
результат оплачивается и устанавливается обычным использованием предмета.
Готовые mind_clockwork и Brain Box клиентскому сценарию не выдаются.
Предыдущие исследования, начальные компоненты и запас ауры — явные фикстуры.
Серверные тесты отдельно проверяют цепочку простого механизма → разума →
Brain Box за 10 + 25 + 50 vis, расход кристаллов и атомарные отказы.

Проверены настоящие сканы железного голема, аспектов бумаги/яблока, оригинальный
набор `f_golem`, оплаты открытия големостроения и стадии MINDCLOCKWORK@2,
исходные родители и sibling-разблокировки/XP. Стадия 2 видна в обычной книге,
рецепт доступен, дальнейшая оплата заблокирована до реализации следующей части.
HEDGEALCHEMY проверено со всеми исходными оплатами и craft proof.
Старый сохранённый PORT_TALLOW остаётся только совместимостью рецепта и
не заменяет завершённое HEDGEALCHEMY для родителей исследования.

Все десять преобразований Hedge Alchemy оплачены в реальном котле. Проверены
динамические исходные формулы аспектов, выбор рецепта, остатки воды и ведро.
Обычный стол крафта проверен для белых свечей и всех 256 сочетаний перекрашивания.
Все 16 цветов имеют свет 14, исходную форму/прочность, реальную опору и
единственный выпад при потере опоры; сохранён учёт свечей в стабильности наполнения.

В промежуточном серверном запуске новые 17 тестов прошли, но два старых теста
наполнения использовали висящие в воздухе свечи. Фикстуры получили физическую
опору и проверки всех 32 свечей/исходного вклада стабильности 1.6.
Проверки NBT, расходов, времени и исходов сохранены; финальный полный запуск
620 тестов прошёл. Механика наполнения ради этих фикстур не менялась.

Первый клиентский запуск подтвердил оплаченный крафт и установку Brain Box,
но обнаружил фильтр книги, скрывавший частично реализованный MINDCLOCKWORK.
Фильтр исправлен на `supportsProgression`; добавлена серверная проверка обычной
видимости/текущей главы. Второй запуск остановился на начальном экране загрузки:
завершён только принадлежащий этому QA процесс. В сценарий добавлено ожидание
выхода из ReceivingLevelScreen до открытия первого меню. Финальный третий
запуск прошёл все 25 сцен и завершился сам с кодом 0.

Эта проверка подтверждает работающий естественный рецептный путь к Brain Box,
но не является полным ручным прохождением выживания или проверкой двух
независимых клиентов. Пресс големов, их AI и печати остаются следующим этапом.
Наполнение сохраняет 11/56 доступных путей, девять установленных обычных
источников выживания и 45 поздних ограничений. Естественные поздние источники,
мировой Flux Rift и связанные поздние материалы/существа пока не завершены.

## Историческая проверка сборки 0.20.0-dev

Дата: 2026-10-04. Эталон — **Thaumcraft 6.1.BETA26**;
Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
Рабочий набор расширен с 43 до **46 исходных ключей**:
ESSENTIASMELTERTHAUMIUM, THAUMATORIUM и INFUSIONSTABLE.
Архив всех 148 записей не означает готовность остальных игровых механик.
Контракты: [ингредиенты](docs/tc6/EARLY-DEVICE-INGREDIENTS-BETA26-AUDIT.md),
[Тауматорий](docs/tc6/THAUMATORIUM-BETA26-AUDIT.md),
[прогрессия](RESEARCH-PROGRESSION.md), [наполнение](INFUSION.md).

Финальные проверки прошли:

- **603/603** обязательных серверных GameTests, `BUILD SUCCESSFUL`:
  `validation/automation-020-gametest5.log`, свежий собственный QA-мир.
- **22/22** клиентских сцен EssentiaProduction,
  `thaumatorium-020-client5`, JVM exit 0.
  Скрытый Win32 desktop; input desktop **Default → Default**, 16 принадлежащих
  JVM окон. Проверены 1084 baked states для 17 рабочих блоков и реальные BER.
- Финальная сборка/reobfuscation: `validation/build-0.20-final.log`.
- Ресурсы: `validation/resources-0.20-final.log`, **2783 JSON / 3542 файла**.
- Все **22 PNG** просмотрены в четырёх контактных листах;
  прогресс Thaumatorium, его передняя иконка, конструкция и рецепт стабилизатора
  дополнительно проверены в полном размере. SHA-256 и результаты —
  [artifact-report-0.20.json](validation/artifact-report-0.20.json).

Новые 26 серверных тестов: пять проверок компонентных рецептов/добычи,
15 сценариев Тауматория, пять проверок стадий/платежей новой прогрессии
и реальный сбой матрицы, открывающий `!INSTABILITY` ближайшему свидетелю.
Проверены атомарные отказы при нехватке знаний, vis, кристаллов или предметов,
повторные запросы, сохранение NBT, главный инвентарь/полная фиала,
оплаченные крафты и естественный редкоземельный бонус к настоящему ore loot.
Исходный несуществующий craft ID `thaumcraft:metal;1;2` игнорируется сервером
и книгой, как в BETA26; заменяющий факт изготовления не придуман.

Клиент создал Тауматорий настоящим Salis Mundus use packet: две конструкции
заменены, тигель сохранён, пыль **2 → 1**. Настоящие menu/C2S перенесли
два угля и выбрали Alumentum. Три типизированных источника отдали
**50 → 25** единиц; redstone остановил смесь на **24/25**, затем
машина завершила оплату, очистила смесь, потратила уголь **2 → 1**
и положила ровно один Alumentum в передний сундук. Проверены S2C, запрет
клиентской мутации и отображение выбранного результата. Facing Brain Box
даёт три доступные программы. Книга показывает плавильню, стабилизатор,
инкрустацию и отдельный неизменяемый preview конструкции; просмотр не меняет
серверные знания.

Первый скрытый запуск не состоялся из-за устаревшего имени vanilla ChestScreen
в новом QA-коде; используется актуальный ContainerScreen. Второй клиент
прошёл реальную машину и остановился на книге: явному тестовому профилю
не хватало UNLOCKINFUSION. Исправлены prerequisites профиля, рабочие gates
не ослаблены. Третий прогон прошёл 22/22; четвёртый повторил их после
исправления проверочного ракурса, чтобы увидеть переднюю иконку результата.
Это изменения стенда, а не обход оплаты или поведения машины.
Финальный пятый прогон снова прошёл 22/22 после исправления block API и дропа Brain Box.

Финальная сверка block API обнаружила два отличия от BETA26. Старые
`setResistance(20/10)` преобразуются в эффективные **12/6**, а прямое
копирование современного железного блока ошибочно требовало правильный
инструмент для получения лута. Оба отличия исправлены; проверяются hardness,
реальный `ExplosionDamageCalculator` и survival `destroyBlock` рукой/палкой.
Эта проверка дополнительно обнаружила отсутствующий штатный дроп Brain Box;
он теперь возвращает собственный предмет. Оба сегмента машины возвращают
алхимическую конструкцию, содержимое выпадает однократно.

Промежуточный `automation-020-gametest3` выявил зависимость прежнего теста
Rift от расположения чанков: центр и выход находились вне `empty` 3×3×3.
Для этого теста используется существующая область `essentia_network` 9×5×9.
Сохранены срок 48 тиков и проверки всех 18 каменных блоков/воздушного выхода;
добавлены проверки активных BE, исходных состояний и countdown на шестом тике.
Рабочий алгоритм Rift не изменён. Следующий серверный прогон выявил
отсутствующий дроп Brain Box; новая проверка сохранена без ослабления.

Стадии родителей, знания клиента, начальные предметы, заполнение трёх
источников и установленный Brain Box являются явными QA fixtures.
Серверные тесты отдельно проверяют реальные платежи целевых стадий/рецептов.
Не установлены полное ручное survival-прохождение и multiplayer с независимым
клиентом. Основная машина работает без Brain Box; естественный clockwork mind
и MINDCLOCKWORK/Golemancy ещё нужны для необязательного улучшения.
Наполнение сохраняет **11/56** research/craft paths, девять с установленными
обычными survival-источниками и 45 поздних gates. Естественные источники
Firebat/Pech/Primordial Pearl, другие устройства и полные warp/taint/Flux Rift
остаются отдельной работой. Звук QA приглушён; исходные файлы и вызовы
проверены кодом, качество звучания этим запуском не оценено.

0.20 повторно проверяет серверные тесты ауромантии; её последний полный
клиентский визуальный прогон относится к 0.19 и приведён ниже отдельно.
Исторические снимки не переименованы в новую проверку.

## Историческая проверка 0.19.0-dev

Дата: 2026-10-04. Контракт — **Thaumcraft 6.1.BETA26**,
Forge 47.4.10 / Java 17. Проверены все **21/21 исходных типа узлов**,
ветвление графа, Advanced/Greater с пределами сложности **15/25/50**
и первый механизм следующего этапа — **центрифуга эссенции**.
Подробные контракты: [узлы](docs/tc6/FOCUS-COMPLETE-BETA26-AUDIT.md),
[отложенные носители](docs/tc6/FOCUS-MEDIA-INTEGRATION.md),
[эффекты](docs/tc6/FOCUS-REMAINING-EFFECTS-INTEGRATION.md),
[центрифуга](docs/tc6/CENTRIFUGE-INTEGRATION-NOTE.md).

Итог: **577/577 required серверных GameTests**, финальный `build/reobfJar`
и валидатор ресурсов прошли. Серверный журнал —
`validation/focus-019-gametest8.log`; запуск использовал свежий собственный
QA-мир. Предыдущие QA-миры архивированы под проверенными соседними путями.
Сборка — `validation/build-0.19-final.log`, ресурсы —
`validation/resources-0.19-final.log`.

Новые проверки охватывают ветви/лимиты/знания/расчёт цены, однократную
оплату и силу отложенных продолжений, NBT/save/load, порядковые номера
целей, Cloud/Mine/Spell Bat, Plan/Scatter/Split, Curse/Exchange/Rift,
реальные сканы/платежи стадий и оба улучшения фокусов на рабочей матрице.
Для центрифуги проверены вход снизу/выход сверху, suction/redstone,
countdown, выбор одного непосредственного компонента, сохранение,
транспорт, уничтожение, миграция старых блоков и исследование/рецепт.
Исправлена исходная проверка магического верстака `isResearchKnown`:
bare key доступен после начала исследования. Строгие gates узлов фокуса
сохраняются; совместимые PORT aliases не заменяют канонические платежи.

Проверка отменённого Forge impact у мины обнаружила ошибку fixture:
ручной `Entity.tick()` не увеличивает возраст, поэтому Age=4 обходил
пятитиковую детонацию. В финальном тесте Age=5, armed/counter проверены
перед реальным столкновением; положительный контроль без удаления
детонирует в той же геометрии. После callback удалённая мина не выполняет
продолжение. Все эти проверки прошли в финальном серверном прогоне.

Финальные клиенты: **Auromancy `full-019-client8`: 33/33 сцен + отдельный
transient Bolt frame**; **EssentiaProduction `centrifuge-019-client2`:
12/12 сцен**. Всего **46 PNG**, все просмотрены в шести контактных листах;
редактор Split, мина, мышь, Rift, модель/рецепт центрифуги дополнительно
просмотрены в полном размере. Журналы и SHA256 сцен записаны в
[artifact-report-0.19.json](validation/artifact-report-0.19.json).

Клиентские сценарии используют настоящие menu/C2S, физическую установку
фокусов и use-item packets; проверены реальное изготовление ветвящегося
Advanced-фокуса, оплата, применение оставшихся узлов и переключение Plan.
Исследования, начальная аура, готовые поздние фокусы, здоровье образцов,
лунная фаза и отдельные creative-invulnerability — явные QA fixtures.
Сервер отдельно проверяет сканы, стадии и физические платежи/улучшения.
Время ожидания телепорта подтверждается положением клиента до открытия
меню; временные носители прежнего QA-заклинания убираются перед следующей
сценой. Опора и камера мины/мыши подготовлены только внутри QA-профиля.
Начальная аура перед выдачей нормализована без изменения рабочего алгоритма.

Production-клиент проверяет настоящий поток топлива и камня через
плавильню/перегонный куб/трубы/банку. Три реальные BE центрифуги показывают
redstone-паузу, обработку и готовый компонент; клиент ждёт синхронизацию
входа/выхода и вращение ротора. Книга показывает исходные ингредиенты,
100 vis и Ordo/Perditio. Аудио QA приглушено: проверены оригинальные файлы,
регистрация и вызовы звуков, качество звучания этим прогоном не оценено.
Cloud/impact particles и shader прохода Rift адаптированы к 1.20.1;
полная пиксельная идентичность FX не установлена. У маленькой серой мины
проверены armed-state, render binding/ресурс и pulse; кадр не является
независимой детальной проверкой её mesh.

Обе JVM завершились с кодом 0 на отдельных скрытых WinSta0 QA-desktop,
по 16 собственных окон; input desktop **Default → Default**. Переключение
рабочего стола, активация окна и глобальный ввод не применялись.
Обычные миры пользователя не использовались для этих проверок.

Валидатор проверил **2777 JSON**, ссылки моделей/текстур и обе локализации;
167 иконок книги, 192 размерные текстуры, 12 GUI и 10 IMG references.
Упаковка проверяет CRC и побайтовое совпадение **3535 ресурсов** с JAR,
**623 PNG / 7 JPG / 35 OGG** с исходным BETA26, два извлечённых значка
эффектов, текущие кадры/журналы и исходники в source ZIP.
SHA256 итогового JAR:
`68b59fd79c290c00effb9e5dda2f84e46454de6a3a89df6c7b591c7266b4642f`.

Граница результата: **43 канонических ключа** имеют игровую прогрессию,
архив книги не означает готовность всей механики мода. Из 56 infusion
recipes для 11 реализованы исследования и крафт; обычные survival-источники
установлены для 9 рецептов экипировки, 45 закрыты поздними исследованиями.
Advanced/Greater ещё требуют исходных мировых источников Firebat/Pech/
Primordial Pearl; рабочий эффект Rift не является мировым Flux Rift.
Ингредиентные пути Golemancy для центрифуги, Thaumatorium, остальные
механизмы и поздняя мировая прогрессия остаются отдельной работой.
Независимый multiplayer-клиент и полная ручная survival-кампания не проверены.

# Проверка сборки 0.18.0-dev

Дата: 2026-10-03. Луч, Flux, Heal и Break **TC6 6.1.BETA26**,
Forge47.4.10 / Java17. Контракты:
[луч](docs/tc6/FOCUS-BOLT-BETA26-AUDIT.md),
[Flux/Heal](docs/tc6/FOCUS-FLUX-HEAL-BETA26-AUDIT.md),
[добыча](docs/tc6/FOCUS-BREAK-BETA26-AUDIT.md),
[четыре исследования](docs/tc6/AUROMANCY-FOUR-PROGRESSION-BETA26-AUDIT.md).

Итог: **499/499 required серверных GameTests**, build/reobfJar и проверка
ресурсов прошли. К прежним441 добавлены58:6 Bolt,3 graph,
19 Flux/Heal,19 Break,7 progression и4 table manufacture/save/load.
Проверены настоящие health/armor/Forge callbacks и damage attribution,
ROOT self-heal, дальность/LOS луча, paid continuation без повторного списания,
процесс изготовления и paid NBT, strict gates/atomic XP+crystals,
реальные сканы аспектов и расход зачарованных предметов/книг исследований.
Break проверяет hardness progression, реальные crack packets, zero-based
ordinal delay, main-hand-at-completion/placeholder enchantments, loot/NBT/XP,
target-chunk vis, отмену break event, отсутствие износа, замену блока,
insufficient aura, detached/reentrant очередь и cleanup при выгрузке.

Первый полный прогон выявил два устаревших assertions о закрытых узлах
и две ошибки новых fixtures: unmodifiable player-list witness и позиции
разных aura chunks. Assertions обновлены по новой доступности; наблюдатель
и позиции исправлены без ослабления условий. Финальный серверный прогон —
`validation/focus-018-gametest2.log`, свежий собственный QA-мир.
Прежние QA-миры сохранены под проверенными соседними путями.

Финальный клиент **four-018-client3:21/21 сцен и отдельный transient Bolt
frame, всего22 PNG**, все просмотрены через контактный лист; новая дуга,
редактор Bolt Heal и трещины дополнительно просмотрены в полном размере.
Настоящие menu/C2S создают Bolt Heal за93vis/3XP/Potentia+Victus; после
physical selection/use-item packet корова в12 блоках получает1hp за
единственный расход1.8vis. S2C пакет реально получен, дуга отрисована и
снята до её исчезновения. В QA под дальней целью добавлена опора.
Flux/Break используют явно подаренные готовые фокусы, физическую установку
и реальные use packets:6damage/2.2vis и постепенная добыча камня с Silk,
stone loot и1.8+0.5vis. Реальное изготовление этих эффектов и save/reload
отдельно проверены серверными тестами. Исследования стола, начальная аура
и фиксированная лунная фаза остаются явно указанными fixtures; игровая
прогрессия отдельно проходит через scans/payments без выдачи новых стадий.
Прежние15 клиентских сцен тоже повторно прошли.

Client1 выявил коллизию автоматических ASM event invokers двух вложенных
`Registration` в одном пакете: новый слушатель переименован в уникальный
`BoltRegistration`, оба сетевых канала присутствуют в финальном handshake.
Client2 прошёл поведение, но журнал показал отсутствующую beam texture;
оригинальная `essentia.png` извлечена побайтово из BETA26. Client3 проверяет
её доступность заранее; ошибок texture load/renderer/инициализации нет.
Существующий алгоритм ауры и обычные миры для QA не изменялись.

JVM завершилась0 на скрытом WinSta0 desktop,16 собственных окон,
input desktop Default/Default. Не использовались переключение рабочего
стола, активация окна или глобальный ввод; ручные игровые миры не тронуты.

Ресурсы:2776JSON,3530 побайтовых проверок ресурсов JAR против исходников,
623PNG/7JPG/31OGG совпадают с закреплённым BETA26, два derived effect icons
совпадают по пикселям. CRC и byte parity JAR/mods ZIP/source ZIP проверены;
source ZIP содержит итоговые документы и artifact report.

JAR SHA-256:
`bb8ce2979863f18ed4af67be42e66d12f144071f80e1835b94027886b5695192`.
Проверяемые хеши, состав и пределы доказательства:
[artifact-report-0.18.json](validation/artifact-report-0.18.json).

**29** канонических записей и **11/21** типов узлов работают.
Остальные10, Advanced/Greater, ветвления и поздние устройства ещё закрыты;
infusion остаётся9/56 доступных путей и47 поздних gates. Impact particles
адаптированы к native API, Bolt join tessellation — к современным quads.
Полное ручное survival-прохождение и независимые multiplayer-клиенты
этими fixtures не подтверждаются.

## История: 0.17.0-dev

Дата: 2026-10-03. Снаряды и стихийные фокусы **TC6 6.1.BETA26**,
Forge47.4.10 / Java17. Контракты и явные адаптации — [AUROMANCY.md](AUROMANCY.md),
[снаряд](docs/tc6/FOCUS-PROJECTILE-BETA26-AUDIT.md),
[эффекты](docs/tc6/FOCUS-EFFECTS-BETA26-AUDIT.md) и
[исследования](docs/tc6/AUROMANCY-ELEMENTAL-PROGRESSION-BETA26-AUDIT.md).

Сервер: **441/441 обязательных GameTests**, включая прежние399.
42 новых:5 графа,3 стола/миграции,18 эффектов,9 снаряда и7 прогрессии.
Проверены частичный FOCUSPROJECTILE@2 и полный option gate, исходные цены,
типизированный NBT и сохранение оплаченных планов, создание всех трёх стихий
через стол, настоящие item/ray сканы и read-only hover. Две новые цепи проходят
без прямой выдачи их стадий; их upstream BASEAUROMANCY и знания — явные fixtures.
Снаряд реально движется, продолжает только оплаченный suffix на END, сохраняет
отскоки, исходный radian cone, выбор пассивной недружественной цели, drag/gravity,
Forge veto, lifetime и вложенные доставки без повторной оплаты/cooldown.
Потеря/очистка владельца, включая save/load, не возобновляет заклинание.
Frost проверяет реальный Slowness, source-water sphere и scheduled melting60..120;
Air — damage attribution/knockback/resistance; Earth — мягкую твёрдость,
main-hand Silk Touch, block entity loot, XP, отсутствие износа и дополнительную
целевую0.1vis, включая исходную оплату при отменённом break event.

Клиент: **15/15 сцен**, все итоговые PNG client2 просмотрены.
Огонь проходит прежний реальный menu/C2S путь43vis/2XP/два кристалла →
физическая установка →4урона/0.8vis. Затем реальный стол создаёт Projectile
Frost за83vis/3XP/по Motus и Gelum; native spawn packet синхронизирует владельца,
option, effect и цвет. Движущийся снаряд наносит4урона и SlownessII за единственный
расход1.6vis. Клиентская и серверная замена пустого catalogue BE стола проверены
отдельным защитным fixture; прежний визуальный стол не объявляется хранилищем.
Воздух и земля используют явно подаренные готовые входные фокусы, настоящие
physical selection/use-item packets и серверные проверки6урона/knockback/2.4vis
и добычи стекла/3.4+0.1vis. Вода проверяется прямым серверным effect fixture,
а не заявляется ещё одним оплаченным клиентским крафтом. На итоговом снимке
видна замороженная ограниченная площадка; camera footing исправляет ракурс
первого успешного прогона. Рабочий GUI, исходные иконки, BER стола, фокусы,
цветной mote и native частицы не имеют отсутствующих текстур/ошибок рендера.

JVM завершилась0 на скрытом WinSta0 desktop,16 собственных окон,
input desktop Default/Default. Переключений, активации окон и глобального ввода
нет. Только собственный профиль `run/auromancy-smoke`; этапы QA явно задают
исследования устройства, один активный aura chunk и нулевую лунную регенерацию.
Алгоритм ауры в обычном мире не меняется. Сервер использует свежий QA-мир;
предыдущие QA-миры сохранены, ручные игровые сохранения не тронуты.

Ресурсы: **2776 JSON**, обе локализации;3529 ресурсов JAR побайтово совпадают
с исходными файлами.622 PNG,7 JPG и31 OGG совпадают с закреплённым BETA26 JAR,
два derived effect icons совпадают по пикселям. Звук ветра использует уже
существующую регистрацию оборудования; повторный ID, обнаруженный при загрузке,
исправлен. Build/reobfJar и CRC/побайтовая проверка JAR, mods ZIP и source ZIP
выполнены; source ZIP включает итоговые документы и artifact report.

**25** канонических записей и **7/21** работающих типов узлов фокуса.
Из56 infusion recipe paths достижимы9,47 сохраняют late gates. Остальные14
узлов, advanced/greater research, Baubles/мешочки и полные WarpEvents ещё
не завершены. Native FX и modern world/ownership protection — адаптации;
полное survival-прохождение и независимые multiplayer-клиенты не проверены.

Отчёт с SHA-256: [artifact-report-0.17.json](validation/artifact-report-0.17.json).
Локальные снимки: `validation/screenshots-0.17`; журналы: elemental-017-gametest5.log,
build-0.17-final.log, resources-0.17-final.log,
auromancy-client-elemental-017-client2-* и auromancy-visual-review-0.17.json.
JAR SHA-256: `213bcb76e1b357817248b7593d40155d83f268010f120e03b516adbb77bc3bea`.

---

# Проверка сборки 0.16.0-dev

Дата: 2026-10-03. Начальная ауромантия **TC6 6.1.BETA26**, Forge47.4.10 / Java17.
Контракт: [AUROMANCY.md](AUROMANCY.md) и три закреплённых аудита графа,
стола обработки и прогрессии в docs/tc6.

Сервер: **399/399 обязательных GameTests**, включая прежние350.
49 новых:13 графа/пакета,15 стола обработки,11 кастинга/выбора и10 прогрессии.
Проверены фактическая оплата43vis/2XP/двух кристаллов, сохранение на27-м тике,
пауза без ауры, отмена при изъятии/замене, девятичанковый зарядник, creative,
NBT/type/Count/ревизии/дальность/потоки, физический обмен фокусов без дубликатов,
общая перезарядка, дробные цены со скидкой и Vis Exhaust, реальный огненный урон,
поджог, иммунитет и серверная видимость цели. ROOT→FIRE и повторные Touch работают.
Отдельная проверка проходит все пять новых research chains без прямой выдачи их стадий.

Клиент: **8/8 сцен**, все итоговые PNG client3 просмотрены.
Реальные menu clicks и C2S edit/start естественно создают фокус:2XP,по одному
Aversio/Ignis,43vis. Физический запрос установки и ванильный use-item packet
наносят корове4урона и списывают0.8vis; XP при кастинге не расходуется.
Проверены исходные GUI/иконки, действующий BER стола, орбиты кристаллов,
готовый цветной фокус, радиальный выбор и аура HUD. Отсутствующих моделей/текстур
и исключений рендера в итоговом журнале нет. Клиент не меняет инвентарь стола.

JVM завершилась0 на скрытом WinSta0 desktop,16 собственных окон,
input desktop Default/Default; переключения и глобального ввода нет.
Профиль Auromancy,тег auromancy-client-auromancy-016-client3.
QA фиксирует полную BASEAUROMANCY и единственный активный чанк/лунную фазу
только в собственном мире для точной проверки платежей. Производственный
алгоритм ауры не меняется. Полный серверный прогон использовал свежий QA-мир;
прежние QA-миры сохранены,игровые сохранения не трогались.

Ресурсы: **2776 JSON**, обе локализации;3529 ресурсов JAR побайтово совпадают
с исходными файлами.622 PNG,7 JPG и31 OGG совпадают с закреплённым BETA26 JAR,
два derived effect icons совпадают по пикселям. Build/reobfJar,CRC модового
и установочного ZIP и source ZIP с побайтовой проверкой пройдены.

**23** канонические записи имеют прогрессию;9/56 рецептов наполнения достижимы,
47 сохраняют поздние gates. Исполнение пока ROOT/TOUCH/FIRE; остальные доставки,
эффекты и модификаторы, Baubles/мешочки и полное позднее выживание не завершены.
Независимые multiplayer-клиенты и ручное survival-прохождение не проверены.
Тики cooldown,серверное подтверждение графа, native particles и исправление
XP-before-crystals/GUI XP являются явными адаптациями порта.

Отчёт с SHA-256: [artifact-report-0.16.json](validation/artifact-report-0.16.json).
Снимки: [screenshots-0.16](validation/screenshots-0.16).
Журналы: auromancy-016-gametest3.log, build-0.16-final.log,
resources-0.16-final.log и auromancy-client-auromancy-016-client3-*.
JAR SHA-256: `0cd756ba70da806093888a2e585445a1c37ba43c953bc68ea9f3feea475e1a38`.

---

# Проверка сборки 0.15.0-dev

Дата: 2026-10-03. Рабочий алтарь наполнения **TC6 6.1.BETA26**;
Forge 47.4.10 / Java 17. Контракт — [INFUSION.md](INFUSION.md),
[алтарь](docs/tc6/INFUSION-ALTAR-BETA26-AUDIT.md),
[рецепты](docs/tc6/INFUSION-RECIPES-BETA26-AUDIT.md) и
[воздушная эссенция](docs/tc6/ESSENTIA-AIRBORNE-BETA26-AUDIT.md).

Сервер: **350/350 обязательных GameTests**, включая прежние 300.
50 новых: 19 воздушной эссенции, 8 пьедесталов, 6 прогрессии,
12 матрицы/рецептов и 5 защиты/очереди миграции. Проверены настоящий
расход 25 эссенции, списание двух реагентов, сохранение посреди ритуала
без повторной оплаты, остатки контейнеров, отсутствие платы XP,
проверки центрального предмета и исследований, точный NBT,
относительный износ, зачарования, runic scaling и отклонение переполнения.
Формирование четырёх колонн проверяет исходные позиции, направления,
нажатие на угол и атомарный откат при отмене Forge placement event.
Симметрия, штраф занятого пьедестала без пары, черепа, материалы,
ускорители и энергозатраты стабилизатора проверены отдельно.

Полный серверный прогон использовал свежий QA-мир. Предыдущий мир
сохранён в проверенной соседней папке; игровые сохранения не трогались.
Расширенный набор выявил голодание очереди миграции: неготовые чанки
занимали первые 64 места повторно. Трубы и алтарь используют общую
FIFO-реализацию с отдельной партией каждого END callback. Очереди раздельные,
бюджет каждой — 64, предел ожидания — 200. Принудительной загрузки чанков нет;
исходные проверки NBT, содержимого и ауры сохранены.

Клиент: **9/9 сцен**, все итоговые PNG запуска `client8` просмотрены.
Проверены **1347 baked block states**, реальные BlockEntityRenderer
матрицы/пьедесталов, три исходные текстуры, запуск, вращение, прозрачное
свечение рун и лучи во время ритуала. Настоящие C2S dust/caster requests
формируют и запускают алтарь; естественные серверные тики расходуют
25 эссенции и два реагента, выдают результат, оставляют банки 20/20/20.
Старые уже загруженные серверные и клиентские catalogue BE заменяются
рабочими типами. Клиент не может сам оплатить ритуал или создать предмет.
Две реальные страницы Таумономикона показывают runtime-рецепты матрицы
и элементального топора; чтение не меняет серверные знания/предметы.
Vis Resonator показывает синхронизированную ауру без награды за сканирование.

Minecraft JVM завершилась с кодом 0 на отдельном скрытом Win32 desktop;
наблюдались 16 собственных окон. Input desktop до/после: Default/Default.
Переключения стола, активации окна и глобального ввода не было.
Профиль — `run_client_smoke_isolated.ps1 -SmokeTest Infusion`,
итоговый тег `infusion-client-infusion-015-client8`. Маркеры
`THAUMCRAFT_INFUSION_CLIENT_SMOKE_OK` и
`THAUMCRAFT_INFUSION_RENDER_AUDIT_OK`, BUILD SUCCESSFUL и выход JVM0.
Снимки: [screenshots-0.15](validation/screenshots-0.15).

Ресурсы: **2773 JSON**, обе локализации и ссылки моделей/текстур.
Проверены CRC и полное побайтовое соответствие всех 3500 ресурсов собранного JAR.
Без изменений сохранены **597 PNG / 7 JPG / 30 OGG** официального BETA26;
две иконки эффектов сверены по пикселям с исходным атласом.
JAR SHA-256: `c45c7addcbb6967bcecc59723f456c43f3f98d2d9d2378809b9807dd7842e692`.
Mods ZIP содержит ровно мод и TerraBlender 3.0.1.10 с проверенными CRC/байтами.
Source ZIP сверяет каждый включённый файл с текущим проектом,
исключает игровые миры, логи и кэши.
[Машиночитаемый отчёт](validation/artifact-report-0.15.json) содержит
SHA-256 JAR, итоговых логов и всех девяти PNG, а также результаты просмотра.

Зарегистрированы **56 рабочих рецептов наполнения**: 41 замена предмета,
6 изменений NBT, 8 зачарований и 1 динамический runic recipe.
Игровая прогрессия реализована у **18 из 148** исходных записей;
**все 56 путей исследования этих рецептов пока закрыты в обычном выживании**.
QA явно выдаёт необходимые поздние факты. Это проверка рабочего алтаря,
а не полное survival-прохождение и не два независимых клиента.
Крафт устройства не означает готовность его механики. Кастинг фокусов,
Baubles-слоты, зеркала/transfuser, Flux Rift stability, полные warp events,
поведение flux goo и лечение tainted champions ещё не завершены.
Частицы адаптированы к native API 1.20.1; исходные специальные FX перенесены
не полностью. Полный книжный прогон 0.14 сохранён ниже как исторический;
в 0.15 заново проверены две связанные страницы и серверные книжные тесты.

---

# Проверка сборки 0.14.0-dev

Дата: 2026-10-03. Полный интерфейс и справочное наполнение Таумономикона
**TC6 6.1.BETA26**; Forge 47.4.10 / Java 17.
Контракт — [THAUMONOMICON.md](THAUMONOMICON.md),
[рецепты](docs/tc6/THAUMONOMICON-RECIPES-BETA26-AUDIT.md),
[конструкции](docs/tc6/THAUMONOMICON-MULTIBLOCK-BETA26-AUDIT.md),
[знания/прочтение](docs/tc6/THAUMONOMICON-KNOWLEDGE-BETA26-AUDIT.md).

Сервер: **300/300 обязательных GameTests**, включая все прежние 272.
28 новых: семь видимости/требований, семь каталога рецептов,
шесть схем и восемь знаний/прочтения. Проверяются скрытые корни,
текущая/заключительная стадия, строгие условия дополнений, точный NBT,
общие резервы основного инвентаря, минимальный уровень зачарования,
исходная заполненная карта, сырые знания и отдельные факты крафта.
Все 203 исходных ссылки классифицированы; 375 отображений сохраняют
стоимости, варианты и типизированный NBT. Схемы проверяют материалы,
ориентацию, древние/Eldritch метаданные и отсутствие изменений мира.
Прочтение меняет только BookRead; проверены повторы, устаревшая стадия,
неразрешённые биты, отдельные игроки, сохранение и тихая миграция.

Полный серверный прогон использовал свежий QA-мир; предыдущий сохранён
в проверенной соседней папке. Старый тест миграции труб теперь дренирует
очередь ограниченным числом END callbacks: полный набор может содержать
больше 64 чанков. Рабочий бюджет 64 не изменён; точные проверки NBT,
содержимого, ауры, направления, dirty и нового BE сохранены.

Клиент: **36/36 сцен**, все итоговые PNG просмотрены после последних
изменений. Отрисованы **148 записей / 271 стадия / 16 дополнений**,
**1007 различных запросов разворота** (1508 реальных render visits),
включая отдельные просмотры всех 375 рецептов. Проверены семь карт,
семь семейств рецептов, все шесть 3D-схем, вращение/слои/сброс,
закладки, ингредиентные ссылки и возврат на прежний разворот, поиск,
три страницы требований, вкладки аспектов/знаний, новые/прочитанные
страницы и уведомление. Снимок уведомления сделан во время штатного
выдвижения; серверная доставка и разделение типов проверяются GameTests.
Два масштаба GUI 2/3 и три русские страницы проверены отдельно.
Исправлены невидимый книжный Nitor, пересечение компонента наполнения
с эссенцией, служебные подписи типов рецептов и выход содержимого за бумагу.

Просмотр полного архива и схем не меняет авторитетные игровые знания
и инвентарь. Реальные C2S/S2C подтверждают прочтение законченной
ESSENTIASMELTER и строго открытого дополнения BELLOWS; последние поздние
факты заданы только в принадлежащем QA-мире. Крафт/оплата исследования
при этом не выполняются. Отображение требований отслеживает удаление
и возврат настоящего образца в клиентском инвентаре.

Minecraft JVM завершилась с кодом 0 на отдельном скрытом Win32 desktop;
наблюдались 16 собственных окон. Input desktop до/после: Default/Default.
Переключение стола, активация окна и глобальный ввод не выполнялись.
Это скрытый Win32 desktop, не виртуальный стол Task View; CPU/GPU общие.
Профиль — `run_client_smoke_isolated.ps1 -SmokeTest ThaumonomiconComplete`,
итоговый тег `book-014-client4`. Маркеры
`THAUMCRAFT_THAUMONOMICON_COMPLETE_CLIENT_SMOKE_OK` и
`THAUMCRAFT_THAUMONOMICON_RENDER_AUDIT_OK`, BUILD SUCCESSFUL и выход JVM0.
Снимки: [screenshots-0.14](validation/screenshots-0.14).

Ресурсы: **2704 JSON**, обе локализации и ссылки моделей/текстур.
JAR CRC и byte parity **3421 ресурса**; побайтово сохранены
**595 PNG /7 JPG /24 OGG** официального BETA26.
JAR SHA-256: `2cb832893b6cde616c714711810c06482d7947939f18d2f45b9aab5825ccb957`.
Mods ZIP содержит ровно мод и TerraBlender 3.0.1.10 с проверенными CRC/байтами.
Source ZIP сверяет каждый включённый файл с текущим проектом,
исключает игровые миры, логи и кэши.
[Машиночитаемый отчёт](validation/artifact-report-0.14.json) содержит
хеши итоговых логов, всех 36 PNG и трёх отчётов визуальной проверки.

Сетевой протокол исследования4; BookRead — необязательные метаданные
совместимого SavedData v2. Завершены интерфейс и справка книги;
игровая прогрессия остаётся у 15 из 148 исходных записей.
Справочные рецепты/схемы не включают будущие устройства, инфузию
или поздние dust triggers. Динамические рецепты зачарований/runic показывают
помеченные примеры. QA задаёт поздние знания явно: это проверка интерфейса
и настоящих сетевых запросов, не полное survival-прохождение и не два
независимых клиента. Предыдущие результаты сохранены ниже как история.

---

# Проверка сборки 0.13.0-dev

Дата: 2026-10-03. Производство и трубный транспорт эссенции TC6 BETA26;
Forge 47.4.10 / Java 17. Контракт — [ESSENTIA.md](ESSENTIA.md),
[производство](docs/tc6/ESSENTIA-PRODUCTION-BETA26-AUDIT.md),
[трубы](docs/tc6/ESSENTIA-TUBES-BETA26-AUDIT.md) и
[исследования/рецепты](docs/tc6/ESSENTIA-PROGRESSION-BETA26-AUDIT.md).

Сервер: **272/272 обязательных GameTests**, включая все прежние 225.
Полный прогон использует свежий QA-мир; предыдущий сохранён в проверенной
соседней папке. Игровые сохранения не использовались. 47 новых тестов:
15 производства, 20 транспорта, 5 стадий исследований и 7 магических рецептов.
Проверяются топливо и стоимость аспектов, вместимость 256/128, интервалы
15/10/15 и алументум 12/8/12, два прохода непрерывной колонны кубов,
эффективность/потери/flux, мехи, насосы и вентиляция, автоматизация,
сохранение, sync, фиалы/бирки, разрушение и точный дроп.
Все шесть труб проверяют тип и силу всасывания, направления, клапан/redstone,
фильтр, ограничитель, конфликт сети без уничтожения содержимого,
смешанный буфер 10, мехи/ограничение сторон и резонатор.

Два теста загружают настоящий прежний `thaumcraft:catalog_visual` NBT
клапана/односторонней трубы через ванильный реестр. Worker callback только
ставит координаты в очередь; замена происходит на серверном END tick.
Проверяются направление, зарегистрированный рабочий ticker, dirty-чанк,
новый ID при повторном сохранении и update packet, отсутствие посторонних
мутаций, загрузки дальних чанков, loot и flux.
Исследования ESSENTIASMELTER/WARDEDJARS/TUBES оплачивают точные образцы
основного инвентаря, знания и факт изготовления; malformed/mixed NBT,
offhand, replay и нехватка ресурсов не вызывают частичной оплаты.
Рецепты проверяют бесформенную сетку, 19 исходных определений, выбор
одинаковых сеток по Aqua/Terra и перенос полного отделённого NBT void-банки.

Клиент: **10/10 сцен**, все итоговые PNG просмотрены после последних изменений.
Сценарий использует реальные C2S QUICK_MOVE в меню, расход камня/угля,
серверные тики плавильня → куб → две трубы → помеченная банка.
При проверке прохождения потока в банке 2 единицы Terra; оставшиеся ресурсы и машинные
хранилища проверяются на сервере. Галереи получают S2C-состояния настоящих BE,
проверяют все типы устройств, клапаны, направления, буфер и бирки на четырёх
сторонах куба. GUI отражает горение/прогресс/запас; рецепты книги показывают
сетки, vis/кристаллы/research gates и не меняют авторитетные знания сервера.
Проверены **1069 baked block states**, ссылки текстур,
исходные плоские иконки клапана/односторонней трубы и отсутствие missing sprites.
Клиентская мутация хранилищ отвергается.

Minecraft JVM завершилась с кодом 0 на отдельном скрытом Win32 desktop;
наблюдались её собственные окна. Активный стол до/после:
`Default` / `Default`.
Переключение стола, активация окна и глобальный ввод не выполнялись.
Это скрытый Win32 desktop, не виртуальный стол Task View; CPU/GPU общие.
Конфигурация — `run_client_smoke_isolated.ps1 -SmokeTest EssentiaProduction`,
итоговый тег `essentia-013-client3`.
Маркеры: `THAUMCRAFT_ESSENTIA_PRODUCTION_CLIENT_SMOKE_OK`,
`THAUMCRAFT_ESSENTIA_PRODUCTION_RENDER_AUDIT_OK`,
`THAUMCRAFT_ESSENTIA_PRODUCTION_REAL_FLOW`, BUILD SUCCESSFUL и выход JVM0.
PNG: [screenshots-0.13](validation/screenshots-0.13).

Ресурсы: **2702 JSON**, локализации и ссылки моделей/текстур.
JAR CRC и byte parity **3419 ресурсов**,
**595 исходных PNG** и
**24 исходных OGG** официального BETA26.
JAR SHA-256: `4134b2c47baefac54ce548c05c07715a2436cd171d0dceda97dd2f1aa77afd12`.
Mods ZIP содержит ровно мод и закреплённый TerraBlender 3.0.1.10;
проверены CRC/byte parity. Source ZIP проверяет каждый включённый файл
по текущему проекту, исключает игровые миры, логи и кэши.
[Машиночитаемый отчёт](validation/artifact-report-0.13.json) содержит хеши
финальных логов/PNG и сведения об изолированном запуске.

Формат сохранённых знаний остаётся совместимым; сетевой протокол исследования 3
отклоняет 0.12 до передачи изменённого формата рецептов.
Продвинутые исследования плавилен/улучшений/мехов/Eldritch ещё закрыты;
мехи пока без рабочей анимации и интеграции с прочими печами/тиглем.
Воздушная сеть, Thaumatorium и инфузия не завершены. Клиентские ресурсы/поздние
знания заданы явными фикстурами: это проверяет реальные игровые операции,
но не полное survival-прохождение или два независимых сетевых клиента.
Предыдущие результаты ниже сохранены как история.

---

# Проверка сборки 0.12.0-dev

Дата: 2026-10-03. Полный зарегистрированный стол теоретических исследований TC6 BETA26;
Forge 47.4.10 / Java 17. Поведение — [THEORYCRAFTING.md](THEORYCRAFTING.md),
[аудит карточек](docs/tc6/THEORY-CARDS-BETA26-AUDIT.md) и
[аудит помощи/модели](docs/tc6/THEORY-AIDS-RENDER-BETA26-AUDIT.md).
Точные исходные эффекты и адаптации GUI —
[THEORY-GUI-BETA26-AUDIT.md](docs/tc6/THEORY-GUI-BETA26-AUDIT.md).

Сервер: **225/225 обязательных GameTests**, включая все прежние 208.
Прогон использует чистый QA-мир. Предыдущий мир сохранён отдельно: старые
ItemEntity за пределами малых шаблонов давали ложное дублирование добычи.
Исходные проверки числа предметов сохранены; игровые миры не изменялись.
17 новых проверяют 21 добавленную карточку, сохранение полного набора из 33,
конкретные предметы и NBT, потребляемые/сохраняемые требования, эффект Curio,
выход Synthesis, фиал Infuse, порядок Synergy, XP и раздельный warp,
дополнительные вызовы Scripting, возврат вдохновения до стоимости,
переполнение и повторные запросы. Проверяются исходные 14 определений помощи,
12 блоковых объектов и конкретный малый портал культистов, область сущностей
AABB ±5 без фильтра сферы, границы/повторы, отсутствие загрузки дальних чанков,
конечные пулы и исходный крафт набора писца через настоящий ванильный результат.
Отдельно проверяются передача полного набора помощников, ограничение вдохновения
и синхронизация инвентаря/сессии BlockEntity без общего изменяемого NBT.
Eldritch остаётся исходно недоступным: его блок не инициализирован в BETA26.
Truth и Dragon Egg не становятся зарегистрированными карточками.

Клиент: конфигурация `runClient -PtheoryCompleteSmokeTest`, **22 PNG**,
каждая сцена текущего клиента отражена в итоговом логе и машинном отчёте.
Все итоговые кадры (22) просмотрены после последних исправлений GUI:
проверены читаемость, границы элементов, подписи оплаты и состояния анимаций.
Gradle подготавливает запуск без создания окна; итоговая JVM Minecraft запускается
на отдельном неактивном рабочем столе Win32. Переключение рабочего стола и
активация окна не вызываются. Активный стол до/после: `Default` /
`Default`. Проверяется фактический успешный выход JVM, отдельно
от BUILD SUCCESSFUL этапа подготовки. Это скрытый Win32 desktop, не виртуальный
стол Task View; GPU и ресурсы компьютера остаются общими.
Настоящий интегрированный сервер и C2S/S2C проходят создание, вытягивание,
оплату выбора, отмену и завершение. Проверяются кнопки помощи, разные масштабы,
анимации раздачи/наведения/разрешения оплаченной карты, точные записи и XP,
сценарии неправильного NBT и успешной оплаты новых предметных карточек,
расходы Scripting и ограничения бумаги, чернил, владельца и ревизии.
Шесть кадров после оплаты требуют ответа именно текущего запроса и новой анимации,
совпадения серверного/клиентского инвентаря и vanilla XP, затем готового кадра GUI/HUD.
Это исключает снимок старого предложения или шкалы XP до обновления клиента.
Блоковая сцена использует зарегистрированный BER и синхронизированное состояние
стола: чернильница/перо при наличии инструментов и свиток при существующей теории.
Анимации происходят после серверной оплаты и не управляют выдачей знаний.
Итоговые маркеры: `THAUMCRAFT_THEORY_COMPLETE_CLIENT_SMOKE_OK`, выход JVM 0,
BUILD SUCCESSFUL серверных проверок и подготовки клиента.
PNG: [screenshots-0.12](validation/screenshots-0.12).

Ресурсы: **2678 JSON**, текущие локализации и ссылки моделей/текстур.
JAR CRC и byte parity **3384 ресурсов**, **593 исходных PNG**
(592 прежних и новая точная текстура исследовательского стола), **17 исходных OGG**.
JAR SHA-256: `232c6d36b1052b8d12bfca233670deb71b0164fb13ceab3762d250ca927c8df9`.
[Машиночитаемый отчёт](validation/artifact-report-0.12.json).

Поздние знания, ресурсы и конкретные предложения заданы явными фикстурами в
изолированном мире. Это проверяет настоящий серверный путь и синхронизацию,
но не заменяет длительное survival-прохождение или два одновременно подключённых
игровых клиента. Помощь от каталоговых устройств/сущностей не доказывает их
собственные механики. Плавильня/перегонка, трубы, инфузия, фокусы, продвинутые
рецепты и полный WarpEvents остаются в работе. Головоломка аспектов TC4 исключена
по запросу пользователя и не относится к исходному столу TC6.

# Проверка сборки 0.11.0-dev

Дата: 2 октября 2026. Ранняя прогрессия TC6 BETA26;
Forge 47.4.10 / Java 17. Поведение и границы — [EARLY-SURVIVAL.md](EARLY-SURVIVAL.md).

Сервер: **208/208 обязательных GameTests**, включая все прежние 186.
22 новых проверяют исходные переходы металлургии и открытия Artifice/Infusion,
реальные продукты тигля и факты изготовления, атомарную оплату/replay/сохранение,
строгие canonical-предпосылки и старые PORT aliases, metadata-сопоставление,
37 аспектных рецептов и NBT, приоритет совпадений, остаток воды и независимые потери,
пять ранних рецептов оборудования, 16 цветов шерсти и скидки.
Проверяется реальное взаимодействие результата верстака, включая pickup,
shift-click и hotbar swap. Новые и старые кристаллы совместимы с первичными
слотами; настоящий крафт Salis принимает составные аспекты, запрещает повтор
одного аспекта в разных представлениях и возвращает кремень/миску.
Фактический частичный подбор ItemEntity открывает Strange Dreams.

Клиент: изолированный `runClient -PearlySurvivalSmokeTest`, **9 настоящих PNG**.
Текущий синхронизированный RecipeManager и S2C-данные тигля дают страницы
ингредиентов, результатов, vis до скидок, шести первичных кристаллов и аспектов.
Проверяются граф, закрытая справочная стадия, платный переход металлургии,
ткань/мантии, очки, таумометр, аспектные кристаллы и Salis Mundus.
Просмотр не меняет знания и не открывает оплату в архиве. Salis показан только
при наличии настоящего CustomRecipe; подписи объясняют разные аспекты и остатки.
Все девять сцен просмотрены визуально; отсутствующих текстур и обрезанного текста
не обнаружено. PNG: [screenshots-0.11](validation/screenshots-0.11).
Итоговые маркеры: `THAUMCRAFT_EARLY_SURVIVAL_CLIENT_SMOKE_OK` и BUILD SUCCESSFUL.

Ресурсы: **2677 JSON**; обе локализации, ссылки моделей/текстур.
JAR CRC и byte parity **3373 ресурсов**, **592 исходных PNG**,
**9 исходных OGG**. JAR SHA-256: `6b29933fc4547827aea4f5006b7e2faca5ac71f562a03b8f2d826a845abb52a9`.
[Машиночитаемый отчёт](validation/artifact-report-0.11.json).

Фикстуры явно задают ранее проверенные начальные действия, запас знаний и аспекты;
новые переходы и транзакции проходят настоящий серверный путь. Скриншоты сделаны
на английском; русские ключи прошли проверку ресурсов. Эти сценарии не устанавливают
полное survival-прохождение или двухклиентский multiplayer. Плавильня/перегонка,
трубы, инфузия, ауромантия и продвинутые рецепты остаются в работе.
Серверный прогон выполнен до последней правки только клиентского предпросмотра
Salis и локализаций; итоговый JAR пересобран после неё, клиент и ресурсы проверены снова.

# Проверка сборки 0.10.0-dev

Дата: 2 октября 2026. Оборудование и расходуемые предметы TC6 BETA26;
Forge 47.4.10 / Java 17. Поведение и границы — [EQUIPMENT.md](EQUIPMENT.md).

Сервер: **186/186 обязательных GameTests**, включая все прежние 149.
Проверяется реальный серверный игрок: защита/износ брони, Fortress и маски,
Void self-repair, оружейные атрибуты и добыча, Traveller Boots, цена магического
верстака, заряд/аура, зарегистрированный ticker пьедестала, capability, NBT,
вставка/изъятие/разрушение, Runic Shielding, еда, curios и loot bags через vanilla
gameMode, Primordial Pearl и раздельное сохранение warp. Отдельные проверки
специальных инструментов и очищения перечислены в исходниках GameTests.

Клиент: изолированный `runClient -PequipmentSmokeTest`, **12 настоящих PNG**.
Надетые комплекты, реальные C2S действия инструментов/пьедестала, серверная
зарядка и BE-синхронизация, очки/содержимое контейнера и Sanity HUD по S2C.
Очищающая ванна, течение, ведро, Ward от фактической collision и удержание мыла
с завершением/изменением warp проверены через настоящий клиент. F проходит
KeyboardHandler и меняет серверный режим лопаты без смены рук.
Итоговые логи содержат `THAUMCRAFT_EQUIPMENT_CLIENT_SMOKE_OK` и BUILD SUCCESSFUL.
PNG: [screenshots-0.10](validation/screenshots-0.10).

Ресурсы: **2633 JSON**; JAR CRC и byte parity **3329 ресурсов**,
**592 исходных PNG**, **9 исходных OGG**. Новая иконка
Warp Ward содержит неизменённые пиксели оригинального potion atlas.
JAR SHA-256: `038ed0bb73c7fa6bb00f0b0ebe72cc8fa2f3f59c232fbf2a5b7508b0ab6350dc`. [Машиночитаемый отчёт](validation/artifact-report-0.10.json).

Промежуточный прогон выявил FakePlayer, обходивший vanilla armor calculation,
shared tier tags, ломавшие добычу обсидиана, и неверное ожидание одного ремонта
вместо двух worn callbacks. Исправлены fixture, отдельные Forge tags и ожидание,
сохранена исходная формула поглощения. Исправлены отображение предмета пьедестала
и совместимость старого WarpCounter. Тест loot bag теперь проходит через реальный
gameMode: creative восстанавливает количество после callback.
Проверка выпадения пьедестала использует уникальную NBT-метку, чтобы предметы
из соседних GameTests не попадали в её подсчёт; полное совпадение NBT и количество
ровно одного собственного stack по-прежнему обязательны.
Первый завершённый клиентский прогон выявил пропущенный CHANGE_GAME_MODE в
фикстуре: ServerPlayer.setGameMode теперь синхронизирует survival, и кирка реально
добывает блок через C2S с износом. Следующий запуск был вручную закрыт во время
загрузки мира; он не считается проверкой. Итоговый клиент получил все 12 PNG.
После клиента отдельно зарегистрирован и проверен раздатчик очищающего ведра:
фактический tick размещает источник и оставляет пустое ведро. Финальная сборка
и полный серверный прогон выполнены после этого изменения; клиентская графика
и сетевые сценарии этим изменением не менялись.

Полное прохождение survival, двухклиентский multiplayer и оставшиеся системы
не установлены этими сценариями. Полнота регистрации не означает готовность всего мода.

# Проверка сборки 0.9.1-dev

Дата: 2 октября 2026. Thaumometer HUD по TC6 BETA26, Forge 47.4.10 / Java 17.
Поведение, управление и явные адаптации — [THAUMOMETER.md](THAUMOMETER.md).

Сервер: **149/149 обязательных GameTests**, включая прежние 143.
Шесть новых проверяют отсутствие начислений/cooldown при наведении,
состав до и после фактического скана, ауру текущего чанка без расхода,
выбор main/offhand, преграды/ближайшую сущность/дальность и сетевые round trips
с отказом на неизвестные аспекты, повторяющиеся теги, отрицательные количества
и нечисловые значения ауры. Посылки направлены только server → client.

Клиент: изолированный `runClient -PthaumometerSmokeTest`, **8 реальных
снимков от первого лица**. Интегрированный сервер размещает блок и корову,
vanilla взаимодействия отправляют настоящие scan packets. Клиент получает
состав и состояние изученности, aura base/vis/flux; наведение не меняет знания.
Проверены основная/вторая рука, Shift, GUI scales 2/3, переполнение vis=450,
flux=350, F1 и снятие таумометра. Счётчики подтверждают вызов HUD/world render
и отсутствие вызовов при F1. Итоговый маркер
`THAUMCRAFT_THAUMOMETER_CLIENT_SMOKE_OK` и BUILD SUCCESSFUL присутствуют.
PNG: [screenshots-0.9.1](validation/screenshots-0.9.1).

Ресурсы: **2627 JSON**; обе локализации и новые scanner keys проверены.
JAR CRC и byte parity: **3314 ресурсов**, **592 исходных PNG**.
Шкала `hud.png` и звук `scan.ogg` побайтово совпадают с официальным BETA26.
JAR SHA-256: `5110f1bdfd3c464b554bc9cf6a032a5455c71d16741f0bcac55183d1213b0d41`. Машиночитаемый отчёт:
[artifact-report-0.9.1.json](validation/artifact-report-0.9.1.json).

Первый compile выявил название Tessellator из старых mappings: исправлено на
Tesselator 1.20.1. Итоговая сборка выполняется после исправления и добавления
dimension в scan-result packet. Исторические промежуточные логи сохранены.
Первые PNG выявили непрозрачную линзу и скрытые world-counts. Линза переведена
с cutout на translucent без изменения оригинальных текстур/OBJ; ориентация
текста исправлена. Отдельный аудит восстановил blend SRC_ALPHA, ONE, исходный
порядок значков и уменьшение tagscale. Финальные PNG получены после исправлений.
Полный multiplayer на двух клиентах и длительное survival-прохождение этим
проверочным сценарием не установлены. Результаты предыдущих отдельных визуальных
проверок остальных каталожных моделей и банок сохранены ниже.

# Проверка сборки 0.9.0-dev

Дата: 2 октября 2026. Forge 47.4.10 / Minecraft 1.20.1 / Java 17.
Срез: normal/void банки, фиалы, метки, скобы, сохранение и рендер;
правила и границы — [ESSENTIA.md](ESSENTIA.md).

Сервер: **143/143 обязательных GameTests**, все прежние 120 сохранены.
Новые проверки: ёмкость/сохранение количества, запрет смешения, void overflow,
допуск/сохранение фильтров, точные переносы по 10 и отказ без расхода,
полный survival/creative инвентарь, снятие метки и намеренное загрязнение,
скоба/верхний ввод, компаратор, filled/filter-only drop -> placement,
save/loadStatic/update packet, сканирование содержимого и отсутствие эссенции
в шаблоне метки, пять рецептов и полный phial remainder, suction guards,
исходная асимметрия minimum и работа реального зарегистрированного ticker.
Отдельно проверено восстановление фиалы без NBT в main/offhand слотах.

Изолированный клиент `runClient -PessentiaSmokeTest`: **16 настоящих банок**
поставлены интегрированным сервером; клиент получил точные amount/aspect/filter/
facing/blocked. Клиентские операции банки, фиала и метки не изменяют NBT или
инвентарь. Два финальных PNG уровней/меток/скоб просмотрены; явный маркер
`THAUMCRAFT_ESSENTIA_CLIENT_SMOKE_OK` и BUILD SUCCESSFUL подтверждают завершение.
PNG — [screenshots-0.9](validation/screenshots-0.9).

Повторный каталог 0.8 в текущих классах прошёл: **28 снимков**, отсутствуют
missing sprites/model errors; 3264 rendered block states, 334 dynamic item samples; 710 item checks, 89570 baked quads, 38 custom item renderers, 1339 custom block states.

Статическая проверка: **2626 JSON** и обе локализации.
JAR CRC и byte parity: **3311 ресурсов**;
**591 оригинальных PNG** побайтово совпадают с BETA26.
Поставка JAR SHA-256: `ec46b0b914b64b8f813ab3ab8bfc2f3b0990e82b396301444994c67538457c9e`.
Машиночитаемый отчёт — [artifact-report-0.9.json](validation/artifact-report-0.9.json).

Первый прогон 141/142: один тест рецептов ошибочно искал ранее зарегистрированный
латунный самородок в новом каталоге. Исправлена тестовая фикстура; полный прогон
повторён. Исторические журналы сохранены отдельно, итоговые имеют суффикс final.
Начальный запуск нового изолированного клиента остановлен на стандартном
экране accessibility onboarding; opt-in QA теперь проходит этот экран автоматически.
Это исправление тестового запуска, не изменение обычного клиентского профиля.

NBT round trips и break/placement не означают опыт настоящей выгрузки чанка и
перезапуска сервера; такой длительный сценарий и два клиента ещё не проверены.
Перегонка/трубы/воздушный handler/инфузия и survival-ветка WARDEDJARS ещё нужны.

# Проверка сборки 0.8.0-dev

Дата: 1 октября 2026. Forge 47.4.10 / Minecraft 1.20.1 / Java 17.
Визуальный этап по запросу пользователя: исходные реестры TC6 BETA26 —
108 предметов/185 metadata-форм, 200 блоков/191 BlockItem и 43 EntityType.
Полнота регистрации не означает завершённость механик; границы —
в [VISUAL-CATALOG.md](VISUAL-CATALOG.md).

Сервер: **120/120 обязательных GameTests**, включая все прежние 112.
Восемь новых проверяют исходные реестры, размещение настоящих блоков,
жизненный цикл визуальных BlockEntity, создание всех EntityType,
NoAI каталожных мобов, 43 нейтральных размера, NBT вариантов существ,
экипируемую броню и damage жемчужины, разлом и big-endian големов,
размещение баннеров/вентилей, сохранение BlockState и render AABB баннеров.

Клиент: `build runClient -PcatalogSmokeTest` прошёл с явным
`THAUMCRAFT_CATALOG_CLIENT_SMOKE_OK`, **28 снимков**.
Фактический аудит: 3264 rendered block states, 334 dynamic item samples; 710 item checks, 89570 baked quads, 38 custom item renderers, 1339 custom block states.
Две банки имеют пять различных baked-геометрий на всех девяти границах;
оба зеркала выбирают нужный sprite linked/unlinked. Проверены регистрации
отдельных item/BER-рендеров, все 14 неживых сущностей и надетые варианты брони.
37 отдельных поз установленных моделей показывают banner floor/wall,
шесть направлений valve/mirror, pattern, полный builder, matrix и brine.
Это preview реальных static/BER путей с BlockState; frustum AABB проверена
сервером. Длительный обход устройств в обычном мире не заявляется.

Итоговые PNG просмотрены: предметы/NBT, блоки/позы, мобы, эффекты,
варианты Pech/големов и броня. По первым кадрам исправлены opaque-банки,
потерянные override-модели, пропавшие материалы OBJ, иконки builtin/entity,
размеры превью и V-flip установленного builder. Независимый аудит выявил
центральное расположение зеркал и узкую frustum AABB баннеров; исправлено
и перепроверено. Стекло сохраняет исходный TRANSLUCENT, эссенция и мозг видны.

Первый серверный прогон 117/118 упал из-за отсутствующего опорного блока
в rail-фикстуре, после исправления все прогоны прошли. Усиленный клиентский
прогон обнаружил вырожденный missingtexture quad исходного empty-моделя,
исправленный на настоящую пустую геометрию. Исходные неудачные журналы и
ранние PNG сохранены отдельно как история, а не итоговые свидетельства.

Ресурсы: **2621 JSON**, локальные модели/текстуры и обе локализации
прошли статическую проверку; **3305 ресурсов** JAR побайтово совпадают
с проектом. **590 PNG** по исходным путям побайтово совпадают с
официальным BETA26 JAR. Изменяемые JSON/MTL адаптированы к 1.20.1 и не
выдаются за неизменённые оригиналы. CRC и побайтовая сверка архивов обязательны.

Клиентский тест создаёт собственный новый мир в `run/visual-catalog` и
закрывает только себя; тесты сервера используют `run/gametest-server`.
Миры ручной проверки не использовались. Старый игровой клиент не перезапускался.

Новые мобы без AI/естественного спавна/боя, устройства без исходных
операций, броня с временными leather-статистиками. Полный survival,
multiplayer и полный TC6 не заявляются. Системы предыдущих этапов сохранены.

JAR SHA-256: `b606017ce0283cf300422f9917654fd77e8db77c25f34910c451d875d3aacdfc`.
Журналы: `validation/verification-0.8-final.log`,
`validation/catalog-client-final.log`, `validation/resources-0.8.log`.
Снимки: `validation/screenshots-0.8`; отчёт: `validation/artifact-report-0.8.json`.

# Проверка сборки 0.7.1-dev

Дата: 1 октября 2026. Forge 47.4.10 / Minecraft 1.20.1 / Java 17.
По пользовательскому скриншоту проверена завышенная глобальная генерация
деревьев в обычных Plains. Официальный BETA26 JAR подтверждает chunk trials
1/25 и 1/80, затем support и support/2. В 0.7.0 Greatwood использовал 1/16
без biome gate, Silverwood 1/80 без gate: завышение в 7.8125 и 10 раз.
Исправлены rarity, natural-only gates и modern biome cohorts; детали — BIOMES.md.

`gradlew.bat build runGameTestServer`: **112/112 обязательных GameTests**.
Четыре новых теста проверяют реальные загруженные placements, строгие
пороги и размещение настоящих деревьев, magical exception с RNG draw,
независимость саженцев и неизменный 18/12/8 выбор Magical Forest.
Границы .2/.1/.5 отклоняются, значения чуть ниже проходят. Подменённые
трёхмерные палитры тестовых чанков восстанавливаются в finally.
Все 108 предыдущих серверных тестов также прошли.
Первый прогон 111/112 выявил пропущенный Windswept Forest в современном
support-теге. Forge 1.12 подтверждает FOREST у старого Extreme Hills With
Trees; современный преемник явно добавлен в full, после чего прогон повторён.
GameTest рабочая папка отделена в `run/gametest-server`, чтобы журналирование
не конфликтовало с открытым игровым клиентом. Первый лог сохранён отдельно.

Ресурсы: **762 JSON**, обе локализации, модели и текстуры;
**1094 ресурсов JAR** побайтово совпадают с исходниками.
Статический граф features после разворачивания nested tags и Forge
ADD/REMOVE не имеет циклов в 67 биомах. Таумономикон: 167 mappings,
192 sized textures, 12 GUI textures и 10 IMG references.

Клиент пользователя с 0.7.0 оставлен запущенным. Новый автоматический
клиентский прогон 0.7.1 не проводился; 16 PNG и disk-reload проверки 0.7
остаются историческими свидетельствами и не выданы за повторные проверки.
После перезапуска на 0.7.1 новые правила действуют только в новых чанках;
созданные ранее деревья не удаляются. Вероятности — попытки, не гарантии.
Адаптации taiga/Grove/Mangrove и отсутствующие старые hills IDs описаны
явно. Полный TC6, multiplayer и длительное прохождение не заявляются.

JAR SHA-256: `0aa89fd4e03ebf51778363c2d592d9988482771ea21cf314c005c817e6cfbff5`.
Логи: `validation/verification-0.7.1.log`, `validation/resources-0.7.1.log`;
отчёт: `validation/artifact-report-0.7.1.json`.

# Проверка сборки 0.7.0-dev

Дата: 1 октября 2026. Windows 11, Java 17.0.18, Gradle 8.8,
Minecraft 1.20.1, Forge 47.4.10, TerraBlender 3.0.1.10.

`gradlew.bat build runGameTestServer`: **108/108 обязательных GameTests**.
Все 96 прежних тестов и 12 новых: четыре биомов, четыре растений,
четыре паучьего Greatwood. Проверены codec/climate/colors/spawn entries,
теги и коэффициенты ауры, полный вертикальный biome palette через настоящий
ChunkSerializer/NBT, normal biome source с TerraBlender, ветвистый дуб,
опоры/дропы/свет/крафты растений, контакт с висомором, размещение дерева,
сохранение cave-spider spawner и сундука, защита существующего block entity
и неразрушаемых блоков, отсутствие гнезда у дерева из четырёх саженцев.

`runClient -PbiomeSmokeTest`: **16 PNG 1600×1000** из настоящего нового мира
`minecraft:normal`, seed `6071756380951235909`. Найден естественный Magical Forest;
25 FULL-чанков содержат 2161 блоков древесины,
164 собственных поверхностных блоков,
25 ambient-grass anchors, 1756 горизонтальных
дубовых брёвен и 28 высоких дубовых стволов.
Счётчики ограничены поверхностью самого Magical Forest, соседние ванильные
леса и подземные руды исключены.

Сняты день/ночь естественного леса и отдельных подготовленных садов трёх
биомов, до и после сохранения. Клиент проверил grass/oak/Greatwood/neutral
Silverwood/water/sky и реальные конечные значения fog из рендера. Тест
создал собственный мир с уникальным именем; существующие миры не открывал.
После закрытия первого integrated server мир открыт новым экземпляром:
**100 полных трёхмерных палитр чанков и 100 записей ауры base/vis/flux**
совпали с диском. Подготовленные сады Eerie/Outer Lands проверяют рендер
и сохранение, не заявляют естественную генерацию этих dormant биомов.
Все 16 PNG просмотрены: нет отсутствующих текстур или непрозрачных
прямоугольников; цвета, прозрачность растений/листвы и ночной свет корректны.
Отдельные наблюдения — `validation/visual-qa-0.7.json`.

Первый клиентский прогон обнаружил production-ошибку: biome filter флоры
работал на подземной высоте и пропускал декорацию. Для флоры и больших
грибов добавлен surface heightmap; поиск почвы проходит сквозь растительность.
Во втором прогоне естественный лес уже прошёл assertions, но клиент
автоматически остановился при потере фокуса после 2/16 снимков. Opt-in
стенд теперь временно выключает pauseOnLostFocus и закрывает PauseScreen
только своего мира, затем восстанавливает настройки. Третий прогон выявил
ошибку сравнения теста: ванильная colormap хранит ARGB, а усреднение рендера
возвращает RGB; обе стороны сравниваются по 24-битному цвету. Дополнительно
сверяются renderer, cached average и свежий расчёт tint. Облака выключены
только в smoke, чтобы сады на высоте Y=192 не перекрывались. Четвёртый
прогон прошёл полностью; итоговая сборка и все серверные тесты повторены
после исправлений.
Первоначальные compile failures касались API новых фикстур и исправлены
по фактическим mappings 1.20.1. Исходные логи сохранены отдельно.

Ресурсы: **758 JSON**; **1090 ресурсов JAR** побайтово
совпадают с проектом. Восемь исходных research JSON, 14 небесных текстур
и три текстуры растений совпадают с официальным BETA26.
JAR: 3366649 байт, SHA-256 `68a6835ee48c8c9b07fac40ec2e08bfa1d126208b62f6e66f060b6eb662a4c36`.
Отдельная обязательная зависимость TerraBlender включена в готовый mods.zip;
её SHA-256 `ca678c11b878ad9c7143636fbec911f29e2c1453a6cc091e16b14e6d9608fb5f`.
Итоговые логи: `validation/verification-0.7.log`,
`validation/client-biomes-0.7.log`; снимки и SHA —
`validation/screenshots-0.7/`, `validation/artifact-report-0.7.json`.

Проверен один seed и один integrated клиент/сервер. Два сетевых клиента,
длительное выживание и миграция миров 1.12.2 не проверены. GUI-прогоны 0.6
сохранены отдельно и не выданы за повторные проверки 0.7. Все три биома
зарегистрированы, но собственные TC6 существа и точный старый flower noise
ещё нужны. Полного Thaumcraft 6 пока нет. Границы — `BIOMES.md` и `PORTING.md`.

# Проверка сборки 0.6.0-dev

Дата: 1 октября 2026. Windows 11, Java 17.0.18, Gradle 8.8,
Minecraft 1.20.1, Forge 47.4.10. Русский клиент 1600×1000, GUI 2/3/4.

`gradlew.bat build runGameTestServer`: **96/96 обязательных GameTests**.
71 прежний тест и 25 новых: 12 небесных, девять карточных,
три сохранений/исследования/временного warp, одна интеграция трёх aids/меню/пакета.
Проверены точные угловые границы и N/S/W/E, все 13 видов записей,
gameTime-квота и dayTime-фазы, strict research, реальный таумометр и cooldown,
приоритет цели над небом, дождь/крыша/измерения, главный инвентарь,
исчерпанные принадлежности без расхода чернил, последняя бумага,
полный инвентарь и отмена Forge entity join без потери бумаги/квоты.

Карточки: две конкретные записи до эффектов/оплаты, relaxed NBT и порядок слотов,
сохранение целей и RNG, один звёздный эффект для двух звёздных записей,
пять уровней XP, Beacon refund cap, конечные пулы, переполнение/повторные запросы.
Сохранения: UUID, long day и 13-битная квота; старые знания v2 и сессии v1;
временный warp 0…500, сохранение и затухание на END каждого 2000-го тика.
CELESTIALSCANNING проверяет родителя и сразу три платежа по 16 сырых наблюдений.

Первый прогон 94/96 выявил две ошибки фикстур: крыша проверялась до
распространения skylight, а старый seed после расширения пула перестал
достигать одного вдохновения. Свет проверяется после обновления; новый seed8
сохранил полный реальный start/draw/select/reload/finish. Production-правила
ради этих проверок не менялись. Исходный лог сохранён отдельно.

`runClient -PtheorySmokeTest`: **45 PNG**, 19 локальных имитаций запросов,
масштабированные клики, все ячейки, aids, предметные/XP-требования,
pending/ack, большие балансы и полные tooltip. Все 45 изображений просмотрены.
`runClient -PclientSmokeTest`: **23 PNG**, 156 архивных записей,
четыре имитации запросов и ноль запросов архива. Добавлены Celestial cost,
pending и complete; все итоговые 23 изображения просмотрены. Перед итоговым
прогоном allowlist обновлён для нового реализованного узла.

Ресурсы: **694 JSON**, все локальные ссылки моделей/текстур
и обе локализации; **1023 ресурсов JAR** побайтово совпали с исходниками.
Восемь исходных research JSON и 14 текстур небесных записей совпали с BETA26.
JAR: 3253628 байт, SHA-256 `b1bc55cd281e6d936431a180d32955d86ee2b0f50ad61eeb29ba7d4518fae9ff`.
Подробности и SHA каждого PNG — `validation/artifact-report-0.6.json`.
Итоговые логи: `verification-0.6.log`, `client-0.6.log`, `client-theory-0.6.log`.

Клиентские проверки используют подготовленные состояния без мира и сети.
Они не подтверждают длительное прохождение или два настоящих сетевых клиента.
Warp перенесён частично; полного Thaumcraft 6 ещё нет. Границы —
`CELESTIALSCANNING.md`, `THEORYCRAFTING.md`, `PORTING.md`.

# Проверка сборки 0.5.0-dev

Дата: 30 сентября 2026. Windows 11, Java 17.0.18, Gradle 8.8,
Minecraft 1.20.1, Forge 47.4.10; русский клиент 1600×1000, GUI 2/3/4.

`gradlew.bat build runGameTestServer` — **71/71 обязательных GameTests**:
54 прежних, 16 проверок теорий и одна проверка исходного THEORYRESEARCH.
Проверены полный цикл start/draw/select/reload/finish через одно вдохновение,
частичные предложения, конечный пул Bookshelf, точный расход бумаги/чернил,
сохранение RNG/целей/истории/владельца/ревизии, повторные и закрытые запросы,
атомарный отказ при недостатке чернил/наблюдений и переполнении наград,
вдохновение по metadata, отрицательная стоимость, проценты/порядок/штрафы.
Подтверждены сохранение израсходованного набора, обычный result take при
перезаправке с NBT, превращение стола через игровой useItemOn и отмена Forge
placement, блокировка кликов другого владельца, извлечение/перезаправка/возврат
своих расходников без сброса сессии, однократный дроп при разрушении без награды.
THEORYRESEARCH требует обоих фактов крафта, пропускает заключительный текст
и не выдаёт повторный XP по старой стадии.

`runClient -PtheorySmokeTest` — **21 PNG**, русский стол при GUI 2/3/4:
создание, две/три карты, завершение, нет чернил/бумаги, ожидаемый ответ.
Проверены настоящие масштабированные клики кнопок и попадание во все ячейки,
расходники, pending/acknowledgement, повторный клик, владение и ревизия;
выполнены восемь локальных имитаций запросов. Все 21 итоговых снимка просмотрены.
После первого просмотра исправлены поля бумаги и контраст кнопок; итоговый
повторный прогон прошёл. Разрешение 1600×1000 и масштабы 2/3/4 применены реально.

`runClient -PclientSmokeTest` — **20 PNG**, 156 записей архива, три
имитированных запроса, ноль запросов архива. Сохранены проверки начальных
стадий, наблюдений, задержанного ответа, поиска/перетаскивания/масштаба,
совместимости практических уроков; текущие снимки просмотрены на регрессии.

Оба клиентских стенда работают без мира с подготовленным NBT, не отправляют
реальные пакеты и не подтверждают два одновременно подключённых клиента.
Игровые операции покрыты серверными GameTests; длительное прохождение
человеком, все 271 разворота и исходный клиент 1.12.2 не запускались этим этапом.

`python scripts/validate_resources.py` — 680 JSON, модели/текстуры,
EN/RU и ресурсы книги. 995 ресурсов JAR побайтово совпадают с проектом;
восемь JSON исследований и 11 текстур стола/набора/GUI — с официальным BETA26.
В финальных клиентских логах нет ошибок ресурсов Thaumcraft. Предупреждения
Forge userdev, sound/shader/Realms тестового профиля остаются; сервер также
сообщает о смене версии и переназначении ID в старом тестовом мире.

Границы: шесть исходных записей, девять из 33 зарегистрированных карточек
и Bookshelf из 14 видов помощи. Особенность Analyze сверена с байткодом;
исправления и адаптации сохранений/GUI явно описаны в THEORYCRAFTING.md.
Полный мод, остальные устройства, сетевой мультiplayer, все сторонние моды
и конвертация миров 1.12.2 не заявляются готовыми.

JAR SHA-256: `64b3bdd8fecdced26c5cd977457c4cc199244c7cf4c22a90839ac46bf0c4bd49`.
Логи и снимки: `validation/verification-0.5.log`, `validation/client-0.5.log`,
`validation/client-theory-0.5.log`, `validation/screenshots-0.5/`.
Машинный отчёт: `validation/artifact-report-0.5.json`.
Ниже сохранены отчёты предыдущих версий.

# Проверка сборки 0.4.0-dev

Дата: 30 сентября 2026. Windows 11, Java 17.0.18, Gradle 8.8,
Minecraft 1.20.1, Forge 47.4.10; клиент 1600×1000, RTX 3090, русский язык.

`gradlew.bat build runGameTestServer` — **54/54 обязательных GameTests**.
Все 34 прежних проверки сохранены. Добавлены 20 проверок:

| Группа | Число | Подтверждённое поведение |
| --- | ---: | --- |
| Модель знаний | 6 | NBT v2/миграция, остатки после расхода и перезапуска, уникальность кредитов, веса категорий, неверные данные и переполнение |
| Игровые события | 8 | Настоящий vanilla result take, частичный/отменённый подбор, журнал один раз, оплаченный arcane craft, Salis через игровой ПКМ, отмена placement/spawn, сохранение соседнего факела, владелец бросившего катализатор |
| Начальная прогрессия | 6 | Стадии/родители, craft proof вместо наличия, атомарный расход нескольких категорий, XP/siblings, Nitor@3 без тупика, ожидаемая стадия/replay, кодирование пакета, книга в руке, запрет legacy-обхода алументума |

`gradlew.bat runClient -PclientSmokeTest` — **THAUMCRAFT_CLIENT_SMOKE_OK**:
20 PNG, 156 архивных записей, три имитированных запроса, ноль запросов архива.
Проверены закрытые категории, текущие стадии, стоимость и недостаток знаний,
двойной клик и задержанный ответ, сохранение карты, поиск и возврат.
Масштабы GUI 2/3/4 реально применены. Скриншоты сохранены в
`validation/screenshots-0.4/`; визуальная проверка охватывает новые состояния
и исходный архив. Recipe GUI остаётся справочным списком, не интерактивным рецептом.

Клиентский стенд работает без мира с подготовленным NBT и локальным получателем
запросов. Реальные пакеты им не отправляются: оплата и проверки контекста покрыты
серверными GameTests, но два запущенных сетевых клиента здесь не проверялись.
Все 271 разворота исходных стадий и длительное прохождение человеком не проверены.

`python scripts/validate_resources.py` — 667 валидных JSON, локальные модели/
текстуры, EN/RU, 167 сопоставлений иконок, 192 размерных текстуры, 12 GUI,
10 ссылок IMG. В финальном клиентском логе нет ошибок ресурсов Thaumcraft;
предупреждения Forge userdev, Minecraft sound/shader и Realms тестового профиля остаются.

Границы: реализована только начальная цепь пяти записей. Исследовательский стол,
теории, остальные исследования и основные непортированные системы не заявляются
проверенными. Старые PORT-рецепты сохраняют доступ без фиктивного завершения TC6.
Никакой конвертации миров 1.12.2, совместимости со всеми модами или полного
прохождения в обычном лаунчере этот отчёт не подтверждает.

JAR SHA-256: `152663d955dd8d0e276f084884d939faa3267fcf855b2df1088dca1db42b0596`.
Логи: `validation/verification-0.4.log`, `validation/client-0.4.log`.
Полная схема и ограничения: [RESEARCH-PROGRESSION.md](RESEARCH-PROGRESSION.md).

Ниже сохранены отчёты предыдущих версий.

# Проверка сборки 0.3.0-dev

Дата: 30 сентября 2026. Java 17.0.18, Gradle 8.8, Forge 47.4.10,
Minecraft 1.20.1, Windows 11, RTX 3090.

- `build runGameTestServer`: **34/34 GameTests**. Два новых теста проверяют
  сохранение исходной структуры графа, разделение стадий/дополнений и отсутствие
  выдачи архивных исследований при разрешении связей для отображения.
- `build runClient -PclientSmokeTest`: успешный запуск нового GUI.
  Проверены реальные обработчики клика узла, перетаскивания, колеса масштаба,
  Esc/ПКМ и поиска. Возврат из книги сохраняет тот же экземпляр и положение карты.
- Клиент открыл первые развороты **156 записей**: 148 исходных и 8 практических.
  Это проверка загрузки и отрисовки, а не прохождение исходных механик.
- Сохранены и просмотрены 10 PNG. Проверены исходные фоны и формы узлов,
  бумажный разворот, русская локализация, иллюстрация янтарной руды, поиск.
  При 1600×1000 масштабы GUI 2, 3 и 4 действительно применяются.
  Предварительная проверка 1280×720 также прошла; там запрос GUI 4 ограничен Minecraft до 3.
- `python scripts/validate_resources.py`: валидны 667 JSON, локальные модели,
  текстуры, оба перевода и ресурсы книги.
- В клиентском логе не найдено ошибок загрузки ресурсов Thaumcraft или
  предупреждений о неразрешённых иконках. Остались стандартные предупреждения
  Forge userdev, Minecraft sound/shader и авторизации Realms тестового пользователя.

Скриншоты новой книги: `validation/screenshots-0.3/`.
Логи: `validation/client-0.3.log`, `validation/verification-0.3.log`.
Происхождение иконок и 212 импортированных файлов: `validation/research-visuals-report.json`.

Клиентская проверка работает без мира на подготовленном состоянии.
Кнопка завершения урока намеренно не отправлялась в этой проверке; серверный
запрет открытия архива проверен GameTests. Не проверены все развороты каждой
стадии, полное прохождение человеком, длительная игра и multiplayer.
Полноценные условия TC6, расход знаний, recipe GUI и 3D-иконки отсутствующих
устройств остаются работой следующих этапов; подробности в [THAUMONOMICON.md](THAUMONOMICON.md).

Ниже сохранён отчёт предыдущей версии для остальных систем.


# Проверка сборки 0.2.0-dev

Дата: 29 сентября 2026. Окружение: Windows 11 x64, Temurin JDK 17.0.18,
Gradle 8.8, ForgeGradle 6.0.54, Minecraft 1.20.1, Forge 47.4.10.
Клиент: NVIDIA RTX 3090, разрешение 1280×720, масштаб GUI 2, русский язык.

## Серверная проверка

`gradlew.bat build runGameTestServer` — успешная сборка и **32 из 32**
обязательных GameTests. Тесты работают в настоящем серверном мире Forge userdev.

| Группа | Тестов | Что проверено |
| --- | ---: | --- |
| Базовые ресурсы | 3 | NBT аспектов, упаковка/распаковка металлов, дроп и теги блоков |
| Знания и исследования | 3 | Сохранение по UUID, уникальность сканов, условия уроков, запрет открытия архива |
| Сканирование | 6 | Явные аспекты, вывод из рецептов, циклические рецепты, защита от повторов через NBT, серверный луч |
| Аура и подземный мир | 5 | Сохранение ауры, ограниченное списание/simulate, лунная регенерация, руды и кристаллы |
| Деревья | 5 | Рост greatwood 2×2, восстановление саженцев при препятствиях, silverwood, дроп/decay, восстановление vis |
| Алхимия | 4 | Разные кристаллы и возврат инструментов Salis, тигель/flux/сохранение, алументум |
| Регрессии тигля | 2 | Полное ведро не растворяется в полном тигле; готовый результат не поглощается повторно |
| Магический верстак | 4 | Атомарная оплата, устаревшие меню двух игроков, Shift-клик и нехватка места, сохранение/дроп, сеть рецептов |

Проверка верстака охватывает серверные операции меню, включая расход vis и
кристаллов. Это не сетевой тест двух запущенных клиентов.
Обычная задача Gradle `test` не содержит JUnit-тестов; GameTests запускаются
отдельной задачей `runGameTestServer`.

## Клиент и ресурсы

`gradlew.bat runClient -PclientSmokeTest` завершился успешно с маркером
`THAUMCRAFT_CLIENT_SMOKE_OK`. Скриншоты просмотрены: русский текст Таумономикона,
архив и практическая алхимия, предметы и модели всех зарегистрированных
предметов мода, интерфейс магического верстака.

Исправлена загрузка старых папок `textures/items` и `textures/blocks` через
атлас 1.20.1. В итоговом клиентском логе нет сообщений о пропавших текстурах
или моделях Thaumcraft. Остаются стандартные предупреждения Forge userdev,
звуков козы, шейдера Minecraft и авторизации Realms тестового пользователя.

Визуальная проверка использует подготовленные состояния без загруженного мира.
У верстака вызываются настоящие init/render через пассивную оболочку: клики,
игрок, пакеты и взаимодействие с блоком этим скриншотом не проверяются.
Три кадра книги, две страницы предметов и кадр верстака сохранены
в `validation/screenshots`. Дополнительная пустая страница ресурсов в
тестовом запуске не включена в отчёт.

`python scripts/validate_resources.py` — успешно проверены 666 JSON-файлов, локальные
ссылки моделей/текстур, старые каталоги атласа и переводы зарегистрированных
предметов/блоков на русский и английский.

## Границы проверки

Не подтверждены: полное прохождение выживания человеком, работа JAR в обычном
лаунчере, совместимость с другими модами, долгий multiplayer, визуал деревьев
в ландшафте и брони на игроке. Автоматическая генерация и взаимодействия
проверялись в GameTests. Сохранения 1.12.2 не поддерживаются.

Архив 148 исследований не означает перенос их механик. Инфузия, производство
и транспорт эссенции, фокусы, големы, warp, theorycrafting, большинство существ
и устройств ещё не реализованы и не заявляются проверенными.

Логи: `validation/verification-0.2.log`, `validation/client-0.2.log`.
