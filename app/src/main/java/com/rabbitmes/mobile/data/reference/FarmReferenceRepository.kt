package com.rabbitmes.mobile.data.reference

import com.rabbitmes.mobile.core.runCatchingCancellable
import ru.profikrol.operator.data.remote.cell.CellApi
import ru.profikrol.operator.data.remote.cell.CellDto
import ru.profikrol.operator.data.remote.cell.RowDto
import ru.profikrol.operator.data.remote.rabbit.RabbitApi
import ru.profikrol.operator.data.remote.rabbit.RabbitDto
import javax.inject.Inject
import javax.inject.Singleton

/** Справочники фермы: кролики, клетки и ряды. Клетки кэшируются по набору ангаров. */
@Singleton
class FarmReferenceRepository @Inject constructor(
    private val rabbitApi: RabbitApi,
    private val cellApi: CellApi,
) {
    var rabbits: List<RabbitDto> = emptyList()
        private set
    var cells: List<CellDto> = emptyList()
        private set
    var rows: List<RowDto> = emptyList()
        private set
    private var cellHangarIds: Set<Long> = emptySet()

    fun rabbitIdForRfid(rfid: String): String? {
        val value = rfid.trim()
        return rabbits.firstOrNull { it.rfid?.trim().equals(value, ignoreCase = true) }?.id?.toString()
    }

    suspend fun refreshRabbits() {
        rabbits = loadAllRabbits()
    }

    /** Возвращает клетки ангаров, загружая их, только если набор ангаров изменился. */
    suspend fun cellsFor(hangarIds: Set<Long>): List<CellDto> {
        if (cells.isNotEmpty() && cellHangarIds == hangarIds) return cells
        cells = loadCells(hangarIds)
        cellHangarIds = hangarIds
        return cells
    }

    suspend fun refreshRows(hangarIds: Set<Long>) {
        rows = hangarIds
            .flatMap { hangarId -> cellApi.getRowsByHangar(hangarId) }
            .distinctBy { Triple(it.id, it.hangarId, it.number) }
    }

    fun clear() {
        rabbits = emptyList()
        cells = emptyList()
        rows = emptyList()
        cellHangarIds = emptySet()
    }

    private suspend fun loadAllRabbits(): List<RabbitDto> {
        val result = mutableListOf<RabbitDto>()
        val knownKeys = mutableSetOf<String>()
        var page = 1
        while (true) {
            val batch = rabbitApi.getRabbits(page = page, pageSize = PAGE_SIZE)
            val newItems = batch.items.filter { rabbit ->
                val key = rabbit.id?.toString() ?: rabbit.rfid?.trim()?.lowercase().orEmpty()
                key.isNotBlank() && knownKeys.add(key)
            }
            result += newItems
            if (page >= batch.totalPages || newItems.isEmpty()) break
            page += 1
        }
        return result
    }

    private suspend fun loadAllCells(): List<CellDto> {
        val result = mutableListOf<CellDto>()
        val knownIds = mutableSetOf<Long>()
        var page = 1
        while (true) {
            val batch = cellApi.getCells(page = page, pageSize = PAGE_SIZE)
            val newItems = batch.items.filter { cell -> knownIds.add(cell.id) }
            result += newItems
            if (page >= batch.totalPages || newItems.isEmpty()) break
            page += 1
        }
        return result
    }

    private suspend fun loadCells(hangarIds: Set<Long>): List<CellDto> {
        if (hangarIds.isEmpty()) return loadAllCells()
        return hangarIds
            .flatMap { hangarId ->
                runCatchingCancellable { cellApi.getCellsInfoByHangar(hangarId) }
                    .getOrElse { cellApi.getCellsByHangar(hangarId) }
            }
            .distinctBy(CellDto::id)
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
