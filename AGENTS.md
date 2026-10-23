# AGENTS.md — instruções para agentes de código

Este arquivo é o ponto de entrada para qualquer agente de IA (ou pessoa) que trabalhe neste
repositório, seja qual for a ferramenta: Android Studio/Gemini, Codex, Claude Code, Cursor,
Copilot, opencode, pi, Aider, scripts próprios.

**Fonte de verdade:** [`docs/engineering-standards.md`](docs/engineering-standards.md) — leia antes
de escrever código. Se algo aqui divergir daquele documento, o documento vence.

## Regras críticas (resumo executável)

1. `MainActivity.kt` e `ui/` **não fazem I/O**: arquivo, ZIP, OCR, cripto, PDF, rede e banco moram
   em `data/`.
2. Regra de negócio (jornada, folha, validação) mora em `domain/`, pura e testável sem Android.
3. Uma regra, um dono. Não duplique cálculo entre caminhos (ex.: duração de turno).
4. Nada de `String` mágica para estado/modo/filtro; número mágico vira `const val` com KDoc.
5. Erro não é silêncio: nada de `catch (_: Exception) {}`; mensagem precisa dizer o que fazer.
6. Toda regra pura nova entra com teste em `app/src/test/`.
7. Commit segue Conventional Commits (`feat(app)`, `fix(domain)`, `refactor(ui)`, `docs`, `test`,
   `chore(ci)`, `style`). Um assunto por commit. `style` nunca junto de `feat`/`fix`.
8. **Não** altere `versionName`/`versionCode`, **não** crie tag/release, **não** adicione dependência
   e **não** faça `push --force` sem pedido explícito do dono.
9. Não apague comentário que documenta regra de negócio nem “simplifique” validação de segurança
   (backup/restore, instalação de APK).
10. Se não foi possível compilar/testar no ambiente, diga isso no relatório. Nunca afirme que
    “compila” sem ter compilado.

## Comandos

```bash
./gradlew :app:testPlayDebugUnitTest :app:testGithubDebugUnitTest --no-daemon   # testes
./gradlew :app:lintPlayDebug :app:lintGithubDebug --no-daemon                   # lint
./gradlew :app:assemblePlayDebug                                               # APK debug
./gradlew :app:assembleGithubRelease                                           # APK release (keystore via env)
./gradlew :app:bundlePlayRelease                                               # AAB para a Play
```

Requisitos: JDK 17 e Android SDK 36 (`local.properties` não vai para o git).

## Mapa do código

| Pasta | Conteúdo |
|---|---|
| `MainActivity.kt` | estado da tela + composição (não é lugar de I/O) |
| `domain/` | `WorkStats.kt` (pareamento/ horas), `PayrollSettings.kt`, `ReceiptLabels.kt` |
| `data/` | Room (`Receipt`, `ReceiptDao`, `AppDatabase`), `ReceiptImporter`, `BackupService`, `PdfExporter`, `CacheMaintenance` |
| `ui/` | `Theme`, `MaskedInput`, `ReceiptFormatting`, `ReceiptSections`, folhas e visualizador |
| `updater/` | atualização in-app via GitHub Releases: `UpdateCoordinator`, `UpdateParser`, `UpdatePreferences`, `ApkIntegrity`, `AppUpdater`, `UpdateUi` |

## Pendências que não devem ser reabertas sem plano

`ENG-1` ViewModel/estado selado · `ENG-2` enums de modo/filtro · `ENG-3` `strings.xml` + escala de
espaçamento · `ENG-4` `Receipt` com `LocalDate`/`LocalTime` · `ENG-5` erros tipados · `ENG-6` quebrar
`EditReceiptSheet` · `P15` CI (`setup-java` v5). `P17` (aviso de assinatura) foi implementado na branch `feat-update-ao-abrir`.

Detalhes e ordem recomendada: `docs/engineering-standards.md`, seção 11. Estado do produto e
próximos passos de release: `docs/status-e-proximos-passos.md`.
