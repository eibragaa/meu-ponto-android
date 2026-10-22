# Plano de correções — editor e teclado (sprints E1–E5)

Plano para execução por subagente ( modelBuilder ) usando o modelo `opencode-go/deepseek-v4.1-flash` (gratuito no OpenCode Go). Um sprint por vez; o subagente termina o sprint, gera relatório e só então o próximo começa.

## Contexto para o executor

- Repo local: `C:\dev2026\meu-ponto-android` (branch `main`; commits diretos, sem PR).
- Android app Kotlin/Compose; principal arquivo de UI: `app/src/main/java/br/com/jean/meuponto/MainActivity.kt`.
- Flavors: `play` e `github`. Build de teste no celular: `:app:assembleGithubRelease` com env `ANDROID_KEYSTORE_PATH/ANDROID_KEYSTORE_PASSWORD/ANDROID_KEY_ALIAS/ANDROID_KEY_PASSWORD` carregadas de `C:\Users\jeanb\.meuponto\credenciais-release.txt`.
- Instalar no celular: `adb install -r app\build\outputs\apk\github\release\app-github-release.apk` (Samsung SM-S938B via USB, transport id 4).
- Testes unitários: Gradle executor quebrado por `JAVA_HOME` com espaços. Usar o caminho direto::

    .\gradlew.bat :app:compilePlayDebugUnitTestKotlin --no-daemon
    java -cp "app\build\tmp\kotlin-classes\playDebugUnitTest;<caminhos de junit/hamcrest/kotlin-stdlib>" org.junit.runner.JUnitCore br.com.jean.meuponto.domain.WorkStatsCalculatorTest

  (caminhos válidos hoje: junit-4.13.2.jar, hamcrest-core-1.3.jar, kotlin-stdlib-2.0.21.jar)
- Regras: não bumpar `versionName`/`versionCode`, não criar tag/release, não usar `git push --force`; commit por sprint.
- UI técnica de teste no celular: `adb shell uiautomator dump /sdcard/ui.xml` + pull para `C:\Windows\Temp\opencode\phone-ui.xml`; uidump guarda pontos por `bounds [x1,y1][x2,y2]`.
- **Estado atual do editor** (MainActivity.kt ~linha 1492): campos de data/horário já usam `TextFieldValue` + `maskedFieldValue` (cursor vai ao fim). Bug P18/P19 corrigidos e testados no celular. Falta E1 (teclado) e E3 (motivo do "Revisar").

## Sprint E1 — teclado cobre a planilha (P21)

**Problema medido no celular (1440×3120):** com o teclado numérico aberto, a `ModalBottomSheet` do editor fica espremida: título em y≈1067, campos de cima até y≈2299; os textos de apoio (`Horário de entrada`) e os botões (Cancelar/Confirmar, y≈2397–2454) ficam **por baixo do teclado**. O usuário não vê o que digita nos campos de baixo e "tudo fica espremido".

**Hipótese:** a sheet não aplica `imePadding()`/insets do teclado; só `navigationBarsPadding`.

**Tarefas**
1. Em `EditReceiptSheet`, aplicar padding de IME na coluna/scroll interno (ex.: trocar `navigationBarsPadding()` por `.windowInsetsPadding(WindowInsets.ime)` combinado com `navigationBarsPadding`), garantindo que o campo focado e os botões fiquem visíveis com o teclado aberto.
2. Verificar `windowSoftInputMode` no Manifest (deve permitir resize; ajust() estiver "pan", trocar para `adjustResize` se necessário).
3. Opcional: `ImeAction.Done` no campo de horário fechando o teclado (já testado manualmente com keyevent 66; formalizar no código).

**Aceite (testado no celular via adb)**
- Abrir editor de um comprovante, focar o campo de horário com teclado aberto: campo visível inteiro + botões alcançáveis (uidump mostra botões acima do topo do IME).
- Digitar 07:00 não inverte dígitos (regressão do P18).
- `assembleGithubRelease` sucesso; instalado no celular; commit `fix(ui): editor visível com teclado aberto`.

## Sprint E2 — máscaras e cursor: extração + testes unitários

**Tarefas**
1. Extrair `formatDateInput`, `formatTimeInput`, `maskedFieldValue` de MainActivity.kt para arquivo próprio `app/src/main/java/br/com/jean/meuponto/ui/MaskedInput.kt` (fns públicas/puras), deixando MainActivity importando.
2. Criar testes `app/src/test/java/br/com/jean/meuponto/ui/MaskedInputTest.kt`: casos dígitos parciais (0,7 -> "0"; 0,7,0,0 -> "07:00"), já formatado, vazio, string longa, datas ("2", "20/1", "20/10/2026", ">8 chars").
3. Rodar via caminho direto JUnit (ver contexto).

**Aceite**
- Todos os testes passam; build versiona; regressão de digitação confirmada no celular (E4 faz o teste final).

## Sprint E3 — motivo do "Revisar" no cabeçalho (P20)

**Tarefas**
1. Em `WorkStats.kt` (cálculo de `dayStats`), fazer cada dia expor lista de motivos, ex.: `pendingReasons: List<String>` com valores "horário sem tipo", "entrada sem saída", "saída sem entrada", "conferir data/horário".
2. No header do dia no MainActivity, quando o dia está pendente, mostrar "Revisar · <motivo principal>" (ou "Revisar" + segunda linha com o motivo). Não perder o botão existente.
3. Testes em `WorkStatsCalculatorTest` para os motivos (casos já existentes cobrem pares; adicionar 2-3 asserts).
4. Atualizar `docs/status-e-proximos-passos.md` (P20 → corrigido/feito).

**Aceite**
- Cabeçalho do dia mostra motivo legível no celular (20/10 deve dizer algo como "saída 07:00 sem entrada").
- Testes passam; build + install + verificação via adb.

## Sprint E4 — build de validação e entrega ao usuário

**Tarefas**
1. Build release assinado (`assembleGithubRelease`) com todas as mudanças de E1–E3.
2. Instalar no celular e rodar checklist manual via adb:
   - editor: digitar 2243 → "22:43"; 0700 → "07:00" (ordem correta)
   - "Só entrada"/"Só saída" salvam só o tipo escolhido
   - teclado aberto: campo e botões visíveis
   - motivo do "Revisar" aparece nos dias pendentes (22/10, 20/10, 19/10, 17/10, 16/10)
3. Relatar resultado (o que passou/falhou) antes de qualquer tag.

**Aceite**
- Checklist verde OU lista clara de falhas; nenhum dado do usuário alterado pelos testes (usar Cancelar quando não for salvar de propósito).

## Sprint E5 — ( requer OK do usuário ) publicar v0.5.1

- Bump `versionName 0.5.1` / `versionCode 9`; tag `v0.5.1`; release notes com E1–E3 + correção de turno noturno; validar updater no celular (P14/painel de telefone).
- Só executar com confirmação explícita do usuário.
