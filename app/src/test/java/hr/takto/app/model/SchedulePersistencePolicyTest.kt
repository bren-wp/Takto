package hr.takto.app.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePersistencePolicyTest {
    @Test
    fun missingSnapshotAlwaysCheckpoints() {
        assertTrue(
            SchedulePersistencePolicy.shouldCheckpoint(
                currentSnapshotExists = false,
                revisionsSinceCheckpoint = 1,
                changedEntries = 1
            )
        )
    }

    @Test
    fun smallEditsStayInJournalUntilInterval() {
        assertFalse(
            SchedulePersistencePolicy.shouldCheckpoint(
                currentSnapshotExists = true,
                revisionsSinceCheckpoint = SchedulePersistencePolicy.CHECKPOINT_REVISION_INTERVAL - 1,
                changedEntries = 1
            )
        )
        assertTrue(
            SchedulePersistencePolicy.shouldCheckpoint(
                currentSnapshotExists = true,
                revisionsSinceCheckpoint = SchedulePersistencePolicy.CHECKPOINT_REVISION_INTERVAL,
                changedEntries = 1
            )
        )
    }

    @Test
    fun largeBulkEditCheckpointsImmediately() {
        assertTrue(
            SchedulePersistencePolicy.shouldCheckpoint(
                currentSnapshotExists = true,
                revisionsSinceCheckpoint = 2,
                changedEntries = SchedulePersistencePolicy.BULK_CHECKPOINT_CHANGE_COUNT
            )
        )
    }
    @Test
    fun validArchiveCursorCanResumeWithoutScanningOldRevisions() {
        assertTrue(
            SchedulePersistencePolicy.canResumeArchiveFromByteOffset(
                fileLength = 8_000L,
                currentRevisionCount = 200,
                checkpointRevisionCount = 192,
                checkpointByteOffset = 7_500L,
                boundaryIsValid = true
            )
        )
    }

    @Test
    fun invalidArchiveCursorFallsBackSafely() {
        assertFalse(
            SchedulePersistencePolicy.canResumeArchiveFromByteOffset(
                fileLength = 8_000L,
                currentRevisionCount = 200,
                checkpointRevisionCount = 201,
                checkpointByteOffset = 7_500L,
                boundaryIsValid = true
            )
        )
        assertFalse(
            SchedulePersistencePolicy.canResumeArchiveFromByteOffset(
                fileLength = 8_000L,
                currentRevisionCount = 200,
                checkpointRevisionCount = 192,
                checkpointByteOffset = 8_001L,
                boundaryIsValid = true
            )
        )
        assertFalse(
            SchedulePersistencePolicy.canResumeArchiveFromByteOffset(
                fileLength = 8_000L,
                currentRevisionCount = 200,
                checkpointRevisionCount = 192,
                checkpointByteOffset = 7_500L,
                boundaryIsValid = false
            )
        )
    }
}
