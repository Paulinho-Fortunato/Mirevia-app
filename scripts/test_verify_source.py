#!/usr/bin/env python3
"""Exercise textual regression guards in memory. No project file is modified, no network is used.

These tests only check known source/configuration patterns, not Kotlin semantics, Android API
compatibility, network isolation, provider availability or a complete security assessment.
"""
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
import runpy
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
VERIFIER = ROOT / "scripts/verify_source.py"
PLAYBACK = "app/src/main/java/ao/mirevia/playback/"
CORE = "core/src/main/kotlin/ao/mirevia/core/playback/"
UI = "app/src/main/java/ao/mirevia/ui/"

# Each edit deliberately removes/reverses one known invariant in the file presented to the verifier.
CASES = [
    ("Android 7 minimum", "app/build.gradle.kts", "minSdk = 24", "minSdk = 26"),
    ("date desugaring", "app/build.gradle.kts", "isCoreLibraryDesugaringEnabled = true", "isCoreLibraryDesugaringEnabled = false"),
    ("file access", PLAYBACK + "SecureBrowserSettings.kt", "allowFileAccess = false", "allowFileAccess = true"),
    ("mixed content", PLAYBACK + "SecureBrowserSettings.kt", "MIXED_CONTENT_NEVER_ALLOW", "MIXED_CONTENT_ALWAYS_ALLOW"),
    ("Safe Browsing setting", PLAYBACK + "SecureBrowserSettings.kt", "setSafeBrowsingEnabled(this, true)", "setSafeBrowsingEnabled(this, false)"),
    ("third-party cookies", PLAYBACK + "SecureBrowserSettings.kt", "setAcceptThirdPartyCookies(this@configurePlayerBrowser, false)", "setAcceptThirdPartyCookies(this@configurePlayerBrowser, true)"),
    ("automatic JS windows", PLAYBACK + "SecureBrowserSettings.kt", "javaScriptCanOpenWindowsAutomatically = false", "javaScriptCanOpenWindowsAutomatically = true"),
    ("multiple windows", PLAYBACK + "SecureBrowserSettings.kt", "setSupportMultipleWindows(true)", "setSupportMultipleWindows(false)"),
    ("fatal NEW_WINDOW", PLAYBACK + "SecureEmbedWebView.kt", "val opened = windows.open(context, windowOwner, null, resultMsg)", "block(PlaybackBlock.local(PlaybackBlockKind.NEW_WINDOW))\n                    val opened = false"),
    ("owner cleanup", PLAYBACK + "SecureEmbedWebView.kt", "windows.closeOwner(windowOwner)", "// owner cleanup removed"),
    ("worker network context", PLAYBACK + "SecureEmbedWebView.kt", "IframeRequestPolicy.isAllowedRequest(request.url.toString(), isMainFrame = false)", "IframeRequestPolicy.isAllowedRequest(request.url.toString(), request.isForMainFrame)"),
    ("initial source validation", PLAYBACK + "SecureEmbedWebView.kt", "require(policy.isValidSource(source))", "// initial validation removed"),
    ("fixed iframe document", CORE + "IframeRequestPolicy.kt", "if (isMainFrame) return url == LOCAL_DOCUMENT_URL", "if (isMainFrame) return true"),
    ("HTTPS boundary", CORE + "IframeRequestPolicy.kt", '!= "https"', '!= "http"'),
    ("renewed consent", CORE + "IframeRequestPolicy.kt", "CONSENT_REVISION = 3", "CONSENT_REVISION = 2"),
    ("window document context", CORE + "PlayerWindowPolicy.kt", "IframeRequestPolicy.isAllowedRequest(url, isMainFrame = false)", "IframeRequestPolicy.isAllowedRequest(url, isMainFrame = true)"),
    ("blank bootstrap", CORE + "PlayerWindowPolicy.kt", 'BLANK_DOCUMENT = "about:blank"', 'BLANK_DOCUMENT = "https://unlisted.example/"'),
    ("transported child", PLAYBACK + "PlayerWindowController.kt", "response.transport.webView = entry.value.browser", "response.transport.webView = null"),
    ("transport reply", PLAYBACK + "PlayerWindowController.kt", "response.message.sendToTarget()\n            entry.value.browser.requestFocus()", "entry.value.browser.requestFocus()"),
    ("attachment before reply", PLAYBACK + "PlayerWindowController.kt", "if (!entry.value.browser.isAttachedToWindow) return", "// attachment check removed"),
    ("bounded windows", CORE + "PlayerWindowStack.kt", "MAX_WINDOWS = 3", "MAX_WINDOWS = 999"),
    ("opener owner identity", CORE + "PlayerWindowStack.kt", "it.id == openerId && it.owner === owner", "it.id == openerId && it.owner == owner"),
    ("popup navigation", PLAYBACK + "PlayerWindowWebView.kt", "val allowed = PlayerWindowPolicy.isAllowedRequest(url)", "val allowed = true"),
    ("popup TLS failure", PLAYBACK + "PlayerWindowWebView.kt", "handler.cancel()", "handler.proceed()"),
    ("popup Safe Browsing failure", PLAYBACK + "PlayerWindowWebView.kt", "callback.backToSafety(true)", "callback.proceed(true)"),
    ("redirected origin refresh", PLAYBACK + "PlayerWindowWebView.kt", "override fun onPageCommitVisible", "fun ignoredPageCommit"),
    ("modal Back dismissal", UI + "player/PlayerWindowSurface.kt", "dismissOnBackPress = true", "dismissOnBackPress = false"),
    ("native Close", UI + "player/PlayerWindowSurface.kt", 'Text("Fechar")', 'Text("Continuar")'),
    ("window disclosure", UI + "components/ExternalPlayerConsentDialog.kt", "Text(PlayerWindowPolicy.DESCRIPTION)", "// window description removed"),
    ("consent revocation", "app/src/main/java/ao/mirevia/data/local/PreferencesRepository.kt", "it.remove(PLAYER_CONSENT_REVISION)", "// revision kept after revocation"),
]


def verify(overrides=None):
    original_read = Path.read_text
    replacements = overrides or {}

    def read_with_override(path, *args, **kwargs):
        replacement = replacements.get(path.resolve())
        return replacement if replacement is not None else original_read(path, *args, **kwargs)

    with patch.object(Path, "read_text", new=read_with_override), redirect_stdout(StringIO()):
        runpy.run_path(str(VERIFIER), run_name="__main__")


def main():
    verify()  # The real tree must pass before any deliberate regressions are introduced in memory.
    for name, relative, before, after in CASES:
        path = ROOT / relative
        text = path.read_text()
        if text.count(before) != 1:
            raise AssertionError(f"Mutation fixture is stale/ambiguous: {name}")
        try:
            verify({path.resolve(): text.replace(before, after, 1)})
        except AssertionError:
            continue
        raise AssertionError(f"Verifier unexpectedly accepted regression: {name}")
    print(f"Source verifier: real tree passed; {len(CASES)} in-memory regressions rejected.")
    print("No files modified; these textual checks do not replace Android build/lint/device tests.")


if __name__ == "__main__":
    main()
