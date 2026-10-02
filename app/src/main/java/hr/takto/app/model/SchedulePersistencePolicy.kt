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

    /**
     * Potvrđuje da se checkpoint byte-offset smije koristiti za brzi replay
     * samo ako još pokazuje unutar iste ili dulje arhive, broj revizija nije
     * ispred aktualnog journala i offset završava na granici retka.
     *
     * Ako bilo koji uvjet nije zadovoljen, caller mora koristiti kompatibilni
     * fallback prema broju revizija.
     */
    fun canResumeArchiveFromByteOffset(
        fileLength: Long,
        currentRevisionCount: Int,
        checkpointRevisionCount: Int,
        checkpointByteOffset: Long,
        boundaryIsValid: Boolean
    ): Boolean {
        if (fileLength < 0L || checkpointByteOffset < 0L || checkpointByteOffset > fileLength) return false
        if (checkpointRevisionCount !in 0..currentRevisionCount) return false
        if (checkpointByteOffset > 0L && !boundaryIsValid) return false
        return true
    }
}
