#!/usr/bin/env python3
"""Dependency-free source/configuration invariants. Not a replacement for lint or device tests."""
from pathlib import Path
import hashlib
import re
import tomllib
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'

def require(value, message):
    if not value:
        raise AssertionError(message)

# This style attribute is XML API 27+, even though minSdk remains 24.
navigation_bar_items = []
for path in (ROOT / 'app/src/main').rglob('*.xml'):
    xml = ET.parse(path)
    for item in xml.iter('item'):
        if item.get('name') == 'android:windowLightNavigationBar':
            qualifier = re.search(r'(?:^|-)v(\d+)(?:-|$)', path.parent.name)
            require(qualifier and int(qualifier.group(1)) >= 27, f'windowLightNavigationBar requires API 27 resource qualification: {path.relative_to(ROOT)}')
            navigation_bar_items.append(path)
require(navigation_bar_items == [ROOT / 'app/src/main/res/values-v27/styles.xml'], 'Define windowLightNavigationBar once, only in values-v27/styles.xml')
base_styles = ET.parse(ROOT / 'app/src/main/res/values/styles.xml')
base_theme = base_styles.find("style[@name='Theme.Mirevia']")
require(base_theme is not None and base_theme.get('parent') == 'Base.Theme.Mirevia', 'Keep the base theme fallback for Android API 24-26')
versioned_theme = ET.parse(navigation_bar_items[0]).find("style[@name='Theme.Mirevia']")
require(versioned_theme is not None and versioned_theme.get('parent') == 'Base.Theme.Mirevia', 'API 27 theme must inherit the common base theme')
with (ROOT / 'gradle/libs.versions.toml').open('rb') as f:
    catalog = tomllib.load(f)

# API 24/25 do not provide java.time natively; minSdk alone is not enough.
app_gradle = (ROOT / 'app/build.gradle.kts').read_text()
require(re.search(r'(?m)^\s*minSdk\s*=\s*24\s*$', app_gradle), 'Android 7.0 requires minSdk 24')
require('isCoreLibraryDesugaringEnabled = true' in app_gradle, 'Enable core library desugaring for java.time below API 26')
require('coreLibraryDesugaring(libs.desugar.jdk.libs)' in app_gradle, 'Missing Android desugaring dependency')
require(catalog['libraries']['desugar-jdk-libs']['module'] == 'com.android.tools:desugar_jdk_libs', 'Unexpected desugaring library')

manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
permissions = {node.get(ANDROID + 'name') for node in manifest.findall('uses-permission')}
require(permissions == {'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE'}, 'Unexpected Android permission')
application = manifest.find('application')
require(application.get(ANDROID + 'usesCleartextTraffic') == 'false', 'Cleartext must be disabled')
require(application.get(ANDROID + 'allowBackup') == 'false', 'Backup must be disabled')
player = next(node for node in application.findall('activity') if node.get(ANDROID + 'name') == '.playback.PlayerActivity')
require(player.get(ANDROID + 'exported') == 'false', 'Player must not be externally exported')
require(player.get(ANDROID + 'supportsPictureInPicture') != 'true', 'Do not claim undocumented native PiP')
network = ET.parse(ROOT / 'app/src/main/res/xml/network_security_config.xml').getroot()
require(network.find('base-config').get('cleartextTrafficPermitted') == 'false', 'Network config must reject cleartext')

sources = list((ROOT / 'app/src/main/java').rglob('*.kt')) + list((ROOT / 'core/src/main/kotlin').rglob('*.kt'))
for path in sources:
    content = path.read_text()
    for forbidden in ['addJavascriptInterface(', 'evaluateJavascript(', 'android.app.DownloadManager', 'allowFileAccess = true', 'allowUniversalAccessFromFileURLs = true', 'handler.proceed(', 'MIXED_CONTENT_ALWAYS_ALLOW']:
        require(forbidden not in content, f'Forbidden API in {path.relative_to(ROOT)}: {forbidden}')
    require(not re.search(r'[\U0001f000-\U0001faff\u2600-\u27bf]', content), f'Emoji in interface source: {path}')
    require(not re.search(r'\b(?:TODO|FIXME)\s*\(', content), f'Unimplemented code in {path}')

# Regressions reported by the real Android Kotlin compiler, not caught by syntax-only checks.
player_activity = (ROOT / 'app/src/main/java/ao/mirevia/playback/PlayerActivity.kt').read_text()
require(not re.search(r'\bfun\s+setImmersive\s*\(', player_activity), 'Do not shadow Activity.setImmersive; use a player-specific system-bars helper')
require('private fun setPlayerImmersiveMode(' in player_activity and '::setPlayerImmersiveMode' in player_activity, 'Keep the player system-bars helper and callback reference consistent')
search_screen = (ROOT / 'app/src/main/java/ao/mirevia/ui/search/SearchScreen.kt').read_text()
require(re.search(r'MediaSearchRow\(\s*media\s*=\s*result\.media,\s*onClick\s*=', search_screen), 'Bind the search result callback explicitly to MediaSearchRow.onClick; subtitle is the last parameter')

webview = (ROOT / 'app/src/main/java/ao/mirevia/playback/SecureEmbedWebView.kt').read_text()
popup = (ROOT / 'app/src/main/java/ao/mirevia/playback/PlayerWindowWebView.kt').read_text()
browser_settings = (ROOT / 'app/src/main/java/ao/mirevia/playback/SecureBrowserSettings.kt').read_text()
window_controller = (ROOT / 'app/src/main/java/ao/mirevia/playback/PlayerWindowController.kt').read_text()
window_policy = (ROOT / 'core/src/main/kotlin/ao/mirevia/core/playback/PlayerWindowPolicy.kt').read_text()
for required in ['allowFileAccess = false', 'allowContentAccess = false', 'MIXED_CONTENT_NEVER_ALLOW', 'setAcceptThirdPartyCookies(this@configurePlayerBrowser, false)', 'WebView.setWebContentsDebuggingEnabled(false)', 'WebSettingsCompat.setSafeBrowsingEnabled(this, true)', 'javaScriptCanOpenWindowsAutomatically = false', 'setSupportMultipleWindows(true)']:
    require(required in browser_settings, f'Missing shared WebView restriction: {required}')
for browser in [webview, popup]:
    for required in ['configurePlayerBrowser()', 'handler.cancel()', 'callback.backToSafety(true)', 'request.deny()', 'filePathCallback.onReceiveValue(null)']:
        require(required in browser, f'Keep the same base protection in iframe and windows: {required}')
    require('OkHttpClient' not in browser and 'openConnection' not in browser, 'WebViews must not fetch/interrogate streams themselves')
require('removeAllCookies' in webview, 'Keep final session cleanup in the iframe owner')
for forbidden in ['removeAllCookies', 'deleteAllData', 'ServiceWorkerController', 'Intent.ACTION_VIEW', 'Intent.parseUri']:
    require(forbidden not in popup, f'Closing a window must not clear global state, reconfigure workers or open apps: {forbidden}')
require(not re.search(r'\bloadUrl\s*\(', popup + window_controller), 'Transport a fresh window without guessing or preloading a URL')
require('PlaybackBlock.request(PlaybackBlockKind.RESOURCE, url)' in webview and 'onSecurityBlocked: (PlaybackBlock) -> Unit' in webview, 'Keep typed origin-only main-player diagnostics, never raw URLs')
# The owner selected HTTPS resources/frames (0.2.1) and internal HTTPS windows (0.2.2), no host list.
request_policy = (ROOT / 'core/src/main/kotlin/ao/mirevia/core/playback/IframeRequestPolicy.kt').read_text()
require('if (isMainFrame) return url == LOCAL_DOCUMENT_URL' in request_policy, 'Keep the iframe main document fixed to the local wrapper')
for required in ['"https"', 'uri.rawUserInfo != null', 'uri.port != -1 && uri.port != 443', "host.trimEnd('.') == LOCAL_HOST"]:
    require(required in request_policy, f'Keep the HTTPS resource boundary: {required}')
require('allowedHosts' not in request_policy + window_policy and 'static.cloudflareinsights.com' not in request_policy + window_policy, 'Do not reintroduce per-host HTTPS resource/window lists')
require('BLANK_DOCUMENT = "about:blank"' in window_policy and 'IframeRequestPolicy.isAllowedRequest(url, isMainFrame = false)' in window_policy, 'Window policy allows a local blank bootstrap and remote HTTPS, not the iframe main-document rule')
require(webview.count('IframeRequestPolicy.isAllowedRequest(url, request.isForMainFrame)') == 2, 'Keep iframe navigation and interception on its scoped policy')
require('policy.isAllowedNavigation' not in webview + popup, 'The strict source allowlist must not filter remote resource/window domains')
require(popup.count('PlayerWindowPolicy.isAllowedRequest(url)') == 3, 'Check popup navigation, resources and observed document URLs with its own HTTPS policy')
require('onPageCommitVisible' in popup and 'updateDocumentOrigin(url)' in popup and 'onOrigin(PlaybackBlock.request(PlaybackBlockKind.NAVIGATION, url))' in popup, 'Refresh the origin-only native label after window redirects')
require('filterWorkerRequest(request)' in webview and 'controller.setServiceWorkerClient(workerClient)' in webview, 'Install the shared worker network gate')
require('IframeRequestPolicy.isAllowedRequest(request.url.toString(), isMainFrame = false)' in webview, 'Worker networking must not apply the iframe-only main document rule to transported windows')
require('require(policy.isValidSource(source))' in webview, 'Keep strict initial source validation before loading')
require('block(PlaybackBlock.local(PlaybackBlockKind.NEW_WINDOW))' not in webview and 'windows.open(context, windowOwner, null, resultMsg)' in webview, 'A popup request must not terminate the iframe')
require('windows.closeOwner(windowOwner)' in webview, 'Destroy this session windows when the iframe is released')
for required in ['as? WebView.WebViewTransport', 'isAttachedToWindow', 'response.transport.webView = entry.value.browser', 'response.message.sendToTarget()', 'addOnAttachStateChangeListener', 'TRANSPORT_TIMEOUT_MS', 'stack.removeOwner(owner)', 'stack.clear()']:
    require(required in window_controller, f'Keep native transport, attachment, bounded lifetime and cancellation: {required}')
transport_completion = window_controller.split('private fun completeTransport(', 1)[1].split('internal fun closeView(', 1)[0]
require('if (!entry.value.browser.isAttachedToWindow) return' in transport_completion and 'response.message.sendToTarget()' in transport_completion, 'Complete the accepted transport only after native attachment, not just during cancellation')
window_stack = (ROOT / 'core/src/main/kotlin/ao/mirevia/core/playback/PlayerWindowStack.kt').read_text()
require('MAX_WINDOWS = 3' in window_stack and window_stack.count('it.owner === owner') == 2, 'Bound native windows and isolate parent/cleanup owners by identity without restricting HTTPS hostnames')
require('CONSENT_REVISION = 3' in request_policy, 'Renew consent because the previous revision promised to block windows')
preferences = (ROOT / 'app/src/main/java/ao/mirevia/data/local/PreferencesRepository.kt').read_text()
for required in ['IframeRequestPolicy.hasCurrentConsent(', 'intPreferencesKey("external_player_consent_revision")', 'it[PLAYER_CONSENT_REVISION] = IframeRequestPolicy.CONSENT_REVISION', 'it.remove(PLAYER_CONSENT_REVISION)']:
    require(required in preferences, f'Renew and revoke the broader player consent: {required}')
require(re.search(r'externalPlayerConsent\s*=\s*IframeRequestPolicy.hasCurrentConsent\(', preferences), 'Do not grant broader access from the old boolean alone')
settings_screen = (ROOT / 'app/src/main/java/ao/mirevia/ui/settings/SettingsScreen.kt').read_text()
player_screen = (ROOT / 'app/src/main/java/ao/mirevia/ui/player/PlayerScreen.kt').read_text()
require('if (enabled) consentDialog = true' in settings_screen and 'else update { container.preferences.setPlayerConsent(false) }' in settings_screen, 'Settings must ask before enabling consent, but allow revocation without approval')
require('ExternalPlayerConsentDialog(' in settings_screen and 'ExternalPlayerConsentDialog(' in player_screen, 'Share the same explicit third-party disclosure in settings and player')
require('preferences?.externalPlayerConsent == true' in player_screen, 'Do not create the iframe before current consent')
require('PlayerWindowsOverlay(windows, snackbar)' in player_screen and 'windows.activeWindow != null -> windows.closeActive()' in player_screen and 'windows.closeAll()' in player_screen, 'Keep visible window chrome, Back and lifecycle cleanup')
window_surface = (ROOT / 'app/src/main/java/ao/mirevia/ui/player/PlayerWindowSurface.kt').read_text()
for required in ['Janela do player · terceiros', 'Origem: https:', 'Text("Fechar")', 'onRelease = {}', 'Dialog(', 'dismissOnBackPress = true', 'dismissOnClickOutside = false']:
    require(required in window_surface, f'Windows need distinct origin-only native chrome and controller-owned lifetime: {required}')

consent_dialog = (ROOT / 'app/src/main/java/ao/mirevia/ui/components/ExternalPlayerConsentDialog.kt').read_text()
for required in ['Cloudflare Insights', 'HTTPS não garante', 'PlayerWindowPolicy.DESCRIPTION', 'Fechar ou Voltar', 'cookies de primeira parte', 'enabled = !storageError', 'onClick = onAccept', 'onClick = onDismiss']:
    require(required in consent_dialog, f'Keep explicit third-party disclosure and fail-closed consent: {required}')

container = (ROOT / 'app/src/main/java/ao/mirevia/app/AppContainer.kt').read_text()
require('EmbedUrlPolicy(setOf(EmbedMoviesUrlBuilder.DOCUMENTED_HOST))' in container, 'Initial playback/share sources must remain on the exact documented host, independently of HTTPS resources')
require(player.get(ANDROID + 'screenOrientation') not in {'landscape', 'sensorLandscape', 'reverseLandscape'}, 'Keep the iframe usable in portrait as well as landscape')
sharing = (ROOT / 'app/src/main/java/ao/mirevia/ui/components/MediaShareSheet.kt').read_text()
require('Intent.ACTION_SEND' in sharing and '"text/plain"' in sharing, 'Share public text through the Android chooser only')
require('EXTRA_STREAM' not in sharing and 'FLAG_GRANT_READ_URI_PERMISSION' not in sharing, 'Sharing must not grant access to video files or content URIs')
identity = (ROOT / 'app/src/main/java/ao/mirevia/security/AppIdentity.kt').read_text()
require('Build.VERSION.SDK_INT >= 28' in identity and 'PackageManager.GET_SIGNATURES' in identity, 'Keep the API 24 signature-inspection fallback')
wrapper = (ROOT / 'app/src/main/java/ao/mirevia/playback/EmbedPage.kt').read_text()
require('<iframe src=' in wrapper and 'Content-Security-Policy' in wrapper, 'A real iframe with CSP is required')
require('<script' not in wrapper and '<video' not in wrapper, 'No script injection or custom video element')
require('IframeRequestPolicy.LOCAL_DOCUMENT_URL' in wrapper and 'frame-src ${IframeRequestPolicy.FRAME_CSP_SOURCES};' in wrapper, 'Wrapper origin/CSP must match the consented resource policy')
require('FRAME_CSP_SOURCES = "https://*:443"' in request_policy and 'require(policy.isValidSource(source))' in wrapper, 'Allow HTTPS internal frames in the app CSP, not arbitrary initial sources')

provider = (ROOT / 'core/src/main/kotlin/ao/mirevia/core/playback/EmbedMoviesUrlBuilder.kt').read_text()
require('DOCUMENTED_HOST = "myembed.biz"' in provider, 'Reconfirm provider documentation before changing the host')
strings = {node.get('name'): node.text for node in ET.parse(ROOT / 'app/src/main/res/values/strings.xml').getroot()}
require(strings['external_player_notice'] == 'A reprodução é fornecida por um player externo através de um embed autorizado.', 'Required provider notice differs')
require(strings['player_load_error'] == 'Não foi possível carregar o player. Verifica a ligação à Internet e tenta novamente.', 'Required error differs')
require(strings['player_unavailable'] == 'Não foi possível encontrar uma fonte de reprodução para este conteúdo.', 'Required unavailable message differs')
require(strings['tmdb_attribution'] == 'This product uses the TMDB API but is not endorsed or certified by TMDB.', 'Missing TMDB attribution')

wrapper_jar = ROOT / 'gradle/wrapper/gradle-wrapper.jar'
expected = (ROOT / 'gradle/wrapper/gradle-wrapper.jar.sha256').read_text().split()[0]
require(hashlib.sha256(wrapper_jar.read_bytes()).hexdigest() == expected, 'Wrapper checksum differs')
with zipfile.ZipFile(wrapper_jar) as jar:
    require('org/gradle/wrapper/GradleWrapperMain.class' in jar.namelist(), 'Incomplete Gradle wrapper')
require('distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6' in (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text(), 'Gradle distribution checksum missing')
# The token-bearing APK workflow is intentionally separate from demo PR validation.
apk_workflow = (ROOT / 'docs/ci/build-apk.yml.example').read_text()
require(re.search(r'(?m)^on:\s*\n  workflow_dispatch:\s*$', apk_workflow), 'APK template must be manually dispatched')
require(not re.search(r'(?m)^  (push|pull_request|pull_request_target|workflow_run):', apk_workflow), 'Do not expose the embedded-token build on automatic events')
for required in ['secrets.TMDB_READ_TOKEN', "TMDB_PROXY_BASE_URL: ''", 'cache-disabled: true', '--no-build-cache', '--no-configuration-cache', ':app:assembleDebug', 'apksigner" verify --verbose --min-sdk-version 24', 'aapt" dump badging', "sdkVersion:'24'", 'artifacts/Mirevia.apk', 'if-no-files-found: error']:
    require(required in apk_workflow, f'Missing personal APK workflow invariant: {required}')
require('pull_request_target' not in apk_workflow, 'Do not execute untrusted PR code with the APK secret')
demo_workflow = (ROOT / 'docs/ci/android.yml.example').read_text()
require('api: [24, 35]' in demo_workflow and 'api-level: ${{ matrix.api }}' in demo_workflow, 'Include Android 7.0 in the emulator test matrix')
# setup-android@v3 defaults to "tools platform-tools"; the legacy tools package is gone.
workflow_paths = [ROOT / 'docs/ci/android.yml.example', ROOT / 'docs/ci/build-apk.yml.example']
workflow_paths += sorted((ROOT / '.github/workflows').glob('*.yml'))
workflow_paths += sorted((ROOT / '.github/workflows').glob('*.yaml'))
sdk_setup_count = 0
for path in workflow_paths:
    for step in re.split(r'(?m)^      - ', path.read_text()):
        if not re.search(r'(?m)^\s*uses:\s*android-actions/setup-android@', step):
            continue
        package_input = re.search(r'(?m)^          packages:\s*([^\n#]+)', step)
        require(package_input, f'Override the legacy SDK tools default in {path.relative_to(ROOT)}')
        packages = package_input.group(1).strip().strip("\"'").split()
        require(packages == ['platform-tools'], f'Use only platform-tools in SDK setup; install the pinned platform/build-tools separately: {path.relative_to(ROOT)}')
        sdk_setup_count += 1
require(sdk_setup_count >= 3, 'Missing Android SDK setup in CI templates')
print(f'Android SDK setup: {sdk_setup_count} steps explicitly avoid the removed legacy tools package.')
print(f'Source checks: {len(sources)} production Kotlin files; XML, TOML, manifest, iframe and security invariants OK.')
print('Theme compatibility: windowLightNavigationBar only in values-v27, with a base fallback for API 24-26.')
print('Iframe/windows: exact initial-source/share hosts and fixed iframe wrapper; HTTPS resources/frames/popups without host lists; consent revision 3 and a separate worker network gate.')
print('Windows: native transport after attachment, distinct origin-only chrome, Close/Back, bounded owner-scoped lifetime and shared browser protections.')
print('Diagnostics/sharing: typed origin-only blocks, Safe Browsing, text-only chooser and API 24 identity fallback present.')
print('Reported Kotlin regressions: player-specific immersive helper and named search onClick present.')
print('Android 7.0 configuration: minSdk 24, core library desugaring and API 24/35 test matrix present.')
print('Personal APK template: manual-only, embedded-token build flags and signature check present.')
print('These checks do not prove compilation, runtime safety or live playback compatibility.')
