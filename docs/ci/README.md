# CI e APK — workflow pessoal e templates

Os ficheiros desta pasta são exemplos. Como estão fora de `.github/workflows` e terminam em `.example`, **não são executados automaticamente**. Existe agora um workflow pessoal activo em [`.github/workflows/build-apk.yml`](../../.github/workflows/build-apk.yml), instalado posteriormente pelo proprietário, com execução manual.

A entrega inicial usou templates depois de a publicação ser recusada por falta da permissão `workflows`. Instalar ou actualizar um workflow activo continua a exigir um responsável autorizado; manter o workflow pessoal e o seu exemplo sincronizados.

## 1. Validação em demonstração

`android.yml.example` prepara testes JVM, lint, build debug e relatórios sem credenciais TMDB. A execução manual acrescenta uma matriz de emuladores **API 24 (Android 7.0) e API 35**, incluindo testes de compatibilidade de datas `java.time`. É o template apropriado para validar pull requests sem incluir segredos num APK.

Para activar apenas essa validação, um responsável autorizado deve publicar o conteúdo em `.github/workflows/android.yml`, depois de o rever. Não confundir o APK demo desse template com o APK pessoal configurado abaixo.

## 2. APK pessoal com TMDB directa, sem backend

`build-apk.yml.example` prepara um **APK debug assinado e instalável**, com o token TMDB incluído no `BuildConfig` pela configuração Gradle já existente.

A API TMDB continua a ser um serviço online: ficam embutidos a integração e o token, não os servidores nem o catálogo offline. Não é necessário um backend da Mirevia para esta variante. A reprodução continua exclusivamente no iframe autorizado, sem alterações ao fornecedor.

### Configurar o token

No repositório do GitHub:

1. Abre **Settings → Secrets and variables → Actions**.
2. Escolhe **New repository secret**.
3. Nome: **`TMDB_READ_TOKEN`**.
4. Valor: o teu **API Read Access Token** da TMDB, sem o prefixo `Bearer`, espaços ou quebras de linha. Não é a API key v3 de 32 caracteres.

**Não coloques o valor no YAML, no Kotlin, num commit ou em chat.** O workflow falha explicitamente quando o secret está vazio ou tem espaços; não gera silenciosamente um APK de demonstração nessa situação. A validade real da credencial só pode ser confirmada pela TMDB; este workflow não promete autenticação bem-sucedida para um valor incorrecto.

### Falha de SDK: `Failed to find package 'tools'`

`android-actions/setup-android@v3` pede, por omissão, `tools platform-tools`. O pacote legado `tools` não foi encontrado no repositório do SDK na execução reportada. A linha `All SDK package licenses accepted` mostra que essa falha não foi uma recusa das licenças. A mensagem anterior sobre a versão preinstalada apenas precedeu o download da versão de command-line tools configurada pela action.

O workflow activo e os dois templates substituem explicitamente os pacotes adicionais:

```yaml
      - uses: android-actions/setup-android@v3
        with:
          packages: 'platform-tools'
```

O `sdkmanager` já é fornecido por `cmdline-tools`; não é necessário pedir o antigo pacote `tools`. O passo seguinte mantém a instalação de `platforms;android-35` e `build-tools;35.0.0`. Não se ignoram erros de instalação nem se desactiva a aceitação de licenças. Isto não altera `minSdk 24`, a integração TMDB ou as restrições do player.

Depois de publicar a correcção no workflow, inicia uma **nova execução** em **Run workflow**, na branch actualizada. **Re-run jobs** de uma execução antiga volta a usar a revisão dessa execução, sem incorporar a alteração do YAML.

### Executar ou instalar o workflow e obter o APK

O repositório actual já contém **`.github/workflows/build-apk.yml`**. Para o actualizar ou reinstalar, um responsável autorizado deve rever e publicar o conteúdo de `build-apk.yml.example` nesse caminho. A branch compilada deve conter também as alterações Android 7.0 do projecto (`minSdk 24`, desugaring, dependência e recursos compatíveis); copiar só o YAML para uma versão antiga com `minSdk 26` não altera o app e faz a verificação do APK falhar. Para copiar localmente:

```sh
mkdir -p .github/workflows
cp docs/ci/build-apk.yml.example .github/workflows/build-apk.yml
```

Para aparecer o botão de execução manual, o workflow deve estar publicado na branch predefinida do repositório. Depois:

1. Abre **Actions → Mirevia - APK instalavel com TMDB → Run workflow**.
2. Selecciona apenas código que controlas e consideras de confiança. Este workflow tem acesso ao teu secret no passo de compilação.
3. Aguarda o resultado real dos testes, lint, compilação e verificação da assinatura.
4. Se terminar com sucesso, abre **Artifacts → Mirevia-APK-<número da execução>**.
5. Descarrega o ZIP, extrai **`Mirevia.apk`** e instala-o num dispositivo Android **7.0/API 24 ou superior**. O ZIP não é o APK.

O artefacto é conservado durante sete dias, sujeito às regras do repositório. O workflow não publica uma release nem envia o APK para a Play Store. A variante release continua sem token embutido e exige assinatura/configuração próprias.

### O que o workflow faz

- Executa apenas por `workflow_dispatch`, não em pull requests ou pushes automáticos.
- Valida a presença do secret sem imprimir o seu valor.
- Prepara JDK 17, SDK Android 35 e Build Tools 35.0.0.
- Executa verificações de fontes, testes e lint **sem credenciais**, em modo demo.
- Passa `TMDB_READ_TOKEN` apenas para a validação de presença e para a compilação do APK pessoal.
- Força `TMDB_PROXY_BASE_URL` vazio, para usar a TMDB directamente.
- Desactiva caches partilhadas, build cache e configuration cache nos passos relevantes.
- Gera `app-debug.apk`, confirma a assinatura para API 24 com `apksigner`, verifica `sdkVersion:24` no manifesto do APK com `aapt` e disponibiliza-o como `Mirevia.apk`. Essa verificação não substitui um teste real em Android 7.0.
- Guarda apenas o APK pretendido e relatórios de testes sem credenciais; não publica fontes geradas de BuildConfig.
- Solicita a limpeza dos outputs e da chave debug no runner no final, inclusive após falhas.

### Aviso obrigatório sobre o token embutido

**Um GitHub secret não torna o token secreto dentro do APK. Quem obtiver o APK pode recuperar o token.** Ofuscação e BuildConfig não eliminam este risco. Num repositório público, os artefactos também podem ficar acessíveis a outras pessoas com acesso às execuções.

Usa este modo apenas com consciência desse risco, para o teu projecto pessoal, e protege o acesso ao repositório e aos artefactos. Não é uma forma de distribuir uma credencial privada com segurança. Para partilha pública mantendo a credencial fora do cliente, seria necessário um backend; isso não é exigido para a configuração pessoal aqui pedida.

### Manter a assinatura entre execuções — opcional, recomendado

Sem uma chave persistente, um runner novo pode gerar uma chave debug diferente. Nesse caso, o Android pode recusar actualizar o APK anterior. Desinstalar a aplicação para instalar outra assinatura elimina a biblioteca e os outros dados locais: **não o faças sem aceitar essa perda**.

O workflow aceita opcionalmente o secret **`ANDROID_DEBUG_KEYSTORE_BASE64`**. Deve conter em Base64 uma keystore debug tua, com alias `androiddebugkey` e passwords padrão de debug `android`. Conserva a mesma chave para todas as builds que queiras instalar como actualizações.

Podes reutilizar a keystore de desenvolvimento já criada pelo Android Studio em `~/.android/debug.keystore`. Se não existir, cria uma **localmente**, fora do repositório, com um JDK instalado:

```sh
mkdir -p "$HOME/.android"
keytool -genkeypair -keystore "$HOME/.android/debug.keystore" \
  -alias androiddebugkey -storepass android -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Mirevia Debug"
```

Não substituas uma chave existente que já esteja a assinar a tua aplicação. Para preparar o valor do secret num terminal Linux/macOS:

```sh
base64 < "$HOME/.android/debug.keystore" | tr -d '\n'
```

Cola esse resultado **apenas** no secret `ANDROID_DEBUG_KEYSTORE_BASE64`, nunca num commit ou em chat. Esta é uma assinatura de desenvolvimento, não uma configuração de release de produção.

## Validação ainda necessária

A [execução inicial](https://github.com/Paulinho-Fortunato/Mirevia-app/actions/runs/43613022549) falhou no setup do SDK ao pedir `tools`. A [execução posterior](https://github.com/Paulinho-Fortunato/Mirevia-app/actions/runs/43613957749) já passou essa preparação e completou `:core:test`, mas parou na compilação Kotlin Android: conflito do nome `setImmersive` e callback de `MediaSearchRow` passado no argumento errado. Os dois pontos foram corrigidos nas fontes desta branch, mas ainda é necessária uma nova execução com esse código publicado; não foi gerado um APK aqui. Consultar [../testing.md](../testing.md) para os testes efectivamente executados e as limitações do ambiente. Uma assinatura válida não comprova o funcionamento da TMDB nem a disponibilidade do iframe; testar ambos num dispositivo depois de instalar.
