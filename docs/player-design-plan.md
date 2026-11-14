# Revisão do player, partilha e design — histórico 0.2.0

> Registo histórico da revisão 0.2.0 (PR #5). A regra de lista exacta abaixo foi substituída **só para recursos e frames internos** pela decisão explícita da revisão 0.2.1 em [iframe-https-policy.md](iframe-https-policy.md); fontes iniciais/partilha continuam exactas. Os resultados abaixo são daquela revisão, não uma nova build.

## Plano antes da implementação

Esta revisão mantém a aplicação Android nativa (Kotlin/Compose, MVVM, Room, Retrofit/TMDB, Navigation e WebView), Android 7.0/API 24+, as cinco áreas de navegação e a integração autorizada **apenas por iframe**. Não introduz backend, SDK de vídeo, extracção, downloads ou fontes alternativas.

### Player e diagnóstico

- Manter `VideoSourceProvider` / `ResolveVideoSource`, IDs positivos e rotas oficiais `myembed.biz/filme/{TMDB|IMDb}`, `/serie/{TMDB}` e `/serie/{TMDB}/{temporada}/{episódio}`.
- Conservar um documento local HTTPS com um iframe responsivo, as permissões HTML oficiais, fullscreen pelo callback WebView e sem bridges/injecção/inspecção do DOM.
- Substituir o erro sem contexto por um diagnóstico **só de domínio/esquema/porta e motivo**. Nunca guardar/copiar o URL bruto do pedido, caminho, query, fragmento, cookies ou token. Diagnóstico apenas em memória; cópia/partilha só por acção explícita.
- Interromper a sessão ao bloquear uma origem. Não autorizar automaticamente CDNs, subdomínios, popups, esquemas ou recursos. Não reescrever CSP, referrer, user agent ou anúncios para fazer o player funcionar.
- Remover a orientação paisagem forçada; manter o iframe e as acções utilizáveis em retrato e paisagem. A capacidade web de PiP não prova suporte a PiP nativo Android.

**Bloqueio conhecido:** o utilizador confirmou que o erro actual só apresenta uma mensagem genérica. Só `myembed.biz` está autorizado. A documentação oficial não fornece o inventário de origens auxiliares. O diagnóstico permitirá identificar a origem observada; a reprodução real continua dependente de confirmação técnica do fornecedor e testes no dispositivo.

### Partilha

- Folha nativa com duas opções distintas: referência do título na TMDB e link de incorporação MyEmbed validado.
- Disponível nos detalhes, no episódio e no player; o episódio partilha a sua rota específica quando válida.
- Android `ACTION_SEND`, apenas texto, sem ficheiros, streams, permissões extra ou consulta às apps instaladas.
- Informar que o link MyEmbed é para iframe e não é um vídeo directo nem confirmação de disponibilidade. A aplicação destinatária tem as suas próprias regras de privacidade.
- Demonstrações não geram links reais de reprodução.

### Direcção visual e episódios

- Identidade própria Mirevia: carvão/preto, texto marfim, acento cereja discreto, tipografia editorial forte, hierarquia e espaço consistentes. Inspiração cinematográfica, não cópia de logótipo, marca ou interface Netflix.
- Início com destaque imersivo, filtros reais sobre os títulos desse feed, cartazes com imagens TMDB e linhas horizontais leves.
- Navegação inferior com cinco destinos acessíveis; ícones vectoriais, contraste, alvos de toque >=48dp e fontes escaláveis.
- Detalhes com hero cinematográfico, acções claras, separação entre episódios/sinopse/recomendações e metadata indisponível assinalada.
- Selecção de temporada em folha pesquisável por número/nome; carregar apenas a temporada seleccionada, apresentar still, número, duração/data, sinopse expansível, visto manual, reprodução e partilha por episódio.
- Preservar loading/erro/vazio/offline, cancelamento, lazy lists/imagens, preferência de movimento reduzido e ausência de progresso/retoma fictícios.

### Play Protect

O utilizador identificou **Google Play Protect**, mas não foi fornecido o texto/captura integral nem o APK sinalizado. Não presumir falso positivo ou malware apenas pela descrição. Não desactivar ou contornar a protecção.

- Mostrar identidade local do pacote, permissões solicitadas e fingerprints SHA-256 dos certificados de assinatura (não chaves privadas nem hash do APK).
- Distinguir APK debug pessoal de publicação Google Play; uma assinatura válida não é um selo de segurança.
- Manter Safe Browsing/TLS/permissões mínimas e orientar para análise/recurso oficial depois de confirmar o artefacto e rever o alerta.
- Lembrar que o APK pessoal contém um token TMDB recuperável; não enviar esse APK a serviços públicos de análise sem considerar a exposição e rotação do token.

## Validação planeada

1. Testes JVM de sanitização do diagnóstico, links de partilha e iframe/rotas oficiais.
2. Testes de ViewModel: selecção inicial de temporada, troca, cancelamento, erro/repetição e especiais sem fonte inventada.
3. Testes Compose: acções do episódio, estados de disponibilidade, selector e diagnóstico sem URL sensível.
4. Verificador de fontes, XML e `git diff --check`; manter minSdk 24/desugaring/API 27 no tema.
5. Executar build/lint/testes Android no CI e ensaios de UI/acessibilidade em API 24 e API 35 quando o toolchain estiver disponível. Só reportar como executados os testes realmente corridos.

## Referências verificadas

- https://embedmovies.org/ — rotas, integração exclusivamente por iframe e alegações web de suporte móvel; não publica lista completa de origens auxiliares ou contrato de progresso nativo.
- https://support.google.com/googleplay/answer/2812853?hl=pt — manter Play Protect activo; alertas abrangem apps de outras fontes, análise e eventual recurso oficial. Não explica o motivo concreto deste alerta sem o seu resultado.

## Estado após implementação nas fontes

- Versão 0.2.0/código 2; identidade visual renovada, navegação de cinco áreas preservada, episódios/selector pesquisável, cache de três temporadas, partilha e diagnóstico de origem ligados à UI.
- Iframe continua intacto e exclusivo; allowlist, dependências, minSdk 24/desugaring, assinatura, token e workflows não foram alargados/relaxados. Só se removeu a orientação horizontal forçada e se preservaram os atributos HTML oficiais/responsivos.
- Identidade pública do pacote/permissões/certificados acessível nas definições, sem scanner ou certificação fictícia. Guia de Play Protect e partilha acrescentado.
- Executado: 37 testes JVM, sintaxe Kotlin de 81 ficheiros, verificador de 56 fontes de produção, seis simulações de regressão de segurança e verificação de whitespace. Quatro pares de cores do tema ultrapassam 4,5:1 em cálculo sRGB; não equivale a validar o contraste de todas as imagens/ecrãs.
- Escrito mas não executado: oito testes de DetailsViewModel, sete novos testes instrumentados e dois previews Compose.
- Pendente: lint/build Android e APK desta revisão; UI em dispositivo/API 24/35; identificar/confirmar origens auxiliares e testar reprodução autorizada; obter o alerta integral e analisar o APK sinalizado pelo Play Protect. Não há alegação de player corrigido em produção ou de alerta resolvido.
