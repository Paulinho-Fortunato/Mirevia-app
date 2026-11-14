# Integração TMDB

Documentação consultada em 14-11-2026:

- https://developer.themoviedb.org/docs/getting-started
- https://developer.themoviedb.org/docs/authentication-application
- https://developer.themoviedb.org/docs/image-basics
- https://developer.themoviedb.org/reference/search-multi
- https://developer.themoviedb.org/reference/discover-movie
- https://developer.themoviedb.org/reference/discover-tv
- https://developer.themoviedb.org/reference/movie-details
- https://developer.themoviedb.org/reference/tv-series-details
- https://developer.themoviedb.org/reference/tv-season-details
- https://developer.themoviedb.org/reference/person-details
- https://developer.themoviedb.org/docs/faq

## Endpoints utilizados

Base oficial `https://api.themoviedb.org/3/`. Apenas os tipos `movie` e `tv`, as categorias `popular` e `top_rated` e os âmbitos `multi`, `movie`, `tv`, `person` são enviados pelas implementações.

| Método | Finalidade |
| --- | --- |
| `GET configuration` | Base HTTPS e tamanhos oficiais das imagens |
| `GET genre/movie/list`, `genre/tv/list` | Géneros reais |
| `GET movie/popular`, `tv/popular` | Populares |
| `GET movie/top_rated`, `tv/top_rated` | Melhores avaliações |
| `GET trending/all/week` | Tendências semanais |
| `GET discover/movie`, `discover/tv` | Exploração e filtros suportados |
| `GET search/multi`, `search/movie`, `search/tv`, `search/person` | Pesquisa por título/nome |
| `GET movie/{id}`, `tv/{id}` | Detalhes, com append de `credits,external_ids,videos,recommendations,similar` |
| `GET tv/{id}/season/{season}` | Episódios apenas da temporada seleccionada |
| `GET person/{id}?append_to_response=combined_credits` | Pessoa e filmografia |

Os identificadores de elenco e realizadores vêm dos créditos. A pesquisa de pessoas é por nome e inclui actores e realizadores. A função é identificada pelo departamento e pelos créditos; não foi criado um endpoint fictício de realizadores.

Pesquisa textual não aceita os mesmos filtros de discover. A Mirevia mantém Pesquisa e Explorar separados. Ano usa `primary_release_year` em filmes e `first_air_date_year` em séries. Género usa `with_genres`, avaliação `vote_average.gte` (com limiar local `vote_count.gte=50`) e idioma original `with_original_language`. O idioma original não é o idioma das legendas do player.

## Configuração

Sem configuração, a aplicação usa **demonstração local**, não chamadas não autenticadas nem resultados inventados da TMDB. Um erro real não muda automaticamente para demo. Itens de demonstração têm origem própria e nunca chegam ao fornecedor de vídeo.

### Desenvolvimento pessoal

No `local.properties` ignorado pelo Git, conserva a linha `sdk.dir` criada pelo Android Studio e adiciona:

```properties
tmdb.readToken=O_TEU_API_READ_ACCESS_TOKEN
```

O exemplo acima é um marcador, não uma chave válida. Usa o **API Read Access Token** da TMDB (bearer), não a API key v3 de 32 caracteres. Também podes exportar `TMDB_READ_TOKEN` no terminal que inicia o Gradle. A variável de ambiente tem prioridade sobre `local.properties`.

**Não partilhes o token em chat, screenshots, commits ou relatórios.** Não há HTTP logging interceptor. O token não é guardado em Room/DataStore.

É usado somente na variante debug, mas é recuperável de qualquer APK debug distribuído. `BuildConfig` não é um cofre. A variante release define o token como vazio, mesmo que esteja configurado no ambiente.

### Backend intermediário

Define `TMDB_PROXY_BASE_URL` ou `tmdb.proxyBaseUrl` com a URL HTTPS real do teu backend, terminada em `/3/`. O valor não deve conter credenciais, query string ou fragmentos. Só é aceite a porta TLS padrão.

O backend deve implementar um **reverse proxy compatível com os caminhos e JSON da API TMDB v3 listados acima** e inserir o bearer token no servidor. Esta é uma interface interna da Mirevia, não um endpoint que se afirme pertencer à TMDB. Não foi inventado nem disponibilizado um backend público. A aplicação não envia o token de desenvolvimento para esse host.

O responsável deve configurar autenticação/limites adequados do backend, não criar um proxy público irrestrito, cumprir as regras da TMDB e actualizar os documentos de privacidade se o backend guardar logs. Não foi implementada sincronização de utilizadores ou de biblioteca.

Se token e proxy estiverem ausentes, a variante usa demo. Configuração HTTPS inválida produz erro visível. Quando ambos existem, o proxy tem prioridade.

## Cache, limites e cancelamento

- Cache OkHttp de metadados: máximo 24 MiB, directório privado de cache.
- Imagens Coil: máximo 32 MiB em memória e 48 MiB em disco, com carregamento lazy pelo tamanho visível.
- Pedido de frescura de cinco minutos; respeita regras HTTP do servidor, validação e `no-store`. Não força cache de respostas não cacheáveis.
- Configuração de imagens mantida em memória por até 24 horas; URLs aceites apenas no host oficial `image.tmdb.org` e base HTTPS devolvida por configuração.
- Posters `w342`, fundos `w780`, perfis `w185`, stills `w300`, quando esses tamanhos constam da configuração; fallback para um tamanho limitado documentado nessa resposta, não para imagens originais gigantes.
- Coroutines cancelam pedidos Retrofit e pesquisas antigas. Debounce de 350 ms, consultas até 200 caracteres e pelo menos dois caracteres na interface.
- Quatro pedidos concorrentes, espaçados em 250 ms por política local. Uma repetição para 429 com Retry-After numérico até 30 segundos; valores maiores mantêm o erro para não repetir prematuramente.
- Paginação carregada a pedido; Explorar antecipa a próxima página perto do fim. Não há prefetch de todas as temporadas.

## Atribuição e trailers

Sobre/Créditos inclui a mensagem exigida e um SVG oficial aprovado, sem modificar as cores ou proporções, menos proeminente do que a marca Mirevia. O endereço do asset vem da página oficial de logos; a imagem pode não carregar offline, mas a mensagem continua visível.

“This product uses the TMDB API but is not endorsed or certified by TMDB.”

https://www.themoviedb.org/about/logos-attribution

Trailers só são apresentados quando a TMDB indica `official=true`, `type=Trailer` e uma chave válida numa plataforma suportada (YouTube/Vimeo). Abrem na plataforma original, após confirmação. Não são extraídas fontes de vídeo dessas plataformas. A ausência de trailer ou tradução é apresentada como informação não disponível.

A TMDB não fornece direitos de transmissão nem ficheiros de vídeo. Metadados presentes na TMDB não garantem fonte disponível no EmbedMovies.
