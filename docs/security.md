# Segurança, privacidade e limites de garantia

Esta implementação restringe a fonte inicial e as capacidades nativas da WebView, mas, por decisão explícita do proprietário na revisão 0.2.1, **permite recursos/frames remotos HTTPS sem lista por domínio** depois de consentimento renovado. Não é uma certificação de segurança nem um firewall completo. A integração real deve ser validada em dispositivos antes de distribuição. Ver [decisão e riscos](iframe-https-policy.md).

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
| Popups/downloads/uploads | Não permitidos; bloqueio explícito, sem abertura de URLs não validados |
| TLS | Confiança de sistema e cancelamento de erros; nunca `SslErrorHandler.proceed()` |
| Permissões | Sem ficheiros, câmara, microfone, localização ou permissões nativas concedidas ao player |
| Cookies/storage | Terceiros desactivados; pedido de limpeza de cookies, cache e WebStorage ao sair |
| Contas e dados | Sem login, analytics próprios, publicidade própria, sincronização ou identificador publicitário; documentos terceiros podem ter métricas/publicidade |

Network Security Configuration desactiva cleartext e aceita apenas CAs do sistema. **Esta configuração não é uma allowlist global de domínios HTTPS**; os controlos de origem ficam nas classes de rede/WebView.

## Configuração WebView

`SecureEmbedWebView` configura:

- JavaScript apenas para o player, sem `addJavascriptInterface`, `evaluateJavascript`, scripts próprios, leitura do DOM ou alterações do player.
- `allowFileAccess=false`, `allowContentAccess=false`, acesso de ficheiros entre origens desactivado.
- `MIXED_CONTENT_NEVER_ALLOW`, gesto de utilizador para media e Safe Browsing quando disponível.
- Sem depuração remota, formulários guardados ou cache persistente pedida pelo leitor.
- Permissões e geolocalização recusadas; file chooser devolve cancelamento.
- Janelas novas não validadas recusadas, com mensagem e interrupção da sessão.
- Cookies de terceiros desactivados. Não são activados silenciosamente para contornar uma incompatibilidade.
- Cliente de service workers com a mesma verificação nos pedidos interceptáveis; ao sair, a rede dos workers é bloqueada quando essa capacidade está disponível e o cliente deixa de reter a Activity.
- Fullscreen do vídeo apenas com a capacidade da fonte e o callback do iframe. Sem PiP Android anunciado.

O wrapper tem CSP `default-src 'none'`, `frame-src https://*:443`, sem scripts, objectos, forms ou base externa. O `src` inicial continua validado estritamente; `https://*:443` no `frame-src` deixa passar frames/redireccionamentos HTTPS sem catálogo de hosts. A verificação dos pedidos observáveis mantém a porta 443 e a reserva do host local. Não remove nem reescreve CSP ou X-Frame-Options do fornecedor. O referrer utiliza a política normal strict-origin-when-cross-origin; não é falsificado para evitar protecções contra hotlink.

Se um pedido violar as restrições restantes, a sessão é interrompida: não se continua a reproduzir com anúncios selectivamente removidos. O filtro não conhece nem classifica publicidade. Um host HTTPS desconhecido, por si só, **já não é motivo de bloqueio**. `static.cloudflareinsights.com` e `cloudflareinsights.com` satisfazem a política de recursos, não a de fontes iniciais. A app não instala/injecta analytics: pode deixar que o documento remoto carregue os seus próprios serviços de métricas. HTTPS protege o transporte, **não torna esses terceiros fiáveis nem valida as suas práticas de dados**.

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

Consentimento antes de criar o iframe; pode ser revogado nas definições. A aprovação anterior só divulgava `myembed.biz` e **não é reutilizada** para o alcance mais amplo. DataStore exige simultaneamente `external_player_consent=true` e `external_player_consent_revision=2` (`IframeRequestPolicy.CONSENT_REVISION`). Revisões ausentes, antigas, desconhecidas ou futuras ficam inválidas. Aceitar grava ambos atomicamente; revogar guarda falso e remove a revisão. Não migra/apaga Room, biblioteca ou outras preferências.

Player e definições usam a mesma confirmação explícita: recursos/frames HTTPS sem aprovação por hostname, incluindo CDNs, publicidade e métricas; terceiros podem receber IP e dados técnicos/navegação. Activar o switch não aceita automaticamente. Cancelar não escreve aprovação. Falha ao ler preferências desactiva histórico e consentimento por segurança e produz aviso. Erros de escrita são apresentados, não convertidos em sucesso fictício; o WebView só nasce depois da leitura de uma aprovação válida.

Room não guarda cookies ou credenciais. Histórico limitado a 300 registos, pesquisas a 20. Favoritos/lista/vistos permanecem até remoção. A limpeza de biblioteca não apaga caches de metadados/imagens ou preferências; isso é indicado na privacidade. Backup da aplicação e transferência de dados são excluídos na configuração Android.

## Antes de distribuir

- Compilar, executar lint e os testes instrumentados, inclusive Android 7.0/API 24 e API 35. O suporte configurado não comprova execução real num dispositivo.
- Rever as políticas de rede em dispositivo, redireccionamentos e origens efectivas com o fornecedor, sem extracção de streams.
- Rever com o fornecedor os terceiros que usa, finalidades/dados e os requisitos de cookies/storage. Não é exigida aprovação de cada host HTTPS para os recursos, mas a exposição e os limites devem ser avaliados.
- Rever o backend, atribuição TMDB e os documentos legais; acrescentar identificação/contacto do responsável.
- Testar recusa/revogação de consentimento, ausência de WebView, offline, TLS inválido, segundo plano, libertação de memória e acessibilidade.
- Um APK pessoal com token embutido implica aceitar que esse token é recuperável. Não o partilhar publicamente se a credencial tiver de permanecer privada; nesse caso, usar um backend.

## Diagnóstico de origem, partilha e identidade (0.2.1)

- A UI recebe `PlaybackBlock`, não o URL bruto do recurso. O objecto só retém motivo, hostname, esquema e porta; caminhos, queries, fragmentos e credenciais são descartados antes de chegar ao estado do player. Não se registam títulos/histórico no diagnóstico. Não há persistência nem envio automático; copiar/partilhar depende do utilizador.
- O relatório distingue **«Iframe inicial autorizado: myembed.biz»** de **«Recursos e frames internos: HTTPS (443), qualquer domínio remoto.»**. Observar/copiar não adiciona fontes iniciais nem muda consentimento. Não se apresenta um hostname HTTPS como bloqueado só por não estar numa lista. Os motivos restantes incluem esquema/porta/URL, navegação principal, janela, download, Safe Browsing ou capacidades do motor.
- A partilha usa `ACTION_SEND`, `text/plain`, apenas título/página TMDB ou link oficial de iframe sem query. Não há `EXTRA_STREAM`, URI de ficheiro, concessão de acesso, exportação de biblioteca ou permissão nova. O destinatário tem a sua política de dados.
- A folha de segurança lê só PackageInfo da própria aplicação, permissões e certificados públicos; não enumera apps nem lê a chave de assinatura. As APIs de assinatura API 28+ estão guardadas e há fallback API 24. A leitura é feita em IO. O fingerprint do certificado **não é o hash do APK** e não certifica segurança.

### Alerta Google Play Protect reportado

O utilizador identificou Google Play Protect, mas não forneceu a captura integral nem o artefacto/revisão analisados. Não se pode concluir falso positivo ou infecção só a partir da descrição. A instalação reportada da versão anterior não é prova de segurança. O manifesto próprio mantém apenas Internet e estado da rede; poucas permissões também não garantem ausência de comportamento prejudicial, especialmente com conteúdo web de terceiros.

Não foi desligado Play Protect, Safe Browsing, TLS ou outra protecção. Não se altera nome/pacote/certificado para fugir à análise. A versão 0.2.1 identifica a mudança funcional de política, não uma tentativa de contornar detecção. Permitir analytics/recursos do fornecedor não resolve nem explica o alerta Google.

O procedimento de recolha do aviso, comparação do APK/certificado e eventual recurso oficial está em [player-troubleshooting.md](player-troubleshooting.md). O APK debug contém um token TMDB recuperável: considerar a exposição e rotação antes de o enviar para análise externa, sobretudo scanners públicos. Nenhum APK foi enviado a terceiros nesta sessão.
