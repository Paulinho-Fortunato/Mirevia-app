package ao.mirevia.ui.components

import android.content.Intent
import ao.mirevia.core.model.SharePayload
import org.junit.Assert.*
import org.junit.Test

class ShareIntentTest {
    @Test
    fun sharesTextOnlyWithoutFilesUriGrantsOrAutomaticPlayback() {
        val payload = SharePayload("Título de teste", "Link de incorporação de teste")
        val intent = publicShareIntent(payload)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(payload.subject, intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(payload.text, intent.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(intent.data)
        assertNull(intent.clipData)
        assertFalse(intent.hasExtra(Intent.EXTRA_STREAM))
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertNull(intent.component)
    }
}
