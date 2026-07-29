package com.rabbitmes.mobile.domain

enum class RoleId(val title: String) {
    OPERATOR("Оператор"), CHIEF_TECHNOLOGIST("Главный технолог"), CHIEF_MECHANIC("Главный механик"), GENERAL_WORKER("Разнорабочий")
}

enum class TaskStatus(val title: String) { NEW("Новая"), IN_PROGRESS("В работе"), BLOCKED("Проблема"), DONE("Выполнена"), SENT("Отправлена"), SKIPPED("Пропущена") }
enum class ChecklistStatus(val title: String) { PENDING("Ожидает"), DONE("Готово"), PROBLEM("Проблема"), SKIPPED("Пропущено") }
enum class ReviewStatus(val title: String) { NOT_REVIEWED("Не проверено"), ACCEPTED("Принято"), REJECTED("Замечание") }
enum class AcceptanceStatus(val title: String) { NOT_REQUIRED("Не требуется"), WAITING("Ожидает"), ACCEPTED("Принята"), REJECTED("Отклонена") }
enum class TargetType { RABBIT, CAGE, ROW, HANGAR }
enum class Priority(val title: String, val weight: Int) { URGENT("Срочно", 0), HIGH("Важно", 1), NORMAL("Планово", 2) }

enum class OperationType(val title: String) {
    INSEMINATION("Осеменение"),
    PALPATION("Пальпация"),
    ANIMAL_SETTLEMENT("Заселение животных"),
    NEST_PREPARATION("Подготовка гнезд"),
    OKROL("Окрол"),
    NEST_SELECTION("Селекция / выравнивание гнезд"),
    LACTATION_CONTROL("Контроль лактации"),
    WEIGHING("Взвешивание"),
    ANIMAL_TRANSFER("Переводы животных"),
    ANIMAL_DEPARTURE("Выбытие"),
    WEANING("Отъем"),
    SLAUGHTER_SHIPMENT("Забой / отгрузка"),
    CLEANING("Уборка"),
    WASHING("Мойка"),
    DISINFECTION("Дезинфекция"),
    HANGAR_ACCEPTANCE("Приемка ангара"),
    FEMALE_DELIVERY("Завоз самок"),
    LIGHT_STIMULATION("Биостимуляция светом"),
    DEWORMING_DOSATRON("Дегельминтизация через Дозатрон"),
    LIGHTING_CHECK("Проверка светового режима"),
    MORTALITY_ROUND("Обход ангара и подсчет падежа"),
    MORTALITY_JOURNAL("Запись падежа в журнал"),
    FEED_CHECK("Проверка корма"),
    WATER_CHECK("Проверка воды"),
    NEST_CONTROL("Контроль гнезд"),
    DAILY_CLEANING("Ежедневная уборка проходов"),
    SECOND_ROUND("Второй обход ангара"),
    OKROL_PREPARATION("Подготовка к окролу"),
    MANUAL_FEEDING("Ручное кормление"),
    FINAL_ROUND("Финальный обход")
}

data class Employee(val id: String, val fullName: String, val role: RoleId, val workshopIds: List<String>, val initials: String)
data class Workshop(val id: String, val name: String, val hangars: List<Hangar>)
data class Hangar(val id: String, val name: String, val rows: List<CageRow>)
data class CageRow(val id: String, val number: Int, val cages: List<Cage>)
data class Cage(val id: String, val rowNumber: Int, val number: Int, val code: String, val rfid: String, val hasNest: Boolean, val occupied: Boolean)
data class Rabbit(val id: String, val rfid: String, val earNumber: String, val cageId: String, val sex: String, val ageDays: Int, val lastWeightKg: Double, val lastInseminationDaysAgo: Int?, val lastPalpation: String?, val lactationStatus: String, val healthStatus: String)

enum class FieldType { TEXT, NUMBER, BOOLEAN, SELECT, PHOTO, VIDEO, FILE, TEMPERATURE, HOURS, FEED_TYPE }
enum class AttachmentType(val title: String, val emoji: String) {
    PHOTO("Фото", "📷"),
    VIDEO("Видео", "🎥"),
    FILE("Файл", "📎"),
}
data class MediaAttachment(val id: String, val type: AttachmentType, val name: String, val localUri: String, val createdAt: String, val uploaded: Boolean = false)
data class OperationField(val id: String, val title: String, val type: FieldType, val required: Boolean = false, val unit: String? = null, val options: List<String> = emptyList(), val placeholder: String = "")
data class OperationDefinition(val type: OperationType, val targetType: TargetType, val requiresScan: Boolean, val completionLabel: String, val fields: List<OperationField>, val allowedRoles: List<RoleId>, val requiresAcceptanceDefault: Boolean = false)

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

data class ChecklistItem(
    val id: String,
    val label: String,
    val targetType: TargetType,
    val targetId: String,
    val status: ChecklistStatus = ChecklistStatus.PENDING,
    val reviewStatus: ReviewStatus = ReviewStatus.NOT_REVIEWED,
    val result: ExecutionResult = ExecutionResult(),
    val reviewerComment: String = "",
    val reviewedAt: String? = null
)

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
    val offlineEvents: Int = 0
) {
    val progress: Int get() = if (checklist.isEmpty()) 0 else checklist.count { it.status == ChecklistStatus.DONE || it.status == ChecklistStatus.PROBLEM || it.status == ChecklistStatus.SKIPPED } * 100 / checklist.size
    fun markOffline() = copy(offlineEvents = offlineEvents + 1)
}

data class ShiftState(val employeeId: String, val startedAt: String? = null, val finishedAt: String? = null, val isOnline: Boolean = true, val pendingSyncEvents: Int = 0)
data class AcceptanceRemark(val id: String, val taskId: String, val itemId: String?, val reason: String, val comment: String, val attachments: List<MediaAttachment> = emptyList(), val createdAt: String)
