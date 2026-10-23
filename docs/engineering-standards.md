# Padrão de engenharia — Meu Ponto

> **Este documento é a fonte de verdade do projeto.** Vale para qualquer pessoa ou agente de IA que
> mexa neste repositório, independente de IDE, modelo, editor ou “harness” (Android Studio, Codex,
> Claude Code, Cursor, Copilot, opencode, pi, Aider, scripts próprios…). Se uma instrução de
> ferramenta contrariar este arquivo, **este arquivo ganha** — ou o padrão é atualizado por commit,
> com justificativa.

- Última revisão: 22/10/2026 (após reorganização do `MainActivity.kt`).
- Escopo: código em `app/src/**`, workflow de CI, documentação técnica em `docs/`.
- Como usar: leia a seção **1 (regras de ouro)** e a **2 (mapa de camadas)** antes de escrever
  qualquer linha. Antes de abrir PR/commit, rode o **checklist da seção 10**.

---

## 1. Regras de ouro (não negociáveis)

1. **Tela não faz I/O pesado.** `MainActivity.kt` e arquivos `ui/` só compõem UI e mantêm estado.
   Arquivo, ZIP, OCR, criptografia, PDF, rede e banco ficam em `data/`.
2. **Regra de negócio não mora na UI nem no banco.** Cálculo de jornada/folha vive em `domain/`, em
   funções puras, testáveis sem Android.
3. **Uma regra, um lugar.** Se a mesma regra aparece em dois caminhos (ex.: duração de turno), ela
   precisa de um único dono. Duplicação de regra é bug esperando data.
4. **Nada de texto mágico.** Estado, modo e filtro não podem ser `String` solta em `if`
   (`"both"`, `"entry"`, `"month"`…). Use `enum class`/`sealed` ou, enquanto a dívida existir,
   constantes nomeadas.
5. **Número mágico tem nome.** Teto de jornada, iterações de KDF, tamanho de página, tempo limite:
   `const val` com KDoc curto explicando **por que** aquele valor.
6. **Erro não é silêncio.** `catch (e: Exception)` só é aceitável na borda da UI, com mensagem ao
   usuário e sem engolir causa relevante. Em `data/`, lance erro tipado.
7. **Nenhum código novo sem teste da regra que ele implementa**, se a regra for pura (domínio,
   máscara, validação, comparação de versão).
8. **Não reformatar arquivo inteiro em commit de feature.** Diff gigante esconde regressão. Mudança
   de estilo é commit separado (`style:`).
9. **Não introduzir dependência nova** sem justificar no corpo do commit (tamanho, licença,
   alternativa nativa/Compose).
10. **Versão e release são do dono do projeto.** Agente não altera `versionName`/`versionCode`, não
    cria tag e não faz release sem pedido explícito.

---

## 2. Mapa de camadas e regra de dependência

```
app/src/main/java/br/com/jean/meuponto/
├── MainActivity.kt        UI: estado da tela + composição       (única "tela"; pode importar tudo)
├── domain/                regras puras, sem Android            (só depende de data.Receipt)
│   ├── WorkStats.kt               pareamento, horas/dia, período
│   ├── PayrollSettings.kt         configuração do período de folha
│   └── ReceiptLabels.kt           rótulos derivados (usados por UI e PDF)
├── data/                  I/O, persistência, arquivos          (não depende de ui/)
│   ├── Receipt.kt / ReceiptDao.kt / AppDatabase.kt   Room
│   ├── ReceiptImporter.kt         cópia + OCR
│   ├── BackupService.kt           backup/restore ZIP + AES-GCM
│   ├── PdfExporter.kt             geração e compartilhamento de PDF
│   └── CacheMaintenance.kt        limpeza de temporários
├── ui/                    componentes e utilitários de tela    (não depende de data/ além do modelo)
│   ├── Theme.kt                   tokens de cor e tipografia
│   ├── MaskedInput.kt             máscaras/validação de data e hora
│   ├── ReceiptFormatting.kt       rótulos de mês/dia/semana
│   ├── ReceiptSections.kt         agrupamento da lista
│   ├── InsightsSheet.kt · PayrollSettingsSheet.kt · ReceiptViewer.kt
└── updater/               atualização in-app (GitHub Releases)
    ├── AppUpdater.kt              download/instalação
    └── UpdateUi.kt                diálogos e estados
```

Regra de dependência (permitido → proibido):

| De | Pode importar | Não pode importar |
|---|---|---|
| `domain/` | `java.*`, `data.Receipt` (modelo), `kotlin.*` | `android.*`, `ui/`, `updater/`, Compose |
| `data/` | `domain/`, `android.*`, libs de I/O | `ui/`, `MainActivity` |
| `ui/` | `domain/`, `data.Receipt`, Compose | `updater/`, `MainActivity` |
| `MainActivity.kt` | tudo | — (é a raiz de composição) |
| `updater/` | `android.*`, `data.CacheMaintenance` | `domain/`, `ui/` |

> Exceção consciente e documentada: `domain/` conhece `data.Receipt` (entidade Room). Trocar isso por
> um modelo próprio é a pendência **ENG-4**; até lá, não agrave (não adicione anotações Room em
> `domain/`).

### Onde colocar código novo

| Tipo de código | Lugar |
|---|---|
| Cálculo, validação, regra de negócio | `domain/` (função pura + teste) |
| Leitura/gravação de arquivo, ZIP, PDF, rede, OCR | `data/` |
| Formatação para exibição (datas, minutos, rótulos) | `ui/` (ou `domain/` se precisar fora da UI) |
| Componente visual reutilizável | `ui/` |
| Composição/estado de tela | `MainActivity.kt` até a pendência **ENG-1** |

---

## 3. Limites de tamanho e coesão

| Item | Alvo | Limite duro (exige justificativa no commit) |
|---|---|---|
| Função/composable | até ~40 linhas | 80 linhas |
| Arquivo | até ~300 LOC | 600 LOC |
| Parâmetros de função | até 4 | 6 (acima: use um objeto de entrada) |
| Aninhamento (`if`/`for`) | 2 níveis | 3 níveis |

Situação atual (baseline de 22/10/2026, para não regredir) — medido por chaves, não por linha:

- `MainActivity.kt`: **1.567 LOC** (era 2.152 antes da extração). Ainda é o maior arquivo do repo.
- Funções acima do limite duro (80 linhas), do maior para o menor:
  `MeuPontoApp` **504**, `UpdateDialog` 266, `PayrollSettingsSheet` 183, `EditReceiptSheet` 176,
  `AboutSheet` 162, `ReceiptViewer` 152, `SummaryCard` 109, `restoreBackup` 93,
  `InsightsSheet` 93, `ReceiptCard` 81.
- Mediana das 64 funções: **18,5 linhas** — a maioria está saudável; o problema é concentrado.
- Produção: 4.435 LOC em 19 arquivos. Testes: 440 LOC, **35 testes** em 4 arquivos, todos em JVM.

**Regra do escoteiro**: ao tocar em algo grande, extraia uma peça para o lugar certo (seção 2) e
deixe o arquivo menor. Não é obrigatório refatorar tudo de uma vez; é obrigatório não piorar.

Ordem sugerida de extração (do maior ganho para o menor risco):

1. `MeuPontoApp` (504): launchers de arquivo (`importReceipt`, `createBackupLauncher`,
   `restoreBackupLauncher`, `scannerLauncher`) viram `ui/ReceiptActions.kt` devolvendo callbacks;
   o `Scaffold`/barra de ações sai para `ui/HomeScaffold.kt`.
2. `EditReceiptSheet` (176): estado de campos + validação para `ui/ReceiptEditorState.kt`.
3. `UpdateDialog`/`AboutSheet` (266/162): dividir por etapa do estado (`Idle`, `Downloading`,
   `ReadyToInstall`, `Error`) — o `sealed` já existe em `updater/AppUpdater.kt`.

---

## 4. Nomenclatura

- **Pacotes/arquivos**: minúsculos, sem prefixo de empresa repetido; nome do arquivo = assunto
  (`BackupService.kt`, `MaskedInput.kt`). Um arquivo, um assunto.
- **Funções**: verbo + objeto, no idioma do domínio (`pairPunches`, `buildMonthSections`,
  `deriveBackupKey`). Evite `processUri`, `handleData`, `doWork`.
- **Booleanos**: `is/has/can/should/needs` (`isProcessing`, `needsReview`).
- **Constantes**: `SCREAMING_SNAKE_CASE` com KDoc curto quando o valor não é óbvio
  (`MAX_SHIFT_MINUTES`, `BACKUP_ITERATIONS`, `MAX_RESTORE_ZIP_ENTRIES`).
- **Tokens de UI**: PascalCase (`Ink`, `Teal`, `Mint`) e nunca hex solto em composable.
- **Testes**: backticks em português, descrevendo comportamento observável, não o método:
  ```kotlin
  @Test fun `pareia entrada e saida de turno que cruza a meia-noite`() { … }
  ```
- **Comentários de pendência**: `// PENDÊNCIA ENG-1: …` com o código da seção 11.

---

## 5. Comentários e KDoc

Comentário existe para explicar **o que o código não consegue dizer**. Neste projeto, comente:

1. **Regra de negócio com número escolhido** (teto de 16h, 120k iterações de PBKDF2, limite de 2.000
   entradas no ZIP): diga de onde vem e o que quebra se mudar.
2. **Formato de arquivo/protocolo** (header do backup: magic + versão + salt + iv + AES-GCM).
3. **Heurística sujeita a erro** (OCR: primeira hora = entrada, segunda = saída).
4. **Workaround/bug histórico** — referencie o problema (`P18`, `P19`) para ninguém “limpar” a
   correção depois.
5. **KDoc em toda API pública/interna** de `domain/` e `data/` (contrato: entradas, saídas,
   invariantes, efeitos colaterais).

Não comente: o que o código já diz (`// incrementa i`), código morto (apague), ou `TODO` sem dono.
TODO só entra com código de pendência da seção 11.

---

## 6. Tratamento de erro

- `domain/`: devolva `null`/tipo próprio; **nunca** lance para controle de fluxo.
- `data/`: lance erro tipado com motivo (`BackupException(reason)`), nunca `error("...")` genérico em
  código novo. Validação de entrada externa (backup, APK) sempre falha **cedo** e **explicando**.
- UI: converta erro em mensagem acionável (“Não foi possível restaurar: arquivo não é um backup do
  Meu Ponto”) e mantenha o app utilizável. `runCatching { }` só na borda, e sempre com
  `onFailure` que produz mensagem específica.
- Proibido: `catch (_: Exception) {}` sem ação; log/`println` como tratamento.
- Pendência conhecida: mensagens genéricas atuais (“Não foi possível…”) devem ser refinadas junto
  da tipagem dos erros (**ENG-5**).

---

## 7. Estado e Compose

- Estado derivado: `remember(chaves)` — nunca recalcular em toda recomposição, nunca guardar em
  `var` o que dá para derivar.
- Estado que sobrevive a rotação/process death: `rememberSaveable`; estado efêmero de folha:
  `remember`.
- Um composable **não** deve receber `Context` para fazer I/O; receba dados + callbacks.
- `LaunchedEffect` só para efeito colateral único por chave; nada de lógica de negócio dentro.
- Toda lista longa (`LazyColumn`) precisa de `key` estável (id do modelo).
- Cores: `MaterialTheme.colorScheme` ou tokens de `ui/Theme.kt`. Espaçamentos: múltiplos de 4dp,
  preferindo constantes nomeadas a `.dp` solto (pendência **ENG-3**).
- Pendências estruturais: 8 flags booleanas de folha + ausência de ViewModel (**ENG-1**) e strings
  hardcoded (**ENG-3**).

---

## 8. Testes

- **Obrigatório** para: `domain/` (regras), máscaras/validação (`ui/MaskedInput.kt`), comparação de
  versão (`updater`), parsers (data, hora, manifest).
- Local: `app/src/test/java/...` (JVM, sem emulador). Nome do arquivo = `<Assunto>Test.kt`.
- Estrutura: arrange/act/assert, um comportamento por teste, sem dependência entre testes.
- Teste de regra de jornada sempre cobre **os dois caminhos** (mesmo comprovante × comprovantes
  separados) — foi assim que o bug de turno noturno apareceu.
- Rodar antes de commitar:
  ```bash
  ./gradlew :app:testPlayDebugUnitTest :app:testGithubDebugUnitTest --no-daemon
  ./gradlew :app:lintPlayDebug :app:lintGithubDebug --no-daemon
  ```

---

## 9. Git, build e release

**Commits** (Conventional Commits, um assunto por commit, corpo em português explicando o porquê):

```
feat(app)     nova funcionalidade
fix(domain)   correção de regra
refactor(ui)  reorganização sem mudar comportamento
docs          documentação
test          testes
chore(ci)     build, workflow, dependências
style         formatação/reformatação (sem mudança lógica)
```

- Branch: `main` é a linha publicada. Trabalho maior em branch curta (`refactor/...`, `feat/...`).
- Nunca `git push --force` em `main`; nunca commitar keystore/credenciais (`*.jks`, `local.properties`).
- Commit de `style` **nunca** mistura com `feat`/`fix`.

**Build/verificação local** (JDK 17 + SDK 36):

```bash
./gradlew :app:assemblePlayDebug                 # APK de desenvolvimento
./gradlew :app:assembleGithubRelease             # APK assinado (precisa das env de keystore)
./gradlew :app:bundlePlayRelease                 # AAB para a Play
```

**CI** (`.github/workflows/android-release.yml`): roda **em tag `v*` ou manualmente**
(`workflow_dispatch`) — não em todo push. Etapas: testes das duas flavors, lint, build do APK
`githubRelease` e do AAB `playRelease` assinados, e publicação do Release quando a ref é uma tag.
A tag **precisa** bater com `versionName` do `app/build.gradle.kts` (há checagem no workflow), e a
assinatura vem dos Secrets:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
Secrets ausentes em build de tag = falha explícita; em build manual, APK sai com chave debug (não
distribuir).

**Versionamento**: `versionName` semântico (`0.5.1`) + `versionCode` inteiro crescente. Mudança de
versão/tag/release só com autorização explícita do dono do projeto.

---

## 10. Checklist antes de commitar (definição de pronto)

- [ ] A mudança está no lugar certo do mapa de camadas (seção 2)?
- [ ] Nenhuma regra duplicada; números mágicos viraram constantes com KDoc?
- [ ] Nenhum `String` mágico novo para estado/modo?
- [ ] Função nova dentro do limite de tamanho; arquivo não cresceu sem necessidade?
- [ ] Regra pura tem teste? Testes e lint rodaram?
- [ ] Erros produzem mensagem útil (nada de `catch` mudo)?
- [ ] Comentários explicam decisões, não código?
- [ ] Commit com tipo/escopo corretos, sem reformatação alheia?
- [ ] Se a pendência da seção 11 mudou, este documento foi atualizado no mesmo commit?

---

## 11. Pendências conhecidas (não reabrir sem plano)

| ID | Pendência | Onde | Por que não agora |
|---|---|---|---|
| ENG-1 | Estado de tela em `ViewModel` + `sealed` de folha ativa | `MainActivity.kt` | mexe em ~30 pontos de UI; precisa de teste de UI antes |
| ENG-2 | `enum`/`sealed` para `mode`, `selectedFilter`, `selectedGroupMode` | `ui/` + `MainActivity` | mecânico, mas amplo; fazer junto de ENG-1 |
| ENG-3 | `strings.xml` (302 literais) + escala de espaçamento/tema | `ui/`, `res/` | pré-requisito para i18n; alto volume |
| ENG-4 | `Receipt` com `LocalDate`/`LocalTime` + conversores Room | `data/`, `domain/` | migração de banco + OCR + UI; fazer por último |
| ENG-5 | Erros tipados e mensagens específicas | `data/`, `updater/`, UI | depende de desenhar os estados de erro |
| ENG-6 | `EditReceiptSheet` (177 linhas) → estado + validação próprios | `MainActivity.kt` | próximo passo natural, depois de ENG-1/ENG-2 |
| P15 | CI: `setup-java` v4 → v5, `ubuntu-latest` fixo | `.github/workflows` | avisos de depreciação, sem quebra hoje |
| P17 | Avisar assinatura diferente antes do instalador | `updater/` | precisa de teste com APK de outra chave |

**Pendências de produto/pipeline** (ver `docs/status-e-proximos-passos.md`): classificação dos
comprovantes de turno noturno no aparelho, publicação da `v0.5.1` e Play App Signing.

---

## 12. Como um agente deve trabalhar neste repo

1. Ler este arquivo e `docs/status-e-proximos-passos.md` antes de propor mudanças.
2. Rodar testes/lint **antes** e **depois** de mexer (se o ambiente não permitir, declarar isso
   explicitamente no relatório — nunca afirmar que “compila” sem ter compilado).
3. Mudanças mecânicas grandes (mover código) em commit próprio, sem alterar lógica no mesmo commit.
4. Não criar arquivo novo sem necessidade: verifique se já existe o lugar certo na seção 2.
5. Não adicionar dependência, versão, tag ou release sem pedido do dono.
6. Não apagar comentário de regra/negócio nem “simplificar” validação de segurança (backup, APK).
7. Entregar relatório curto: o que mudou, por que, o que não foi testado e qual o próximo passo.
