package ao.mirevia.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI

fun openTrustedExternal(context: Context, url: String, allowedHosts: Set<String>): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    if (
        uri.scheme != "https" ||
            uri.host !in allowedHosts ||
            uri.rawUserInfo != null ||
            uri.port != -1
    )
        return false
    return try {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        context.startActivity(Intent.createChooser(intent, "Abrir no navegador"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
