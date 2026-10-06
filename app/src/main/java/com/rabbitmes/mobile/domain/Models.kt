package com.rabbitmes.mobile.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class RoleId(val title: String) {
    OPERATOR("Оператор"), CHIEF_TECHNOLOGIST("Главный технолог"), CHIEF_MECHANIC("Главный механик"), GENERAL_WORKER("Разнорабочий")
}

@Serializable
enum class TaskStatus(val title: String) { NEW("Новая"), IN_PROGRESS("В работе"), BLOCKED("Проблема"), DONE("Выполнена"), SENT("Отправлена"), SKIPPED("Пропущена") }
@Serializable
enum class ChecklistStatus(val title: String) { PENDING("Ожидает"), DONE("Готово"), PROBLEM("Проблема"), SKIPPED("Пропущено") }
@Serializable
enum class ReviewStatus(val title: String) { NOT_REVIEWED("Не проверено"), ACCEPTED("Принято"), REJECTED("Замечание") }
@Serializable
enum class AcceptanceStatus(val title: String) { NOT_REQUIRED("Не требуется"), WAITING("Ожидает"), ACCEPTED("Принята"), REJECTED("Отклонена") }
@Serializable
enum class TargetType { RABBIT, CAGE, ROW, HANGAR }
@Serializable
enum class Priority(val title: String, val weight: Int) { URGENT("Срочно", 0), HIGH("Важно", 1), NORMAL("Планово", 2) }

/**
 * Операции, которые приложение умеет выполнять. [code] — код операции на бэке, тип задачи
 * определяется только по нему. Всё, чего здесь нет, выполняется общим обработчиком [GENERAL].
 */
@Serializable
enum class OperationType(val code: String?, val title: String) {
    MORTALITY_ROUND("mortality_round", "Обход ангара"),
    FEMALE_ARRIVAL("female_arrival", "Завоз самок"),
    INSEMINATION("insemination", "Осеменение"),
    PALPATION("palpation", "Пальпация"),
    NEST_PREPARATION("nest_preparation", "Подготовка гнезд"),
    NEST_EQUALIZATION("nest_equalization", "Выравнивание гнезд"),
    ANIMAL_SETTLEMENT("animal_settlement", "Переселение животных"),
    SLAUGHTER_SHIPPING("slaughter_shipping", "Забой"),
    WEIGHING_RABBIT("weighing_rabbit", "Взвешивание кролика"),
    WEIGHING_CAGE("weighing_cage", "Взвешивание клетки целиком"),
    GENERAL(null, "Задача");

    /** Цели задачи строит сервер на старте. У обхода их добавляет оператор, у общей задачи их может не быть. */
    val expectsTargets: Boolean get() = this != MORTALITY_ROUND && this != GENERAL

    companion object {
        fun fromCode(code: String?): OperationType =
            entries.firstOrNull { it.code != null && it.code == code?.trim() } ?: GENERAL
    }
}

@Serializable
data class Employee(val id: String, val fullName: String, val role: RoleId, val workshopIds: List<String>, val initials: String)
@Serializable
data class Workshop(val id: String, val name: String, val hangars: List<Hangar>)
@Serializable
data class Hangar(val id: String, val name: String, val rows: List<CageRow>)
@Serializable
data class CageRow(val id: String, val number: Int, val cages: List<Cage>)
@Serializable
data class Cage(val id: String, val rowNumber: Int, val number: Int, val code: String, val rfid: String, val hasNest: Boolean, val occupied: Boolean)
@Serializable
data class Rabbit(val id: String, val rfid: String, val earNumber: String, val cageId: String, val sex: String, val ageDays: Int, val lastWeightKg: Double, val lastInseminationDaysAgo: Int?, val lastPalpation: String?, val lactationStatus: String, val healthStatus: String)

@Serializable
enum class FieldType { TEXT, NUMBER, BOOLEAN, SELECT, PHOTO, VIDEO, FILE, TEMPERATURE, HOURS, FEED_TYPE }
@Serializable
enum class AttachmentType(val title: String, val emoji: String) {
    PHOTO("Фото", "📷"),
    VIDEO("Видео", "🎥"),
    FILE("Файл", "📎"),
}
@Serializable
data class MediaAttachment(val id: String, val type: AttachmentType, val name: String, val localUri: String, val createdAt: String, val uploaded: Boolean = false)
@Serializable
data class OperationField(val id: String, val title: String, val type: FieldType, val required: Boolean = false, val unit: String? = null, val options: List<String> = emptyList(), val placeholder: String = "")
@Serializable
data class OperationDefinition(val type: OperationType, val targetType: TargetType, val requiresScan: Boolean, val completionLabel: String, val fields: List<OperationField>)

@Serializable
data class ExecutionResult(
    val values: Map<String, String> = emptyMap(),
    val photos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val attachments: List<MediaAttachment> = emptyList(),
    val comment: String = "",
    val scannedRfid: String? = null,
    val completedAt: String? = null,
    val problemReason: String? = null
)

@Serializable
data class ChecklistItem(
    val id: String,
    val label: String,
    val targetType: TargetType,
    val targetId: String,
    val rabbitId: String? = null,
    val cageId: String? = null,
    val cageLabel: String? = null,
    val rabbitCount: Int? = null,
    val scanIdentifier: String? = null,
    val serverType: String = "",
    val status: ChecklistStatus = ChecklistStatus.PENDING,
    val reviewStatus: ReviewStatus = ReviewStatus.NOT_REVIEWED,
    val result: ExecutionResult = ExecutionResult(),
    val reviewerComment: String = "",
    val reviewedAt: String? = null
)

@Serializable
data class TaskTarget(
    val id: String,
    val label: String,
    val targetType: TargetType,
    val targetId: String,
    val status: ChecklistStatus = ChecklistStatus.PENDING,
    val result: ExecutionResult = ExecutionResult(),
)

@Serializable
data class MobileTask(
    val id: String,
    val title: String,
    val operationType: OperationType,
    val workshopId: String,
    val hangarId: String,
    val assignedEmployeeId: String,
    val dueDate: String,
    val plannedStart: String,
    val plannedDurationMinutes: Int,
    val priority: Priority,
    val status: TaskStatus,
    val checklist: List<ChecklistItem>,
    val requiresAcceptance: Boolean,
    val acceptanceRole: RoleId? = null,
    val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.NOT_REQUIRED,
    val acceptedByEmployeeId: String? = null,
    val acceptanceComment: String = "",
    val result: ExecutionResult = ExecutionResult(),
    val offlineEvents: Int = 0,
    val description: String = "",
    val operationTypeTitle: String = operationType.title,
    val targets: List<TaskTarget> = emptyList(),
    val sortOrder: Int = Int.MAX_VALUE,
) {
    val progress: Int get() {
        val executionItems = checklist.size + targets.size
        if (executionItems == 0) return 0
        val processed = checklist.count { it.status != ChecklistStatus.PENDING } +
            targets.count { it.status != ChecklistStatus.PENDING }
        return processed * 100 / executionItems
    }

    /** Плановая дата ([dueDate], yyyy-MM-dd), если она раньше сегодняшней; иначе null. */
    val overdueDate: LocalDate? get() =
        runCatching { LocalDate.parse(dueDate) }.getOrNull()?.takeIf { it.isBefore(LocalDate.now()) }

    /** Задача с прошлых дней: в очереди она идёт раньше сегодняшних. */
    val isOverdue: Boolean get() = overdueDate != null

    fun markOffline() = copy(offlineEvents = offlineEvents + 1)
}

@Serializable
data class ShiftState(val employeeId: String, val startedAt: String? = null, val finishedAt: String? = null, val isOnline: Boolean = true, val pendingSyncEvents: Int = 0, val hangarId: Long? = null)
@Serializable
data class AcceptanceRemark(val id: String, val taskId: String, val itemId: String?, val reason: String, val comment: String, val attachments: List<MediaAttachment> = emptyList(), val createdAt: String)
