package ru.profikrol.operator.data.remote.cell

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CellApi {
    @GET("api/v1/cells")
    suspend fun getCells(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
    ): CellPageDto

    @GET("api/v1/cells/by-hangar/{hangarId}")
    suspend fun getCellsByHangar(@Path("hangarId") hangarId: Long): List<CellDto>

    @GET("api/v1/cells/by-hangar/{hangarId}/info")
    suspend fun getCellsInfoByHangar(@Path("hangarId") hangarId: Long): List<CellDto>

    @GET("api/v1/rows/hangar/{hangarId}")
    suspend fun getRowsByHangar(@Path("hangarId") hangarId: Long): List<RowDto>
}

@Serializable
data class RowDto(
    val id: Long? = null,
    val hangarId: Long,
    val number: Int,
)

@Serializable
data class CellPageDto(
    val items: List<CellDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 0,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
)

@Serializable
data class CellDto(
    val id: Long,
    val rowId: Long,
    val number: Int,
    val type: String? = null,
    val meatRabbitsCount: Int = 0,
    val weightRabbits: Int? = null,
) {
    val displayName: String
        get() = "ID $id · ряд $rowId · клетка $number · ${type.toCellPositionLabel()}".trimEnd(' ', '·')
}

fun String?.toCellPositionLabel(): String = when (this?.trim()?.uppercase()) {
    "TOP_RIGHT" -> "ВЕРХНИЙ_ПРАВЫЙ"
    "TOP_LEFT" -> "ВЕРХНИЙ_ЛЕВЫЙ"
    "BOTTOM_RIGHT" -> "НИЖНИЙ_ПРАВЫЙ"
    "BOTTOM_LEFT" -> "НИЖНИЙ_ЛЕВЫЙ"
    else -> this.orEmpty()
}

fun String.localizeCellPositions(): String =
    replace("TOP_RIGHT", "ВЕРХНИЙ_ПРАВЫЙ", ignoreCase = true)
        .replace("TOP_LEFT", "ВЕРХНИЙ_ЛЕВЫЙ", ignoreCase = true)
        .replace("BOTTOM_RIGHT", "НИЖНИЙ_ПРАВЫЙ", ignoreCase = true)
        .replace("BOTTOM_LEFT", "НИЖНИЙ_ЛЕВЫЙ", ignoreCase = true)
