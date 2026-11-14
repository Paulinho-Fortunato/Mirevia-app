package ao.mirevia.security

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class AppIdentityTest {
    @Test
    fun readsOnlyTheInstalledPackagePublicIdentityIncludingApi24Fallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = readAppIdentity(context)
        assertNotNull(info)
        requireNotNull(info)
        assertEquals(context.packageName, info.packageName)
        assertTrue(info.signerSha256.isNotEmpty())
        assertTrue(info.signerSha256.all { Regex("[A-F0-9]{2}(?::[A-F0-9]{2}){31}").matches(it) })
        assertTrue(info.requestedPermissions.contains("android.permission.INTERNET"))
        listOf(
                "CAMERA",
                "RECORD_AUDIO",
                "ACCESS_FINE_LOCATION",
                "READ_CONTACTS",
                "REQUEST_INSTALL_PACKAGES",
                "QUERY_ALL_PACKAGES",
            )
            .forEach { assertFalse(info.requestedPermissions.contains("android.permission.$it")) }
    }
}
