# Meu Ponto

App Android nativo (Kotlin + Compose) para arquivar, pesquisar e compartilhar comprovantes de ponto.
Tudo fica no aparelho: sem conta, sem nuvem, sem anúncio.

## Recursos

- Captura pelo Document Scanner do ML Kit (recorte e correção de perspectiva) ou pela galeria, com
  OCR local que preenche data e horários.
- Um comprovante pode ter entrada e saída, apenas uma batida, ou um horário sem tipo.
- Horas por dia com pareamento entre comprovantes e entre dias (turno noturno), resumo do período de
  folha configurável e tela de insights.
- Lista hierárquica (mês, dia ou semana), busca, filtros, miniaturas e visualizador com zoom.
- Exportação em PDF (um ou vários comprovantes) e backup ZIP com senha opcional.
- Tema claro/escuro persistente, splash nativa e atualização in-app no flavor `github`.

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · ML Kit (Document Scanner + Text Recognition) ·
SplashScreen · GitHub Actions · `minSdk 26`, `target/compileSdk 36`.

## Como o código é organizado

`MainActivity.kt` e `ui/` compõem a tela (sem I/O), `domain/` tem as regras puras e testadas, `data/`
concentra Room, OCR, backup e PDF, e `updater/` cuida da atualização in-app.

O padrão completo está em [`docs/engineering-standards.md`](docs/engineering-standards.md); o resumo
executável para agentes de IA está em [`AGENTS.md`](AGENTS.md).

## Começando

Requisitos: JDK 17, Android SDK 36 e o `local.properties` apontando para o SDK (esse arquivo não vai
para o git). Abra a raiz no Android Studio, aguarde a sincronização e rode a variante `playDebug`.

Pelo terminal:

```bash
./gradlew :app:assemblePlayDebug
adb install -r app/build/outputs/apk/play/debug/app-play-debug.apk
```

## Builds

| Comando | Resultado |
|---|---|
| `:app:assemblePlayDebug` | APK de desenvolvimento |
| `:app:assembleGithubRelease` | APK assinado, com updater in-app |
| `:app:bundlePlayRelease` | AAB assinado para a Play |
| `:app:testPlayDebugUnitTest` | testes JVM (sem emulador) |
| `:app:lintPlayDebug` | lint |

Dois flavors de distribuição: `play` (sem updater e sem `REQUEST_INSTALL_PACKAGES`) e `github` (com
atualizador via GitHub Releases). Para instalar em paralelo com a versão oficial, use
`-PlocalTestSuffix=.localtest` — o `applicationId` ganha o sufixo e os dados ficam separados.

## Release

Empurre uma tag `vX.Y.Z` **igual** ao `versionName` do `app/build.gradle.kts`:

```bash
git tag vX.Y.Z && git push origin vX.Y.Z
```

O workflow `.github/workflows/android-release.yml` (tag `v*` ou execução manual) roda testes e lint,
assina com o keystore dos Secrets e publica o Release com o APK do flavor `github` e o AAB do `play`.
Sem o keystore, um build local sai assinado com a chave debug — serve só para teste, nunca para
distribuir. Versão, tag e release são decisão do dono do projeto.

## Dados e privacidade

Sem Android Auto Backup/Drive Backup: banco Room, imagens e cache ficam fora do backup do sistema.
O usuário exporta e restaura pelo backup ZIP do app (senha opcional). A sincronização automática com
o Drive ainda não existe: exige OAuth no Google Cloud (cliente Android, SHA-1/SHA-256 do certificado)
e credenciais fora do repositório.

[Política de privacidade](docs/privacy-policy.md) · [Checklist de produção](docs/google-play-readiness.md)

## Documentação

- [Padrão de engenharia](docs/engineering-standards.md) — camadas, nomenclatura, testes, git e release
- [Status e próximos passos](docs/status-e-proximos-passos.md)
- [Plano do release v0.5.1 (publicado)](docs/plano-release-v0.5.1.md)
- [Modelo de release notes](docs/release-notes-template.md)
- [Plano da feature de atualização ao abrir](docs/plano-feature-atualizacao-ao-abrir.md)
- [Plano da feature de swipe para apagar](docs/plano-feature-swipe-delete.md)
- [Roadmap para a Play Store](docs/roadmap-aaa.md)
- [Brand book](docs/brand-book.md)
- [Plano de correções do editor](docs/plan-correcoes-editor.md)
