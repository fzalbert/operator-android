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
            OperationField("rfid", "RFID самки", FieldType.TEXT, true),
            OperationField("inseminated", "Самка осеменена", FieldType.BOOLEAN, true),
            OperationField("comment", "Комментарий", FieldType.TEXT)
        ), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.PALPATION, TargetType.RABBIT, true, "Зафиксировать пальпацию", listOf(
            OperationField("rfid", "RFID самки", FieldType.TEXT, true), OperationField("result", "Результат", FieldType.SELECT, true, options = listOf("Сукрольная", "Не сукрольная", "Сомнительно")), OperationField("comment", "Комментарий", FieldType.TEXT)
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.WEIGHING, TargetType.CAGE, false, "Вес сохранён", listOf(
            OperationField("weightGrams", "Вес клетки", FieldType.NUMBER, true, "г"),
            OperationField("photo", "Фото весов", FieldType.PHOTO)
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.WEIGHING_CAGE, TargetType.CAGE, false, "Сохранить общий вес клетки", listOf(
            OperationField("totalWeightGrams", "Общий вес клетки", FieldType.NUMBER, true, "г"),
            OperationField("photo", "Фото показаний весов", FieldType.PHOTO)
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.WEIGHING_RABBIT, TargetType.RABBIT, false, "Сохранить вес кролика", listOf(
            OperationField("weightGrams", "Вес мясного кролика", FieldType.NUMBER, true, "г"),
            OperationField("photo", "Фото показаний весов", FieldType.PHOTO)
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.NEST_PREPARATION, TargetType.CAGE, false, "Гнездо подготовлено", listOf(
            OperationField("nestReady", "Гнездо готово", FieldType.BOOLEAN, true)
        ), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.NEST_CONTROL, TargetType.CAGE, false, "Добавить замечание", listOf(
            OperationField("issue", "Проблема в клетке", FieldType.SELECT, true, options = listOf("Мертвые крольчата", "Голодные крольчата", "Мокрое гнездо", "Мало подстилки", "Требуется вмешательство", "Другая проблема")),
            OperationField("count", "Количество", FieldType.NUMBER),
            OperationField("comment", "Комментарий", FieldType.TEXT)
        ), listOf(RoleId.OPERATOR), true),
        OperationDefinition(
            OperationType.CUSTOM_TASK,
            TargetType.HANGAR,
            false,
            "Выполнить пункт",
            emptyList(),
            listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST, RoleId.GENERAL_WORKER),
        ),
        OperationDefinition(OperationType.NEST_SELECTION, TargetType.CAGE, false, "Сохранить по клетке", listOf(
            OperationField("alive", "Живые", FieldType.NUMBER, true),
            OperationField("stillborn", "Мертворождённые", FieldType.NUMBER, true),
            OperationField("removed", "Забрали", FieldType.NUMBER, true),
            OperationField("added", "Добавили", FieldType.NUMBER, true)
        ), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST), true),
        OperationDefinition(OperationType.ANIMAL_TRANSFER, TargetType.HANGAR, true, "Переселить", listOf(OperationField("rfid", "RFID", FieldType.TEXT, true), OperationField("cellId", "Клетка назначения", FieldType.TEXT, true), OperationField("comment", "Комментарий", FieldType.TEXT)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.ANIMAL_SETTLEMENT, TargetType.RABBIT, true, "Выполнить", emptyList(), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.OKROL, TargetType.CAGE, false, "Окрол учтен", listOf(OperationField("cellId", "Клетка", FieldType.SELECT, true), OperationField("bornAlive", "Живых", FieldType.NUMBER, true), OperationField("bornDead", "Мертвых", FieldType.NUMBER, true)), listOf(RoleId.OPERATOR), true),
        OperationDefinition(OperationType.LACTATION_CONTROL, TargetType.CAGE, false, "Лактация проверена", listOf(OperationField("cellId", "Клетка", FieldType.SELECT, true), OperationField("status", "Статус", FieldType.SELECT, true, options = listOf("Норма", "Недостаточно молока", "Нужна подсадка", "Нужен технолог"))), listOf(RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.LIGHT_STIMULATION, TargetType.HANGAR, false, "Уставка применена", listOf(OperationField("lightHours", "Длительность светового дня", FieldType.HOURS, true, "ч"), OperationField("mode", "Режим", FieldType.SELECT, true, options = listOf("База 14:00", "Стимуляция 22:00"))), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.FEED_CHECK, TargetType.HANGAR, false, "Корм проверен", listOf(OperationField("feedType", "Тип корма", FieldType.FEED_TYPE, true, options = listOf("Откорм", "Отъем", "Лактация")), OperationField("feedAvailable", "Корм есть", FieldType.BOOLEAN, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.WATER_CHECK, TargetType.ROW, false, "Сохранить проверку", listOf(
            OperationField("waterStatus", "Наличие воды", FieldType.SELECT, true, options = listOf("Вода есть", "Нет воды"))
        ), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.LIGHTING_CHECK, TargetType.HANGAR, false, "Свет проверен", listOf(OperationField("allLamps", "Все лампы горят", FieldType.BOOLEAN, true), OperationField("lightHours", "Фактический световой день", FieldType.HOURS, true, "ч"), OperationField("broken", "Перегоревшие лампы", FieldType.NUMBER)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.MORTALITY_ROUND, TargetType.HANGAR, false, "Обход завершен", listOf(OperationField("deadCount", "Падеж", FieldType.NUMBER, true), OperationField("ammonia", "NH₃", FieldType.NUMBER, false, "ppm")), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.MORTALITY_JOURNAL, TargetType.HANGAR, false, "Запись сохранена", listOf(OperationField("journalEntry", "Запись в журнал", FieldType.TEXT, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.CLEANING, TargetType.HANGAR, false, "Уборка выполнена", emptyList(), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.DAILY_CLEANING, TargetType.HANGAR, false, "Уборка завершена", listOf(OperationField("passesSwept", "Проходы подметены", FieldType.BOOLEAN, true), OperationField("photo", "Фото после уборки", FieldType.PHOTO)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.WASHING, TargetType.HANGAR, false, "Мойка завершена", listOf(OperationField("foam", "Пена нанесена", FieldType.BOOLEAN, true), OperationField("washed", "Смыто водой", FieldType.BOOLEAN, true), OperationField("photoAfter", "Фото после", FieldType.PHOTO)), listOf(RoleId.GENERAL_WORKER), true),
        OperationDefinition(OperationType.DISINFECTION, TargetType.HANGAR, false, "Дезинфекция завершена", listOf(OperationField("chemical", "Препарат", FieldType.TEXT, true), OperationField("concentration", "Концентрация", FieldType.TEXT, true), OperationField("exposure", "Экспозиция", FieldType.NUMBER, true, "мин")), listOf(RoleId.GENERAL_WORKER, RoleId.CHIEF_MECHANIC), true),
        OperationDefinition(OperationType.HANGAR_ACCEPTANCE, TargetType.HANGAR, false, "Ангар принят", listOf(OperationField("accepted", "Ангар готов", FieldType.BOOLEAN, true), OperationField("comment", "Комментарий", FieldType.TEXT)), listOf(RoleId.CHIEF_TECHNOLOGIST), false),
        OperationDefinition(OperationType.MANUAL_FEEDING, TargetType.HANGAR, false, "Кормление выполнено", listOf(OperationField("feedType", "Тип корма", FieldType.FEED_TYPE, true, options = listOf("Откорм", "Отъем", "Лактация")), OperationField("amount", "Количество", FieldType.NUMBER, true, "кг")), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.SECOND_ROUND, TargetType.HANGAR, false, "Второй обход завершен", listOf(OperationField("behavior", "Поведение", FieldType.SELECT, true, options = listOf("Норма", "Не едят", "Не пьют", "Выделения")), OperationField("ammonia", "NH₃", FieldType.NUMBER, true, "ppm")), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.FINAL_ROUND, TargetType.HANGAR, false, "Финальный обход завершен", listOf(OperationField("journal", "Замечания в журнал", FieldType.TEXT, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.OKROL_PREPARATION, TargetType.HANGAR, false, "Подготовка завершена", listOf(OperationField("materials", "Материалы готовы", FieldType.BOOLEAN, true), OperationField("nests", "Гнезда готовы", FieldType.BOOLEAN, true)), listOf(RoleId.OPERATOR)),
        OperationDefinition(OperationType.FIRST_WEIGHING, TargetType.HANGAR, false, "Первое взвешивание завершено", emptyList(), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.ANIMAL_DEPARTURE, TargetType.HANGAR, false, "Выбытие учтено", listOf(OperationField("count", "Количество", FieldType.NUMBER, true), OperationField("reason", "Причина", FieldType.SELECT, true, options = listOf("Падеж", "Выбраковка", "Перемещение"))), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.WEANING, TargetType.HANGAR, false, "Отъем завершен", listOf(OperationField("youngCount", "Количество молодняка", FieldType.NUMBER, true)), listOf(RoleId.OPERATOR, RoleId.CHIEF_TECHNOLOGIST)),
        OperationDefinition(OperationType.SLAUGHTER_SHIPMENT, TargetType.CAGE, false, "Забой завершён", listOf(OperationField("count", "Количество", FieldType.NUMBER, true)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.FEMALE_DELIVERY, TargetType.CAGE, false, "Заселить самку", listOf(OperationField("age", "Возраст, дней", FieldType.NUMBER, true), OperationField("femaleRfid", "RFID самки", FieldType.TEXT, true)), listOf(RoleId.OPERATOR, RoleId.GENERAL_WORKER)),
        OperationDefinition(OperationType.DEWORMING_DOSATRON, TargetType.HANGAR, false, "Дозатрон запущен", listOf(OperationField("drug", "Препарат", FieldType.TEXT, true), OperationField("dosage", "Дозировка", FieldType.TEXT, true)), listOf(RoleId.OPERATOR, RoleId.CHIEF_MECHANIC))
    )

    fun operation(type: OperationType) = operationDefinitions.first { it.type == type }
    fun rabbitByRfid(rfid: String) = rabbits.firstOrNull { it.rfid.equals(rfid, ignoreCase = true) }
    fun cageByRfid(rfid: String) = allCages.firstOrNull { it.rfid.equals(rfid, ignoreCase = true) }
    fun rabbit(id: String) = rabbits.firstOrNull { it.id == id }
    fun cage(id: String) = allCages.firstOrNull { it.id == id }

}
