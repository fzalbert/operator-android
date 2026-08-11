package ru.profikrol.operator.data.remote.cell

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface CellApi {
    @GET("api/v1/cells")
    suspend fun getCells(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
    ): CellPageDto
}

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
) {
    val displayName: String
        get() = "ID $id · ряд $rowId · клетка $number · ${type.orEmpty()}".trimEnd(' ', '·')
}
