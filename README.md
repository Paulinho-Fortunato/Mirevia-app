# Mirevia

Aplicação Android pessoal e não comercial para descobrir filmes e séries, consultar a TMDB e organizar uma biblioteca local. **Kotlin · Jetpack Compose · MVVM · Room · TMDB · iframe EmbedMovies**.

Interface em português, tema escuro com destaque verde-menta, ícones vectoriais e sem emojis. Não existem subscrições, pagamentos, publicidade própria, downloads ou extracção de vídeos.

> **Estado da entrega:** fontes Android, testes e documentação implementados. Foram executados 22 testes JVM do domínio/fornecedor/iframe e verificações de fontes/sintaxe. **Ainda não foi validada a build Android nem gerado um APK neste ambiente**: faltam SDK/emulador e o acesso à distribuição Gradle falhou na rede restrita. Reprodução real requer validação num dispositivo e confirmação de eventuais hosts de recursos do fornecedor.

## Funcionalidades

- Início com destaque, populares, tendências, melhores avaliações, lançamentos e recomendações por título quando a TMDB está configurada.
- Explorar com tipo, género, ano, avaliação e idioma original; paginação e lazy loading.
- Pesquisa de filmes, séries, actores e realizadores, com debounce, cancelamento, paginação e histórico local.
- Detalhes, elenco, realizadores, criadores, filmografia, temporadas e episódios carregados a pedido.
- Trailers identificados como oficiais pela TMDB, abertos na plataforma original após confirmação.
- Lista pessoal, favoritos, vistos e episódios vistos em Room.
- Histórico de players abertos e “Continuar a ver” **sem percentagens/progresso inventados**.
- `VideoSourceProvider`, `EmbedMoviesProvider`, `SecureEmbedWebView` e `PlayerScreen`.
- Iframe oficial, consentimento prévio, fullscreen, repetição e mensagens de erro/indisponibilidade.
- Preferências DataStore, redução de animações, documentos legais editáveis e atribuição TMDB.
- Modo de demonstração local, claramente identificado, sem reprodução.

## Abrir e executar no Android Studio

1. Instala Android Studio com suporte a AGP 8.9.2, **JDK 17**, Android SDK Platform **35** e Build Tools 35.0.0.
2. Abre a **raiz deste repositório** como projecto. Deixa o Android Studio criar `local.properties` com o teu `sdk.dir`.
3. Sincroniza o Gradle Wrapper 8.11.1. A primeira sincronização precisa de acesso normal a Gradle, Google Maven e Maven Central.
4. Selecciona a variante **`app:debug`** e um dispositivo/emulador Android **7.0/API 24 ou superior**, com a versão mais recente do Android System WebView disponível para o aparelho.
5. Executa **Run app**. Sem configuração TMDB, arranca em demonstração; não é necessária uma chave para explorar a interface e a biblioteca local.

A aplicação é Android nativa; não existe uma versão web equivalente ou um servidor localhost a iniciar.

### Compatibilidade Android 7.0+

A versão mínima configurada é **Android 7.0 (API 24)**, incluindo Android 7.1 e versões posteriores. `compileSdk` e `targetSdk` continuam em 35: não são a versão mínima de instalação. O core library desugaring com `desugar_jdk_libs` 2.1.5 fornece as APIs `java.time` usadas nos lançamentos TMDB e nas datas do histórico, que não existem nativamente no Android 7.

O template de APK verifica o `minSdk` do pacote e a assinatura para API 24; o template de validação inclui emuladores API 24 e 35. Estes testes Android ainda não foram executados aqui. A reprodução continua dependente das capacidades do WebView instalado e do fornecedor; o app não reduz as restrições de segurança para fazer um player incompatível funcionar.

## Configurar TMDB

Não coloques credenciais no código nem no Git. Não envies tokens em chat.

### Desenvolvimento local (debug)

Obtém o **API Read Access Token** na tua conta TMDB. Adiciona-o ao `local.properties`, que está ignorado pelo Git:

```properties
# Mantém também a tua linha sdk.dir, criada pelo Android Studio.
tmdb.readToken=O_TEU_API_READ_ACCESS_TOKEN
```

O valor acima é um marcador, não uma chave. Em alternativa, exporta `TMDB_READ_TOKEN` no terminal antes de executar o Gradle. Não uses o ficheiro `.env` como se fosse carregado automaticamente; o projecto lê variáveis de ambiente e `local.properties`.

**BuildConfig protege o repositório, não o APK.** O token de debug pode ser extraído de um APK partilhado. A variante release não o inclui.

### APK pessoal com a TMDB embutida, sem backend

O workflow completo está em [`docs/ci/build-apk.yml.example`](docs/ci/build-apk.yml.example). Publica-o como `.github/workflows/build-apk.yml` através de uma conta autorizada e configura o secret de repositório **`TMDB_READ_TOKEN`** com o teu API Read Access Token. Depois executa-o manualmente em Actions e, se a build for bem-sucedida, descarrega o artefacto `Mirevia-APK`, extrai o ZIP e instala `Mirevia.apk`.

É um **APK debug assinado e instalável**. O Gradle já inclui o token nessa variante: não é necessário escrever a credencial no Kotlin nem manter um backend da Mirevia. A API TMDB continua online; embutir o token não transforma o catálogo num serviço offline. **Quem tiver o APK pode extrair o token**, mesmo que ele venha de um GitHub secret. Usa esta opção pessoal apenas aceitando esse risco; um repositório/artefacto público não mantém a credencial privada.

Para manter a assinatura entre execuções, podes configurar também `ANDROID_DEBUG_KEYSTORE_BASE64`. Sem uma chave persistente, uma nova build pode não actualizar a anterior, e desinstalar apaga os dados locais. As instruções e os cuidados estão em [docs/ci/README.md](docs/ci/README.md).

### Backend intermediário (recomendado para distribuir)

Configura `TMDB_PROXY_BASE_URL` ou `tmdb.proxyBaseUrl` com a URL HTTPS real do teu backend, terminada em `/3/`. O backend deve expor os caminhos/JSON compatíveis listados em [docs/tmdb.md](docs/tmdb.md) e inserir a credencial da TMDB no servidor. A aplicação não envia o token local para o proxy.

Não foi criado um backend público ou uma URL fictícia. Sem proxy numa release, a aplicação usa demonstração. Se uma configuração real estiver inválida, apresenta erro em vez de simular resultados.

## Reprodução exclusivamente por iframe

A documentação oficial de [embedmovies.org](https://embedmovies.org/#guia-rapido), consultada em 14-11-2026, usa **`myembed.biz`** como host dos iframes. O proprietário do projecto confirmou expressamente a sua autorização. A allowlist actual contém **apenas esse host exacto**, não todos os seus subdomínios.

```text
Filme TMDB:  https://myembed.biz/filme/{tmdbId}
Filme IMDb:  https://myembed.biz/filme/{imdbId}
Série:       https://myembed.biz/serie/{tmdbId}
Episódio:    https://myembed.biz/serie/{tmdbId}/{season}/{episode}
```

O player é incorporado numa página local HTTPS com um `<iframe>` real. Não é carregado em ExoPlayer, não são extraídos URLs, não há downloads, conversão, remoção de publicidade ou contorno de DRM/paywalls/CAPTCHA/bloqueios geográficos.

**Uma URL válida não garante conteúdo disponível.** Hosts de CDNs, scripts, anúncios ou outros recursos não confirmados podem impedir a sessão. A allowlist não é alargada automaticamente. Consultar [docs/provider-integration.md](docs/provider-integration.md) antes de alterar a integração.

- Temporada isolada e especiais/temporada 0 não têm formato de reprodução confirmado; não são inventadas rotas.
- PiP Android, posição, duração e conclusão automática **não são anunciados nem simulados**, por falta de contrato oficial de eventos. O iframe mantém os seus controlos originais.
- “Continuar a ver” volta ao conteúdo/episódio aberto, não promete retoma ao segundo.
- Abrir no navegador é opcional e leva ao **playground oficial com iframe**, com indicação do ID, nunca a um stream directo.
- Ao ir para segundo plano, o WebView é destruído; ao regressar, a sessão é recarregada.

A WebView aplica validação de origens, mas os callbacks públicos **não são um firewall completo de todos os transportes/redireccionamentos internos**. Os limites e validações ainda necessárias estão documentados em [docs/security.md](docs/security.md).

## Build de teste e instalação

Com JDK/SDK configurados e rede normal:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK esperado após uma build bem-sucedida:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Instalar num dispositivo ligado, com depuração USB autorizada:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No Windows usa `gradlew.bat`. O APK debug é assinado com a chave de desenvolvimento gerida pelo Android SDK; não é uma release de distribuição. A release exige a tua própria configuração de assinatura, fora do repositório. Não foi incluído nenhum keystore.

## Testes

```sh
python3 scripts/verify_source.py       # Python 3.11+, sem SDK
./gradlew :core:test :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest  # dispositivo/emulador
```

[docs/testing.md](docs/testing.md) distingue testes executados, testes escritos e validações pendentes. `docs/ci/android.yml.example` prepara validação em demonstração, sem segredos; `docs/ci/build-apk.yml.example` prepara a geração manual de um APK pessoal com o token TMDB embutido. **Os dois templates estão inactivos: não foi instalado um workflow GitHub Actions.** A activação por um responsável autorizado e a configuração do secret do APK estão em [docs/ci/README.md](docs/ci/README.md). Não foi validada uma execução remota destes workflows.

## Estrutura e documentos

| Caminho | Conteúdo |
| --- | --- |
| `core/` | Domínio, modelos, contratos, fornecedor, validação HTTPS/URLs e testes |
| `app/` | Android, Compose, dados TMDB/Room/DataStore, navegação e WebView |
| `gradle/libs.versions.toml` | Versões fixas das dependências |
| `docs/architecture.md` | Os 12 pontos do plano, navegação, dados e etapas |
| `docs/tmdb.md` | Endpoints oficiais, configuração, cache e atribuição |
| `docs/provider-integration.md` | Documentação consultada, URLs, capacidades e limitações |
| `docs/security.md` | Políticas reais e limites de segurança da WebView |
| `docs/testing.md` | Testes, resultados e plano de validação em dispositivo |
| `docs/ci/` | Templates inactivos de validação e APK instalável, com instruções de activação e secrets |
| `app/src/main/assets/privacy.txt` | Política de privacidade editável |
| `app/src/main/assets/terms.txt` | Termos de utilização editáveis |

Room exporta o esquema v1 em `app/schemas` na primeira build. Conserva esse esquema e acrescenta migrações explícitas antes de alterar a base de dados; não há destruição silenciosa de dados.

## Privacidade e créditos

Sem conta, analytics ou sincronização na cloud. Lista, favoritos, vistos e histórico permanecem no dispositivo. O player externo e as APIs recebem dados técnicos de ligação; o contacto com o player exige consentimento. Há controlos de limpeza e documentos legais na aplicação. Antes de distribuir, acrescenta identificação/contacto do responsável e revê as autorizações e políticas reais de terceiros/backend.

**This product uses the TMDB API but is not endorsed or certified by TMDB.**

A TMDB fornece metadados e imagens, não direitos de transmissão. A Mirevia não é afiliada, certificada, patrocinada ou aprovada pela TMDB. O ecrã Sobre utiliza um logótipo oficial aprovado, menos proeminente do que a marca da aplicação.
