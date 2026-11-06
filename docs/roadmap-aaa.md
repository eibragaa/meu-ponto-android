# Meu Ponto — roadmap para publicação na Play Store (nível AAA)

_Análise feita em 22/10/2026 sobre a versão `0.5.0` (versionCode 8)._

Objetivo: aprovação na Google Play e qualidade de produto acima da média (estabilidade, acessibilidade, privacidade, performance e testes).

Legenda: `[ ]` pendente · `[x]` concluído · **(B)** bloqueador de publicação.

Convenções de código, limites e definição de pronto: [`engineering-standards.md`](engineering-standards.md).

> Atualização de 05/11/2026: a trilha de animações e visualização está na
> [seção 12](#12-roadmap-de-animações-e-visualização--meu-ponto). Ela não substitui as sprints 0–7 e
> não autoriza publicação.

---

## 0. Resumo

Estado atual: **não aprovável**. Base técnica boa (Room, Compose, scanner do Google Play Services, sem permissão de câmera, backup com AES-GCM), mas há bloqueadores de política e de privacidade.

Bloqueadores principais:
1. Auto-update que baixa e instala APK fora da Play (`REQUEST_INSTALL_PACKAGES`).
2. Auto Backup do Android ativo, contrariando a política de privacidade.
3. Sem keystore de produção; release público é APK debug.

---

## 1. Sprint 0 — Bloqueadores de publicação

- [ ] **(B1) Atualização in-app:** criar product flavors.
  - [x] `play`: removido updater e `REQUEST_INSTALL_PACKAGES` (sem API Play In-App ainda).
  - [x] `github`: manter updater atual para sideload.
  - [x] Remover `REQUEST_INSTALL_PACKAGES` da flavor `play` (manifest por flavor).
  - [ ] Usar In-App Updates API da Play no flavor `play`.
- [x] **(B2) Auto Backup:** define `android:dataExtractionRules`, `fullBackupContent` e `allowBackup=false`. O banco Room, `files/receipts/` e `cache/` não participam de backup automático.
  - [ ] Detectar em dispositivo real e comemorar na política e no Data Safety.
- [ ] **(B3) Assinatura:**
  - [x] Gerar keystore de distribuição e guardar fora do repositório (`C:\Users\jeanb\.meuponto`), com Secrets no GitHub. Chave de upload da Play ainda pendente.
  - [ ] Configurar Play App Signing no Play Console.
  - [x] Parar de publicar APK debug como Release público no GitHub (release `v0.5.0` assinada; `v0.4.0` ainda é debug, ver status P3).
  - [ ] Validar que a assinatura de release é a mesma entre versões.
- [ ] **(B4) Política de privacidade:**
  - [x] Reescrever com requisitos LGPD (controlador, encarregado, direitos do titular, retenção, base legal, segurança).
  - [ ] Substituir o placeholder de contato por canal real.
  - [ ] Hospedar em URL HTTPS pública.
  - [x] Adicionar link dentro do app (tela Sobre).
- [ ] **(B5) Play Console:**
  - [ ] Preencher Data Safety com base no tráfego real (testar com proxy).
  - [ ] Classificação de conteúdo (IARC).
  - [ ] Público-alvo e conteúdo.
  - [ ] Declaração de anúncios: sem anúncios.
  - [ ] Verificar se `com.google.android.gms.permission.AD_ID` entra no manifest mesclado; declarar.
- [ ] **(B6) Conta pessoal nova:** planejar closed test com 12 testers por 14 dias antes de produção. Confirmar a regra vigente no console.
- [ ] **(B7) Compatibilidade targetSdk 36:**
  - [x] Predictive back: `android:enableOnBackInvokedCallback="true"` no manifest (Compose trata o back padrão).
  - [ ] Layout responsivo para telas grandes (orientação ignorada em telas grandes).
  - [ ] Verificar libs nativas com page size de 16 KB (`zipalign -P 16`).
- [ ] **(B8) Assets de loja:**
  - [ ] Ícone 512×512 PNG.
  - [ ] Adaptive icon em `mipmap`, com camada monocromática para Android 13+.
  - [ ] Feature graphic 1024×500.
  - [ ] Screenshots sem dados reais (revisar `work/` e `phone-screen.png`).
- [x] **(B9) Aviso legal:** texto visível na tela Sobre informando que o app não é registro oficial de ponto e que OCR exige revisão.
  - [ ] Adicionar também no onboarding e na listagem da Play Store.
  - [ ] Validar com jurídico trabalhista.

---

## 2. Sprint 1 — Arquitetura e robustez

- [ ] Quebrar `MainActivity.kt` (~2070 linhas) em telas e componentes.
- [ ] Criar ViewModel + `UiState` (sealed) por tela.
- [ ] Repository para receipts e settings; UseCases para OCR, backup, PDF e cálculo.
- [ ] Mover cálculo de `WorkStatsCalculator`, `buildMonthSections` e filtros para fora da thread principal.
- [ ] Substituir `LocalDate.now()` e `Date()` dentro de composição por fonte de tempo injetada (ex.: `Clock`); atualizar após meia-noite.
- [ ] Unificar estado: `rememberSaveable` onde a tela precisa sobreviver a rotação e process death.
- [ ] Criar `strings.xml` (pt-BR default, en). Remover strings hardcoded.
- [ ] Tratar erros com tipos próprios; remover `catch` genéricos silenciosos.
- [x] **Bug:** restaurar backup duplica comprovantes. Dedupe por `capturedAt` no restore, com validação de path traversal e manifesto. **Pendente:** dedupe por hash da imagem e limite de tamanho/entradas do ZIP (zip bomb).
- [x] Room: configurar `room.schemaLocation` e versionar schemas.
- [x] Limpeza de `cache/shared` (PDFs), `cache/updates` (APKs) e temporários de restore ao iniciar o app.

---

## 3. Sprint 2 — Testes e CI/CD

- [ ] Unit tests:
  - [ ] OCR: `normalizeWorkDate`, `normalizeClockTime`, regex de horário.
  - [ ] WorkStats: turno noturno, 29/02, período que cruza o ano, dias de folga, dia com batida única.
  - [ ] Crypto de backup: round-trip, senha errada, arquivo truncado, versão desconhecida.
  - [ ] Restore: path traversal (zip slip), zip bomb (limite de tamanho e de entradas).
- [ ] Instrumented tests:
  - [ ] DAO (busca, update, delete, ordenação).
  - [ ] Migração 1→2 com `MigrationTestHelper`.
  - [ ] Geração de PDF (1 e lote).
- [ ] Compose UI tests (fluxos: adicionar, revisar, excluir, exportar, backup).
- [ ] Screenshot tests (Paparazzi ou Roborazzi), light e dark, fonte normal e 200%.
- [ ] Macrobenchmark + Baseline Profile (cold start e scroll).
- [ ] CI:
  - [x] `lintRelease` além de `lintDebug` (`lintPlayRelease` no CI).
  - [ ] ktlint ou detekt.
  - [ ] Dependabot.
  - [ ] CodeQL e scan de dependências.
  - [ ] Actions pinadas por SHA.
  - [ ] Secrets injetados só no step que usa.
  - [ ] Validação do AAB e deploy automático para track interno (Gradle Play Publisher ou fastlane).
- [x] Ligar R8 em release (`isMinifyEnabled`, `isShrinkResources`); gerar mapping e validar AAB assinado ainda pendente.
- [ ] Atualizar dependências: Compose BOM, activity, lifecycle, Room, AGP e Kotlin.
- [ ] Remover `material-icons-extended`; manter só os ícones usados.

---

## 4. Sprint 3 — UX e acessibilidade AAA

- [ ] TalkBack: fluxo completo navegável.
- [ ] Seleção de comprovantes com custom action (não só long-press).
- [ ] `contentDescription` na thumbnail do card.
- [ ] Touch target mínimo de 48dp em todos os botões e ícones.
- [ ] Fonte em 200% sem corte nem sobreposição.
- [ ] Contraste WCAG AA em todos os estados (inclusive chips, erro e placeholder).
- [ ] Respeitar reduce motion.
- [ ] `DatePicker` e `TimePicker` no lugar (ou além) da digitação com máscara.
- [ ] Undo após exclusão (Snackbar com ação).
- [ ] Onboarding curto: privacidade, OCR, revisão e aviso legal (B9).
- [ ] Predictive back em todas as telas e sheets.
- [ ] Layout adaptativo (`WindowSizeClass`): tablet e dobrável.
- [ ] Paridade dark/light: remover cores hardcoded (ex.: `SummaryCard`).
- [ ] Haptics sutis em ações críticas (salvar, excluir).

---

## 5. Sprint 4 — Segurança e privacidade

- [ ] Backup V3:
  - [ ] PBKDF2-HMAC-SHA256 com 600k iterações, ou Argon2id via lib.
  - [ ] Header versionado (suporte a V1/V2 na leitura).
  - [x] Limite de tamanho e de número de entradas no ZIP (no restore; falta no formato/header V3).
- [x] Network security config (HTTPS only, sem cleartext).
- [x] Revisar `allowBackup` e exportação de `FileProvider` após ajustes (`allowBackup=false` + regras de extração).
- [ ] Updater (flavor `github`): checksum do APK publicado e comparação por `versionCode`.
- [ ] `FLAG_SECURE` opcional na visualização de comprovantes.
- [ ] App lock com biometria/credencial do aparelho (opcional, configurável).
- [ ] Fluxo LGPD no app: exportar dados e "apagar todos os dados".
- [ ] Scan com MobSF; inspecionar manifest mesclado (sem `debuggable`, sem permissões extras).
- [ ] Confirmar com proxy que o único tráfego é o esperado.

---

## 6. Sprint 5 — Performance e escala

- [ ] Paging 3 para listas grandes.
- [ ] Compressão de imagem na captura (ex.: máx. 2000 px, JPEG 80%), com opção de qualidade.
- [ ] Thumbnails em cache LRU.
- [ ] OCR em fila com retry (WorkManager), sem bloquear a UI.
- [ ] Gravar imagem com extensão real (`.jpg`, `.png` ou convertida).
- [ ] Meta: 5 mil comprovantes sem jank perceptível; cold start dentro do orçamento definido após o Baseline Profile.

---

## 7. Sprint 6 — Recursos AAA opcionais

- [ ] Backup automático no Google Drive com `drive.appdata` ou `drive.file` (evitar escopo amplo; ver `docs/google-play-readiness.md`).
- [ ] Observabilidade: Crashlytics com consentimento, ou só Play Vitals. Atualizar Data Safety se mudar.
- [ ] Feature flags para rollout controlado.
- [ ] Tela "O que há de novo" após atualização.

---

## 8. Sprint 7 — Lançamento

- [ ] Closed test: 12 testers por 14 dias (validar regra vigente).
- [ ] Listagem pt-BR e en: título, descrição curta (≤80 caracteres), descrição completa, screenshots, vídeo curto.
- [ ] Pre-launch report sem alertas críticos.
- [ ] Rollout em etapas: 5% → 20% → 50% → 100%.
- [ ] Acompanhar Vitals: crash abaixo de 1,09% e ANR abaixo de 0,47% (limites de "comportamento ruim").
- [ ] Plano de hotfix e canal de suporte respondendo no prazo.

---

## 9. Matriz de testes

### 9.1 OCR (funcional)
- [ ] 30+ comprovantes reais: sem data, um horário, dois horários, turno noturno, baixa luz, rotacionado, PNG, HEIC, print de tela.
- [ ] Revisão manual quando OCR falha; nunca gravar valor fictício.

### 9.2 Backup e restore
- [ ] Criar sem senha e com senha.
- [ ] Restaurar em instalação limpa.
- [ ] Senha errada.
- [ ] Arquivo truncado ou corrompido.
- [ ] Backup da versão 0.5.0 (compatibilidade).
- [ ] Restaurar duas vezes (sem duplicata após fix).
- [ ] Cancelar seletor de arquivo.
- [ ] Disco cheio durante criação e durante restore.

### 9.3 Ciclo de vida
- [ ] Rotação durante OCR, edição e backup.
- [ ] Process death ("Não manter atividades" nas opções do desenvolvedor).
- [ ] App morto durante OCR ou backup.
- [ ] Volta do background com sheet aberta.

### 9.4 Data e hora
- [ ] Virada de meia-noite com app aberto.
- [ ] Mudança de data e de fuso do aparelho.
- [ ] 29/02; mês com 5 semanas; período de folha cruzando o ano.
- [ ] Batida noturna (ex.: entrada 22:00, saída 06:00).

### 9.5 Dispositivos e sistemas
- [ ] Low-end (2–3 GB de RAM).
- [ ] Android 8.0, 10, 12, 14, 15 e 16.
- [ ] Marcas: Samsung, Xiaomi, Motorola, Pixel.
- [ ] Tablet e dobrável.
- [ ] Armazenamento quase cheio.

### 9.6 Acessibilidade
- [ ] Fluxo completo com TalkBack.
- [ ] Accessibility Scanner sem erros críticos.
- [ ] Fonte 200% e tamanho de exibição máximo.
- [ ] Contraste em light e dark.
- [ ] Switch Access.

### 9.7 Performance
- [ ] Cold start (com e sem Baseline Profile).
- [ ] Scroll com 5 mil itens; jank abaixo de 1%.
- [ ] Imagens grandes (48 MP) sem OOM.
- [ ] Geração de PDF com 50 comprovantes.

### 9.8 Updater (flavor `github`)
- [ ] Sem rede.
- [ ] Redirect.
- [ ] Download interrompido.
- [ ] APK corrompido ou checksum divergente.
- [ ] Release com versão menor (não deve oferecer).

### 9.9 Play e atualização
- [ ] Pre-launch report.
- [ ] Atualização de 0.5.0 para a versão assinada pela upload key (valida assinatura e migração do banco).
- [ ] Instalação limpa a partir do track interno.

### 9.10 Segurança e privacidade
- [ ] Manifest sem `debuggable`, sem segredos no build.
- [ ] Tráfego de rede via proxy (somente o esperado).
- [ ] Auto Backup conforme regra definida em B2.
- [ ] Data Safety compatível com o comportamento real.
- [ ] Compartilhamento (WhatsApp, Gmail, Drive) abre o PDF corretamente.

### 9.11 Jurídico e conteúdo
- [ ] Política de privacidade revisada (LGPD).
- [ ] Aviso legal presente na listagem e no app.
- [ ] Textos em pt-BR revisados.

---

## 10. Dependências entre sprints

- Sprint 0 é pré-requisito para qualquer envio à Play.
- Sprint 1 (arquitetura) antes de Sprint 2 (testes de UI), para evitar reescrever testes.
- Sprint 4 (backup V3) depende do dedupe do Sprint 1.
- Sprint 7 só começa com Sprints 0 a 5 concluídos e pre-launch report limpo.

---

## 11. Referências no repositório

- [`google-play-readiness.md`](google-play-readiness.md): checklist original.
- [`privacy-policy.md`](privacy-policy.md): base da política a ser reescrita.
- [`brand-book.md`](brand-book.md): identidade visual e acessibilidade.
- [`../README.md`](../README.md): visão geral e limites conhecidos.

---

## 12. Roadmap de animações e visualização — Meu Ponto

_Adicionado em 05/11/2026. É uma trilha de experiência visual, paralela às sprints 0–7 deste
documento. Não muda regra de jornada, não altera banco, não adiciona dependência e não autoriza
mudança de versão, tag ou release._

### 12.1 Objetivo e princípios

Objetivo: dar ao Meu Ponto a sensação de fluidez e continuidade que o dono admira no Samsung Health,
**sem copiar** layout, cores, ícones ou movimento do app de referência. A identidade continua sendo a
do [`brand-book.md`](brand-book.md): calma, precisão e discrição.

Regras de movimento para todas as sprints:

- Movimento comunica estado; não é decoração. Nenhum estado pode depender só de animação ou de cor.
- Animações de entrada duram **150–250 ms**; a animação do relógio dura **500–700 ms** e termina.
  Não há laço infinito por célula, nem atraso artificial para “apreciar” a animação.
- Uma única fonte de animação por grade visível. Evitar `rememberInfiniteTransition` por dia: com
  42 células isso vira jank e bateria, e o estado final fica imprevisível.
- `reduce motion` do sistema desliga o movimento e mostra o estado final imediatamente.
- A tela continua utilizável durante a animação; toque, rolagem e TalkBack não esperam.
- Cores e espaçamentos vêm dos tokens (`ui/Theme.kt`) e do brand book; nada de hex solto em
  composable.
- Sem dependência nova. As APIs de animação do Compose já presentes no projeto bastam.
- Dados exibidos vêm de `PeriodStats`/`WorkDayStats` já calculados. A UI não pareia batidas, não lê
  banco e não inventa horário.

### 12.2 Estado de partida verificado no repositório (05/11/2026)

- **Calendário multi-mês:** `ui/InsightsSheet.kt` já tem `monthsInPeriod(start, end)` e
  `MonthHeatCard` renderizando uma grade por mês. Essa alteração e os testes de 1 mês, 2 meses,
  virada de ano e período invertido estão **no working tree, ainda sem commit**. São a base do
  Sprint 1, não uma entrega concluída.
- **Horário de saída:** `domain/Journey` já expõe `exitTime` e `crossesMidnight`; `WorkDayStats`
  ainda **não** expõe saída. A grade do Insights recebe `WorkDayStats` por dia, então hoje não
  consegue mostrar o horário real de saída sem recalcular ou procurar jornada na UI.
- **Relógio:** o commit `4fae987` adicionou um ícone de relógio com rotação infinita nos dias
  incompletos. Ele **não atende** a esta trilha: não termina no horário real, não tem estado final
  de pendência e não respeita `reduce motion`. Serve apenas como referência visual.
- **Splash:** `Theme.MeuPonto.Starting` existe em `values/themes.xml` e `values-night/themes.xml`,
  com `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon` (`@drawable/ic_brand_glyph`) e
  `postSplashScreenTheme`; `installSplashScreen()` já é chamado em `MainActivity`. O
  `androidx.core:core-splashscreen` já é dependência do projeto.
- **Build local:** pendente, mas o SDK **não** é mais o bloqueio. Verificado em 05/11/2026:
  `local.properties` aponta para `sdk.dir=G:/DADOS/Tools/host/android-sdk`, e o SDK 36 com
  `platform-tools` existe nesse caminho. A execução chegou à etapa de compilação. O bloqueio
  remanescente é duplo: (a) cache incremental do Gradle/KSP com raízes antigas `C:/Dev2026`
  convivendo com o checkout atual em `G:/DADOS/Dev2026`; (b) falha do executor/JDK com
  `ClassNotFoundException: Files` e erro de `jdkImage`. Nada nesta trilha pode ser declarado
  “compila” ou “testado” sem execução real completa.
- **Pipeline:** `.github/workflows/android-release.yml` já roda testes das duas flavors, lint, APK
  `githubRelease` e AAB `playRelease`, em tag `v*` ou `workflow_dispatch`. O workflow é o caminho de
  validação enquanto o build local não estiver resolvido.

### 12.3 Sprint 1 — Calendário multi-mês consolidado e saída única no modelo

**Objetivo:** fechar a correção do calendário multi-mês e fazer o modelo do dia expor o horário de
saída real por um único caminho, sem duplicar pareamento nem criar regra na UI.

**Escopo:**

- Consolidar `monthsInPeriod` como **único dono** da enumeração de meses. `MonthHeatCard` só itera o
  resultado; nenhum composable calcula `YearMonth` por conta própria.
- Commitar a correção local do multi-mês junto dos testes de regressão (mesmo mês, dois meses,
  dezembro→janeiro, período invertido).
- Em `domain/WorkDayStats`, adicionar `exitTime: String?` (formato `HH:mm`) e
  `exitCrossesMidnight: Boolean` (padrão `false`), preenchidos no **mesmo passe** de
  `PunchPairing`/`journeys` que já produz `minutes` e `issues`. Sem segundo pareamento, sem join por
  texto, sem consulta ao banco.
- Semântica única, documentada em KDoc:
  - `exitTime` é a saída pareada da **última jornada creditada ao dia** (o dia creditado é o dia da
    entrada, como hoje).
  - É `null` quando o dia tem **qualquer** item em `issues` (pendência de pareamento, jornada
    implausível ou revisão marcada) ou não tem saída pareada. Assim “dia completo” é exatamente
    `issues.isEmpty() && exitTime != null`; a UI nunca mostra horário em dia pendente.
  - Turno que cruza a meia-noite continua creditado no dia da entrada; `exitCrossesMidnight` indica
    que a saída é do dia seguinte. O dia seguinte **não** herda essa saída.
  - Dia só com saída, só com ocorrência ou sem registro: `exitTime = null`.
- Adicionar os campos **no fim** do data class, com valor padrão, para não quebrar chamadas
  existentes.

**Critérios de aceite:**

- [ ] `monthsInPeriod` continua sendo a única função que enumera meses; a grade cobre todo o
      período, inclusive virada de ano, sem mês faltando ou duplicado.
- [ ] `WorkDayStats.exitTime` e `exitCrossesMidnight` são preenchidos pelo mesmo passe de
      pareamento; a UI não recalcula nem procura `Journey` para descobrir a saída.
- [ ] Horas, pendências e `PeriodStats` permanecem idênticos aos de antes da mudança.
- [ ] Um turno 22:00→06:00 aparece com saída no dia da entrada, inclusive `exitCrossesMidnight =
      true`; o dia seguinte não mostra 06:00 como saída própria.
- [ ] Nenhuma migração de banco, dependência, versão ou release é criada.
- [ ] Testes e lint passam no CI (`workflow_dispatch`); se o build local continuar inviável, isso é
      registrado no relatório.

**Testes obrigatórios (JVM, `app/src/test/`):**

- [ ] Mesmo comprovante com entrada e saída → `exitTime` correto.
- [ ] Entrada e saída em comprovantes separados no mesmo dia → `exitTime` correto.
- [ ] Turno noturno 23:50→06:57 → saída no dia da entrada, `exitCrossesMidnight = true`.
- [ ] Duas jornadas completas no mesmo dia → `exitTime` da última saída cronológica.
- [ ] Entrada sem saída, saída sem entrada, horário sem tipo e ocorrência → `exitTime = null`.
- [ ] Dia sem registro → `exitTime = null`.
- [ ] Invariante preservada: soma de `Journey.minutes` do dia = `WorkDayStats.minutes`.
- [ ] `monthsInPeriod`: mesmo mês, dois meses, dezembro→janeiro e período invertido.

**Dependências:** `PunchPairing`, `Journey` e `WorkDayStats` atuais; nenhuma dependência externa.
As sprints 2 e 3 dependem desta entrega.

**Riscos:**

- “Última saída” é ambígua em dia com várias jornadas. A regra fica explícita no KDoc e no teste,
  não na cabeça de quem lê a UI.
- Esconder a saída de dia pendente pode parecer perda de informação. A alternativa é pior: mostrar
  horário real em dia incompleto e o usuário achar que o dia fechou.
- Alterar `WorkDayStats` mexe em testes e chamadas; usar valor padrão e revisar o diff inteiro.

### 12.4 Sprint 2 — Relógio por dia, abertura do Insights e estado final de saída

**Objetivo:** ao abrir o calendário de Insights, cada dia ter uma animação sutil de relógio; dia
com entrada e saída terminar mostrando o horário real de saída; dia incompleto terminar em pendência
clara; com `reduce motion`, ir direto ao estado final.

**Escopo:**

- Animação de entrada do conteúdo do Insights na primeira composição da folha: fade/deslocamento
  curto (≤250 ms), uma vez por abertura, sem repetir em recomposição ou rolagem.
- Animação do relógio por dia visível: varredura única de 500–700 ms. Uma fonte de animação por
  grade (shared transition), não uma `rememberInfiniteTransition` por célula.
- Estado final:
  - dia completo: o ponteiro para e a célula mostra o `exitTime` real em `HH:mm`, com no mínimo
    11sp, contraste AA; `exitCrossesMidnight` acrescenta “dia seguinte” na descrição acessível;
  - dia incompleto: o relógio para em estado de pendência (ícone de pausa/atenção + ponto Coral) e
    **não** mostra horário; a descrição acessível usa `dayIssueSummary` para dizer o motivo;
  - dia sem registro: sem relógio e sem animação.
- O texto `HH:mm` é a fonte de verdade do horário; se a posição dos ponteiros for derivada do
  horário, ela é apenas reforço visual e não substitui o texto. Não usar “última leitura” do OCR
  como saída.
- `reduce motion`: detectar a escala de animação do sistema (`ValueAnimator.areAnimatorsEnabled()`,
  disponível no `minSdk` atual) ou equivalente; com movimento reduzido, renderizar o estado final
  imediatamente, sem varredura e sem laço.
- Acessibilidade: `contentDescription` final inclui dia, horas/pendência e saída; a animação não é
  anunciada repetidamente pelo TalkBack; alvos de toque ≥44dp; nada de informação só por cor.
- Performance: nenhuma animação roda fora da tela ou depois que a folha fecha; nada de alocação por
  frame; sem jank perceptível em aparelho de entrada.

**Critérios de aceite:**

- [ ] Abrir o Insights inicia a animação uma vez; fechar e reabrir reinicia; rolar ou recompor não
      reinicia.
- [ ] Todo dia completo termina exibindo o `exitTime` real; nenhum horário é estimado, arredondado
      para “bonito” ou copiado do dia vizinho.
- [ ] Todo dia incompleto termina em pendência visível e acessível, sem horário de saída.
- [ ] Com `reduce motion` ligado, o resultado é o estado final imediato, sem rotação.
- [ ] TalkBack lê o mesmo estado que a tela mostra; fonte 200% não corta o horário.
- [ ] Medição no aparelho não mostra jank perceptível ao abrir a folha nem ao rolar a grade.
- [ ] A identidade visual é a do Meu Ponto (tokens e brand book), não uma cópia do Samsung Health.

**Testes obrigatórios:**

- [ ] Testes puros em JVM para a regra de estado: completo → rótulo com saída; incompleto →
      pendência; só saída/ocorrência/sem registro → sem relógio; `reduceMotion = true` → estado
      final imediato.
- [ ] Testes puros da formatação/ângulo (se existir): `00:00`, `06:57`, `12:00`, `18:32`, `23:59` e
      entrada inválida; a saída textual continua sendo o contrato.
- [ ] QA manual no aparelho: abrir/fechar 10 vezes, com e sem `reduce motion`, TalkBack, fonte 200%,
      tema claro e escuro.
- [ ] Testes de regressão de `WorkStats`/`Journey` verdes (a animação não pode mudar horas).

**Dependências:** Sprint 1 (campos de saída no modelo). Sprint 3 pode começar em paralelo, mas a
integração final acontece depois desta sprint para não refazer a grade.

**Riscos:**

- Muitas animações simultâneas causam jank em aparelho de entrada. Mitigação: uma transição por
  grade, só células visíveis, duração curta.
- Movimento pode incomodar ou confundir. Mitigação: `reduce motion`, animação finita, estado final
  textual.
- Relógio de 12h não distingue 06:57 de 18:57. Mitigação: o texto `HH:mm` é obrigatório; o ponteiro
  é secundário.
- Célula pequena com horário pode ficar ilegível. Mitigação: mínimo de 11sp, fallback para resumo
  do dia selecionado em fonte ampliada.

### 12.5 Sprint 3 — Calendário mensal compacto no topo, sem perder a lista

**Objetivo:** colocar um calendário mensal compacto e sutil no topo da visualização do Insights,
preservando a leitura da lista/cards e a rolagem vertical.

**Escopo:**

- Um mês por vez no bloco compacto, no topo do conteúdo do Insights (depois do cabeçalho “Insights
  do período”), com setas anterior/próximo dentro dos meses do período.
- Mês inicial: o mês de hoje quando estiver dentro do período; senão o mês de `periodEnd`. A regra
  fica em função pura e testada.
- Célula compacta mostra número do dia, preenchimento por `HeatLevel` e o estado de saída/pendência
  da Sprint 2. O horário pode ficar em linha secundária; não pode sumir em dia completo.
- O bloco **não é sticky**: rola junto com o conteúdo. Não criar uma segunda rolagem vertical dentro
  da folha; a lista usa o mesmo contêiner rolável de hoje.
- Nenhum item da lista/cards é removido. Tocar no dia mantém o comportamento atual (`onSelectDay` →
  busca por data). Fechar/reabrir a folha não pode perder o mês selecionado de forma silenciosa.
- Em fonte 200%, o calendário pode mudar para uma apresentação mais compacta (ex.: uma linha por
  semana), mas não pode cortar, sobrepor nem empurrar a lista para fora de alcance.
- Visual: superfície discreta, sem sombra pesada, tokens do tema, grade de 8dp quando possível;
  o calendário não compete com o conteúdo principal.

**Critérios de aceite:**

- [ ] Em 360×640dp, fonte 100%, o bloco compacto (título + cabeçalho + grade + legenda) ocupa no
      máximo **300dp** e o primeiro conteúdo abaixo dele aparece sem rolagem.
- [ ] A lista/cards continuam roláveis com o calendário visível; não há rolagem aninhada nem
      “pulo” de posição ao trocar de mês.
- [ ] Cada célula tem alvo de toque efetivo ≥44dp (mesmo que o círculo visual seja menor), sem alvos
      sobrepostos; TalkBack lê dia, horas, saída/pendência e estado selecionado.
- [ ] Fonte 200% e tema claro/escuro não cortam texto; contraste AA; cor não é o único indicador.
- [ ] Setas desabilitadas nos limites do período; o mês nunca sai do intervalo
      `periodStart..periodEnd`.
- [ ] Nenhuma lógica de mês duplicada entre UI e domínio.

**Testes obrigatórios:**

- [ ] JVM: mês inicial dentro/fora do período; avançar/voltar com clamp no primeiro e no último mês;
      período de um mês não mostra navegação ativa; período inválido não quebra.
- [ ] QA manual: 360×640dp, tablet, fonte 100%/200%, claro/escuro, TalkBack e Accessibility Scanner
      sem erro crítico.
- [ ] Regressão: tocar em dia ainda filtra a lista; rolagem da lista continua fluida; nenhum
      comprovante desaparece.

**Dependências:** Sprint 1 (modelo de saída) e Sprint 2 (estado visual por dia). A Sprint 4 pode
rodar em paralelo.

**Riscos:**

- “Compacto” conflitar com alvo de 44dp. Mitigação: círculo visual menor com área de toque real
  maior e validação no Accessibility Scanner.
- O calendário roubar espaço da lista. Mitigação: teto de 300dp, não sticky, fallback em fonte
  ampliada.
- Navegação de mês brigar com o gesto de fechar a folha. Mitigação: setas explícitas; swipe só se
  não conflitar com o `ModalBottomSheet`.

### 12.6 Sprint 4 — Splash moderna, rápida e acessível

**Objetivo:** modernizar a splash mantendo a marca, sem dependência nova, com abertura rápida e
coerente nos temas claro e escuro.

**Escopo:**

- Usar os recursos nativos já existentes: `Theme.MeuPonto.Starting` (`Theme.SplashScreen`),
  `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, `postSplashScreenTheme`,
  `@drawable/ic_brand_glyph` e `installSplashScreen()`.
- Ajustar somente o que for necessário em `values/` e `values-night/`; sem nova Activity de splash,
  sem biblioteca nova, sem animação que atrase a primeira tela.
- Fundo da splash igual ao fundo do primeiro frame do Compose, para não piscar branco/preto na
  transição. Ícone centralizado, dentro da zona segura, sem esticar, cortar ou perder contraste.
- Acessibilidade: glifo é decorativo; não há texto essencial na splash; funciona com fonte 200% e
  com TalkBack sem anunciar informação falsa; contraste adequado nos dois temas.
- Não usar movimento obrigatório. Se houver animação de ícone, ela é curta e respeita
  `reduce motion`; a opção mais segura é o glifo estático.

**Critérios de aceite:**

- [ ] Cold start não mostra tela em branco/preto entre splash e primeiro frame do app.
- [ ] Splash não adiciona espera artificial; o tempo até a primeira tela não piora.
- [ ] Ícone e fundo corretos em claro e escuro, Android 12+ e versões anteriores suportadas
      (`minSdk` atual), sem distorção.
- [ ] Nenhuma dependência, permissão, versão ou recurso de terceiros é adicionado.
- [ ] QA em aparelho físico: cold start 5 vezes, comparar mediana antes/depois; regressão ≤100 ms ou
      justificativa registrada.
- [ ] Sem crash/ANR e sem mudança de fluxo de abertura.

**Testes obrigatórios:**

- [ ] Build/lint das duas flavors no CI e instalação do APK gerado em aparelho físico.
- [ ] QA visual: claro/escuro, Android 12+, Android 8/10, aparelho de entrada e tela grande.
- [ ] Medição de cold start (5 execuções) e comparação com a baseline registrada.
- [ ] TalkBack, fonte 200% e `reduce motion` sem regressão na abertura.

**Dependências:** nenhuma das sprints anteriores; pode rodar em paralelo. Depende apenas do tema e
dos recursos já existentes.

**Riscos:**

- Flash branco/preto por tema ou `windowBackground` divergente. Mitigação: alinhar fundo da splash e
  primeiro frame.
- “Modernizar” virar animação longa. Mitigação: sem atraso artificial, glifo estático por padrão.
- Fabricantes alteram a splash do sistema. Mitigação: validar em aparelho real, não só emulador.

### 12.7 Gate de release

**Nada desta trilha pode ser publicado enquanto todos os itens abaixo não estiverem verdes.** O gate
vale para APK do GitHub e AAB da Play.

- [ ] **Testes:** `./gradlew :app:testPlayDebugUnitTest :app:testGithubDebugUnitTest --no-daemon`
      verde nas duas flavors.
- [ ] **Lint:** `./gradlew :app:lintPlayDebug :app:lintGithubDebug :app:lintPlayRelease
      :app:lintGithubRelease --no-daemon` sem erro novo; warnings justificados.
- [ ] **Build:** APK `githubRelease` e AAB `playRelease` gerados pelo
      `.github/workflows/android-release.yml` (tag ou `workflow_dispatch`) sem falha. Build local só
      conta se realmente executado; não afirmar “compila” sem compilar.
- [ ] **QA real do aparelho:** aparelho físico, não WSA/emulador, com os cenários abaixo.
- [ ] **Sem regressão de dados:** horas, pendências, saldo, filtro por dia e lista idênticos aos de
      antes da trilha; nenhum comprovante some ou muda de dia.
- [ ] **Acessibilidade:** TalkBack, fonte 200%, contraste AA, `reduce motion` e alvos ≥44dp aprovados
      no Accessibility Scanner.
- [ ] **Performance:** abertura do Insights e rolagem sem jank perceptível; cold start dentro do
      orçamento da Sprint 4.
- [ ] **Escopo:** nenhuma dependência nova, nenhuma migração de banco, nenhum `versionName`/
      `versionCode` alterado.

**QA mínimo no aparelho (obrigatório antes de release):**

- [ ] Abrir o Insights com período de um mês e de dois meses; conferir todas as grades.
- [ ] Dia completo mostra a saída real; dia incompleto mostra pendência; dia sem registro não anima.
- [ ] Turno noturno: saída aparece no dia da entrada, com indicação de dia seguinte quando aplicável.
- [ ] `reduce motion` ligado: estado final imediato, sem rotação; desligado: animação curta e finita.
- [ ] Fechar/reabrir a folha; girar a tela; voltar do background; rolar a lista.
- [ ] Fonte 200%, tema claro/escuro, TalkBack e Accessibility Scanner.
- [ ] Splash: cold start 5 vezes, claro/escuro, Android 12+ e versão antiga.
- [ ] Aparelho de entrada (2–3 GB) sem jank ou ANR.

**Estado atual do build e do release (05/11/2026):**

- Build local **pendente**, porém o Android SDK 36 **existe e está configurado**: `local.properties`
  aponta para `sdk.dir=G:/DADOS/Tools/host/android-sdk`, com SDK 36 e `platform-tools` presentes. O
  build chegou à compilação; o que falta é (a) limpar/realinhar o cache incremental, que ainda
  referencia raízes antigas `C:/Dev2026` em vez de `G:/DADOS/Dev2026`, e (b) resolver a falha do
  executor/JDK com `ClassNotFoundException: Files` e erro de `jdkImage`. Ainda não há resultado
  local completo de testes/lint/build para esta trilha.
- O workflow `.github/workflows/android-release.yml` **já** gera APK e AAB e roda testes/lint; usar
  `workflow_dispatch` como dry-run de validação.
- A release atual **não deve ser acionada** por esta trilha: não criar tag, não publicar release,
  não alterar versão e não fazer push de tag. A decisão de publicar é do dono, depois do gate verde e
  do QA no aparelho.

### 12.8 Rastreabilidade das ideias do dono

| Ideia original | Sprint | Como fica verificável |
|---|---|---|
| Fluidez inspirada no Samsung Health, com identidade própria | 2 e 3 | Movimento curto e finito, tokens do brand book, checklist de identidade e ausência de cópia de layout |
| Relógio por dia; dia com entrada/saída termina no horário real; dia incompleto comunica pendência | 1 e 2 | `WorkDayStats.exitTime` único, estado final textual `HH:mm`, pendência sem horário e testes de domínio/estado |
| Calendário mensal compacto no topo, preservando lista e rolagem | 3 | Teto de 300dp, primeiro conteúdo visível, rolagem única, alvos ≥44dp e QA de acessibilidade |
| Splash moderna, rápida, acessível, sem dependência | 4 | Tema nativo existente, sem atraso artificial, QA de cold start e temas claro/escuro |

### 12.9 Fora de escopo

- Mudar `PunchPairing`, cálculo de horas, regra de ocorrência, banco ou migração.
- Adicionar biblioteca de animação, splash, calendário ou ícones.
- Refatorar `MainActivity.kt`, `InsightsSheet.kt` inteiro ou `ENG-1`/`ENG-2` por causa desta trilha.
- Criar dados fictícios para “ficar bonito” em captura, teste ou release.
- Alterar `versionName`/`versionCode`, criar tag, publicar release ou mudar o pipeline.
