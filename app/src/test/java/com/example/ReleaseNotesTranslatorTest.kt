package com.example

import com.example.util.ReleaseNotesTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTranslatorTest {

    @Test
    fun testFinancialAnalyticsFeaturesTranslation() {
        val input = "Add financial analiz ve tahminleme and risk summary features"
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Finansal analiz, KPI göstergeleri ve reyon risk özeti özellikleri eklendi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testFinancialKpiMetricsImplementationTranslation() {
        val input = "Implement financial KPI metrics and reyon risk summary components to the analiz ve tahminleme ana sayfa gösterge paneli."
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Analiz ekranına finansal KPI metrikleri ve reyon risk özeti panelleri entegre edildi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testYellowLabelTaggingTranslation() {
        val input = "Added support for tagging ürünler with yellow labels to track potential inventory reductions."
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Yüksek riskli ürünler için sarı etiket ve indirimli satış desteği eklendi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testBarcodeScannerCrashTranslation() {
        val input = "Fix crash on barcode scanner when camera permission denied"
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Kamera izni verilmediğinde barkod tarayıcıda oluşan çökme sorunu giderildi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testExtendUrgentProductWindowTranslation() {
        val input = "Extend urgent product window to 7 days"
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Acil müdahale vitrini 7 güne çıkarıldı.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testUrgentFilterRangeTranslation() {
        val input = "Urgent ürün filter range from 2 to 7 days"
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Kritik SKT filtre aralığı 0-7 gün olarak güncellendi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testViewAllUrgentCarouselTranslation() {
        val input = "View all navigation to urgent carousel"
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Acil vitrin için 'Tümünü Gör' yönlendirmesi eklendi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testConventionalCommitPrefixStripping() {
        val input = "feat(analytics): add financial analiz ve tahminleme and risk summary features"
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "Finansal analiz, KPI göstergeleri ve reyon risk özeti özellikleri eklendi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testAlreadyCleanTurkishPreservation() {
        val input = "SKT yaklaşan ürünler için anlık bildirim sistemi güncellendi."
        val result = ReleaseNotesTranslator.translateSingleLine(input)
        assertEquals(
            "SKT yaklaşan ürünler için anlık bildirim sistemi güncellendi.",
            result
        )
        assertNoEnglishResiduals(result)
    }

    @Test
    fun testFullChangelogBulkParsing() {
        val rawChangelog = """
            ## What's Changed
            * feat: Add financial analiz ve tahminleme and risk summary features by @author in https://github.com/repo/pull/1
            * Implement financial KPI metrics and reyon risk summary components to the analiz ve tahminleme ana sayfa gösterge paneli.
            * Added support for tagging ürünler with yellow labels to track potential inventory reductions.
            * Fix crash on barcode scanner when camera permission denied
            * Extend urgent product window to 7 days
            * chore: bump dependencies
            * Merge pull request #45 from dev
            
            Full Changelog: https://github.com/repo/compare/v1.0...v1.1
        """.trimIndent()

        val bullets = ReleaseNotesTranslator.translateToBulletPoints(rawChangelog, "1.4.2")

        assertEquals(5, bullets.size)
        assertEquals("Finansal analiz, KPI göstergeleri ve reyon risk özeti özellikleri eklendi.", bullets[0])
        assertEquals("Analiz ekranına finansal KPI metrikleri ve reyon risk özeti panelleri entegre edildi.", bullets[1])
        assertEquals("Yüksek riskli ürünler için sarı etiket ve indirimli satış desteği eklendi.", bullets[2])
        assertEquals("Kamera izni verilmediğinde barkod tarayıcıda oluşan çökme sorunu giderildi.", bullets[3])
        assertEquals("Acil müdahale vitrini 7 güne çıkarıldı.", bullets[4])

        for (bullet in bullets) {
            assertNoEnglishResiduals(bullet)
        }
    }

    @Test
    fun testEmptyReleaseNotesFallback() {
        val bullets = ReleaseNotesTranslator.translateToBulletPoints("", "1.5.0")
        assertTrue(bullets.isNotEmpty())
        assertEquals("v1.5.0 sürüm güncellemesi.", bullets[0])
        assertTrue(bullets.contains("Sistem performansı ve kararlılık iyileştirmeleri yapıldı."))
    }

    private fun assertNoEnglishResiduals(text: String) {
        val englishWords = listOf(
            "\\band\\b", "\\bthe\\b", "\\bto\\b", "\\bwith\\b", "\\bfeatures\\b",
            "\\bsupport for\\b", "\\bfor\\b", "\\bfrom\\b", "\\bcomponents\\b",
            "\\bmetrics\\b", "\\badd\\b", "\\badded\\b", "\\bimplement\\b", "\\bfix\\b"
        )
        for (pattern in englishWords) {
            val hasMatch = Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
            assertFalse("Sentence should not contain English residual '$pattern': '$text'", hasMatch)
        }
    }
}
