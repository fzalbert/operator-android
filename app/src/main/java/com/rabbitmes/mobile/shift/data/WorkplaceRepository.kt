package com.rabbitmes.mobile.shift.data

import com.rabbitmes.mobile.session.ShiftRepository
import com.rabbitmes.mobile.shift.model.AssignedTask
import com.rabbitmes.mobile.shift.model.Hangar
import com.rabbitmes.mobile.shift.model.Workshop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Цеха оператора с ангарами. */
interface WorkplaceRepository {
    val workshops: Flow<List<Workshop>>

    suspend fun refresh()
}

/** Цеха из профиля сотрудника (`GET profile/me`, поле `manufactures`). */
class ProfileWorkplaceRepository @Inject constructor(
    private val shiftRepository: ShiftRepository,
) : WorkplaceRepository {
    override val workshops: Flow<List<Workshop>> = shiftRepository.manufactures.map { manufactures ->
        manufactures.map { manufacture ->
            Workshop(
                id = manufacture.id.toString(),
                name = manufacture.name,
                hangars = manufacture.hangars.map { Hangar(id = it.id.toString(), name = it.name) },
            )
        }
    }

    override suspend fun refresh() {
        shiftRepository.refreshFromProfile()
    }
}

/** Моковые данные для превью. */
class FakeWorkplaceRepository(count: Int = 3) : WorkplaceRepository {
    override val workshops: Flow<List<Workshop>> = MutableStateFlow(fakeWorkshops(count))

    override suspend fun refresh() = Unit

    companion object {
        fun fakeWorkshops(count: Int): List<Workshop> = List(count) { index ->
            Workshop(
                id = "${index + 1}",
                name = "Цех ${index + 1}",
                hangars = listOf(index * 2 + 1, index * 2 + 2).map { number ->
                    Hangar(id = "$number", name = "Ангар $number")
                },
            )
        }

        fun fakeTask(hangar: Hangar, overdue: Boolean = false) = AssignedTask(
            id = "task-feeding",
            title = "Кормление",
            hangarName = hangar.name,
            dueTime = "11:00",
            overdueDate = if (overdue) LocalDate.now().minusDays(2) else null,
        )
    }
}
