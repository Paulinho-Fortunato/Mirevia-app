# Player interrompido e alerta do Play Protect

## O que foi relatado

O proprietário conseguiu instalar a versão anterior. No player surgiu a mensagem da Mirevia sobre um recurso de domínio/esquema não autorizado. Confirmou que o erro **não mostrava o domínio**, e que o alerta de risco vem do **Google Play Protect**. Não recebemos uma captura integral, o APK sinalizado, o modelo/versão Android ou a identificação da execução de build. Não é possível determinar a causa específica do alerta Google a partir desta descrição.

A revisão de fontes 0.2.0 acrescenta um diagnóstico local de origem, partilha e melhorias de interface. **Não contém uma lista nova de CDNs nem demonstra que o player já reproduz. Não afirma que o APK é seguro ou que o Play Protect deixará de o sinalizar.**

## Porque é que o iframe pode ser bloqueado?

As rotas fornecidas pelo proprietário correspondem às da [documentação oficial](https://embedmovies.org/):

- `https://myembed.biz/filme/969681`
- `https://myembed.biz/filme/tt22084616`
- `https://myembed.biz/serie/30984`
- `https://myembed.biz/serie/30984/1/1`

Esses valores são exemplos de formato, não confirmação de disponibilidade ou direitos sobre esses títulos. O app usa apenas metadados/IDs reais da TMDB configurada, nunca estes exemplos como catálogo real.

A página local da Mirevia contém **um iframe real**, não um player personalizado. O `src` é gerado com IDs validados e conserva as permissões `autoplay; encrypted-media; picture-in-picture; fullscreen` e `allowfullscreen`. JavaScript pertence ao documento remoto: a app não injecta código, não lê o DOM e não extrai vídeo.

O fornecedor pode depender de outras origens para scripts, imagens, publicidade, licenças ou distribuição. A lista actualmente aprovada só contém **`myembed.biz` exacto**. A documentação não publica o inventário completo. Se um pedido observável usar outra origem ou um esquema não permitido, a sessão inteira é interrompida — não é uma filtragem selectiva de anúncios.

### Recolher o diagnóstico sem expor URLs sensíveis

1. Se houver um alerta activo do Play Protect, **não ignores o alerta para testar**. Primeiro investiga-o conforme a secção seguinte.
2. Depois de uma build actualizada ser analisada e considerada adequada para teste, abre um conteúdo autorizado e confirma o consentimento do iframe.
3. Se surgir **Player interrompido**, consulta `Domínio bloqueado`, `Esquema` e os domínios autorizados.
4. Usa **Copiar diagnóstico** ou **Partilhar diagnóstico** se quiseres enviar esses dados ao responsável. O relatório só inclui motivo, domínio, esquema, porta explícita e a lista permitida. Não inclui título/histórico, modelo do aparelho, caminho do recurso, query, fragmento, cookies, bearer ou token.
5. Pede ao fornecedor confirmação da origem exacta e da sua finalidade. Um domínio que apareceu num pedido **não é automaticamente fiável**. Não adoptar `*`, confiança por sufixo ou uma lista aleatória de hosts.
6. Só depois de confirmação técnica/documental e autorização explícita se deve rever a política de recursos e repetir os testes de segurança e reprodução.

Se o motivo for `UNSAFE_PAGE`, mantém o Safe Browsing e interrompe a utilização dessa origem. Se for `WEBVIEW_FEATURE`, a versão instalada do Android System WebView não oferece as capacidades exigidas; actualizar o motor não deve implicar desactivar protecções. Para `NEW_WINDOW` ou `DOWNLOAD`, não há um domínio verificável: a app não inventa um nem abre a janela/ficheiro.

Repetir o carregamento **não** autoriza o domínio. Não há fallback para outra fonte, stream, extractor, UA/referrer falsificado, remoção de CSP/X-Frame-Options, CAPTCHA ou restrições do serviço.

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
- Retrato/paisagem, fullscreen/back, partilha/regresso ao player, consentimento revogado e Android System WebView em API 24 e API 35.
- Fontes a 1,5×/2×, TalkBack, teclado do selector, listas longas, episódios especiais, ausência de stills, erro/repetição/offline.
- Identificação da origem bloqueada, confirmação do fornecedor, revisão explícita da allowlist e uma reprodução real autorizada.
- O alerta exacto do Play Protect e o artefacto ao qual se refere. Não foi feita análise do APK nem obtida certificação de segurança.
