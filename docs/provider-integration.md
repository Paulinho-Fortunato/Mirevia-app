# EmbedMovies — integração oficial por iframe

## Fonte documental e autorização

Documentação consultada: https://embedmovies.org/#guia-rapido, em **14 de Novembro de 2026**. O site indica que a reprodução funciona exclusivamente através de iframe e não fornece links directos para players externos.

A documentação actual usa **`myembed.biz`**, não `embedmovies.org`, no atributo `src`. O proprietário deste projecto confirmou expressamente esse host na allowlist durante a sessão de implementação. Essa aprovação continua sem abranger subdomínios ou futuras alterações **da fonte inicial/partilhável**. Separadamente, depois do diagnóstico de `static.cloudflareinsights.com`, o proprietário escolheu expressamente recursos e frames internos HTTPS (443) sem lista por hostname, após consentimento renovado. A decisão e os riscos estão em [iframe-https-policy.md](iframe-https-policy.md).

## Rotas documentadas

| Conteúdo | URL de iframe |
| --- | --- |
| Filme por TMDB | `https://myembed.biz/filme/{tmdbId}` |
| Filme por IMDb | `https://myembed.biz/filme/{imdbId}` |
| Série/lista do fornecedor | `https://myembed.biz/serie/{tmdbId}` |
| Episódio | `https://myembed.biz/serie/{tmdbId}/{season}/{episode}` |

As chavetas são parâmetros descritivos, nunca URLs enviadas ao serviço. Não existe no código uma rota inventada de temporada isolada. A temporada é seleccionada no catálogo e é passado um episódio específico.

A documentação não confirma explicitamente a convenção de especiais (temporada 0), uma API de eventos, retoma por timestamp ou integração Android de PiP. Não são pressupostas essas capacidades.

## Contratos e responsabilidades

```text
VideoSourceProvider
    └── EmbedMoviesProvider
          ├── EmbedMoviesUrlBuilder  — formatos e identificadores
          └── EmbedUrlPolicy         — fonte inicial/partilha: HTTPS, host exacto, path e flags

ResolveVideoSource
    ├── rejeita a origem DEMO
    ├── chama o fornecedor com metadados válidos
    └── revalida a VideoSource recebida

SecureEmbedWebView / service workers
    └── IframeRequestPolicy           — main frame local fixo; recursos/frames HTTPS (443)
```

A interface e `VideoSource` seguem os modelos pedidos. O fornecedor não faz pedidos de rede, scraping ou tentativas de disponibilidade. É uma construção local determinística, cancelável pelo contexto coroutine do chamador e fácil de testar.

- `tmdbId` deve ser positivo, sem overflow de `Int`.
- IMDb, se presente, deve ser `tt` com 7–10 dígitos e não apenas zeros. Vazio, espaços, prefixo de pessoa, caracteres adicionais ou query string invalidam a fonte.
- Um IMDb malformado não provoca fallback silencioso para TMDB.
- `title`/`year` não participam na URL e não são usados para descobrir vídeos.
- Temporada e episódio têm de estar ambos ausentes ou ambos presentes e positivos.
- A policy rejeita credenciais, esquemas não HTTPS, portas não padrão, queries, fragmentos e caminhos não documentados para uma fonte.
- Só os hosts exactos entram na allowlist **da fonte inicial/partilhável**. `myembed.biz.attacker.com`, `sub.myembed.biz` e nomes parecidos não são fontes aceites, mesmo que um documento remoto possa carregar recursos HTTPS nesses domínios.
- `isIframe=true`, `requiresWebView=true`, `type=IFRAME`.
- `supportsFullscreen=true` reflecte a permissão documentada e os callbacks normais do WebView.
- `supportsPictureInPicture=false` significa que **a aplicação não anuncia PiP nativo**. O atributo original do iframe pode permitir os controlos web do fornecedor, sem garantia de compatibilidade Android.
- `subtitlesUrl=null`: as legendas pertencem ao player; não são extraídas.

## Ciclo real

1. O detalhe é obtido da TMDB.
2. ResolveVideoSource confirma a origem, os identificadores e a fonte.
3. O botão Reproduzir só é activado se a fonte não for nula.
4. PlayerActivity recebe somente a chave do conteúdo e a selecção de episódio.
5. O leitor volta a obter metadados e a resolver/validar a fonte.
6. O utilizador confirma o contacto com o fornecedor e com os terceiros que o seu documento carregar via HTTPS. A revisão 2 do consentimento é obrigatória; o booleano legado que só mencionava `myembed.biz` não basta.
7. SecureEmbedWebView carrega uma página local HTTPS através de WebViewAssetLoader.
8. Essa página tem um único `<iframe>` com o `src` oficial, sem scripts próprios. A CSP do wrapper usa `frame-src https://*:443`; recursos/frames internos HTTPS (443) não dependem de aprovação por domínio, mas o main frame não pode sair do wrapper.
9. É registada a abertura do componente, se o histórico estiver activo. A saída é registada numa transacção local quando possível, inclusive após o fecho da Activity.
10. Não é registado um “fim do vídeo” que não possa ser observado oficialmente.

O sinal `onPageFinished` refere-se ao documento de incorporação, não a um vídeo iniciado. Há timeout de carregamento do wrapper, erros de HTTP/TLS/renderização e uma acção “O player não carregou?” para falhas internas que o iframe não expõe à aplicação.

## Histórico e retoma

“Players recentes” apresenta o último player/episódio aberto, sem percentagens nem minutos inventados. Não há integração documentada que permita ler posição, duração, legendas ou fim do vídeo. Campos de progresso ficam nulos; marcas de visto são manuais. A limpeza da sessão WebView pode impedir que o próprio fornecedor retenha estado; não é prometida retoma entre sessões.

Se o fornecedor publicar uma API oficial de eventos, esta deverá ser revista quanto a origem, consentimento e segurança antes de se acrescentar um adaptador. Não usar mensagens arbitrárias, bridges nativas ou leitura de DOM para simular essa API.

## Navegador externo

Activação opcional nas definições e confirmação a cada abertura. É aberto **`https://embedmovies.org/#playground`**, página documentada que contém a incorporação oficial. O utilizador selecciona o conteúdo e introduz o identificador mostrado.

Não se abre o `src` como reprodução top-level, não se cria um wrapper público inexistente, não se transmite um stream a ExoPlayer e não se contorna o requisito de iframe. O navegador externo tem armazenamento e regras de privacidade próprios.

## Recursos HTTPS: autorização e dependências por verificar

Não foi possível testar uma sessão real do fornecedor num Android neste ambiente. A revisão 0.2.1 aceita domínios remotos HTTPS na porta 443 para pedidos e frames internos observáveis sem lista por hostname, também pelo cliente de service workers. `static.cloudflareinsights.com` deixa de ser recusado apenas por estar fora de `myembed.biz`. A [FAQ oficial Cloudflare](https://developers.cloudflare.com/web-analytics/faq/) identifica o script `https://static.cloudflareinsights.com/beacon.min.js` como Web Analytics e `cloudflareinsights.com` como possível destino de métricas. O diagnóstico do proprietário só incluiu a origem, não o caminho; não prova que aquele pedido era vídeo nem que uma obra está disponível.

Não é uma autorização para qualquer `VideoSource` ou para navegar a página principal. HTTP/esquemas perigosos, userinfo, portas alternativas, URL malformado e uso remoto da origem do wrapper continuam recusados. TLS, Safe Browsing quando suportado, cookies de terceiros desactivados, ficheiros/downloads/permissões/janelas bloqueados mantêm-se. Se um destes limites impedir o fornecedor, a sessão é interrompida por inteiro, sem extractor, alteração de anúncios ou controlo de segurança contornado.

A confirmação renovada informa que o fornecedor e terceiros (incluindo CDNs, publicidade e métricas) podem receber IP/dados técnicos/navegação. HTTPS é segurança de transporte, não confiança da origem. A app não instala scripts de analytics nem garante isolamento de todos os transportes; os [limites WebView](security.md) continuam relevantes.

Antes de usar regularmente, rever com o proprietário do serviço:

- Origens/redireccionamentos e finalidades de terceiros, incluindo publicidade/métricas; é transparência e avaliação de exposição, não uma lista obrigatória para autorizar cada recurso HTTPS.
- Requisitos de cookies/storage e WebView Android.
- Compatibilidade com o iframe numa origem de aplicação AndroidX.
- Eventuais eventos oficiais de progresso, conclusão ou retoma.
- Confirmação de especiais/temporada 0 e de PiP na plataforma Android.

Consultar também [security.md](security.md). Não afirmar suporte técnico ou disponibilidade de uma obra apenas porque a URL foi construída.

## Diagnóstico e partilha (0.2.1)

O proprietário reportou instalação bem-sucedida da versão anterior, mas uma sessão interrompida pela política de origem. A revisão 0.2.0 acrescentou o diagnóstico; posteriormente forneceu `RESOURCE`, `https`, `static.cloudflareinsights.com`, sem porta explícita. Isto não prova um erro de TMDB ou de formato do iframe: as quatro rotas e permissões HTML foram reconferidas na documentação oficial. Os exemplos de filme 969681/tt22084616 e série 30984/1/1 são cobertos pelos testes do wrapper, sem pedidos ao fornecedor.

`PlaybackBlock` transforma o pedido numa estrutura sem URL bruto: motivo, host, esquema e porta. A UI mostra o domínio observado, a fonte inicial exacta e o alcance HTTPS dos recursos, com cópia/partilha explícita. Não guarda paths, queries, fragmentos, credenciais, títulos ou cookies. Não há telemetria própria, descoberta de streams, expansão das fontes iniciais ou envio automático do diagnóstico. Um hostname HTTPS não listado já não causa bloqueio por si só. As violações das restrições restantes continuam a interromper a sessão inteira.

A partilha do título envia a página TMDB. A opção MyEmbed envia apenas o `src` validado e um aviso de utilização exclusivamente em iframe; o episódio conserva os seus números. Partilhar texto não significa abrir o `src` como reprodução top-level ou enviar um ficheiro. A disponibilidade continua por confirmar.

O wrapper é dimensionado a 100% da superfície nativa e conserva `scrolling="no"`, `allow="autoplay; encrypted-media; picture-in-picture; fullscreen"` e `allowfullscreen`. A Activity já não força paisagem. São melhorias de layout/observabilidade, não comprovação de fullscreen/PiP/reprodução em todos os motores móveis. Ver o [guia de diagnóstico e Play Protect](player-troubleshooting.md).
