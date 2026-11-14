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
for required in ['handler.cancel()', 'allowFileAccess = false', 'allowContentAccess = false', 'MIXED_CONTENT_NEVER_ALLOW', 'setAcceptThirdPartyCookies(this@SecureEmbedWebView, false)', 'request.deny()', 'removeAllCookies', 'WebView.setWebContentsDebuggingEnabled(false)']:
    require(required in webview, f'Missing WebView restriction: {required}')
require('OkHttpClient' not in webview and 'openConnection' not in webview, 'WebView must not fetch/interrogate streams itself')
wrapper = (ROOT / 'app/src/main/java/ao/mirevia/playback/EmbedPage.kt').read_text()
require('<iframe src=' in wrapper and 'Content-Security-Policy' in wrapper, 'A real iframe with CSP is required')
require('<script' not in wrapper and '<video' not in wrapper, 'No script injection or custom video element')
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
print('Reported Kotlin regressions: player-specific immersive helper and named search onClick present.')
print('Android 7.0 configuration: minSdk 24, core library desugaring and API 24/35 test matrix present.')
print('Personal APK template: manual-only, embedded-token build flags and signature check present.')
print('These checks do not prove compilation, runtime safety or live playback compatibility.')
