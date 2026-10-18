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

class BackupRestoreRulesTest {
    private val allowed = setOf("id", "ref", "quote")

    @Test fun anUnknownColumnIsRejectedBeforeAnythingIsDeleted() {
        val rows = listOf(mapOf<String, Any?>("id" to 1L, "ref" to "1.1.1", "from_a_newer_version" to "x"))
        var thrown = false
        try { BackupRows.requireColumns("annotation", rows, allowed) } catch (e: IllegalArgumentException) { thrown = true }
        org.junit.Assert.assertTrue(thrown)
    }

    @Test fun knownColumnsAndEmptyTablesPass() {
        BackupRows.requireColumns("annotation", listOf(mapOf<String, Any?>("id" to 1L, "ref" to "r")), allowed)
        BackupRows.requireColumns("annotation", emptyList(), allowed)
    }
}
