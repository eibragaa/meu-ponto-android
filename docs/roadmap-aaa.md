# Meu Ponto — roadmap para publicação na Play Store (nível AAA)

_Análise feita em 22/10/2026 sobre a versão `0.5.0` (versionCode 8)._

Objetivo: aprovação na Google Play e qualidade de produto acima da média (estabilidade, acessibilidade, privacidade, performance e testes).

Legenda: `[ ]` pendente · `[x]` concluído · **(B)** bloqueador de publicação.

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
