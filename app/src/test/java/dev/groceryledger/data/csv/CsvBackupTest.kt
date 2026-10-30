package dev.groceryledger.data.csv

import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class CsvBackupTest {
    private val today = LocalDate.of(2026, 10, 29)
    private val now = 1_000_000L

    @Test
    fun exportAndImportRoundTripPreservesData() {
        val firstUid = UUID.randomUUID().toString()
        val secondUid = UUID.randomUUID().toString()
        val entries = listOf(
            expense("Milk, \"full cream\"", 6_050, "2026-10-29", firstUid),
            expense("Bread\n loaf", 4_000, "2026-10-27", secondUid),
        )

        val csv = CsvBackup.write(entries)
        assertTrue(csv.startsWith("\uFEFFformat_version,entry_uid,date,item,amount_inr,category"))

        val analysis = CsvBackup.analyze(csv, emptySet(), today, now)
        assertEquals(2, analysis.entries.size)
        assertEquals(0, analysis.duplicateCount)
        assertTrue(analysis.errors.isEmpty())

        val byUid = analysis.entries.associateBy { it.uid }
        assertEquals("Milk, \"full cream\"", byUid.getValue(firstUid).name)
        assertEquals(6_050L, byUid.getValue(firstUid).amountPaise)
        assertEquals("Bread\n loaf", byUid.getValue(secondUid).name)
        assertEquals(LocalDate.parse("2026-10-27"), byUid.getValue(secondUid).date)
    }

    @Test
    fun reimportingTheSameFileSkipsDuplicates() {
        val entries = listOf(expense("Milk", 6_000, "2026-10-29", UUID.randomUUID().toString()))
        val csv = CsvBackup.write(entries)

        val analysis = CsvBackup.analyze(
            text = csv,
            existingUids = entries.map { it.uid }.toSet(),
            today = today,
            nowMillis = now,
        )

        assertTrue(analysis.entries.isEmpty())
        assertEquals(1, analysis.duplicateCount)
    }

    @Test
    fun duplicateUidsInsideTheFileAreSkipped() {
        val uid = UUID.randomUUID().toString()
        val csv = """
            format_version,entry_uid,date,item,amount_inr
            1,$uid,2026-10-29,Milk,60.00
            1,$uid,2026-10-29,Milk,60.00
        """.trimIndent()

        val analysis = CsvBackup.analyze(csv, emptySet(), today, now)

        assertEquals(1, analysis.entries.size)
        assertEquals(1, analysis.duplicateCount)
    }

    @Test
    fun newerFormatVersionRejectsTheWholeFile() {
        val uid = UUID.randomUUID().toString()
        val csv = """
            format_version,entry_uid,date,item,amount_inr,category
            3,$uid,2026-10-29,Milk,60.00,Dairy & Eggs
        """.trimIndent()

        assertThrows(NewerBackupException::class.java) {
            CsvBackup.analyze(csv, emptySet(), today, now)
        }
    }

    @Test
    fun versionOneFileStillImportsAndGetsUncategorized() {
        val uid = UUID.randomUUID().toString()
        val csv = """
            format_version,entry_uid,date,item,amount_inr
            1,$uid,2026-10-29,Milk,60.00
        """.trimIndent()

        val analysis = CsvBackup.analyze(csv, emptySet(), today, now)

        assertEquals(1, analysis.entries.size)
        assertEquals(Categories.UNCATEGORIZED, analysis.entries.single().category)
    }

    @Test
    fun categoryRoundTripsAndIsQuotedWhenNeeded() {
        val uid = UUID.randomUUID().toString()
        val entries = listOf(
            expense("Rice", 95_000, "2026-10-27", uid).copy(category = "Grains, Pulses"),
        )

        val csv = CsvBackup.write(entries)
        val analysis = CsvBackup.analyze(csv, emptySet(), today, now)

        assertEquals("Grains, Pulses", analysis.entries.single().category)
    }

    @Test
    fun missingRequiredHeaderIsRejected() {
        val csv = """
            date,item,amount_inr
            2026-10-29,Milk,60.00
        """.trimIndent()

        assertThrows(BadBackupException::class.java) {
            CsvBackup.analyze(csv, emptySet(), today, now)
        }
    }

    @Test
    fun badRowsAreReportedButValidRowsStillImport() {
        val validUid = UUID.randomUUID().toString()
        val csv = """
            format_version,entry_uid,date,item,amount_inr
            1,$validUid,2026-10-29,Milk,60.00
            1,not-a-uuid,2026-10-29,Bread,40.00
            1,${UUID.randomUUID()},29-10-2026,Tea,20.00
            1,${UUID.randomUUID()},2026-10-30,Eggs,30.00
            1,${UUID.randomUUID()},2026-10-29,,10.00
            1,${UUID.randomUUID()},2026-10-29,Rice,abc
            1,${UUID.randomUUID()},2026-10-29,Sugar
            0,${UUID.randomUUID()},2026-10-29,Salt,10.00
        """.trimIndent()

        val analysis = CsvBackup.analyze(csv, emptySet(), today, now)

        assertEquals(1, analysis.entries.size)
        assertEquals(7, analysis.errors.size)
        assertEquals(
            listOf(
                ImportRowErrorReason.INVALID_UID,
                ImportRowErrorReason.INVALID_DATE,
                ImportRowErrorReason.FUTURE_DATE,
                ImportRowErrorReason.INVALID_NAME,
                ImportRowErrorReason.INVALID_AMOUNT,
                ImportRowErrorReason.COLUMN_COUNT,
                ImportRowErrorReason.INVALID_VERSION,
            ),
            analysis.errors.map { it.reason },
        )
    }

    @Test
    fun columnsMayBeInAnyOrderAndUnknownColumnsAreIgnored() {
        val uid = UUID.randomUUID().toString()
        val csv = """
            item,date,extra,amount_inr,entry_uid,format_version
            Milk,2026-10-29,ignored,60.50,$uid,1
        """.trimIndent()

        val analysis = CsvBackup.analyze(csv, emptySet(), today, now)

        assertEquals(1, analysis.entries.size)
        assertEquals("Milk", analysis.entries.single().name)
        assertEquals(6_050L, analysis.entries.single().amountPaise)
    }

    @Test
    fun brokenQuotingIsRejectedAsABadBackup() {
        val csv = """
            format_version,entry_uid,date,item,amount_inr
            1,${UUID.randomUUID()},2026-10-29,"Milk,60.00
        """.trimIndent()

        assertThrows(BadBackupException::class.java) {
            CsvBackup.analyze(csv, emptySet(), today, now)
        }
    }

    private fun expense(
        name: String,
        amountPaise: Long,
        date: String,
        uid: String,
    ): Expense = Expense(
        id = 0,
        uid = uid,
        name = name,
        amountPaise = amountPaise,
        date = LocalDate.parse(date),
        createdAt = now,
        updatedAt = now,
    )
}
