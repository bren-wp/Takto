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
}
