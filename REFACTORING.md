# План рефакторинга operator-android

Ветка: `refactoring` (создана от `mes-mobile-integration`, `master` отстаёт на 70 коммитов).
Пути ниже относительно `app/src/main/java/`, если не указано иное.
`SOS` = `com/rabbitmes/mobile/ui/operations/SimpleOperationScreen.kt`, `VM` = `com/rabbitmes/mobile/MobileMesViewModel.kt`.

## Как устроено сейчас (коротко)

- **Живое приложение — стек `com.rabbitmes.mobile`.** `MainActivity.kt:42-46` делает `setContent { ProfikrolTheme { RabbitMesApp(vm) } }`. Вся навигация — `var screen: AppScreen` в VM и большой `when` в `RabbitMesApp.kt:87-176`.
- **Стек `ru.profikrol.operator` — наполовину брошенное первое поколение.** Из него живы только data/DI/network-слой (Retrofit API, DTO, `SessionStore`, `OfflineRepository`, `NfcReader`, `AuthRepository`), тема `uikit/theme`, `AuthScreen` и `RabbitProfileScreen`. Остальное недостижимо (см. п. 2).
- **Слоёв почти нет: «god ViewModel + API».** `MobileMesViewModel` (2522 строки) сам ходит в 6 Retrofit API, держит весь UI-стейт, навигацию, офлайн-очередь, NFC-сканирование, маппинг DTO, валидацию и формирование JSON-результатов. Репозитория задач и use-case слоя нет.
- **Тестов практически нет:** 8 осмысленных юнит-тестов (маппинг ошибок и `RabbitProfileMapper`), VM, офлайн-очередь, refresh токена и gRPC не покрыты.

---

## Приоритет 0 — исправить до любого рефакторинга (баги и риски в проде)

Это не «красота кода», а места, которые уже сейчас могут ломать работу или безопасность. Мелкие точечные правки.

1. **Захардкоженные адреса серверов и незакоммиченный `10.0.2.2`.** `ru/profikrol/operator/core/di/NetworkModule.kt:36-38` сейчас указывает на эмулятор (`http://10.0.2.2:5216`), в HEAD были боевые IP. Fallback-API указывает туда же, куда основной. gRPC-хост лежит отдельно в `app/build.gradle.kts` (`NOTIFICATIONS_GRPC_HOST`). Если случайно закоммитить, сборка уйдёт с адресом эмулятора.
   → Вынести все URL в `buildConfigField` по buildType/flavor (`dev`/`prod`), убрать из Kotlin-констант.
2. **Боевые экраны работают на моках.**
   - `ru/profikrol/operator/core/di/RabbitModule.kt:17` биндит `InMemoryRabbitRepository` → профиль кролика (`RabbitProfileScreen`) показывает выдуманные данные с искусственными задержками, хотя `RabbitApi` есть.
   - `MockRepository` — источник истины для определений операций (`MockRepository.operation(type).fields/targetType`, VM:135, 369, 1396), списка сотрудников (VM:596, 629-633), поиска кролика/клетки по RFID (VM:1647-1648) и ~19 мест в UI (SOS:730, 752, 773, 775, 832, 1012, 2035, 2089-2093, `RabbitMesApp.kt:167`). Например, в `scanRfidAndCompleteItem` целевой id может взяться из мок-кролика.
   - После логина реальный пользователь «натягивается» на мок-сотрудника по роли (VM:726-748).
   → Разделить `MockRepository` на (а) статический справочник операций (`OperationCatalog`, это не мок) и (б) мок-данные, которые выпилить из прод-путей.
3. **`runCatching` вокруг suspend-вызовов глотает `CancellationException`** (40 вхождений в VM, напр. `loadMyTasks` VM:878-1084, `executeOfflineAction` VM:1247-1321). При уходе с экрана/отмене корутины код продолжает работать и пишет в стейт. → Хелпер `suspendRunCatching`, который пробрасывает отмену.
4. **Сомнительное сопоставление RFID в `scanRfidAndCompleteItem`** (VM:1594-1763):
   - `?: pendingServerRabbits.singleOrNull()` (VM:1635) — сам VM при одном оставшемся кролике закроет его на **любой** RFID. В осеменении и пальпации это сейчас не срабатывает: экраны показывают кнопку только если RFID нашёлся в чек-листе (`SpecificOperationScreens.kt:76-86, 133, 225-235, 268`). Но защита держится только на UI: универсальная панель `SimpleScanPanel` (SOS:1582-1597) такой проверки не делает, и любой новый экран с кроличьими целями унаследует дыру. Проверку надо перенести в VM/домен.
   - Сравнение `item.label.contains(rfid)` (VM:1632 и те же места в UI) даёт ложные совпадения по подстроке: короткий или частичный RFID совпадёт с чужой подписью «RFID: …».
   - Пять вариантов «RFID совпадает с пунктом» скопированы в VM и ещё в 3–5 местах UI (`SpecificOperationScreens.kt:58-66, 79-86, 228-235`, SOS:753-757, 1541-1544).
   → Одна доменная функция `ChecklistItem.matchesRfid()` + явное правило, когда допустим fallback.
5. **`tasks.first { it.id == taskId }`** (VM:1597, 2272, 2423 и ещё 1) и `rabbits.first()` (`RabbitMesApp.kt:166`) — падение, если задача исчезла после фонового обновления (автообновление каждые 30 с). → `taskOrNull` + ранний выход.
6. **Причина проблемы подменяется комментарием:** `ChecklistExecutionBlock` передаёт комментарий и как reason, и как comment (`OperationCommon.kt:454`), так же в `InseminationScreen` (`SpecificOperationScreens.kt:90, 170`). На сервер уходит неверная причина.
7. **Безопасность:**
   - Trust-all `X509TrustManager` + `HostnameVerifier { true }` в DEBUG — в двух местах (`NetworkModule.kt:154-169`, `com/rabbitmes/mobile/data/NotificationRepository.kt:133-139, 225`).
   - `usesCleartextTraffic="true"`, `allowBackup="true"` в манифесте.
   - Access/refresh токены в обычных `SharedPreferences` (`ru/profikrol/operator/data/local/SessionStore.kt:24, 38-39`) → `EncryptedSharedPreferences`/DataStore + исключить из бэкапа.
   - HTTP-логирование включено и в release (`NetworkModule.kt:51-56`).
   - `DatabaseModule.kt:22-25`: `fallbackToDestructiveMigration()` — при смене схемы Room молча сотрёт офлайн-очередь с неотправленными действиями. Для офлайн-first приложения это потеря данных оператора.

## Приоритет 1 — удалить мёртвый код (быстро, безопасно, сразу минус тысячи строк)

Делается первым из рефакторинга, потому что сильно сужает поле работ и убирает путаницу «какой из двух `WeighingScreen` настоящий». Всё ниже проверено поиском ссылок по `app/src`.

- **Весь параллельный стек `ru.profikrol.operator`, кроме инфраструктуры:**
  - `navigation/AppNavGraph.kt` (`AppNavGraph()` нигде не вызывается) и `navigation/Routes.kt`;
  - `feature/home`, `moving`, `nestalignment`, `nestpreparation` (вместе с Hilt-модулем), `notifications` (две VM, одна не используется даже внутри пакета), `rabbitculling` (вместе с модулем), `rfidinstallation`, `rfidscan`, `rfidscanresult`, `settings`, `weighing`;
  - `core/role/*`;
  - `data/repository/FakeAuthRepository.kt`;
  - неиспользуемые компоненты `uikit/components` (`ActionCard`, `MainTopBar`, `IconBox`, `ConfirmationDialog`, `OutlinedButton`, `PlaceholderScreen`, `RabbitSummaryCard`, `StatusBanner`).
  - Оставить: `feature/auth`, `feature/rabbitprofile`, `data/*`, `domain/*`, `core/di`, `uikit/theme`, `PrimaryButton`, `AppTopBar`, `ActionButton`.
- **Мёртвые экраны в живом стеке:**
  - SOS: `AnimalSettlementScreen` (1230-1388), `NestControlRoundScreen` (1391-1485), все ветки `useGeneralTemplate` (флаг `USE_GENERAL_TEMPLATE_FOR_ALL_OPERATIONS = false`, SOS:39 и VM:79, 2274), неиспользуемые параметры `onChecklistDone`, `onSkip`, `onGeneralComplete`, `SimpleScanPanel.onOpenAnimal`.
  - `SpecificOperationScreens.kt`: `WeighingScreen` (310), `CageOperationScreen` (433), `NestPreparationScreen` (491), `HangarGenericOperationScreen` (528), `LightAutomationTaskScreen` (537), `FeedOperationScreen` (547), ветка `SHOW_BLUETOOTH_SCALE_BUTTON` (15, 401-406).
  - `OperationCommon.kt`: `ScanPanel` (246), `RabbitMiniCard` (358), `GenericFields` (531), `ProblemAndMediaControls` (482).
  - `AdditionalProductionScreens.kt`: `ProductionTargetPage` (392).
  - `ui/components/Common.kt`: `PriorityBadge` (259), `operationAccent` (269), `MetricTile` (286).
  - `com/rabbitmes/mobile/ui/theme/Theme.kt`: алиас `RabbitMesTheme`.
  - `AppScreen.Map` / `AppScreen.AnimalHistory` — в них никто не навигирует, `HangarMapScreen`/`AnimalHistoryScreen` недостижимы.
  - Ветка `mock-` в `RabbitMesApp.kt:88, 94, 104` и `isProductionTaskId` (VM:83-86): мок-задачи в `vm.tasks` сейчас не загружаются, это остатки демо-режима.
  - В VM: `val completesWithoutPayload = false` (VM:2024) и все ветки от неё, отладочный лог `RFID_TEST` (VM:1595).
- **Гигиена репо:** закоммиченные `outputs/*.pptx` (в т.ч. lock-файл `~$…pptx`), добавить `.kotlin/` в `.gitignore`; убрать `navigation-compose` и `espresso-core`, если навигацию не переводим (см. П3).

## Приоритет 2 — разрезать `MobileMesViewModel` (главная боль)

Почему второй, а не первый: резать VM без тестов страшно, а после П0/П1 видно, что реально живое. Порядок ниже — от самого безопасного (чистые функции) к самому рискованному (стейт).

**2.1. Вынести чистые функции из файла (без изменения поведения).** Верхние ~570 строк VM — это не ViewModel, а маппинг и утилиты:
- маппинг DTO → домен: `WorkTaskDto.toMobileTask` (128-205), `ProductionTaskDetailsDto.toMobileTask` (386-443), `toRabbitChecklist`/`toCageChecklist` (445-475), статусы (477-498) → `data/mapper/TaskMappers.kt`;
- определение типа операции: `resolveOperationType`, `OPERATION_ALIASES`, `GENERAL_FORM_OPERATION_TYPES` (209-299) → `domain/OperationResolver.kt`;
- ошибки: `toUserMessage`, `extractServerErrorMessage`, `toHttpDebugMessage` (505-572) → `core/error/ErrorMessages.kt` (тесты на них уже есть).
Сразу покрыть маппинг юнит-тестами — это самый дешёвый способ получить страховку перед следующими шагами.

**2.2. Ввести `TaskRepository` вместо прямых вызовов API из VM.**
- `loadMyTasks` (866-1091) — 225 строк в одной функции: production-задачи, work-задачи, дедупликация, решение «нужны ли кролики/клетки», пагинация, мердж с локальным стейтом, переход на экран приёмки. Списки типов операций, для которых нужны кролики/клетки (945-980), захардкожены — должны быть свойством операции в каталоге.
- `productionCall` с fallback по 404 (660-671) — внутрь репозитория.
- После каждой операции VM вручную перезагружает задачу `getTask(...).toMobileTask(...)` — 11 копий. → `repository.refreshTask(id)`.
- Обработка 409 «уже выполнено» скопирована 8 раз (VM:1255-1320, 1988-2000, 2150-2160 и др.) → одна функция `completeIdempotent { }`.

**2.3. Выделить офлайн-синхронизацию в `SyncManager`.** `syncNow`, `restoreOfflineState`, `persistTasks`, `enqueueOffline`, `executeOfflineAction` (1176-1322). Сейчас каждое действие в VM имеет две ветки — `if (!shift.isOnline) enqueue... else launchServerAction...` (13 проверок `shift.isOnline`), и одна и та же бизнес-логика пишется дважды (например, автозакрытие задачи после последней цели: офлайн VM:2079-2083, онлайн VM:2139-2147), так что правки легко внести только в одну ветку. Правильнее: любое действие → в очередь → `SyncManager` отправляет сразу, если онлайн. Один путь вместо двух. Это ещё и уберёт `persistTasks()`, который запускает несинхронизированные корутины записи (возможна гонка снимков).

**2.4. Выделить исполнение операций.** `scanRfidAndCompleteItem` (1594-1763), `completeChecklistItemOnServer` (1952-2240), `addMortalityRoundProblem` (1794-1950), `completeTask` (2271-2418). Здесь `when` по типу операции для построения JSON результата (VM:2026-2054) и правила валидации (RFID обязателен, возраст > 0). → Стратегия на тип операции: `OperationHandler { validate(values); buildResult(item, values) }` в домене. Тогда добавление новой операции не требует правок в VM.

**2.5. Разделить VM по экранам.** После 2.1-2.4 в VM останется координация. Разбить на `SessionViewModel` (логин, смена, профиль), `TasksViewModel` (список, автообновление), `TaskExecutionViewModel` (одна задача, RFID/NFC), `AcceptanceViewModel`, `NotificationsViewModel`. Состояние — `StateFlow<UiState>` вместо россыпи `mutableStateOf`/`mutableStateMapOf` (17 полей стейта, VM:594-637).

## Приоритет 3 — UI-слой операций

**3.1. Общая «обвязка» экрана.** Шапка задачи (← Назад, бейдж приоритета, «время · N мин», заголовок, прогресс, «Приступить») скопирована 6 раз в SOS (141-199, 324-350, 479-505, 785-811, 1047-1073, 1260-1300), хотя есть готовый `UnifiedTaskHeader` (`OperationCommon.kt:43-102`) и каркас `ProductionPage` (`AdditionalProductionScreens.kt:53-81`). Аналогично блок «проблема + причина + комментарий + вложения» — 4 реализации (SOS:1860, SOS:835-880, `OperationCommon.kt:482`, `SpecificOperationScreens.kt:138-162`). Бейдж приоритета — 3 копии, Card/Button/Metric/Empty — по 2-3 копии. → Пакет `ui/operations/components`.

**3.2. Разрезать `SimpleOperationScreen.kt` (2093 строки)** на файлы по экрану: `NestAlignmentScreen`, `AnimalTransferScreen`, `MortalityRoundScreen`, `AnimalSettlementScreen`, `GenericChecklistScreen`. Диспетчеризацию свести в один `when` (сейчас она размазана между `OperationScreenFactory`, `SpecificOperationScreens.kt:558-594`, с 24 параметрами в одну строку, и вторым ветвлением внутри SOS:65-84). Колбэки упаковать в интерфейс `OperationActions` / sealed `OperationEvent` вместо 21-24 лямбд и `onMortalityRoundProblem` с 8 позиционными параметрами.

**3.3. Убрать бизнес-логику из composable'ов.** Валидация в `onClick` (SOS:578-596, 899-921, 1186-1203, 1577-1597, 1696-1722), правило «нет воды = проблема» (SOS:114-121), списки причин проблем (SOS:2049-2084), RFID-матчинг в `LaunchedEffect` (SOS:750-769), «успех» по подсчёту DONE-пунктов (SOS:311-317, `SpecificOperationScreens.kt:436-449`) → в доменные `OperationHandler` из 2.4 / state holder экрана. Состояние форм хранить через `rememberSaveable` или в VM (сейчас теряется при повороте); `remember` без ключа в SOS:1406-1408 протекает между задачами.

**3.4. Навигация.** Заменить `vm.screen` + ручной back stack (`RabbitMesApp.kt:66-81`) на Navigation Compose (зависимость уже есть). Заодно убрать копии вызова `TaskListScreen(...)` с одинаковыми 7 аргументами (`RabbitMesApp.kt:89, 94, 104`) и бизнес-правило `canEdit` из навигации (106-109). Кнопки «Взвешивание/Перемещение/Выбраковка» в профиле кролика сейчас просто возвращают назад (171-174) — решить, нужны ли они.

## Приоритет 4 — унификация и гигиена (можно постепенно)

- **Цвета и тема:** ~80 литералов `Color(0x…)` в `ui/`, палитра переопределена 4 раза (SOS:32-38, `OperationCommon.kt:38-40`, `AdditionalProductionScreens.kt:46-50`, `ShiftScreen.kt:25-26`) → токены в `uikit/theme`.
- **Строки:** все тексты — литералы в коде, нет `stringResource`. Особенно важно для списков причин/опций, которые по смыслу должны приходить с сервера (в UI даже висит заметка «После публикации API списки будут загружаться с сервера», SOS:1307).
- **Дублирующиеся модели:** `RoleId` vs `UserRole`, `Employee` vs `User`, два `Rabbit`, два `NotificationUi`. Плюс цикл зависимостей: `ru/.../data/local/offline/OfflineRepository.kt:3-4` импортирует `com.rabbitmes.mobile.domain.MobileTask`. Свести пакеты в одну структуру `data / domain / ui`.
- **Магические строки:** `"production-target"` (9 раз), `"production-checklist"`, `completedAt = "now"` (12 раз — пишется буквально строка `"now"` вместо времени), ключи `"_resultJson"`, `"_noPayload"` → enum/константы, реальный timestamp.
- **Логи:** 28 `Log.*` в VM с разными тегами (`RabbitApi`, `RFID_SETTLEMENT`, `RFID_TEST`), часть с RFID и id в release → Timber + выключение в release.
- **Сборка:** `isMinifyEnabled = false`, нет `isShrinkResources` (тянется весь `material-icons-extended`), нет signingConfig и flavors, Java 11 → 17. Подключить detekt/ktlint, чтобы новый код не расползался обратно.

---

## Рекомендуемый порядок PR-ов

1. **П0** — точечные фиксы (URL в buildConfig, `CancellationException`, `tasks.first`, reason/comment, RFID fallback). Маленькие PR, по одному на пункт.
2. **П1** — удаление мёртвого кода одним-двумя PR (отдельно `ru`-стек и отдельно мёртвые экраны в `com`).
3. **2.1** + тесты на маппинг — первая страховочная сетка.
4. **2.2 → 2.3 → 2.4** — репозиторий, синхронизация, обработчики операций. После каждого шага ручной прогон основных операций на стенде (заселение, осеменение, пальпация, обход падежа, взвешивание, приёмка).
5. **3.1 → 3.2 → 3.3** — UI операций.
6. **2.5 + 3.4** — разделение VM по экранам и навигация (идут вместе).
7. **П4** — по ходу или фоном.

Скорее всего, после П1 и 2.4 станет видно ещё больше лишней логики: часть веток в VM существует только ради моков и демо-режима.
