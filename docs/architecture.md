# Arquitectura e plano da Mirevia

Plano original apresentado antes do código em 14 de Novembro de 2026. O proprietário confirmou o host **exacto `myembed.biz`** para a fonte inicial/partilhável, conforme a documentação de embedmovies.org. Na revisão 0.2.1 escolheu explicitamente permitir **recursos e frames internos HTTPS (443) sem lista por domínio**, incluindo métricas/publicidade, com consentimento renovado. Ver o [plano da decisão](iframe-https-policy.md); não se alargou o validador das fontes iniciais.

## 1. Arquitectura

Android nativo, Kotlin, Compose, MVVM e Clean Architecture simplificada. `:core` contém domínio, contratos de repositórios, modelos e regras de reprodução, sem Android. `:app` contém adaptadores, persistência, navegação e apresentação. O contentor explícito `AppContainer` injecta as dependências; ViewModels expõem StateFlow. Nenhum composable faz chamadas Retrofit directamente.

```text
Compose → ViewModels → contratos/casos de utilização → implementações de dados
                                      ├─ TMDB: Retrofit + OkHttp + Serialization
                                      ├─ Room: biblioteca, episódios vistos, histórico, pesquisas
                                      ├─ DataStore: preferências e consentimento
                                      └─ ResolveVideoSource → VideoSourceProvider
                                                                   └─ EmbedMoviesProvider
```

`ResolveVideoSource` rejeita demonstrações e revalida a saída do fornecedor. `EmbedMoviesUrlBuilder` é o único local que conhece os formatos de iframe. O leitor separa `EmbedUrlPolicy` (fontes iniciais/partilha) de `IframeRequestPolicy` (main frame local fixo, recursos/frames HTTPS e revisão de consentimento).

## 2. Estrutura de pastas

```text
app/src/main/java/ao/mirevia/
  app/                       Application, Activity, contentor
  data/demo/                 exemplos fictícios e locais
  data/remote/tmdb/          API, DTOs, mapeamentos, imagens, limites
  data/local/                Room, DAOs, DataStore
  data/network/              conectividade do sistema, sem telemetria
  data/repository/           implementações dos contratos
  navigation/                grafo Compose e barra inferior
  ui/theme/                  tema escuro, tipografia, redução de animações
  ui/components/             cartões, skeletons, estados, partilha de texto e ligações externas
  ui/home/                   início e continuar a ver
  ui/explore/                catálogo com filtros e paginação
  ui/search/                 debounce, pessoas, histórico e paginação
  ui/details/                separadores, selector pesquisável, stills e episódios
  ui/person/                 pessoa e filmografia
  ui/library/                lista, favoritos, vistos e histórico
  ui/settings/               créditos, documentos legais e identidade do pacote
  ui/preview/                previews Compose fictícios, sem rede
  security/                  metadados públicos da assinatura/permissões próprias
  ui/player/                 estados e ecrã de reprodução
  playback/                  iframe, WebView, fullscreen, Activity isolada
app/src/main/assets/         termos e privacidade editáveis
app/src/main/res/            strings, marca vectorial, configurações de rede
app/src/test/                testes JVM de dados e apresentação
app/src/androidTest/         testes instrumentados Room, Compose e WebView
core/src/main/kotlin/ao/mirevia/core/
  model/                     modelos de domínio
  repository/                contratos para dados locais/remotos
  playback/                  fornecedor, URL builder, políticas de fonte/recursos e diagnóstico sem URL bruto
core/src/test/               testes independentes de Android
gradle/                      versões e wrapper com checksums
scripts/                     verificações de fontes e sintaxe
docs/                        arquitectura, integrações, segurança e testes
docs/ci/                     template de CI inactivo e instruções de activação
```

## 3. Navegação

Barra inferior: **Início → Explorar → Pesquisa → Minha lista → Definições**. Ícones vectoriais, texto, estado seleccionado e semântica acessível. Os separadores conservam estado com Navigation Compose.

- Início/exploração/pesquisa/biblioteca → `details/{origin}/{type}/{id}`.
- Pesquisa/elenco/criação/realização → `person/{id}` → detalhes.
- Detalhes de série → primeira temporada regular (ou especiais se for a única) → carregar apenas esses episódios. A folha de temporadas permite pesquisa e troca; até três respostas de metadados ficam em cache da ViewModel, removidas ao recarregar o detalhe.
- Detalhes/episódio → `PlayerActivity`, não exportada, sem orientação forçada: o iframe ocupa a superfície disponível em retrato/paisagem.
- A Activity isolada utiliza o mesmo `PlayerScreen` Compose, sem alterar a orientação do catálogo. Não são passados URLs por Intent: apenas identificadores que serão novamente resolvidos.
- Detalhes/episódio/player → folha de partilha → chooser Android com texto público validado.
- Definições → `legal/privacy`, `legal/terms` e folha local de segurança/assinatura.
- Voltar no leitor sai primeiro do fullscreen do fornecedor, depois do modo expandido, depois da Activity.

## 4. Modelo de dados

- `MediaKey(origin, type, id)`: chave composta que impede colisões entre TMDB/demo e filme/série.
- `MediaSummary`, `MediaDetails`, `Genre`, `PersonSummary`, `PersonDetails`, `Season`, `Episode`, `OfficialTrailer`.
- `LibraryEntry`: flags independentes de favorito, lista e visto, com actividade recente.
- `PlaybackEntry`: abertura, saída, episódio, posição/duração **nulas** enquanto não existir suporte comprovado.
- `EpisodeWatchState`: marcação manual de episódio visto.
- `SearchEntry` é representada pelo `SearchEntity` local e exposta como consultas ordenadas.
- `VideoSource`, `VideoSourceType.IFRAME`: contrato solicitado, sem ficheiros ou streams.
- `Page`, `DiscoverFilters`, `HomeFeed`, `CatalogSection`, `SearchResult`.
- `SharePayload`/`MediaSharing`: links públicos TMDB/MyEmbed validados, sem media ou token.
- `PlaybackBlock`: motivo, domínio, esquema e porta; o construtor privado impede retenção acidental do URL bruto no estado/relatório.

Room guarda snapshots mínimos dos conteúdos para mostrar a biblioteca mesmo sem rede. O esquema v1 é exportado por KSP para `app/schemas` na primeira build. Alterações posteriores exigem migração explícita e testes; não existe `fallbackToDestructiveMigration`.

Sem contas ou sincronização. Os contratos de repositório permitem uma futura implementação remota, que não está simulada.

## 5. TMDB

Ver [tmdb.md](tmdb.md). API oficial para metadados e imagens, sem alegação de direitos de transmissão. API Read Access Token (bearer) apenas em debug, obtido de configuração ignorada pelo Git. Releases não incluem esse token; usar um proxy HTTPS ou aceitar modo demo.

Sem configuração: exemplos fictícios com banner e reprodução desactivada. Configuração inválida ou erro de API: estado de erro, nunca substituição silenciosa por demonstrações.

Idioma de metadados inicial `pt-PT`. Imagens e tamanhos vêm de `/configuration`. Paging máximo 500 conforme o limite de paginação da API. Pedidos concorrentes limitados a quatro, com intervalo local mínimo de 250 ms e uma única repetição controlada de 429. Esse é um limite conservador da aplicação, não uma promessa de capacidade do servidor.

## 6. EmbedMovies

Ver [provider-integration.md](provider-integration.md). Apenas quatro formas de iframe confirmadas na documentação. Não há scraping, pesquisa por título, downloads, links alternativos ou endpoints inferidos.

O host inicial documentado é `myembed.biz`, aprovado pelo proprietário. As rotas iniciais e partilháveis continuam exactas; os recursos/frames internos seguem HTTPS (443) sem lista de hosts, por decisão explícita. Os terceiros podem receber dados técnicos, publicidade/métricas próprias, sem a app injectar os seus scripts. Uma URL estruturalmente válida não confirma disponibilidade, confiança ou autorização sobre uma obra específica.

## 7. EmbedMoviesProvider

`VideoSourceProvider → EmbedMoviesProvider → EmbedMoviesUrlBuilder → EmbedUrlPolicy`.

IDs TMDB positivos, IMDb `tt` com 7–10 dígitos, sem zeros exclusivos. Parâmetros de episódio exigem temporada e episódio positivos. Sem rota de temporada isolada; os especiais TMDB (temporada 0) podem ter metadados, mas não são reproduzidos por um formato não documentado. Falha devolve `null`, sem tentar outro serviço ou fazer fallback de um IMDb malformado.

Fullscreen do iframe é documentado. A flag PiP nativo é falsa: não existe contrato de eventos/integracão Android confirmado. Legendas externas ficam nulas.

## 8. WebView

Wrapper local HTTPS com WebViewAssetLoader e um iframe real, CSP `default-src 'none'; frame-src https://*:443`, sem scripts próprios nem bridges. `EmbedUrlPolicy` valida apenas a fonte inicial/partilhável: host/rota/IDs/flags. `IframeRequestPolicy` permite domínios HTTPS (443) para frames e recursos observáveis sem lista por hostname; `shouldOverrideUrlLoading`, `shouldInterceptRequest` e o cliente de service workers delegam a essa política. A página principal só pode ser o URL exacto do wrapper servido localmente; o host reservado não é requisitado remotamente como recurso. TLS sem excepções, sem ficheiros, uploads, permissões sensíveis, janelas não validadas, conteúdo misto ou cookies de terceiros. O filtro não descarrega nem lê respostas.

**A WebView não é um firewall de rede.** Ver [security.md](security.md) para limites em redireccionamentos de sub-recursos, WebSockets, armazenamento interno e observabilidade do iframe. Esta implementação restritiva não é uma certificação de isolamento absoluto de rede.

## 9. PlayerScreen

```text
┌──────────────────────────────────────────────────────┐
│ Voltar   Título                             Expandir │
├──────────────────────────────────────────────────────┤
│                                                      │
│           Iframe externo, sem controlos fictícios    │
│                                                      │
├──────────────────────────────────────────────────────┤
│ Aviso de origem externa · Repetir · Não carregou?     │
│ Página oficial (apenas com preferência e confirmação) │
└──────────────────────────────────────────────────────┘
```

Estado inicial de resolução de metadados, indisponibilidade, consentimento, carregamento do wrapper, erro de rede/TLS/renderização e bloqueio de segurança com diagnóstico de origem copiável/partilhável por gesto explícito. O relatório distingue a fonte inicial das permissões HTTPS de recursos: observar/copiar não acrescenta fontes iniciais nem altera consentimento. A página carregada não é confundida com um vídeo iniciado. Ecrãs de erro e diálogos têm scroll para acomodar orientação horizontal e letras maiores.

Sem slider de progresso, selector de qualidade/legendas ou botões falsos. O modo expandido funciona sobre a superfície WebView; fullscreen de vídeo vem de `onShowCustomView`. No segundo plano o iframe é destruído e o regresso recarrega a sessão; não há reprodução de fundo nem PiP nativo implícito.

`ExternalPlayerConsentDialog` é comum ao player e às definições. DataStore só expõe aprovação com booleano verdadeiro e revisão actual (2); aprovação anterior sem revisão não autoriza o novo alcance. Aceitar grava os dois campos atomicamente, revogar remove a revisão, erros falham fechados. Não altera a base de dados da biblioteca.

Os textos de origem, falha e indisponibilidade pedidos estão em `strings.xml`. A abertura externa usa o playground oficial, que também contém iframe; não abre o `src` como player de vídeo solto.

## 10. Etapas

1. Fundação Android, domínio e configuração segura.
2. Tema escuro, navegação e início com demonstração identificada.
3. TMDB, DTOs, pesquisa, exploração, limites e paginação.
4. Room, biblioteca, detalhes, pessoas e episódios a pedido.
5. Construtor de URLs e fornecedor isolado, com testes negativos.
6. Wrapper iframe, consentimento, WebView, fullscreen e histórico observável.
7. Definições, atribuição, documentos legais, erros e acessibilidade.
8. Testes de domínio, configuração, unitários Android, instrumentação, lint e build.

As fontes das etapas 1–7 estão implementadas. A validação Android da etapa 8 ainda depende de Android SDK e acesso aos repositórios Gradle/Google Maven; não é afirmado que foi gerado um APK neste ambiente.

## 11. Dependências

Versões fixas em `gradle/libs.versions.toml`: AGP 8.9.2, Gradle 8.11.1, Kotlin/Compose plugin 2.1.20, Compose BOM 2025.04.01, Navigation, Lifecycle, Material 3, Coroutines/Flow, Retrofit, OkHttp, Kotlin Serialization, Room/KSP, DataStore, Coil e AndroidX WebKit.

Sem anúncios próprios, SDK de pagamento, analytics próprios ou ExoPlayer para o embed. Os documentos do fornecedor podem carregar publicidade/métricas depois do consentimento, sem injecção/alteração pela app. Trailers oficiais abrem a plataforma original após confirmação. Media3 só deve ser acrescentado para conteúdos explicitamente compatíveis/autorizados, não para streams extraídos do fornecedor.

## 12. Testes

Ver [testing.md](testing.md): testes de fornecedor e URLs, wrapper iframe, filtros, mapeamentos, Retrofit/MockWebServer, debounce/cancelamento, Room, navegação e WebView. Fontes estáticas e sintaxe são verificadas separadamente da compilação. O template de CI em `docs/ci/android.yml.example` usa demonstração e não necessita de chaves; fica inactivo até ser instalado por um responsável com permissão de escrita de workflows, por decisão do proprietário. Testes reais do fornecedor devem ser manuais, em dispositivo e com conteúdo autorizado.

## Revisão visual e de transparência 0.2.0

O plano prévio e os limites estão em [player-design-plan.md](player-design-plan.md). O tema usa preto/carvão, marfim e cereja, sem copiar a marca Netflix. Cartazes e stills reais continuam a vir da TMDB; fallback abstracto/demo é explícito. Filtros do início só filtram o feed já carregado, e isso é indicado; Explorar é o catálogo paginado. A biblioteca mostra contagens reais de flags locais.

Os detalhes separam Episódios/Sobre/Mais títulos. Trocar de temporada cancela a anterior, verifica cancelamento antes de publicar e oferece repetição de erro sem fallback demo. Especiais conservam metadados, mas não inventam uma rota de reprodução. A marcação de visto é manual.

`readAppIdentity` usa o PackageManager apenas para o próprio pacote e fingerprints SHA-256 públicos, em IO, com fallback de API 24 e API 28+ guardada. Não enumera outras apps nem lê chaves privadas. A folha de segurança não faz análise antivírus nem certifica a aplicação; mantém a recomendação de investigar o Play Protect sem o desactivar.

Historicamente, a revisão 0.2.0/código 2 manteve as dependências, minSdk 24, desugaring, compile/target 35, fornecedor e allowlist exacta então aplicada também aos recursos. Não alterou workflows, assinatura ou token. Os resultados dessa revisão não comprovam uma build nova.

## Revisão 0.2.1 — recursos HTTPS e consentimento

Após o diagnóstico `RESOURCE`/`https`/`static.cloudflareinsights.com` sem porta explícita, o proprietário escolheu eliminar a lista por host **dos recursos e frames internos**. A implementação separa essa política da fonte inicial/partilha, alinha o `frame-src` da CSP própria e renova a aprovação nas duas entradas da UI. Versão de fontes 0.2.1/código 3; dependências, mínimo API 24/desugaring, compile/target 35, permissões, workflows, assinatura e token preservados.

HTTPS não certifica uma origem; este alcance aumenta a exposição a terceiros e não promete reprodução nem resolve o Play Protect. Ver [testing.md](testing.md) para testes realmente executados e validação Android ainda pendente.
