# Testes e validação

## Resultados nesta sessão (14-11-2026)

| Verificação | Resultado |
| --- | --- |
| Compilação Kotlin JVM de `:core` e `EmbedPage` | Executada com Kotlin 2.1.20 e Java 17 |
| JUnit: domínio, URLs, fornecedor, filtros, wrapper, diagnóstico e partilha | **37 testes passaram** nesta revisão, compilados contra o código real de `:core` e `EmbedPage`, sem rede de catálogo/player |
| Parser PSI Kotlin, incluindo testes Android/JVM, previews e scripts Gradle | **81 ficheiros sem erros de sintaxe** nesta revisão; não resolve tipos/dependências Android |
| `python3 scripts/verify_source.py` | Passou: XML, TOML, manifesto, permissões, invariantes do iframe/WebView, checksums, templates e qualificação API 27 do tema sem duplicação em API 26 |
| Workflow pessoal e dois templates em `ci/` | YAML analisado; scripts passaram `bash -n`; os quatro setups Android pedem explicitamente `platform-tools`, sem `tools` legado; matriz API 24/35 conferida |
| Gates de minSdk e secret | Verificados anteriormente com dados simulados, sem APK ou credencial real |
| Actions: preparação Android SDK | A execução `43613957749` passou setup Java/Android/Gradle e instalação SDK após a configuração explícita de pacotes |
| Actions: Kotlin Android | A execução `43613957749` teve as falhas de `Activity.setImmersive` e `MediaSearchRow`; num log posterior fornecido pelo proprietário, a compilação Kotlin e as tarefas de unitários avançaram até ao lint |
| Actions: lint Android (histórico) | O log anterior falhou em `:app:lintDebug` com um erro `NewApi` em API 26; o PR #4 removeu a cópia antiga. O proprietário reportou instalação posterior, mas os relatórios dessa build não foram fornecidos; a revisão 0.2.0 continua sem lint Android local |
| `./gradlew :core:test :app:assembleDebug` | **Bloqueado antes da compilação**: SSLHandshakeException/EOF ao obter a distribuição Gradle no ambiente com rede restrita |
| Android SDK / emulador | Não disponíveis neste ambiente; JDK 17/compilador Kotlin temporários preparados apenas para as verificações JVM |
| Regressões de segurança simuladas | **6 casos rejeitados** pelo verificador: debugging remoto, Safe Browsing desligado, callback bruto, hosts extra, partilha de ficheiro e ausência do fallback API 24 |
| `DetailsViewModelTest` | 8 testes novos escritos para Gradle; ainda não executados aqui |
| Novos testes instrumentados | 7 casos escritos (episódios/selector, diagnóstico, partilha de texto e identidade); ainda não executados aqui |
| Previews Compose | 2 previews fictícios preparados para Android Studio, incluindo fonte a 2×; não renderizados/inspeccionados num dispositivo aqui |
| Contraste de tokens do tema | Cálculo sRGB: corpo/superfície 16,45:1; secundário/superfície 7,76:1; acento/fundo 7,53:1; texto de botão 16,50:1. Não é uma auditoria de todas as imagens/estados/ecrãs |
| Instalação de versão anterior | Reportada pelo proprietário; sem APK, log de build, API do dispositivo ou assinatura fornecidos |
| Testes de Retrofit, mapeamentos e ViewModels via Gradle | Escritos, ainda não executados aqui |
| Room, Compose, WebView e compatibilidade Android 7.0 instrumentados | Escritos, ainda não executados aqui; execução em API 24 e 35 preparada no template de CI |
| Reprodução real, PiP do fornecedor e fullscreen em dispositivo | Não validados aqui |
| APK | Não gerado nem assinado nesta sessão |

A verificação PSI é de **sintaxe**, não resolve dependências nem prova compatibilidade das APIs Android. Os resultados JVM também não comprovam que um iframe real reproduz um título. O número de ficheiros pode crescer com alterações; executar novamente os comandos abaixo.

Foram usadas ferramentas JVM temporárias em `.cache/validation-tools/`, ignoradas pelo Git e fora do patch: Java/javac 17.0.8 e compilador Kotlin 2.1.20. Os arquivos npm usados apenas como distribuição foram conferidos pelo seu digest de integridade antes da extracção; não foram adicionados ao projecto nem executados scripts npm. JUnit foi compilado das fontes oficiais `junit-team/junit4`, tag `r4.13.2` (o banner dessa fonte identifica-se como `4.13.2-SNAPSHOT`), com Hamcrest incluído nessa fonte. Os 37 testes usam o código real de domínio/wrapper, não uma reimplementação das suas regras. A execução isolada usa coroutines do compilador; não substitui a resolução das dependências fixadas pelo Gradle. Os ficheiros Kotlin novos/alterados foram formatados com ktfmt 0.59.0 no estilo Kotlin (4 espaços).

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

O template inactivo [ci/android.yml.example](ci/android.yml.example) está preparado para executar unitários, lint e build em push/PR com dados de demonstração, sem segredos. Depois de instalado, `workflow_dispatch` acrescenta uma matriz de emuladores **API 24 (Android 7.0) e API 35**. Este CI de demonstração continua como template. O proprietário instalou separadamente o workflow pessoal [`.github/workflows/build-apk.yml`](../.github/workflows/build-apk.yml), apenas manual; isso não activa testes automáticos em PRs. A instalação ou actualização de workflows exige um responsável autorizado, conforme [ci/README.md](ci/README.md).

O segundo template, [`ci/build-apk.yml.example`](ci/build-apk.yml.example), destina-se à geração **manual** de um APK pessoal com o token TMDB embutido. Primeiro executa testes/lint em demonstração; só a compilação final recebe o secret. Confirma a assinatura para API 24 com `apksigner` e verifica com `aapt` que o manifesto do APK declara `minSdk 24`, antes de o disponibilizar. A falha inicial na preparação do SDK foi ultrapassada pedindo apenas `platform-tools` à action, mantendo plataforma 35 e Build Tools 35.0.0 no passo seguinte. A [execução `43613957749`](https://github.com/Paulinho-Fortunato/Mirevia-app/actions/runs/43613957749) chegou à compilação Kotlin Android e reportou dois problemas de código, descritos abaixo. O proprietário reportou mais tarde instalar um APK da versão anterior, sem fornecer a identificação da build. Isso não valida a revisão actual nem a reprodução. Ver [ci/README.md](ci/README.md) para configuração, download e riscos de exposição do token.

## Erros de compilação Kotlin reportados em Actions

A execução `43613957749` falhou em `:app:compileDebugKotlin`, antes dos unitários Android, do lint e da geração do APK:

- `PlayerActivity` declarava `private fun setImmersive(Boolean)`, colidindo com um membro herdado de `android.app.Activity`. O helper foi renomeado para `setPlayerImmersiveMode`, incluindo a referência enviada ao `PlayerScreen`. Não se substitui o método da plataforma: o comportamento pretendido continua a ser controlar as barras via `WindowInsetsCompat`.
- `MediaSearchRow(result.media) { ... }` deixava a lambda fora dos parênteses, apesar de o último parâmetro ser `subtitle: String?`, não `onClick`. A chamada dos resultados passa agora `media = ...` e **`onClick = { ... }`** explicitamente; a assinatura do componente e as outras chamadas válidas não foram alteradas.

O verificador de fontes passou a recusar estes padrões conhecidos e referências inconsistentes ao helper. São checks de regressão textual, **não compilação Kotlin**. `MediaSearchRowTest` acrescenta dois testes Compose: seleccionar a chave correcta ao tocar no resultado e não executar `onClick` durante composição com subtítulo/conteúdo final. Usa apenas conteúdo fictício local, sem URLs de imagens, TMDB ou player; estes testes instrumentados ainda não foram executados.

Para validar as correcções, publicar os ficheiros Kotlin actualizados na branch compilada e iniciar uma nova execução. Alterar apenas o YAML não corrige estes erros de código. A confirmação real continua a exigir:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

## Lint `NewApi`: atributo do tema exige API 27

O log posterior do proprietário chegou a `:app:lintDebug` e reportou `android:windowLightNavigationBar` em `values-v26/styles.xml`. Esse atributo XML exige API 27. Na revisão anterior à correcção do PR #4 existiam **as duas cópias**, em `values-v26` e `values-v27`; criar a pasta nova sem eliminar a definição antiga não resolvia o erro.

Foi removido apenas `values-v26/styles.xml`, depois de confirmar que era idêntico ao ficheiro API 27. Naquela correcção, o conteúdo de `values-v27/styles.xml`, o tema base e o Gradle foram preservados. Nesta revisão 0.2.0 só se actualizam as cores base e a versão da app; a qualificação API 27, a herança e o mínimo API 24 permanecem. Não se aumentou `minSdk`, não se desactivou lint/NewApi e não se criou uma baseline para ocultar a falha.

O verificador de fontes exige agora uma única definição do atributo em `values-v27`, qualificação mínima API 27 e herança da base comum para API 24–26. Antes da remoção, o novo check rejeitou a cópia API 26 existente; depois da remoção passou. É uma verificação de regressão deste caso, não uma análise completa das APIs Android.

A instalação da versão anterior foi reportada pelo proprietário; esta revisão ainda precisa de execução Android completa. Depois de publicar a alteração, executar `./gradlew :app:lintDebug :app:assembleDebug` ou iniciar uma nova execução manual do workflow na revisão actualizada. Os avisos de ícones `OpenInNew` obsoletos e de criação de `Json` não eram o erro fatal indicado no log.

## Compatibilidade Android 7.0 — pendente de execução

A versão mínima configurada é API 24. Como `TmdbCatalogRepository` e `LibraryScreen` usam `java.time`, o módulo Android activa core library desugaring com `desugar_jdk_libs` 2.1.5; reduzir apenas `minSdk` não seria suficiente. `compileSdk`/`targetSdk` permanecem em 35.

`AndroidCompatibilityTest` verifica o `minSdkVersion` do APK instalado e as operações de `LocalDate`, `Instant`, `ZoneId` e `DateTimeFormatter` usadas pelo catálogo/histórico. Deve ser executado em **API 24**, não apenas num JVM de desenvolvimento ou num Android 8+ com essas classes nativas. O teste não contacta a TMDB nem o fornecedor e não contém credenciais.

O atributo XML `android:windowLightNavigationBar` exige **API 27 (Android 8.1)**, não API 26. Fica apenas em `values-v27/styles.xml`, herdando `Base.Theme.Mirevia`; a cópia antiga em `values-v26` foi removida. Em API 24–26 usa-se `Theme.Mirevia` de `values/styles.xml`, com a base escura comum. O mínimo de instalação permanece em API 24. Os callbacks WebView de API 26/27 estão anotados para essas versões e as funcionalidades AndroidX WebKit continuam protegidas por verificações de capacidade, sem relaxar a política de segurança.

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
  -jvm-target 17 -classpath "$CP" -d .tools/core-tests.jar
java -cp ".tools/core-tests.jar:$CP:$KOTLIN_HOME/lib/kotlin-stdlib.jar" \
  org.junit.runner.JUnitCore \
  ao.mirevia.core.playback.EmbedMoviesProviderTest \
  ao.mirevia.core.playback.EmbedUrlPolicyTest \
  ao.mirevia.core.playback.SourceValidationTest \
  ao.mirevia.core.model.DiscoverFiltersTest \
  ao.mirevia.core.model.MediaSharingTest \
  ao.mirevia.core.playback.PlaybackBlockTest \
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
- Wrapper real com iframe/CSP, sem scripts, bridges ou elemento video; exemplos documentados 969681, tt22084616 e 30984/1/1 intactos e atributos responsivos.
- Diagnóstico descarta caminhos/queries/fragmentos/credenciais, normaliza apenas host/esquema e não aprova origens. Casos de URL malformado, controlo, tamanho excessivo, esquemas perigosos, portas e domínios parecidos.
- Partilha de título TMDB e link de iframe: origem demo recusada, fontes hostis/queries/streams recusados, correspondência de tipo/ID e selecção de episódio, aviso iframe-only e assunto limitado sem caracteres de controlo.

### Dados e apresentação (para Gradle)

- DTOs sem inventar duração, rating, IMDb, datas ou temporadas ausentes.
- Trailers exclusivamente oficiais e URLs de imagem restritas.
- MockWebServer: caminhos, query, paginação, vazio, 401 e 429 com repetição limitada.
- SearchViewModel: debounce, cancelamento e resultados vazios sem substituição por exemplos.
- DetailsViewModel (8 testes novos, não executados aqui): primeira temporada regular, carregamento lazy/cache limitada, cancelamento não cooperativo, repetição de erro, especiais sem fonte, temporada inválida/vazia, recarregar com selecção e filme sem pedidos de temporada.

### Instrumentação

- Room: flags independentes, IDs isolados, histórico sem progresso falso, vistos por episódio e pesquisas limitadas/deduplicadas.
- Compose: cinco separadores, detalhe de demonstração com reprodução desactivada, favorito; linha de pesquisa com `onClick` nomeado, subtítulo e conteúdo final opcionais.
- WebView: restrições de settings, sem contactar o fornecedor (`autoLoad=false` no teste).
- Android 7.0+: mínimo de instalação API 24 e APIs de datas `java.time` no dispositivo, via desugaring.
- EpisodeComponents (4 testes novos): acções explícitas, fonte ausente, sinopse/acções com fonte a 2× e pesquisa/selecção de temporada, sem imagens ou pedidos de rede.
- PlaybackBlockPanel: só apresenta a origem, sem caminho/token; cópia/partilha/repetição apenas por gesto.
- ShareIntent: `ACTION_SEND` texto, sem stream, URI, componente forçado ou permissões de ficheiros.
- AppIdentity: pacote próprio, fingerprints e ausência de permissões sensíveis; executar também no API 24 para cobrir o fallback de assinatura. Todos os testes instrumentados novos continuam pendentes.

## Plano manual antes de distribuição

1. Android 7.0/API 24 e API 35, smartphone de gama média, orientação vertical/horizontal, 6,1 polegadas e fontes a 1,5×/2×.
2. Primeira abertura sem API: banner demo, lista local funcional e nenhuma reprodução falsa.
3. API configurada: conteúdos reais, ausência de resultados, 401/403/404/429, ligação lenta e offline, cancelamento ao escrever/voltar.
4. Favoritos/lista/vistos persistentes após reinício; remoção, limpeza e pesquisa recente.
5. Séries: só a temporada seleccionada carrega episódios; pesquisar/trocar/revisitar temporadas, cancelar pedidos lentos, tratar vazio/erro/repetição, still ausente e especiais; seleccionar episódio real e verificar identificadores.
6. Iframe autorizado: consentimento, recusa, revogação, carregamento e mensagens do fornecedor; nunca inserir uma URL de vídeo.
7. Origem desconhecida, popup, esquema perigoso, TLS inválido ou dependência não aprovada: bloquear e mostrar apenas diagnóstico de origem, sem fallback ou autorização automática. Copiar/partilhar não deve conter paths, queries, fragmentos, credenciais ou histórico.
8. CSP/X-Frame-Options: não remover headers; testar o estado de falha e a acção manual “não carregou”.
9. Fullscreen normal do fornecedor, expansão da superfície, botão Voltar, ida ao segundo plano e destruição/libertação do WebView.
10. PiP, progresso, duração e fim do vídeo: **nenhum controlo nativo ou valor fictício** enquanto não houver API oficial validada.
11. Navegador externo: só com preferência/confirmacão; abrir o playground oficial com iframe, não o stream.
12. TalkBack, descrições de ícones, foco, contraste, teclado, touch targets, listas longas e uso de memória/bateria.
13. Rever [security.md](security.md), em particular limites de WebView como controlo de rede. Um teste de navegação não comprova isolamento absoluto de sub-recursos.

## Revisão 0.2.0: limites específicos

O utilizador confirmou apenas um erro genérico de origem e que o alerta é do Google Play Protect. O domínio adicional, o texto integral do alerta, o APK sinalizado e a execução de build não foram fornecidos. A allowlist não mudou. Não foi executada uma sessão real de reprodução, uma análise de APK ou um recurso ao Google. Não é declarado que o alerta é um falso positivo, que a reprodução já funciona ou que a UI foi aprovada visualmente no dispositivo.

Além dos testes acima, validar partilha/regresso ao player, revogação de consentimento, postura retrato/paisagem, fullscreen/back, selector com teclado, fontes grandes e navegação das cinco áreas. A folha de identidade só apresenta metadados locais: comparar com o certificado de uma build conhecida e não confundir com certificação de segurança. Ver [player-troubleshooting.md](player-troubleshooting.md).
