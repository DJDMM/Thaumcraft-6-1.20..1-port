# Мехи и клиент адской печи: BETA26

Эталон — выпущенный Thaumcraft 6.1.BETA26, JAR SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Читаемый источник закреплён на `954022bb777b7546281fb36df8522f0ba6b43f81`.
Проверены `TileBellows`, `BlockBellows`, `ModelBellows`,
`TileBellowsRenderer`, `ProxyTESR`, `TileCrucible` и блоковые ресурсы.
Спорные условия отдельно сверены `javap -c -p` по JAR;
порядок построения UV проверен по Minecraft 1.12.2 `ModelRenderer`/`ModelBox`.
Ни механики TC4/TC5, ни поведение дополнений не используются.

## Серверные мехи

`thaumcraft:bellows` теперь имеет настоящий `BellowsBlockEntity`.
Сохраняются исходные шесть направлений, wood, hardness1 и эффективная
взрывостойкость12: исходный BlockTCTile устанавливает resistance20, которое
Minecraft1.12 преобразует через множитель3 и делитель5.
Размещение смотрит в противоположную сторону нажатой грани; redstone
выключает `enabled`. Ранее реализованные потребители — эссенциеплавильня
и буфер труб — сохраняют свои правила подсчёта и обработки.

Каждый **второй включённый серверный тик** мехи проверяют непосредственно
соседнюю обычную furnace по своему направлению. При `cookTime>0 && cookTime<199`
увеличивается ровно cooking progress на1. Не изменяются топливо, вход,
выход, recipe time или число результатов. Это именно FurnaceBlockEntity,
а не современные smoker/blast-furnace.

В BETA26 верхний предел буквально199. Он не зависит от времени рецепта.
Перенос сохраняет этот предел даже для более коротких современных рецептов;
само завершение остаётся у native furnace. Один access-transformer делает
доступным только `AbstractFurnaceBlockEntity.f_58318_` / `cookingProgress`.
Mixin, поиск поля на каждом тике и дополнительный обработчик переплавки
не вводятся.

При отключении не сбрасывается текущая половина двухтикового цикла.
Исходные delay, direction, inflation и firstRun не записывались в NBT:
после восстановления delay0 и inflation1. Проверка существующего физического
BE, актуального блока и загруженного соседнего чанка — современные меры
против работы удалённого устройства и принудительной загрузки.

Проверка released `TileCrucible` показала, что его поле `bellows` только
инициализируется в конструкторе. Нет чтения поля или вызова TileBellows.
Поэтому ускорение нагрева тигля **не добавлено**: в BETA26 оно не работало.
Адская печь имеет отдельный исходный подсчёт на расстоянии2, кроме UP,
по фактическому redstone, с максимумом4; контракт сервера находится в
`INFERNAL-FURNACE-BETA26-AUDIT.md`.

## Живая модель и звук

Включённая клиентская tile начинает с `inflation=.35+random*.55`.
Сжимается на.075, затем при достижении нижней границы меняет направление
и расширяется на.025 до1. В том же тике смены направления выполняется
исходный дополнительный шаг расширения. После полного расширения звучит
native GHAST_SHOOT, громкость.01 и pitch `.5+(random-random)*.2`.
Отключённая tile сохраняет положение, а не возвращается в декоративную позу.

Renderer переносит пять ModelBellows cuboids, исходные сдвиги/масштаб мешка
и верхней/нижней досок, а также все шесть матриц ориентации. При соседнем
buffer рисуются два исходных ModelBoreBase nozzle cuboids с оригинальной
текстурой `bore.png` и матрицей направления. Render bounds расширены на.3.
Все материалы остаются имеющимися byte-identical оригинальными PNG.

Две особенности старого построения ModelBox важны для фактического вида:
`addBox` создаёт UV и mirror сразу, до поздних присваиваний constructor.
Поэтому у Bag используются UV128×128, хотя после addBox исходник вызывает
setTextureSize64×32. Присваивание mirror=true после addBox также не зеркалит
уже созданные грани; перенос не создаёт такого отсутствующего эффекта.

`ProxyTESR` привязывает только world tile renderer. Обычный inventory в JAR
использует статический `bellows` JSON с `forge:default-block`.
Неиспользуемая ветвь `TileBellowsRenderer(null)` не превращена в новую
анимацию инвентаря. Предмет сохраняет уже перенесённую исходную модель.

Адская печь использует исходную трёхблочную block geometry и PNG;
восстановлен исходный CUTOUT_MIPPED через model render_type.
Её client event3 испускает пять native Lava particles по исходному
FXDispatcher.furnaceLavaFx и проигрывает пять native LAVA_POP с исходными
громкостью/pitch. Это не новый beam, огонь-заглушка или декоративная анимация.

Отдельная исходная ветвь `polluteAura(...,1,true)` передаёт PacketFXPollute
игрокам в строгой сфере32 от целочисленных координат источника в этой
размерности. Она не смешана с event3. Перенесённый S2C создаёт настоящий
frame56 из имеющейся1024×1024 `particles.png`, grid16: положение
`pos+.2+random*.6`, vx/z `(random-random)*.005`, vy.02, lifetime100…159,
RGB1/.3/.9, alpha.5→0, scale2→5, layer1/обычное alpha blending/depth test
без записи depth, slowDown1 и native collision/light. Particle и packet
не изменяют ауру или платёж; звука у этой ветви в released code нет.

Wind.001 умножается на исходный вектор длиной.1: постоянное ускорение
X/Z равно.0001×cos/sin угла `moonPhase*(40+nextInt10)` градусов.
Rotation setter сначала переводит±1 в радианы, после чего update снова
умножает на2π: сохранён исходный шаг±.109662rad за тик. Ключевые кадры
alpha/scale используют целочисленный age, а angle extrapolates
currentAngle+step×partialTick. Новый native collision backend и ограничение
4096 живых puffs — явные современные меры; источник PNG не перерисован.

## Проверки и границы

Добавлены семь серверных GameTests: реальные tile/шесть направлений/статы;
точная двухтиковая мутация только CookTime; ноль/199 и короткий recipe;
заморозка delay при redstone; неправильное направление и отказ smoker/blast;
транзитный NBT/удалённая tile; native150 world ticks с двумя реально
оплаченными vanilla furnaces, из которых ускоренная заканчивает раньше.

Opt-in `thaumcraft.infernalSmokeTest` живёт только на Dist.CLIENT.
Драйвер создаёт отдельный Survival-мир и15 обычных сцен:

- Настоящий IS_NETHER biome и ожидание original200 Living ticks для
  m_hellandback; actual native dimension transport — явная транспортная
  fixture, строительство/использование портала не проверяется.
- Реальный Thaumometer C2S scan held Blaze Rod для Ignis.
- C2S исследования INFERNALFURNACE/BELLOWS с оригинальными предметами,
  Observation16 ARTIFICE +16 GOLEMANCY и заработанными20XP.
- Четыре paid arcane bench crafts: по25vis/Aer1/четыре planks,
  две leather и iron ingot; final Bellows не выдаются fixture.
- Native CraftingMenu payment Salis: redstone +три разных crystals;
  исходные flint/bowl возвращаются. Его final dust не выдаётся fixture.
- Исходная raw26part конструкция; C2S Salis formation, четыре survival
  placement, native vanilla furnace GUI и реальное ускорение.
- Raw32RawGold на полублочной поверхности: ordinary ItemEntity collision,
  world countdown/recipe, physical ingots/bonuses/XP, vis и загрязнение.
  RawGold — явная адаптация получения из современной обычной добычи,
  а не старый creative ore. Сервер отдельно проверяет старую таблицу бонусов.
- Scene11 снимается во время действительно полученного/нарисованного
  purple-puff, ещё до завершения переплавки; строгие32output/32XP/40vis
  проверки затем выполняются в Scene12 до настоящего подбора.
  ServerRandomseed `0x54433633` — явная fixture, устанавливаемая один раз
  до физического raw-input. После начала production random/aura не
  сбрасываются, pollute/send/receive/draw не вызываются driver.
  Если исходные32 trials не дают события, QA завершается явной ошибкой
  с реальными counters и seed; отсутствие дыма не объявляется успехом.
- Подбор настоящих результатов, native chest transfers, redstone pause
  и survival shell harvest с обратной сборкой исходных блоков.

Arena/bedrock, родительские исследования, raw observations/materials,
native consumer furnace/chest/bench и исходные блоки конструкции — явные
fixtures. 80vis/high-flux9chunk aura устанавливается до существования core:
native formation затем заряжает20speedy за20vis уже в простое, без входа.
Последующие32 переплавки тратят этот бюджет и получают ещё20 за20vis;
ожидаются40vis суммарно и8 оставшихся speedy. Между этими операциями driver
не сбрасывает ни бюджет, ни ауру. High-flux aura препятствует регенерации для измеримого vis;
она не меняет runtime aura или правила Rift. Driver не вызывает напрямую
runtime furnace ticks/queue/eject/formation и не заменяет outputs или XP.
Read-only XP observer учитывает native сохранённый `Count` merged-orb,
умноженный на его `Value`; чтение одного Value занижает реальное владение.
Отдельно требуется настоящий player pickup:32 заработанных XP переплавки
и итоговое totalExperience52 вместе с20XP исследований.
Сохранение/проверка физической руки предотвращает потерю оплаченных предметов
при постановке следующей сцены.

Этот контракт описывает реализацию и подготовку QA. Фактические native exit,
число пройденных сцен, измеренные бонусы/Flux, SHA и просмотр PNG записываются
только после состоявшегося запуска в `VALIDATION.md` и artifact-report0.33.
Наличие кодовых тестов не является утверждением, что они уже прошли.
Многопользовательская игра, полный survival, все углы/skin и аудио не заявлены.
Клиент запускается только проверенным hidden non-input desktop launcher;
переключение рабочего стола, global input и manual saves запрещены.

## Фактически завершённый QA0.33

Server4 1283/1283 и infernal-033-client4 15/15, оба native exit0. Все15 полныхPNG и оба
превью просмотрены двумя проверяющими. Все4366 файлов финальных
server/client/build совпадают. Точные измерения/подготовленные
условия/ранние failures и ограничения сохранены в
[VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.33](../../validation/artifact-report-0.33.json).
