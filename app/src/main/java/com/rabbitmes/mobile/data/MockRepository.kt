package com.rabbitmes.mobile.data

import com.rabbitmes.mobile.domain.*

object MockRepository {
    val employees = listOf(
        Employee("emp-1", "Иван Петров", RoleId.OPERATOR, listOf("ws-1"), "ИП"),
        Employee("emp-2", "Анна Соколова", RoleId.CHIEF_TECHNOLOGIST, listOf("ws-1"), "АС"),
        Employee("emp-4", "Михаил Орлов", RoleId.CHIEF_MECHANIC, listOf("ws-1"), "МО")
    )

    private fun cages(row: Int, hangarPrefix: String): List<Cage> = (1..18).map { n ->
        Cage("$hangarPrefix-r$row-c$n", row, n, "Р$row-К$n", "CAGE-$hangarPrefix-$row-${n.toString().padStart(2, '0')}", hasNest = n % 3 != 0, occupied = n % 5 != 0)
    }

    val workshop = Workshop(
        "ws-1", "Цех №1", listOf(
            Hangar("h-1", "Ангар А", listOf(CageRow("h1-r1", 1, cages(1, "A")), CageRow("h1-r2", 2, cages(2, "A")), CageRow("h1-r3", 3, cages(3, "A")))),
            Hangar("h-2", "Ангар Б", listOf(CageRow("h2-r1", 1, cages(1, "B")), CageRow("h2-r2", 2, cages(2, "B"))))
        )
    )

    val allCages: List<Cage> = workshop.hangars.flatMap { it.rows }.flatMap { it.cages }

    val rabbits: List<Rabbit> = allCages.filter { it.occupied }.take(60).mapIndexed { index, cage ->
        Rabbit(
            id = "rabbit-${index + 1}",
            rfid = "RFID-${(100000 + index).toString()}",
            earNumber = "F-${(4500 + index)}",
            cageId = cage.id,
            sex = if (index % 4 == 0) "Самец" else "Самка",
            ageDays = 160 + (index * 7) % 340,
            lastWeightKg = 3.1 + (index % 17) * 0.12,
            lastInseminationDaysAgo = if (index % 4 == 0) null else 20 + index % 40,
            lastPalpation = if (index % 3 == 0) "Сукрольная" else "Не проверялась",
            lactationStatus = if (index % 5 == 0) "Требует контроля" else "Норма",
            healthStatus = if (index % 11 == 0) "Наблюдение" else "Норма"
        )
    }

    val operationDefinitions: List<OperationDefinition> = listOf(
        OperationDefinition(OperationType.INSEMINATION, TargetType.RABBIT, true, "Осеменить", listOf(
            OperationField("rfid", "RFID самки", FieldType.TEXT, true), OperationField("inseminated", "Самка осеменена", FieldType.BOOLEAN, true), OperationField("seedBatch", "Партия семени", FieldType.TEXT, true, placeholder = "Например: S-26-07"), OperationField("comment", "Комментарий", FieldType.TEXT)
        ), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.PALPATION, TargetType.RABBIT, true, "Зафиксировать пальпацию", listOf(
            OperationField("rfid", "RFID самки", FieldType.TEXT, true), OperationField("result", "Результат", FieldType.SELECT, true, options = listOf("Сукрольная", "Не сукрольная", "Сомнительно")), OperationField("comment", "Комментарий", FieldType.TEXT)
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.WEIGHING, TargetType.CAGE, false, "Сохранить вес", listOf(
            OperationField("cageNumber", "Номер клетки", FieldType.NUMBER, true), OperationField("weightGrams", "Вес", FieldType.NUMBER, true, "г"), OperationField("photo", "Фото весов", FieldType.PHOTO)
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.NEST_PREPARATION, TargetType.CAGE, false, "Гнездо подготовлено", listOf(
            OperationField("nestReady", "Гнездо готово", FieldType.BOOLEAN, true)
        ), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.NEST_CONTROL, TargetType.CAGE, true, "Сохранить контроль", listOf(
            OperationField("cageRfid", "RFID клетки", FieldType.TEXT, true), OperationField("fed", "Сытых", FieldType.NUMBER, true), OperationField("hungry", "Голодных", FieldType.NUMBER, true), OperationField("dead", "Мертвых", FieldType.NUMBER, true), OperationField("nestState", "Состояние гнезда", FieldType.SELECT, true, options = listOf("Норма", "Мокрое", "Мало стружки", "Нужно вмешательство"))
        ), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.NEST_SELECTION, TargetType.CAGE, true, "Выравнивание выполнено", listOf(
            OperationField("cageRfid", "RFID клетки", FieldType.TEXT, true), OperationField("movedCount", "Переложено крольчат", FieldType.NUMBER, true), OperationField("reason", "Причина", FieldType.SELECT, true, options = listOf("Много в гнезде", "Мало в гнезде", "Голодные", "Слабые"))
        ), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST), true),
        OperationDefinition(OperationType.ANIMAL_TRANSFER, TargetType.RABBIT, true, "Перевести", listOf(OperationField("rfid", "RFID", FieldType.TEXT, true), OperationField("toCage", "Куда переведен", FieldType.TEXT, true)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.ANIMAL_SETTLEMENT, TargetType.CAGE, true, "Заселить", listOf(OperationField("cageRfid", "RFID клетки", FieldType.TEXT, true), OperationField("animalCount", "Количество животных", FieldType.NUMBER, true)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.OKROL, TargetType.CAGE, true, "Окрол учтен", listOf(OperationField("cageRfid", "RFID клетки", FieldType.TEXT, true), OperationField("bornAlive", "Живых", FieldType.NUMBER, true), OperationField("bornDead", "Мертвых", FieldType.NUMBER, true)), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.LACTATION_CONTROL, TargetType.CAGE, true, "Лактация проверена", listOf(OperationField("cageRfid", "RFID клетки", FieldType.TEXT, true), OperationField("status", "Статус", FieldType.SELECT, true, options = listOf("Норма", "Недостаточно молока", "Нужна подсадка", "Нужен технолог"))), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.LIGHT_STIMULATION, TargetType.HANGAR, false, "Уставка применена", listOf(OperationField("lightHours", "Длительность светового дня", FieldType.HOURS, true, "ч"), OperationField("mode", "Режим", FieldType.SELECT, true, options = listOf("База 14:00", "Стимуляция 22:00"))), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.FEED_CHECK, TargetType.HANGAR, false, "Корм проверен", listOf(OperationField("feedType", "Тип корма", FieldType.FEED_TYPE, true, options = listOf("Откорм", "Отъем", "Лактация")), OperationField("feedAvailable", "Корм есть", FieldType.BOOLEAN, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.WATER_CHECK, TargetType.HANGAR, false, "Вода проверена", listOf(OperationField("pressure", "Давление", FieldType.SELECT, true, options = listOf("Норма", "Слабое", "Нет воды"))), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.LIGHTING_CHECK, TargetType.HANGAR, false, "Свет проверен", listOf(OperationField("allLamps", "Все лампы горят", FieldType.BOOLEAN, true), OperationField("lightHours", "Фактический световой день", FieldType.HOURS, true, "ч"), OperationField("broken", "Перегоревшие лампы", FieldType.NUMBER)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.MORTALITY_ROUND, TargetType.HANGAR, false, "Обход завершен", listOf(OperationField("deadCount", "Падеж", FieldType.NUMBER, true), OperationField("ammonia", "NH₃", FieldType.NUMBER, false, "ppm")), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.MORTALITY_JOURNAL, TargetType.HANGAR, false, "Запись сохранена", listOf(OperationField("journalEntry", "Запись в журнал", FieldType.TEXT, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.CLEANING, TargetType.HANGAR, false, "Уборка выполнена", listOf(OperationField("passesSwept", "Проходы подметены", FieldType.BOOLEAN, true), OperationField("corpseFridge", "Падеж убран в холодильник", FieldType.BOOLEAN)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.DAILY_CLEANING, TargetType.HANGAR, false, "Уборка завершена", listOf(OperationField("passesSwept", "Проходы подметены", FieldType.BOOLEAN, true), OperationField("photo", "Фото после уборки", FieldType.PHOTO)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.WASHING, TargetType.HANGAR, false, "Мойка завершена", listOf(OperationField("foam", "Пена нанесена", FieldType.BOOLEAN, true), OperationField("washed", "Смыто водой", FieldType.BOOLEAN, true), OperationField("photoAfter", "Фото после", FieldType.PHOTO)), listOf(RoleId.GENERAL_WORKER), true),
        OperationDefinition(OperationType.DISINFECTION, TargetType.HANGAR, false, "Дезинфекция завершена", listOf(OperationField("chemical", "Препарат", FieldType.TEXT, true), OperationField("concentration", "Концентрация", FieldType.TEXT, true), OperationField("exposure", "Экспозиция", FieldType.NUMBER, true, "мин")), listOf(RoleId.GENERAL_WORKER, RoleId.CHIEF_MECHANIC), true),
        OperationDefinition(OperationType.HANGAR_ACCEPTANCE, TargetType.HANGAR, false, "Ангар принят", listOf(OperationField("accepted", "Ангар готов", FieldType.BOOLEAN, true), OperationField("comment", "Комментарий", FieldType.TEXT)), listOf(RoleId.CHIEF_TECHNOLOGIST), false),
        OperationDefinition(OperationType.MANUAL_FEEDING, TargetType.HANGAR, false, "Кормление выполнено", listOf(OperationField("feedType", "Тип корма", FieldType.FEED_TYPE, true, options = listOf("Откорм", "Отъем", "Лактация")), OperationField("amount", "Количество", FieldType.NUMBER, true, "кг")), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.SECOND_ROUND, TargetType.HANGAR, false, "Второй обход завершен", listOf(OperationField("behavior", "Поведение", FieldType.SELECT, true, options = listOf("Норма", "Не едят", "Не пьют", "Выделения")), OperationField("ammonia", "NH₃", FieldType.NUMBER, true, "ppm")), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.FINAL_ROUND, TargetType.HANGAR, false, "Финальный обход завершен", listOf(OperationField("journal", "Замечания в журнал", FieldType.TEXT, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.OKROL_PREPARATION, TargetType.HANGAR, false, "Подготовка завершена", listOf(OperationField("materials", "Материалы готовы", FieldType.BOOLEAN, true), OperationField("nests", "Гнезда готовы", FieldType.BOOLEAN, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.ANIMAL_DEPARTURE, TargetType.HANGAR, false, "Выбытие учтено", listOf(OperationField("count", "Количество", FieldType.NUMBER, true), OperationField("reason", "Причина", FieldType.SELECT, true, options = listOf("Падеж", "Выбраковка", "Перемещение"))), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.WEANING, TargetType.HANGAR, false, "Отъем завершен", listOf(OperationField("youngCount", "Количество молодняка", FieldType.NUMBER, true)), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.SLAUGHTER_SHIPMENT, TargetType.HANGAR, false, "Отгрузка завершена", listOf(OperationField("count", "Количество", FieldType.NUMBER, true), OperationField("weightTotal", "Общий вес", FieldType.NUMBER, true, "кг")), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.FEMALE_DELIVERY, TargetType.RABBIT, true, "Самки заведены", listOf(OperationField("rfid", "RFID", FieldType.TEXT, true), OperationField("source", "Поставщик/группа", FieldType.TEXT)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.DEWORMING_DOSATRON, TargetType.HANGAR, false, "Дозатрон запущен", listOf(OperationField("drug", "Препарат", FieldType.TEXT, true), OperationField("dosage", "Дозировка", FieldType.TEXT, true)), listOf(RoleId.OPERATOR, RoleId.CHIEF_MECHANIC))
    )

    fun operation(type: OperationType) = operationDefinitions.first { it.type == type }
    fun rabbitByRfid(rfid: String) = rabbits.firstOrNull { it.rfid.equals(rfid, ignoreCase = true) }
    fun cageByRfid(rfid: String) = allCages.firstOrNull { it.rfid.equals(rfid, ignoreCase = true) }
    fun rabbit(id: String) = rabbits.firstOrNull { it.id == id }
    fun cage(id: String) = allCages.firstOrNull { it.id == id }

    private fun rabbitChecklist(prefix: String, count: Int = 12) = rabbits.take(count).mapIndexed { index, rabbit -> ChecklistItem("$prefix-r-${index + 1}", "${rabbit.earNumber} · ${rabbit.rfid}", TargetType.RABBIT, rabbit.id) }
    private fun cageChecklist(prefix: String, count: Int = 18) = allCages.take(count).mapIndexed { index, cage -> ChecklistItem("$prefix-c-${index + 1}", "${cage.code} · ${cage.rfid}", TargetType.CAGE, cage.id) }
    private fun cageNumberChecklist(prefix: String, count: Int = 18) = allCages.take(count).mapIndexed { index, cage -> ChecklistItem("$prefix-c-${index + 1}", "Клетка ${cage.code}", TargetType.CAGE, cage.id) }
    private fun weighingChecklist(count: Int = 4) = allCages.take(count).mapIndexed { index, cage ->
        ChecklistItem("weight-c-${index + 1}", "Ряд ${cage.rowNumber} · клетка ${cage.number}", TargetType.CAGE, cage.id)
    }

    fun initialTasks(): List<MobileTask> = listOf(
        MobileTask("task-1", "Осеменение самок", OperationType.INSEMINATION, "ws-1", "h-1", "emp-1", "2026-07-09", "08:30", 180, Priority.URGENT, TaskStatus.NEW, rabbitChecklist("ins", 4), true, RoleId.CHIEF_TECHNOLOGIST, AcceptanceStatus.NOT_REQUIRED),
        MobileTask("task-2", "Взвешивание контрольной группы", OperationType.WEIGHING, "ws-1", "h-1", "emp-1", "2026-07-09", "11:00", 90, Priority.HIGH, TaskStatus.NEW, weighingChecklist(), false),
        MobileTask("task-3", "Подготовка гнезд", OperationType.NEST_PREPARATION, "ws-1", "h-1", "emp-1", "2026-07-09", "13:00", 120, Priority.HIGH, TaskStatus.NEW, cageNumberChecklist("nestprep", 18), true, RoleId.CHIEF_TECHNOLOGIST, AcceptanceStatus.NOT_REQUIRED),
        MobileTask("task-4", "Контроль гнезд", OperationType.NEST_CONTROL, "ws-1", "h-1", "emp-1", "2026-07-09", "15:00", 150, Priority.NORMAL, TaskStatus.NEW, cageChecklist("nestctl", 12), true, RoleId.CHIEF_TECHNOLOGIST, AcceptanceStatus.NOT_REQUIRED),
        MobileTask("task-5", "Проверка светового режима", OperationType.LIGHTING_CHECK, "ws-1", "h-1", "emp-1", "2026-07-09", "06:05", 20, Priority.NORMAL, TaskStatus.NEW, emptyList(), false),
        MobileTask("task-6", "Мойка ангара после цикла", OperationType.WASHING, "ws-1", "h-2", "emp-3", "2026-07-09", "09:00", 240, Priority.HIGH, TaskStatus.DONE, emptyList(), true, RoleId.CHIEF_TECHNOLOGIST, AcceptanceStatus.WAITING),
        MobileTask("task-7", "Дезинфекция ангара", OperationType.DISINFECTION, "ws-1", "h-2", "emp-3", "2026-07-09", "14:00", 180, Priority.NORMAL, TaskStatus.NEW, emptyList(), true, RoleId.CHIEF_TECHNOLOGIST, AcceptanceStatus.NOT_REQUIRED),
        MobileTask("task-8", "Приемка ангара", OperationType.HANGAR_ACCEPTANCE, "ws-1", "h-2", "emp-2", "2026-07-09", "17:00", 60, Priority.HIGH, TaskStatus.NEW, emptyList(), false),
        MobileTask("task-9", "Пальпация", OperationType.PALPATION, "ws-1", "h-1", "emp-1", "2026-07-09", "16:00", 120, Priority.NORMAL, TaskStatus.NEW, rabbitChecklist("pal", 10), false),
        MobileTask("task-10", "Выравнивание гнезд", OperationType.NEST_SELECTION, "ws-1", "h-1", "emp-1", "2026-07-09", "16:40", 90, Priority.HIGH, TaskStatus.NEW, cageChecklist("sel", 10), true, RoleId.CHIEF_TECHNOLOGIST, AcceptanceStatus.NOT_REQUIRED),
        MobileTask("task-11", "Проверка корма", OperationType.FEED_CHECK, "ws-1", "h-1", "emp-1", "2026-07-09", "07:30", 20, Priority.NORMAL, TaskStatus.NEW, emptyList(), false),
        MobileTask("task-12", "Проверка воды", OperationType.WATER_CHECK, "ws-1", "h-1", "emp-1", "2026-07-09", "07:40", 20, Priority.NORMAL, TaskStatus.NEW, emptyList(), false)
    )
}
