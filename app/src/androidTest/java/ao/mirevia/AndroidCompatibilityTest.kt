package ao.mirevia

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run on API 24 as well as newer devices: host JVM tests do not exercise Android desugaring. */
@RunWith(AndroidJUnit4::class)
class AndroidCompatibilityTest {
    @Test
    fun packagedAppSupportsAndroidNougat() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(24, context.applicationInfo.minSdkVersion)
    }

    @Test
    fun releaseDatesWorkBelowAndroidOreo() {
        // Same java.time operations as the TMDB release-date filters.
        val today = LocalDate.now()
        assertEquals(today, LocalDate.parse(today.toString()))
        assertEquals("2024-11-14", LocalDate.of(2025, 2, 14).minusMonths(3).toString())
    }

    @Test
    fun historyTimestampsWorkBelowAndroidOreo() {
        // Same java.time operations as the library's opening history.
        val instant = Instant.ofEpochMilli(0)
        val utcFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.ROOT)
        assertEquals("01/01/1970 00:00", utcFormatter.format(instant.atZone(ZoneId.of("UTC"))))

        val localFormatter =
            DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.forLanguageTag("pt-PT"))
        assertTrue(localFormatter.format(instant.atZone(ZoneId.systemDefault())).isNotBlank())
    }
}
