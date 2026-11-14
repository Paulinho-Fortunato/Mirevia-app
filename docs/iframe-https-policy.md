# Política de recursos HTTPS do iframe — histórico 0.2.1

> Registo da decisão e dos resultados da revisão 0.2.1 (PR #6), não da revisão actual. A política de recursos/frames HTTPS continua em vigor. A regra antiga de recusar janelas foi substituída pela escolha explícita **dentro da Mirevia** na revisão 0.2.2, com consentimento 3; ver [player-windows-plan.md](player-windows-plan.md). Os testes e a versão abaixo são históricos.

## Decisão confirmada antes de programar

O proprietário forneceu um diagnóstico real: `RESOURCE`, domínio `static.cloudflareinsights.com`, esquema `https`, sem porta explícita. A versão anterior só permitia `myembed.biz` também para recursos, interrompendo a sessão inteira. O caminho do pedido não foi fornecido e não é procurado/extraído pela aplicação.

Na pergunta de alcance, o proprietário escolheu expressamente **«Permitir recursos HTTPS do player»**, em vez de autorizar apenas o domínio Cloudflare. Esta decisão substitui a antiga lista exacta **para recursos e frames internos**, não para os endereços iniciais de reprodução.

A [documentação oficial Cloudflare](https://developers.cloudflare.com/web-analytics/faq/) identifica `static.cloudflareinsights.com` como origem do script de Web Analytics e `cloudflareinsights.com` como possível destino de métricas. Isso não o transforma num servidor de vídeo nem prova disponibilidade de uma obra. A Mirevia não instala/injecta esse script: limita-se a deixar o documento do fornecedor funcionar normalmente, depois de consentimento renovado.

## Plano e limites

1. **Separar políticas:** `EmbedUrlPolicy` continua a validar as fontes iniciais/partilháveis apenas em `myembed.biz` e nas rotas oficiais. `IframeRequestPolicy` valida a página principal local e os pedidos internos separadamente.
2. **Recursos e frames internos:** aceitar domínios remotos HTTPS, porta padrão 443, sem lista por domínio. Recusar esquemas perigosos/HTTP, credenciais no URL, portas alternativas, URLs malformados e utilização remota do host reservado ao wrapper local. Não resolver DNS, ler corpos, descobrir streams ou construir um proxy.
3. **Página principal:** só `https://appassets.androidplatform.net/player/index.html`, servido localmente. Navegação HTTPS externa continua no iframe, não substitui a página principal nem abre outra app automaticamente.
4. **CSP própria:** manter `default-src 'none'`, sem script/object/form/base no wrapper; mudar apenas o seu `frame-src` para `https://*:443` (qualquer host, só porta 443) para não bloquear frames/redireccionamentos HTTPS consentidos. Não alterar CSP/X-Frame-Options/respostas/DOM do fornecedor, não falsificar referrer/UA.
5. **Preservar protecções:** TLS de sistema e cancelamento de erros, Safe Browsing quando suportado, sem ficheiros/uploads/downloads, permissões sensíveis, janelas externas não verificadas, cookies de terceiros ou bridges/injecção. Sem PiP/progresso/retoma fictícios.
6. **Renovar consentimento:** a aprovação antiga só mencionava `myembed.biz`. Não a reutilizar para este alcance mais amplo. Guardar revisão da política junto à aceitação; consentimento legado/desconhecido/futuro fica inválido. Reconfirmar também ao activar pelas definições; revogar não apaga biblioteca/histórico.
7. **Mensagens e privacidade:** mostrar separadamente a origem inicial e o alcance dos recursos. Um bloqueio restante não significa que o hostname está ausente de uma lista; pode ser esquema, porta, navegação principal ou protecção do motor. Diagnóstico conserva apenas motivo/host/esquema/porta, sem URL bruto, query, cookies, tokens ou histórico.
8. **Versão 0.2.1/código 3:** manter minSdk 24, desugaring, compile/target 35, assinatura/token e workflows inalterados. Nova build necessária; não afirmar que um APK já foi actualizado ou que reproduziu.

## Riscos aceites e garantias que não existem

Permitir HTTPS não certifica domínios como seguros. O fornecedor e terceiros podem receber IP, dados técnicos/navegação e métricas, e apresentar os seus anúncios. A Mirevia não aplica uma verificação de reputação própria por hostname, não é um ad blocker e não garante isolamento de rede. As limitações de callbacks WebView (redireccionamentos internos, WebSockets, recursos não observáveis) continuam em [security.md](security.md).

O Safe Browsing/TLS ou as políticas próprias do fornecedor ainda podem bloquear. Esta revisão não promete resolver todo o player, não contorna restrições e não resolve/certifica o alerta anterior do Google Play Protect. Esse alerta continua a exigir análise do artefacto e do aviso exactos.

## Validação prevista

- JVM: Cloudflare e domínios HTTPS não listados aceites como recursos/frames, mas nunca como fonte inicial; página principal fixa; HTTP/esquemas/portas/credenciais/URLs malformados/host local recusados; consentimento antigo e revogado inválido.
- Wrapper: iframe real, origem inicial oficial, CSP própria consistente com HTTPS, sem scripts/elementos de vídeo novos; links partilháveis não são alargados.
- Compose: confirmação explícita, cancelar sem aceitação e falha de preferências sem aceitação. Não contactar o fornecedor nos testes.
- Verificador de fontes, sintaxe e whitespace; validação Android/consentimento em API 24 e 35, service workers, fullscreen/back e reprodução real continuam dependentes do toolchain/dispositivo.

## Resultado desta implementação nas fontes

- Políticas separadas e ligadas aos callbacks WebView/worker, CSP própria `frame-src https://*:443`, fonte inicial/partilha ainda exactas e main frame apenas local.
- Consentimento de revisão 2 persistido atomicamente com aceitação; aprovação legada/desconhecida/futura recusada, revogação remove a revisão. Player e definições partilham o mesmo diálogo de confirmação. Privacidade, termos e relatórios actualizados.
- Versão 0.2.1/código 3. Não foram alteradas dependências, permissões, minSdk 24/desugaring, compile/target 35, workflows, assinatura ou token.
- Executado: **47 testes JVM** compilados contra o core/wrapper real; **86 ficheiros Kotlin** sem erro de sintaxe PSI; verificador de **58 fontes**; **14 mutações em memória** rejeitadas pelo verificador; `git diff --check` limpo. São verificações limitadas, não um teste de segurança completo.
- Escrito mas não executado: **16 casos instrumentados novos** (6 de clientes WebView/workers com `autoLoad=false`, 5 DataStore isolados e 5 de diálogo Compose). Os casos anteriores também continuam dependentes do SDK/dispositivo.
- Pendente: lint/compilação Android, APK, validação API 24/35 de actualização/consentimento, motor WebView/worker/fullscreen e reprodução real autorizada. Nenhum workflow foi iniciado nem APK publicado nesta revisão. O Play Protect continua por investigar, sem contorno ou declaração de falso positivo.
