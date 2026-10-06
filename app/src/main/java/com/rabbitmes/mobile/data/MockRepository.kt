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

    fun rabbitByRfid(rfid: String) = rabbits.firstOrNull { it.rfid.equals(rfid, ignoreCase = true) }
    fun cageByRfid(rfid: String) = allCages.firstOrNull { it.rfid.equals(rfid, ignoreCase = true) }
    fun rabbit(id: String) = rabbits.firstOrNull { it.id == id }
    fun cage(id: String) = allCages.firstOrNull { it.id == id }

}
