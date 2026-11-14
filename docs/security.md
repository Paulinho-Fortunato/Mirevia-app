# Segurança, privacidade e limites de garantia

Esta implementação restringe a fonte inicial e as capacidades nativas da WebView, mas permite **recursos/frames HTTPS sem lista por domínio** (decisão 0.2.1) e **janelas HTTPS dentro da Mirevia com Fechar/Voltar** (decisão 0.2.2). O novo alcance exige consentimento de revisão 3. Não é certificação de segurança nem firewall completo. A integração real deve ser validada em dispositivos antes de distribuição. Ver [recursos](iframe-https-policy.md) e [janelas](player-windows-plan.md).

## Superfícies e políticas

| Superfície | Política |
| --- | --- |
| API TMDB | HTTPS; origem do cliente fixa; sem redireccionamentos automáticos ou logging de bearer |
| Proxy opcional | HTTPS, sem credenciais no URL; não recebe token local da TMDB |
| Imagens | Caminhos e base da configuração oficial; posters apenas em `image.tmdb.org` |
| Wrapper | `https://appassets.androidplatform.net/player/index.html`, resolvido localmente, sem HTTP real para esse host |
| Iframe inicial / partilha | `EmbedUrlPolicy`: rotas oficiais em `myembed.biz` exacto; validação antes do botão, da Activity, do wrapper, do WebView e da partilha |
| Navegação | Main frame permanece no wrapper exacto; frames internos podem usar domínios HTTPS (443) não listados |
| Pedidos observáveis / workers | `IframeRequestPolicy`: HTTPS (443) sem lista de hosts, sem userinfo/URL malformado; origem do wrapper reservada ao main frame local. Sem ler corpos, extrair media ou criar proxy |
| Janelas do player | `PlayerWindowPolicy`: contexto blank local ou HTTPS (443) sem lista por host; `WebViewTransport` real, diálogo nativo na mesma Activity, origem sem path/query, Fechar/Voltar, até três janelas. Não substituem o wrapper nem abrem apps |
| Downloads/uploads/instalações | Recusados; sem ficheiros, chooser de upload, DownloadManager ou intents de instalação |
| TLS | Confiança de sistema e cancelamento de erros; nunca `SslErrorHandler.proceed()` |
| Permissões | Sem ficheiros, câmara, microfone, localização ou permissões nativas concedidas ao player |
| Cookies/storage | Cookies de terceiros desactivados em todas as WebViews; janelas top-level podem usar primeira parte/WebStorage. Só o proprietário do iframe solicita limpeza global no fim da sessão, não ao fechar um filho |
| Contas e dados | Sem login, analytics próprios, publicidade própria, sincronização ou identificador publicitário; documentos terceiros podem ter métricas/publicidade |

Network Security Configuration desactiva cleartext e aceita apenas CAs do sistema. **Esta configuração não é uma allowlist global de domínios HTTPS**; os controlos de origem ficam nas classes de rede/WebView.

## Configuração WebView

`SecureEmbedWebView` e `PlayerWindowWebView` usam as mesmas settings (`SecureBrowserSettings`):

- JavaScript para os documentos externos consentidos, sem `addJavascriptInterface`, `evaluateJavascript`, scripts próprios, leitura do DOM ou alterações do player.
- `allowFileAccess=false`, `allowContentAccess=false`, acesso de ficheiros entre origens desactivado.
- `MIXED_CONTENT_NEVER_ALLOW`, gesto de utilizador para media e Safe Browsing quando disponível.
- Sem depuração remota, formulários guardados ou cache persistente pedida pelo leitor.
- Permissões e geolocalização recusadas; file chooser devolve cancelamento.
- Múltiplas janelas activadas, mas a opção de abertura automática por JavaScript mantém-se desactivada. Janelas pedidas pelo motor são transportadas para WebViews novas; não se adivinha um destino nem se interrompe o iframe só pelo evento `onCreateWindow`.
- Cookies de terceiros desactivados. Não são activados silenciosamente para contornar uma incompatibilidade.
- O proprietário do iframe instala um único cliente de service workers para a sessão. O gate é de recursos HTTPS, não de documento principal: workers não identificam a WebView de origem. Pedidos fora do gate são recusados sem atribuição fictícia nem `NEW_WINDOW` fatal. Filhos não reconfiguram o cliente global; ao terminar a sessão, a rede de workers é bloqueada quando suportado e o cliente deixa de reter a Activity.
- Fullscreen do vídeo apenas com a capacidade da fonte e o callback do iframe. Uma janela aceite faz sair desse fullscreen para expor Fechar. Não há novo fullscreen/PiP para as janelas nem PiP Android anunciado.

O wrapper tem CSP `default-src 'none'`, `frame-src https://*:443`, sem scripts, objectos, forms ou base externa. O `src` inicial continua validado estritamente; `https://*:443` no `frame-src` deixa passar frames/redireccionamentos HTTPS sem catálogo de hosts. A verificação dos pedidos observáveis mantém a porta 443 e a reserva do host local. Não remove nem reescreve CSP ou X-Frame-Options do fornecedor. O referrer utiliza a política normal strict-origin-when-cross-origin; não é falsificado para evitar protecções contra hotlink.

No iframe principal, um pedido observável fora dos limites continua a interromper a sessão; o filtro não conhece nem classifica publicidade. Na janela, a recusa/falha é local àquela janela, que continua com Fechar, sem destruir o iframe ou limpar dados globais. Não há fecho automático de anúncios, cliques injectados ou conteúdos modificados. Um host HTTPS desconhecido, por si só, **não é motivo de bloqueio de recursos/janelas**. `static.cloudflareinsights.com` e `cloudflareinsights.com` satisfazem a política de recursos, não a de fontes iniciais. A app não instala/injecta analytics: pode deixar que o documento remoto carregue os seus próprios serviços de métricas. HTTPS protege o transporte, **não torna esses terceiros fiáveis nem valida as suas práticas de dados**.

## Transporte, UI e ciclo de vida das janelas

`onCreateWindow` não contém o URL de destino e não comprova um iniciador fiável: pode vir de um iframe de terceiros. A app cria um WebView filho novo/sem navegação, anexa-o ao diálogo nativo e só depois entrega o `WebViewTransport`/mensagem ao motor. `about:blank` é um contexto local do motor; não é uma fonte inicial autorizada. Não é feito `loadUrl`, fetch de destino, injecção ou envio ao navegador do Android.

O diálogo modal mantém foco/TalkBack fora do iframe coberto, sem parar a Activity. O cabeçalho identifica terceiros e mostra apenas a origem observada nos callbacks de início/commit/fim (actualizada em redireccionamentos); não usa um selo de confiança. Caminho/query/fragmento são descartados antes do estado Compose. Destino ainda desconhecido tem um rótulo honesto, não um hostname inventado.

O grafo limita a três janelas; um opener tem no máximo um transporte pendente até anexação. Transporte inválido/limite/timeout só recusa ou fecha a janela correspondente e mostra feedback, não um bloqueio fatal do player. Fechar/Voltar/`onCloseWindow` destrói descendentes primeiro e mantém o iframe. Falha local destrói a WebView daquela janela, preservando o painel de erro/Fechar. Não existe isolamento de processo/cookies entre janelas: um crash do renderer partilhado pode afectar também o iframe.

Retry/troca de fonte liberta o host anterior antes de instalar o worker gate da sessão seguinte. Sair, segundo plano e revogação observada encerram a sessão e janelas desse proprietário; transports pendentes são cancelados e callbacks/timeout removidos. Fechar um filho não limpa cookies/storage globalmente nem muda o cliente worker enquanto o iframe vive. A limpeza global final é apenas solicitada; não é uma garantia de eliminação imediata.

## Limitações reais da API WebView

1. **`shouldInterceptRequest` não é um firewall.** A plataforma pode chamar o callback apenas para a URL inicial de um redireccionamento de sub-recurso. Nem todos os transportes internos do motor, em particular WebSockets, são expostos por estes callbacks. A aplicação não promete que estes hooks garantam isolamento absoluto de todo o tráfego de uma página de terceiros.
2. A CSP do wrapper restringe a incorporação ao esquema HTTPS, **não substitui a CSP do documento cross-origin**. Não se injecta uma CSP no iframe nem se altera a resposta do fornecedor.
3. `onPageFinished` não comprova vídeo iniciado, player funcional ou conformidade de todos os recursos. Bloqueios CSP/X-Frame-Options e erros internos podem não gerar um callback distinto. O leitor disponibiliza repetição, informação de erro e a acção manual de falha, sem inspeccionar o DOM.
4. A API pública de limpeza não garante a eliminação imediata de todos os detalhes internos do motor, caches de workers ou dados guardados pelo navegador externo. O utilizador pode limpar os dados completos nas definições do Android. Não é prometida retoma persistente.
5. A versão instalada do Android System WebView influencia permissões, protecções e fullscreen. O responsável deve manter o motor actualizado e testar o conjunto de dispositivos suportado.
6. Não há resolução de DNS, classificação de reputação própria ou isolamento de rede local. Aceitar um URL HTTPS não prova que o endereço resolvido é público, nem que o conteúdo é seguro. TLS/Safe Browsing e políticas próprias do motor/fornecedor mantêm-se; esta revisão não os contorna.

**Se o requisito de produto for uma garantia estrita de que nenhum pacote pode contactar outro host, os callbacks WebView não são suficientes.** É necessária uma solução de isolamento de rede validada fora desta implementação e autorização/revisão do fornecedor. Não implementar para isso um extractor, proxy de vídeos ou remoção de restrições do player.

## Segredos

O repositório não inclui tokens. `local.properties`, `.env`, keystores e artefactos compilados são ignorados. A configuração debug via BuildConfig evita exposição no Git, **não evita engenharia reversa do APK**. Release esvazia o token e aceita proxy ou demonstração. Um backend real deve proteger credenciais, controlar abuso, minimizar logs e cumprir os termos da TMDB.

Não adicionar logging HTTP de headers/corpos nem registos de URLs de reprodução, cookies ou dados pessoais. Não desactivar TLS no ambiente local para fazer uma build passar.

### Opção pessoal: token dentro do APK

A pedido do proprietário, `docs/ci/build-apk.yml.example` prepara um APK debug com TMDB directa: o token é obtido de um GitHub secret na compilação e fica no `BuildConfig` da aplicação. Não é escrito literalmente neste repositório. **Todos os destinatários do APK podem recuperá-lo**; em repositórios públicos, ter acesso ao artefacto pode bastar. O backend mantém-se como alternativa para quem necessite de guardar a credencial fora do cliente.

Este template só aceita execução manual, evita fornecer o token aos testes/lint em demonstração, desactiva caches partilhadas e de build/configuração e não publica fontes geradas ou logs de headers. Estas medidas reduzem exposição acidental no CI, mas não escondem o token no binário disponibilizado. A assinatura debug garante a assinatura do pacote, não a confidencialidade do token.

A opção de keystore debug persistente usa um secret separado para manter a assinatura entre builds. Não a guardar no Git, não a publicar como artefacto e não a confundir com assinatura de produção. A activação exige uma conta autorizada a escrever workflows.

## Dados locais e consentimento

Consentimento antes de criar o iframe; pode ser revogado nas definições. Aprovações anteriores, incluindo a revisão 2 que divulgava recursos/frames mas prometia bloquear janelas, **não são reutilizadas**. DataStore exige simultaneamente `external_player_consent=true` e `external_player_consent_revision=3` (`IframeRequestPolicy.CONSENT_REVISION`). Revisões ausentes, antigas, desconhecidas ou futuras ficam inválidas. Aceitar grava ambos atomicamente; revogar guarda falso e remove a revisão. Não migra/apaga Room, biblioteca ou outras preferências.

Player e definições usam a mesma confirmação explícita: recursos/frames e janelas HTTPS sem aprovação por hostname, incluindo CDNs, publicidade e métricas; terceiros podem receber IP e dados técnicos/navegação e as janelas podem usar primeira parte/WebStorage durante a sessão. Fechar/Voltar e as protecções preservadas são divulgados. Activar o switch não aceita automaticamente. Cancelar não escreve aprovação. Falha ao ler preferências desactiva histórico e consentimento por segurança e produz aviso. Erros de escrita são apresentados, não convertidos em sucesso fictício; o WebView só nasce depois da leitura de uma aprovação válida.

Room não guarda cookies ou credenciais. Histórico limitado a 300 registos, pesquisas a 20. Favoritos/lista/vistos permanecem até remoção. A limpeza de biblioteca não apaga caches de metadados/imagens ou preferências; isso é indicado na privacidade. Backup da aplicação e transferência de dados são excluídos na configuração Android.

## Antes de distribuir

- Compilar, executar lint e os testes instrumentados, inclusive Android 7.0/API 24 e API 35. O suporte configurado não comprova execução real num dispositivo.
- Rever as políticas de rede em dispositivo, redireccionamentos e origens efectivas com o fornecedor, sem extracção de streams.
- Rever com o fornecedor os terceiros que usa, finalidades/dados e os requisitos de cookies/storage. Não é exigida aprovação de cada host HTTPS para os recursos, mas a exposição e os limites devem ser avaliados.
- Rever o backend, atribuição TMDB e os documentos legais; acrescentar identificação/contacto do responsável.
- Testar recusa/revogação de consentimento, ausência de WebView, offline, TLS inválido, segundo plano, libertação de memória e acessibilidade.
- Um APK pessoal com token embutido implica aceitar que esse token é recuperável. Não o partilhar publicamente se a credencial tiver de permanecer privada; nesse caso, usar um backend.

## Diagnóstico de origem, partilha e identidade (0.2.2)

- A UI recebe `PlaybackBlock`, não o URL bruto do recurso. O objecto só retém motivo, hostname, esquema e porta; caminhos, queries, fragmentos e credenciais são descartados antes de chegar ao estado do player. Não se registam títulos/histórico no diagnóstico. Não há persistência nem envio automático; copiar/partilhar depende do utilizador.
- O relatório distingue **«Iframe inicial autorizado: myembed.biz»**, **«Recursos e frames internos: HTTPS (443), qualquer domínio remoto.»** e **«Janelas do player: HTTPS (443), qualquer domínio remoto, dentro da Mirevia.»**. Observar/copiar não adiciona fontes iniciais nem muda consentimento. Não se apresenta um hostname HTTPS como bloqueado só por não estar numa lista. Os motivos restantes do iframe incluem esquema/porta/URL, navegação principal, download, Safe Browsing ou capacidades do motor. `NEW_WINDOW` mantém-se como código legível de diagnóstico antigo; não é emitido na criação de janelas actual.
- A partilha usa `ACTION_SEND`, `text/plain`, apenas título/página TMDB ou link oficial de iframe sem query. Não há `EXTRA_STREAM`, URI de ficheiro, concessão de acesso, exportação de biblioteca ou permissão nova. O destinatário tem a sua política de dados.
- A folha de segurança lê só PackageInfo da própria aplicação, permissões e certificados públicos; não enumera apps nem lê a chave de assinatura. As APIs de assinatura API 28+ estão guardadas e há fallback API 24. A leitura é feita em IO. O fingerprint do certificado **não é o hash do APK** e não certifica segurança.

### Alerta Google Play Protect reportado

O utilizador identificou Google Play Protect, mas não forneceu a captura integral nem o artefacto/revisão analisados. Não se pode concluir falso positivo ou infecção só a partir da descrição. A instalação reportada da versão anterior não é prova de segurança. O manifesto próprio mantém apenas Internet e estado da rede; poucas permissões também não garantem ausência de comportamento prejudicial, especialmente com conteúdo web de terceiros.

Não foi desligado Play Protect, Safe Browsing, TLS ou outra protecção. Não se altera nome/pacote/certificado para fugir à análise. A versão 0.2.2 identifica a mudança funcional de janelas, não uma tentativa de contornar detecção. Permitir analytics/recursos/janelas do fornecedor não resolve nem explica o alerta Google.

O procedimento de recolha do aviso, comparação do APK/certificado e eventual recurso oficial está em [player-troubleshooting.md](player-troubleshooting.md). O APK debug contém um token TMDB recuperável: considerar a exposição e rotação antes de o enviar para análise externa, sobretudo scanners públicos. Nenhum APK foi enviado a terceiros nesta sessão.
