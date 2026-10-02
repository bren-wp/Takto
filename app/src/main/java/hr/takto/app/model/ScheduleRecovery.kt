package hr.takto.app.model

data class ScheduleRecoveryResolution(
    val shouldApply: Boolean,
    val next: ShiftEntry?
)

object ScheduleRecovery {
    fun resolve(
        current: ShiftEntry?,
        before: ShiftEntry?,
        after: ShiftEntry?
    ): ScheduleRecoveryResolution = when {
        current == after -> ScheduleRecoveryResolution(false, current)
        current == before -> ScheduleRecoveryResolution(true, after)
        else -> ScheduleRecoveryResolution(false, current)
    }
}
