// Optional source syntax check using the official Kotlin compiler distribution (no Android SDK).
// Compile with kotlin-compiler.jar on the classpath; see docs/testing.md.
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory

fun main(args: Array<String>) {
    val disposable = Disposer.newDisposable()
    try {
        val environment =
            KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
        val factory = KtPsiFactory(environment.project, false)
        val files =
            args.flatMap { path ->
                File(path)
                    .walkTopDown()
                    .filter {
                        it.isFile && (it.extension == "kt" || it.name.endsWith(".gradle.kts"))
                    }
                    .toList()
            }
        var errors = 0
        for (file in files) {
            val text = file.readText()
            val parsed = factory.createFile(file.name, text)
            for (error in PsiTreeUtil.collectElementsOfType(parsed, PsiErrorElement::class.java)) {
                val line = text.take(error.textOffset).count { it == '\n' } + 1
                System.err.println("${file.path}:$line: ${error.errorDescription}")
                errors++
            }
        }
        check(errors == 0) { "$errors Kotlin syntax errors" }
        println("Kotlin syntax: ${files.size} files OK (syntax only, not Android type-checking).")
    } finally {
        Disposer.dispose(disposable)
    }
}
