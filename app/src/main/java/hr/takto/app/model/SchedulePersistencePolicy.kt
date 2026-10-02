package hr.takto.app.model

/**
 * Određuje kada append-only journal treba konsolidirati u punu atomsku snimku.
 *
 * Male pojedinačne izmjene ostaju trajno zapisane u journalu i ne prisiljavaju
 * O(n) serijalizaciju cijelog višegodišnjeg rasporeda pri svakom dodiru.
 */
object SchedulePersistencePolicy {
    const val CHECKPOINT_REVISION_INTERVAL = 64
    const val BULK_CHECKPOINT_CHANGE_COUNT = 32

    fun shouldCheckpoint(
        currentSnapshotExists: Boolean,
        revisionsSinceCheckpoint: Int,
        changedEntries: Int
    ): Boolean {
        if (!currentSnapshotExists) return true
        if (changedEntries >= BULK_CHECKPOINT_CHANGE_COUNT) return true
        return revisionsSinceCheckpoint >= CHECKPOINT_REVISION_INTERVAL
    }
}
