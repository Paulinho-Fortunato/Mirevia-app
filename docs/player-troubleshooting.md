# Player, janelas e alerta do Play Protect

## Play abre uma janela: correcção 0.2.2

As capturas mais recentes mostram o player renderizado e, ao tocar em Play, `NEW_WINDOW` com domínio/esquema «não identificado». A versão 0.2.1 interrompia incondicionalmente a sessão no callback `onCreateWindow`. Esse evento **não fornece um URL**, por isso o diagnóstico não prova um destino inválido nem exige outra excepção Cloudflare.

O proprietário escolheu **«Dentro da Mirevia»**. Na fonte 0.2.2/código 4, uma WebView nova é ligada pelo transporte nativo ao motor e apresentada sobre o iframe na mesma Activity, com **Fechar/Voltar** e origem sem caminho/query. Qualquer domínio HTTPS (443) é aceite para a janela sem aprovação por host. Fechar ou falha local da janela não destrói o iframe nem limpa cookies globais. Até três janelas; se não houver capacidade/transporte válido, o aviso é local e o player não recebe `NEW_WINDOW` fatal. Não se carrega uma URL adivinhada nem se removem anúncios ou restrições do fornecedor.

É preciso renovar o consentimento para **revisão 3**, mesmo se a 2 já estava aceite. TLS/Safe Browsing, ficheiros/uploads/downloads/instalações/permissões e esquemas externos continuam protegidos. As janelas podem usar cookies de primeira parte e armazenamento WebView; terceiros podem receber IP/dados técnicos/navegação. HTTPS não é confiança. Sair/repetir/revogar/segundo plano destrói todas as janelas da sessão. Crash de um renderer partilhado ainda pode afectar o iframe.

**Validação:** 61 testes JVM, sintaxe de 96 ficheiros e verificador de 64 fontes passaram; não há build/lint Android, APK ou teste no dispositivo desta revisão. Uma app já instalada só muda depois de uma nova build analisada/instalada. Ver [plano/resultado das janelas](player-windows-plan.md) e [testing.md](testing.md).

## Histórico: recursos HTTPS e decisão 0.2.1

O proprietário conseguiu instalar uma versão anterior e reportou um bloqueio sem domínio visível. A revisão 0.2.0 acrescentou um diagnóstico só de origem; depois forneceu estes dados reais:

```text
Motivo: RESOURCE
Domínio: static.cloudflareinsights.com
Esquema: https
Porta explícita: nenhuma
Domínios autorizados (política antiga): myembed.biz
```

A antiga política aplicava a lista exacta também aos recursos. O proprietário escolheu explicitamente **permitir os recursos e frames internos HTTPS sem aprovar cada hostname**, em vez de acrescentar só Cloudflare. A revisão 0.2.1/código 3 implementa isso nas fontes; **não foi gerado/instalado um novo APK aqui nem comprovada reprodução**. A alteração não actualiza automaticamente um APK já instalado.

O alerta de risco foi identificado como **Google Play Protect**. Ainda não recebemos o aviso/captura integral, o APK sinalizado, o modelo/versão Android ou a identificação da execução de build. Permitir um recurso não explica nem resolve esse alerta; não há declaração de falso positivo ou certificação de segurança.

### O que mudou na política

| Superfície | Regra actual |
| --- | --- |
| Fonte inicial / link MyEmbed partilhado | Só HTTPS `myembed.biz` exacto e rotas oficiais com IDs positivos |
| Recursos e frames internos | HTTPS, porta padrão 443, qualquer domínio remoto, sem lista por host |
| Página principal do iframe | Só o documento local `https://appassets.androidplatform.net/player/index.html` |
| Janelas do player | HTTPS (443), qualquer domínio, sobre o iframe na mesma Activity, Fechar/Voltar; blank local do motor não é uma fonte inicial |
| Restrições preservadas | TLS/Safe Browsing quando suportado; sem HTTP, esquemas perigosos, credenciais no URL, portas alternativas, ficheiros, uploads/downloads, instalações ou permissões sensíveis |

`static.cloudflareinsights.com` já não é recusado apenas por não constar de uma lista. A [FAQ oficial Cloudflare](https://developers.cloudflare.com/web-analytics/faq/) identifica `https://static.cloudflareinsights.com/beacon.min.js` como Web Analytics e `cloudflareinsights.com` como possível destino de métricas. O diagnóstico não incluiu o caminho; identificar o hostname não demonstra que aquele pedido era vídeo, que um título está disponível ou que o player já funciona.

A Mirevia não injecta analytics, não filtra anúncios e não modifica o documento remoto. O fornecedor pode carregar publicidade/métricas/CDNs de terceiros que recebam IP e dados técnicos/navegação. **HTTPS protege o transporte, não certifica que o domínio é fiável.** A CSP própria permite `frame-src https://*:443`, sem remover a CSP/X-Frame-Options do fornecedor. Ver [decisão completa](iframe-https-policy.md) e [limites WebView](security.md).

### Consentimento renovado e nova build

1. O consentimento legado ou da revisão 2 não é reutilizado. A revisão 2 permitia recursos/frames mas dizia bloquear janelas; agora é obrigatória a 3. Mesmo com aprovação antiga guardada, o WebView não é criado até confirmação actual.
2. Depois de uma build actualizada ter sido analisada e considerada adequada para teste, o diálogo do player explica os terceiros/HTTPS e permite **Aceitar** ou **Cancelar**. Se houver alerta activo do Play Protect, não o ignores para testar: investiga-o primeiro.
3. Activar **Consentimento do player externo** nas definições abre a mesma confirmação, não aceita directamente. Desactivar revoga a aprovação. A biblioteca/histórico não são apagados por essa renovação.
4. Falhas de leitura/escrita não contam como aprovação. O diálogo mantém o iframe por carregar e apresenta erro. Repetir ou copiar um diagnóstico não autoriza mais fontes iniciais nem altera as protecções.
5. Para instalar a política nova é necessária uma nova build da revisão actual, com assinatura compatível com a anterior para a actualizar sem perder dados. Não se iniciou um workflow nem se gerou um APK nesta revisão aqui.

## Porque é que o iframe ainda pode ser bloqueado?

As rotas do proprietário correspondem às da [documentação oficial](https://embedmovies.org/):

- `https://myembed.biz/filme/969681`
- `https://myembed.biz/filme/tt22084616`
- `https://myembed.biz/serie/30984`
- `https://myembed.biz/serie/30984/1/1`

São exemplos de formato, não confirmação de disponibilidade ou direitos sobre esses títulos. O app usa metadados/IDs reais da TMDB configurada, nunca estes exemplos como catálogo real. A página local contém **um iframe real**, com `src` validado e as permissões oficiais, não um player personalizado. A app não lê o DOM nem extrai vídeo.

Permitir hosts HTTPS não remove erros do fornecedor, requisitos de cookies incompatíveis, indisponibilidade, CSP/X-Frame-Options ou problemas de WebView. HTTP, URLs malformados, credenciais no endereço, porta não padrão, tentativa de sair do main frame do iframe, downloads e Safe Browsing continuam restringidos. Criar uma janela não interrompe o iframe; uma recusa/falha de janela é apresentada nela. O iframe mantém falhas próprias e o motor pode partilhar processos. Não é filtragem selectiva de publicidade.

### Recolher o diagnóstico sem expor URLs sensíveis

1. Se surgir **Player interrompido** numa build actual adequada para teste, consulta **Domínio observado**, **Esquema**, **Porta explícita**, **Iframe inicial autorizado** e o alcance dos recursos HTTPS. O domínio observado não é uma nova fonte inicial aprovada.
2. Usa **Copiar diagnóstico** ou **Partilhar diagnóstico** opcionalmente. O relatório só inclui motivo, domínio, esquema, porta e descrições separadas da fonte inicial, recursos/frames e janelas. Não inclui título/histórico, aparelho, caminho do recurso, query, fragmento, cookies, bearer ou token.
3. Um domínio HTTPS desconhecido não deve, por si só, produzir `RESOURCE` nesta revisão. Se o relatório ainda mostrar só «Domínios autorizados: myembed.biz», confirma se instalaste a nova build/versão; não recolhas URLs de vídeo para resolver isso. Num bloqueio novo, verifica motivo/esquema/porta/restrições com o responsável, não acrescentes hosts à lista de fontes iniciais.
4. Para `UNSAFE_PAGE`, mantém Safe Browsing e interrompe a utilização dessa origem. Para `WEBVIEW_FEATURE`, actualiza o Android System WebView sem desligar protecções. `NEW_WINDOW` fatal ao tocar em Play indica o comportamento antigo: confirma a versão instalada antes de procurar domínios. A criação actual não conhece o destino no callback mas permite o transporte, com origem mostrada depois. Downloads continuam recusados, sem inventar um host ou abrir ficheiros.

Não há fallback para outra fonte/stream, extractor, UA/referrer falsificado, remoção de CSP/X-Frame-Options, CAPTCHA ou restrições do serviço. **Uma URL ou recurso permitido não garante reprodução.**

## Partilhar o título ou o link MyEmbed

A acção de partilha aparece nos detalhes e no player. Um episódio com fonte validada dispõe da sua própria partilha:

- **Partilhar título:** título e página oficial da TMDB. Não promete reprodução.
- **Partilhar link MyEmbed:** o `src` de incorporação validado. Para episódios, conserva os números de temporada/episódio da rota. O texto avisa que a reprodução funciona exclusivamente por iframe; abrir esse link directamente noutra aplicação pode não reproduzir.

O app só envia texto ao chooser Android. Não exporta vídeo, ficheiros, cookies, credenciais ou biblioteca. A aplicação destinatária passa a tratar o texto segundo a sua política. Os títulos de demonstração não geram ligações reais de reprodução.

O navegador opcional continua a abrir o playground oficial com iframe, não o `src` como vídeo solto nem um stream directo. A divulgação de suporte móvel/PiP do fornecedor refere-se ao seu player web; não prova integração com PiP nativo Android, progresso ou retoma no app.

## Investigar o Google Play Protect

A [ajuda oficial Google](https://support.google.com/googleplay/answer/2812853?hl=pt) explica que o Play Protect analisa também apps de outras fontes, pode apresentar avisos e pode bloquear/remover aplicações. Recomenda mantê-lo activo. Um pedido de análise de uma app desconhecida não é o mesmo diagnóstico que uma classificação explícita de software prejudicial; **é necessário ler o texto exacto**.

1. **Pára a utilização se houver um alerta de risco.** Guarda uma captura com o texto/categoria completos, nome da app e versão; retira dados pessoais. Não envies tokens ou keystores. Se o Android recomendar a remoção, segue a indicação em vez de contornar a protecção.
2. Confirma que o APK veio da execução pretendida deste repositório, sem alteração por terceiros. Compara o hash do ficheiro e a assinatura com uma build de origem conhecida. Não basta o ficheiro chamar-se Mirevia.apk.
3. Em **Definições → Segurança e assinatura do APK**, quando for seguro abrir a aplicação para a investigar, consulta pacote, versão, permissões do pacote instalado e SHA-256 dos certificados. Isso é identidade local, **não aprovação Google**. A mesma informação pode ser obtida externamente, sem executar a app, com as ferramentas Android:

   ```sh
   sha256sum Mirevia.apk
   apksigner verify --verbose --print-certs --min-sdk-version 24 Mirevia.apk
   aapt dump permissions Mirevia.apk
   ```

4. Revê código, dependências, origem da build e comportamento do iframe. O manifesto próprio continua a pedir apenas Internet e estado de rede. Não há serviços de acessibilidade/administração do dispositivo, instalação de pacotes ou permissões de câmara/microfone/contactos/localização/ficheiros. Bibliotecas podem acrescentar permissões internas no manifesto final; o ecrã mostra a lista real instalada. **Poucas permissões não provam ausência de comportamento prejudicial**.
5. Mantém Play Protect, Safe Browsing e as actualizações de segurança activos. Uma assinatura estável e uma build release correctamente assinada ajudam a identificar versões, mas não garantem a remoção de uma detecção.
6. Se a revisão do artefacto exacto indicar uma detecção incorrecta, o responsável pode usar o [recurso oficial indicado pela Google](https://support.google.com/googleplay/android-developer/answer/2992033). Não se deve renomear o pacote, ofuscar comportamento ou trocar de certificado para fugir ao alerta. Não foi submetido qualquer recurso nesta sessão.

**Credencial dentro do APK:** a build pessoal debug integra o token TMDB. Quem receber o APK pode recuperá-lo. Scanners públicos podem conservar/partilhar amostras; não lhes envies esse APK sem avaliar a exposição e eventual rotação da credencial. Nunca partilhes a chave privada de assinatura. O fingerprint público do certificado não é essa chave.

## O que ainda falta validar

- Android lint/build/testes instrumentados desta revisão; os testes JVM isolados não compilam a UI Android.
- Retrato/paisagem, fullscreen/back, diálogo modal/TalkBack, transporte nativo real, janelas/descendentes, limite de três, partilha/regresso ao player, consentimento revogado e Android System WebView em API 24 e API 35.
- Fontes a 1,5×/2×, TalkBack, teclado do selector, listas longas, episódios especiais, ausência de stills, erro/repetição/offline.
- Validar a renovação de consentimento de revisão 2 para 3, o mesmo diálogo nas definições, recursos/frames/janelas/redireccionamentos HTTPS não listados e workers, mantendo as recusas de esquema/porta e o wrapper principal fixo. As escolhas de destino e recursos já estão resolvidas; reprodução real autorizada ainda não foi testada.
- O alerta exacto do Play Protect e o artefacto ao qual se refere. Não foi feita análise do APK nem obtida certificação de segurança.
