# Эссенциальные зеркала и физическая оплата — 0.32

Основание: `TileMirrorEssentia` и `EssentiaHandler` закреплённого TC6
6.1.BETA26, сверенные с официальным JAR в
[аудите зеркал](MIRRORS-BETA26-AUDIT.md). SHA-256 JAR:
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Прежние правила расстояния, порядка, кэша и rollback — в
[airborne handler](AIRBORNE-TRANSFUSER-HANDLER-BETA26-AUDIT.md).

## Исходный API и поиск

`EssentiaMirrorBlockEntity` реализует только `IAspectSource`.
Нет внутреннего запаса, tube-портов, suction или нового GUI.
`getAspects()` возвращает null; bulk contain/take возвращают false,
`containerContains()` — 0, `setAspects()` ничего не меняет.
Одноаспектные add/take/find разрешают ровно одну положительную единицу.
Нулевые/отрицательные количества и неизвестные аспекты отвергаются.

Физическая взаимная пара открывает поиск около удалённого зеркала:
его facing, исходный направленный объём range8 и ext5. Удалённый проход
пропускает все эссенциальные зеркала: второго зеркального перехода нет.
Discovery зеркала также исключает кандидата, чья записанная ссылка указывает
назад на зеркало-потребитель. Ключ общего кэша остаётся world identity +
позиция, без аспекта, направления, range и режима операции.

Первый `canAccept` после discovery проверяет **наличие**, как выпущенный
бинарник `findEssentia`; следующий cached-вызов проверяет acceptance.
Это не проверка фактической свободной ёмкости. Add сохраняет два прохода
с приоритетом совместимых непустых банок, включая void-переполнение.
Исключение исходного первого прохода подтверждено JAR: `addEssentia`
вызывает acceptance на offsets184 и229. Если первое cold-query вернуло
false, второе уже cached-query может вернуть true и вставить через зеркало
до более поздней непустой местной банки. Следующая операция классифицирует
то же null-aspects зеркало как empty и откладывает его во второй проход.
Эта released-особенность сохранена отдельной проверкой; два вызова не
объединяются в новую, отличающуюся от оригинала ветку.
Метка ограничивает вставку; drain читает настоящее содержимое.
Скоба блокирует воздушный доступ. Пропавший ранний cached-source прерывает
проход; failed drain/add удаляет список и ставит общие десять секунд
настенного времени. Read-only find сохраняет положительный список.
Refresh не удаляет retry delay; deadline разрешает повтор при точном равенстве.

Успешный вызов добавляет **1 instability только вызываемому локальному
зеркалу**, без второго расхода на удалённом endpoint. Порог >64 и спад
счётчика обрабатывает общий native mirror ticker.

## Физические владельцы и современные guards

Обычный consumer выбирает локальное зеркало. Внутри одного `SourceCache`
guard маршрут разрешается в настоящее удалённое зеркало и одну native jar.
Рекурсивного входа в публичный airborne API нет. Зеркало никогда не владеет
выдуманной единицей эссенции: единственный debit/add выполняется на банке.
Оплаченная матрица и оба transfuser используют тот же путь, что локальные банки.

`DrainConfirmation` сохраняет consumer, логический local source, фактическую
удалённую leaf tile и оба ServerLevel. MirrorPair фиксирует физические
экземпляры, ссылки, reciprocal endpoints, revisions, facing и loaded identity.
Commit одноразовый; relink, поворот, замена, removed state, brace или lifecycle
делают прежний promise недействительным. Повторный вход из suction callback
отказывает без оплаты и изменения shared cache.

В paid-маршруте удалённые leaves ограничены native final
`EssentiaJarBlockEntity`, как прежний transactional transport. Сторонний
`IAspectSource` без rollback-контракта не получает гарантию удалённой оплаты.
Обычные local generic direct операции по-прежнему поддерживают интерфейс.
Read-only acceptance сохраняет исходный interface query, а реальная вставка
повторно подтверждает physical leaf, type, brace и capacity.

Отказ input/output, смена пары во время native buffer suction или поздняя
невалидность endpoint восстанавливают точные живые source/destination NBT.
Rollback никогда не воссоздаёт уничтоженную tile и не заменяет новую tile
старым объектом. Самоперенос одного physical endpoint в него же отвергается.
Разрушение физического мира произвольным внешним callback остаётся за пределами
контракта native endpoints; это не причина создавать запас внутри зеркала.

Доступ выполняется только на серверном потоке к уже присутствующим FULL
чанкам через `getChunkNow`. Недоступный мир или чанк не загружается ради
транспорта. Записанная link остаётся; отсутствующий endpoint не означает оплату.

## Эффекты и проверяемые границы

`Transfer` сохраняет `source/target/aspect/ext` и прежний четырёхаргументный
конструктор. Новый список `Leg` фиксирует dimension, source, target и ext
каждого местного перелёта. Для drain: remote jar → remote mirror (ext5),
затем local mirror → consumer (исходный ext); matrix target по-прежнему below.
Для add направления обращаются. `publish(ServerLevel, Transfer)` отправляет
местные native trails в записанных мирах только после принятого commit.
Другие, не-essentia эффекты матрицы не изменены.
Прямой mirror API публикует лишь удалённый leg, поскольку внешнего consumer нет.
Частицы являются визуальной адаптацией текущего airborne trail, не сущностью,
контейнером или вторичной оплатой.

Определены **22** `MirrorAirborneGameTests`: interface/no tank/bulk guards,
одна physical jar и два qualified FX legs, native matrix, все шесть facing,
brace/label, nonempty priority, cold acceptance, shared delay/deadline,
no extra hops, reciprocal relink, facing, leaf replacement/brace/lifecycle,
неизвестное измерение и незагруженный далёкий чанк, input/output conservation,
refusal rollback/retry, void overflow, relink внутри native buffer debit,
reentry из suction, off-thread guards и одинаковые координаты разных миров.
Прежняя проверка decorative mirror исправлена на native unlinked source,
который не может оплачивать consumer. Возвышенные временные арены и данные
tile восстанавливаются; загрузка remote chunk в cross-dimension тесте —
явно подготовленное условие, а не работа транспортного API.

Это определения и контракт, **не результаты прогона**. Полный свежий native
server, скрытый клиент и финальная сборка запускаются ведущим агентом.
Фактические counts, screenshots, fingerprints и пределы прохождения должны
появиться в VALIDATION.md только после завершения этих проверок.
