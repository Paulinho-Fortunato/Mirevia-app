# Template de CI Android — inactivo

`android.yml.example` é uma configuração de exemplo para GitHub Actions. Por estar fora de `.github/workflows` e ter a extensão `.example`, **não é executada automaticamente**. Este projecto não inclui um workflow activo.

O proprietário escolheu manter a configuração como template depois de o GitHub recusar a publicação de `.github/workflows/android.yml`: a ligação utilizada não tem a permissão `workflows`. O commit ainda não publicado foi actualizado para incluir apenas este exemplo, sem instalar ou modificar workflows activos.

## O que o template prepara

- JDK 17, SDK Android 35 e Gradle Wrapper do projecto.
- Verificações de fontes, testes JVM, lint e build debug com demonstração, sem tokens TMDB.
- Artefactos de APK debug e relatórios após uma build bem-sucedida.
- Testes instrumentados num emulador API 35 através de `workflow_dispatch`.

A configuração ainda não foi validada numa execução remota. Consultar [../testing.md](../testing.md) para os testes efectivamente executados e as limitações do ambiente.

## Activar mais tarde

Um responsável com permissão para criar workflows deve rever a configuração, as permissões das actions e as regras do repositório antes de a instalar:

```sh
mkdir -p .github/workflows
cp docs/ci/android.yml.example .github/workflows/android.yml
```

A criação e publicação desse ficheiro exigem a permissão de escrita de workflows no GitHub. A instalação não foi realizada nesta entrega. Não remover verificações de segurança nem alterar credenciais para tentar ultrapassar essa autorização.

Depois de publicar o workflow com autorização, confirmar o resultado real em Actions e corrigir eventuais falhas antes de declarar a build Android validada. Não adicionar segredos de produção ao template; o CI deve testar o modo de demonstração.
