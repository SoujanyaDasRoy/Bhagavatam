package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRowsTest {
    @Test fun rowsSurviveAJsonRoundTrip() {
        val rows = listOf(
            mapOf<String, Any?>("id" to 1L, "ref" to "8.2.31", "quote" to "ଗଜେନ୍ଦ୍ର \"quoted\"\nline", "colour" to 0xFFE8B931L, "note" to null),
            mapOf<String, Any?>("id" to 2L, "ref" to "10.29.1", "quote" to "q", "colour" to 0L, "note" to ""),
        )
        assertEquals(rows, BackupRows.fromJson(BackupRows.toJson(rows)))
    }

    @Test fun emptyTableRoundTrips() = assertEquals(emptyList<Map<String, Any?>>(), BackupRows.fromJson(BackupRows.toJson(emptyList())))

    @Test fun anOldBackupWithoutTheKeyReadsAsNull() = assertTrue(BackupRows.fromRoot(org.json.JSONObject("{\"format\":\"bhagavatam-backup-1\",\"items\":[]}"), "review") == null)
}
