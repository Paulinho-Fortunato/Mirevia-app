package ao.mirevia.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

/** Public installed-package metadata. Does not read the keystore or contact a scanner. */
data class AppIdentity(
    val packageName: String,
    val versionName: String,
    val signerSha256: List<String>,
    val requestedPermissions: List<String>,
) {
    fun publicReport() = buildString {
        appendLine("Mirevia — identidade local do pacote")
        appendLine("Pacote: $packageName")
        appendLine("Versão: $versionName")
        signerSha256.forEach { appendLine("Certificado SHA-256: $it") }
        appendLine("Permissões solicitadas:")
        requestedPermissions.forEach { appendLine(it) }
        append("A assinatura identifica o pacote; não certifica que é seguro.")
    }
}

@Suppress("DEPRECATION")
fun readAppIdentity(context: Context): AppIdentity? =
    try {
        val flags =
            PackageManager.GET_PERMISSIONS or
                if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
                else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        val signers =
            if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty()
            else info.signatures.orEmpty()
        AppIdentity(
            packageName = context.packageName,
            versionName = info.versionName ?: "Informação não disponível",
            signerSha256 =
                signers
                    .map { signature ->
                        MessageDigest.getInstance("SHA-256")
                            .digest(signature.toByteArray())
                            .joinToString(":") {
                                String.format(Locale.ROOT, "%02X", it.toInt() and 0xff)
                            }
                    }
                    .distinct()
                    .sorted(),
            requestedPermissions = info.requestedPermissions.orEmpty().sorted(),
        )
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: SecurityException) {
        null
    }
