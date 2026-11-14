# Testes e validação

## Resultados nesta sessão (14-11-2026)

| Verificação | Resultado |
| --- | --- |
| Compilação Kotlin JVM de `:core` e `EmbedPage` | Executada com Kotlin 2.1.20 e Java 17 |
| JUnit: domínio, URLs, fornecedor, filtros, wrapper | **22 testes passaram** |
| Parser PSI Kotlin, incluindo testes e scripts Gradle | **64 ficheiros sem erros de sintaxe** na verificação anterior; o novo teste de compatibilidade Android 7.0 ainda não foi verificado com PSI |
| `python3 scripts/verify_source.py` | Passou: XML, TOML, manifesto, permissões, invariantes do iframe/WebView, checksums e restrições do template de APK |
| Templates `ci/android.yml.example` e `ci/build-apk.yml.example` | YAML analisado; 11 scripts passaram `bash -n`; matriz API 24/35 conferida; gates de minSdk e secret testados com dados simulados, sem APK ou credencial real |
| `./gradlew :core:test :app:assembleDebug` | **Bloqueado antes da compilação**: SSLHandshakeException/EOF ao obter a distribuição Gradle no ambiente com rede restrita |
| Android SDK / emulador | Não disponíveis neste ambiente |
| Testes de Retrofit, mapeamentos e ViewModels via Gradle | Escritos, ainda não executados aqui |
| Room, Compose, WebView e compatibilidade Android 7.0 instrumentados | Escritos, ainda não executados aqui; execução em API 24 e 35 preparada no template de CI |
| Reprodução real, PiP do fornecedor e fullscreen em dispositivo | Não validados aqui |
| APK | Não gerado nem assinado nesta sessão |

A verificação PSI é de **sintaxe**, não resolve dependências nem prova compatibilidade das APIs Android. Os resultados JVM também não comprovam que um iframe real reproduz um título. O número de ficheiros pode crescer com alterações; executar novamente os comandos abaixo.

Foram usadas ferramentas JVM temporárias em `.tools/`, ignoradas pelo Git. Não fazem parte do projecto entregue. JUnit foi compilado das fontes oficiais `junit-team/junit4`, tag `r4.13.2` (o banner dessa fonte identifica-se como `4.13.2-SNAPSHOT`). Os 22 testes usam o código real de domínio/wrapper, não uma reimplementação das suas regras.

## Execução normal, com Android Studio/SDK

Sem credenciais, para testar apenas a demonstração:

```sh
./gradlew :core:test :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

No Windows, usa `gradlew.bat`. O último comando requer dispositivo/emulador e Android System WebView activo. Não depende de títulos disponíveis no fornecedor nem de uma conta TMDB.

Relatórios:

- `core/build/reports/tests/test/index.html`
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`
- `app/build/reports/androidTests/connected/debug/index.html`

O template inactivo [ci/android.yml.example](ci/android.yml.example) está preparado para executar unitários, lint e build em push/PR com dados de demonstração, sem segredos. Depois de instalado, `workflow_dispatch` acrescenta uma matriz de emuladores **API 24 (Android 7.0) e API 35**. **Não existe um workflow GitHub Actions activo nem uma execução remota validada neste projecto.** O proprietário optou por manter CI como template porque a ligação GitHub não tem permissão para publicar workflows. A activação exige um responsável com essa permissão, conforme [ci/README.md](ci/README.md).

O segundo template, [`ci/build-apk.yml.example`](ci/build-apk.yml.example), destina-se à geração **manual** de um APK pessoal com o token TMDB embutido. Primeiro executa testes/lint em demonstração; só a compilação final recebe o secret. Confirma a assinatura para API 24 com `apksigner` e verifica com `aapt` que o manifesto do APK declara `minSdk 24`, antes de o disponibilizar. Não foi executado remotamente aqui: a existência do template e a validação da sua estrutura não equivalem a uma build Android bem-sucedida. Ver [ci/README.md](ci/README.md) para configuração, download e riscos de exposição do token.

## Compatibilidade Android 7.0 — pendente de execução

A versão mínima configurada é API 24. Como `TmdbCatalogRepository` e `LibraryScreen` usam `java.time`, o módulo Android activa core library desugaring com `desugar_jdk_libs` 2.1.5; reduzir apenas `minSdk` não seria suficiente. `compileSdk`/`targetSdk` permanecem em 35.

`AndroidCompatibilityTest` verifica o `minSdkVersion` do APK instalado e as operações de `LocalDate`, `Instant`, `ZoneId` e `DateTimeFormatter` usadas pelo catálogo/histórico. Deve ser executado em **API 24**, não apenas num JVM de desenvolvimento ou num Android 8+ com essas classes nativas. O teste não contacta a TMDB nem o fornecedor e não contém credenciais.

Os recursos de navegação exclusivos de API 26 ficam em `values-v26`; o tema de API 24 mantém a base escura. Os callbacks WebView de API 26/27 estão anotados para essas versões e as funcionalidades AndroidX WebKit continuam protegidas por verificações de capacidade, sem relaxar a política de segurança.

Em Android Studio, cria um emulador Android 7.0/API 24 (ou liga um dispositivo real equivalente), selecciona-o e executa:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Depois repete os testes em API 35. Em caso de múltiplos dispositivos ligados, selecciona o pretendido no IDE ou define `ANDROID_SERIAL` para o serial correcto antes do comando. Ainda não foram executados aqui o lint Android, os testes instrumentados ou uma instalação API 24; não se declara compatibilidade de execução comprovada.

## Verificações independentes do SDK

Python 3.11 ou superior:

```sh
python3 scripts/verify_source.py
```

Se tiveres uma distribuição oficial do compilador Kotlin, JDK 17 e JUnit/Hamcrest locais, podes reproduzir a compilação JVM independente:

```sh
# KOTLIN_HOME aponta para a distribuição do compilador Kotlin 2.1.20.
# JUNIT_CP contém os caminhos locais de junit-4.13.2.jar e hamcrest-core-1.3.jar.
CP="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar:$JUNIT_CP"
mkdir -p .tools
"$KOTLIN_HOME/bin/kotlinc" \
  $(find core/src/main/kotlin core/src/test/kotlin -name '*.kt') \
  app/src/main/java/ao/mirevia/playback/EmbedPage.kt \
  app/src/test/java/ao/mirevia/playback/EmbedPageTest.kt \
  -classpath "$CP" -d .tools/core-tests.jar
java -cp ".tools/core-tests.jar:$CP:$KOTLIN_HOME/lib/kotlin-stdlib.jar" \
  org.junit.runner.JUnitCore \
  ao.mirevia.core.playback.EmbedMoviesProviderTest \
  ao.mirevia.core.playback.EmbedUrlPolicyTest \
  ao.mirevia.core.playback.SourceValidationTest \
  ao.mirevia.core.model.DiscoverFiltersTest \
  ao.mirevia.playback.EmbedPageTest
```

Verificação opcional PSI, não equivalente à compilação Android:

```sh
"$KOTLIN_HOME/bin/kotlinc" scripts/verify_kotlin_syntax.kt \
  -classpath "$KOTLIN_HOME/lib/kotlin-compiler.jar" -d .tools/syntax-check.jar
java -cp ".tools/syntax-check.jar:$KOTLIN_HOME/lib/*" Verify_kotlin_syntaxKt \
  app/src core/src app/build.gradle.kts core/build.gradle.kts build.gradle.kts settings.gradle.kts
```

Separador de classpath no Windows: `;` em vez de `:`.

## Cobertura implementada

### Domínio e fornecedor

- Filme TMDB e IMDb, série e episódio, formato exacto do iframe.
- IDs vazios/malformados/negativos, overflow, temporada isolada, episódio sem temporada e especiais não documentados.
- Rejeição de fontes não iframe, flags inconsistentes, legendas externas e host não aprovado.
- Recusa de identificadores demo e de saídas inválidas de outro fornecedor.
- Host exacto, credenciais no URL, portas, trailing dot, subdomínios, esquemas perigosos, barras codificadas, query/fragmentos e domínios parecidos.
- Filtros movie/TV, chave composta de dados e não colisão demo/TMDB.
- Wrapper real com iframe/CSP, sem scripts, bridges ou elemento video.

### Dados e apresentação (para Gradle)

- DTOs sem inventar duração, rating, IMDb, datas ou temporadas ausentes.
- Trailers exclusivamente oficiais e URLs de imagem restritas.
- MockWebServer: caminhos, query, paginação, vazio, 401 e 429 com repetição limitada.
- SearchViewModel: debounce, cancelamento e resultados vazios sem substituição por exemplos.

### Instrumentação

- Room: flags independentes, IDs isolados, histórico sem progresso falso, vistos por episódio e pesquisas limitadas/deduplicadas.
- Compose: cinco separadores, detalhe de demonstração com reprodução desactivada, favorito.
- WebView: restrições de settings, sem contactar o fornecedor (`autoLoad=false` no teste).
- Android 7.0+: mínimo de instalação API 24 e APIs de datas `java.time` no dispositivo, via desugaring.

## Plano manual antes de distribuição

1. Android 7.0/API 24 e API 35, smartphone de gama média, orientação vertical/horizontal, 6,1 polegadas e fontes a 1,5×/2×.
2. Primeira abertura sem API: banner demo, lista local funcional e nenhuma reprodução falsa.
3. API configurada: conteúdos reais, ausência de resultados, 401/403/404/429, ligação lenta e offline, cancelamento ao escrever/voltar.
4. Favoritos/lista/vistos persistentes após reinício; remoção, limpeza e pesquisa recente.
5. Séries: só a temporada pedida é carregada; seleccionar episódio real e verificar identificadores.
6. Iframe autorizado: consentimento, recusa, revogação, carregamento e mensagens do fornecedor; nunca inserir uma URL de vídeo.
7. Origem desconhecida, popup, esquema perigoso, TLS inválido ou dependência não aprovada: bloquear e informar, sem fallback automático.
8. CSP/X-Frame-Options: não remover headers; testar o estado de falha e a acção manual “não carregou”.
9. Fullscreen normal do fornecedor, expansão da superfície, botão Voltar, ida ao segundo plano e destruição/libertação do WebView.
10. PiP, progresso, duração e fim do vídeo: **nenhum controlo nativo ou valor fictício** enquanto não houver API oficial validada.
11. Navegador externo: só com preferência/confirmacão; abrir o playground oficial com iframe, não o stream.
12. TalkBack, descrições de ícones, foco, contraste, teclado, touch targets, listas longas e uso de memória/bateria.
13. Rever [security.md](security.md), em particular limites de WebView como controlo de rede. Um teste de navegação não comprova isolamento absoluto de sub-recursos.
