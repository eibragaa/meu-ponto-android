# Meu Ponto — status atual e próximos passos

_Atualizado em 22/10/2026. Versão do código: `0.5.0` (`versionCode 8`)._

Este documento registra o que foi feito até aqui, o estado atual do projeto, os problemas conhecidos e os próximos passos. Complementa o [`roadmap-aaa.md`](roadmap-aaa.md), que é o plano por sprints.

---

## 1. Histórico de trabalho

### 1.1 Análise e planejamento
- Análise completa do projeto para publicação na Play Store.
- Lista de bloqueadores (B1 a B9) e melhorias por área (build, arquitetura, dados, segurança, testes, UX e acessibilidade).
- Roadmap em sprints (0 a 7) e matriz de testes, salvo em [`roadmap-aaa.md`](roadmap-aaa.md).

### 1.2 Trabalho encontrado no working copy (ainda não commitado antes desta atualização)
Mudanças já existentes no projeto ao iniciar esta rodada, que implementam parte do Sprint 0 e 1:
- **Flavors** `play` e `github`. A `play` não tem auto-update; a `github` mantém o updater para sideload. Flag `BuildConfig.UPDATER_ENABLED`.
- **Permissão** `REQUEST_INSTALL_PACKAGES` removida da flavor `play` (`app/src/play/AndroidManifest.xml`).
- **Backup**: `allowBackup="false"`, `dataExtractionRules`, `fullBackupContent` (excluem banco, `receipts/` e `cache/`).
- **Network security config** com `cleartextTrafficPermitted="false"`.
- **Predictive back**: `enableOnBackInvokedCallback="true"`.
- **Room**: schema exportado para `app/schemas/` (`room.schemaLocation` configurado).
- **Restore sem duplicar**: checagem por `capturedAt` antes de inserir (`getByCapturedAt`).
- **Limpeza de cache** de PDFs, APKs e backups temporários (`cleanupUpdateAndPdfCaches`).
- **Novos módulos** no código: `domain/` (cálculo de folha e horas), `ui/` (insights, configuração de folha, visualizador de comprovante).
- **Política de privacidade** reescrita com estrutura LGPD (ver seção 4).
- **CI** atualizado: roda testes e lint por flavor, gera `assemblePlayDebug` e `bundlePlayRelease` (ver seção 3).

### 1.3 Release local para teste (esta rodada)
- `app/build.gradle.kts`: release com R8 (`isMinifyEnabled`), shrink de recursos e `proguard-android-optimize.txt` + `proguard-rules.pro`.
- Sem keystore de produção, o release é assinado com a chave debug (só para teste local).
- `app/proguard-rules.pro` criado, sem regras extras por enquanto.
- README: nova seção "Testar a versão release localmente".
- Build validado: `assemblePlayRelease` gerou `app-play-release.apk` (42,5 MB). Assinatura conferida com `apksigner`. Mapping do R8 gerado.

### 1.4 Teste no WSA (Windows Subsystem for Android)
- Conexão via `adb connect 127.0.0.1:58526`.
- Release `0.5.0` instalado por cima do `br.com.jean.meuponto` já existente (mesma assinatura debug, dados mantidos).
- App abre sem crash. Nenhum `AndroidRuntime` no logcat.
- Tela inicial com lista vazia (0 comprovantes).
- Imagem de teste enviada para `/sdcard/Pictures/MeuPontoTeste/comprovante-teste.png`.
- O WSA **não tem Google Play Services nem Play Store**. Consequência: o scanner de documentos (câmera) não funciona nele.
- Observado: o período de folha mudou para 05/10 a 06/10 com o aviso "Período de folha atualizado", e a sheet "Configurar folha" ficou aberta. Não foi enviado nenhum toque para isso. Confirmar com o usuário se foi uso manual.

### 1.5 Git
- Mudanças commitadas em grupos por tema e enviadas ao remote (`origin/main`). Ver seção 5.

---

## 2. Estado atual do código

| Área | Estado |
|---|---|
| Flavors `play` / `github` | Implementado |
| Auto-update | Só na `github`. Na `play` não existe |
| Permissões | `INTERNET` em ambas. `REQUEST_INSTALL_PACKAGES` só na `github` |
| Backup automático do Android | Desativado (`allowBackup=false` + regras) |
| Backup do app (ZIP, AES-GCM com senha) | Implementado (PBKDF2 com 120k iterações) |
| Restore | Implementado, com checagem de duplicata por `capturedAt` |
| Room | v2, schema exportado. Migração 1→2 sem teste automatizado |
| R8 em release | Ativado, sem regras extras. **Não testado em runtime além do WSA (sem fluxo completo)** |
| Testes unitários | `WorkStatsCalculatorTest` e `AppUpdaterTest`. Nenhum teste novo |
| Strings | Hardcoded em pt-BR. Sem `strings.xml` |
| Arquitetura | `MainActivity.kt` com ~2135 linhas. Sem ViewModel nem Repository |
| Acessibilidade | Não auditada. Seleção de comprovantes só por long-press |

---

## 3. Releases e pipeline no GitHub

- **Último release publicado:** `v0.4.0`, em 20/10/2026, marcado como Latest.
  - Asset: `Meu-Ponto-v0.4.0.apk` (61 MB), **build debug** (debuggable), assinada com a chave do runner do CI.
  - **Não existe tag `v0.5.0`.** O código atual é `0.5.0`, mas nada foi publicado.
- **Runs recentes do workflow `Android APK Release`:**
  - 22/10 14:00 (`workflow_dispatch` no `main`): sucesso, mas rodou o workflow **antigo** (ainda sem commit).
  - 20/10 13:18 e 13:16 (commit `f72f86d`): falharam com 0s. Corrigidos no commit `29de4f6`.
  - 20/10 (tag `v0.4.0`): sucesso.
- **Efeito no updater:** quem está na `0.4.0` não recebe atualização, porque o updater só compara com o último release (`v0.4.0`). Isso é esperado até existir a `v0.5.0`.

---

### 3.1 Keystore de distribuição (criado em 22/10/2026)
- Arquivo: `C:\Users\jeanb\.meuponto\meu-ponto-release.jks` (fora do repo; `.gitignore` bloqueia `*.jks`).
- Credenciais: `C:\Users\jeanb\.meuponto\credenciais-release.txt`. Guardar uma cópia do `.jks` e das senhas fora deste PC. **Sem eles não há atualização futura.**
- Alias: `meuponto-release`. SHA-256 do certificado: `4E:F4:64:D5:F9:EC:8E:6C:15:6A:48:26:FB:2A:0C:0F:BA:34:4C:7B:15:CD:C7:E7:6A:55:C8:CD:C2:BB:36:83`.
- Secrets no GitHub cadastrados: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Validado: `assembleGithubRelease` local assinado com este certificado.
- **Pendente:** o celular ainda tem APK com outra assinatura. A migração (backup pelo app, desinstalar, reinstalar) não foi feita. O workflow ainda precisa de correção (ver P11).

## 4. Política de privacidade (`docs/privacy-policy.md`)

Reescrita com estrutura LGPD (controlador, base legal por dado, segurança e retenção, direitos do titular, permissões). Precisa de revisão antes de publicar:

- [x] **Corrigir texto:** o item do OCR agora diz "extraído da imagem do comprovante".
- [x] **Confirmar o termo:** "DVT" trocado por **encarregado** em todo o texto. Falta apenas definir a pessoa ou canal do encarregado (ver decisões pendentes).
- [ ] **Preencher placeholders:** responsável e contato de suporte real.
- [ ] **Publicar em URL HTTPS** pública antes da submissão na Play.
- [ ] **Conferir** se o texto bate com o comportamento real (Auto Backup desativado, updater só na `github`).

---

## 5. Commits desta rodada

Ver `git log` no remote. Grupos:

1. `build`: flavors, R8, proguard, schema Room e seção de teste local no README.
2. `chore(security)`: manifest, regras de backup, network security config e `play` sem permissão de instalação.
3. `feat(app)`: folha, insights, visualizador, restore sem duplicata e updater por flavor.
4. `test`: testes de `WorkStatsCalculator`.
5. `ci`: testes e lint por flavor, builds `play`.
6. `docs`: política de privacidade, roadmap e este status.

---

## 6. Problemas conhecidos

| # | Problema | Impacto | Ação |
|---|---|---|---|
| P1 | Testes unitários não rodam neste PC: o executor do Gradle falha porque o `JAVA_HOME` tem espaço (`C:\Program Files\...`). Compilação funciona. | Não dá para validar testes localmente | Mover o JDK para caminho sem espaço, ou rodar pelo Android Studio / CI |
| P2 | Lint e build de release não rodaram via CI após as mudanças (o último run usou o workflow antigo) | Falhas de CI só aparecem depois do push | Rodar `lintPlayDebug` e `assemblePlayRelease` localmente; acompanhar o run do CI |
| P3 | Release público `v0.4.0` é APK debug e debuggable | Exposição de build de teste e confusão para o usuário | Marcar como pré-release ou remover (decisão do usuário) |
| P4 | Não existe `v0.5.0` | Quem está na `0.4.0` não recebe update | Criar tag após testes no celular |
| P5 | Scanner de câmera não funciona no WSA | Não dá para testar o fluxo de câmera nesse ambiente | Testar em celular real |
| P6 | `allowBackup=false` | Reinstalar o app sem backup manual perde os dados | Confirmar se é a decisão desejada |
| P7 | Restore deduplica por `capturedAt` | Pode deixar passar comprovantes diferentes com o mesmo timestamp | Trocar por hash da imagem (Sprint 1) |
| P8 | Política com termos incorretos (seção 4) | Risco de reprovação ou de informação errada | Revisar antes de publicar |
| P9 | Período de folha alterado no WSA sem ação registrada | Dúvida sobre dados de teste | Confirmar com o usuário |
| P10 | Updater não identifica o celular atual | Não sabemos a versão instalada no celular | Informar a versão do app no celular |
| P11 | Workflow de release quebrado: caminhos antigos (`apk/debug`, `bundle/release`) e não gera o APK da `github` | Nova release não tem APK para o updater | Corrigir workflow (aguardando confirmação) |
| P12 | Releases anteriores assinadas com certificados debug diferentes (`v0.3.3` = `8f347d…`, `v0.4.0` = `2aa251…`) | Update por cima entre releases do CI falha | Usar o keystore fixo (seção 3.1) |

---

## 7. Próximos passos

### Agora (prioridade alta)
1. **Testar no celular real** com o release local: scanner, galeria com OCR, edição, PDF individual e em lote, backup com e sem senha, restore, tema claro e escuro.
2. **Diagnosticar o updater no celular:** informar a versão instalada e se é a build da `github`.
3. **Revisar a política de privacidade** (seção 4) com as correções apontadas.
4. **Decidir sobre o release `v0.4.0`** (P3) e quando publicar a `v0.5.0` (P4).
5. **Resolver P1:** rodar testes unitários e lint localmente.

### Sprint 0 pendente (ver roadmap)
- Keystore de upload e Play App Signing (B3).
- Data Safety, IARC, público-alvo, declaração de anúncios e AD_ID (B5).
- Adaptive icon em `mipmap` e assets de loja (B8).
- `BackHandler` / predictive back nos sheets e telas (B7), layout para telas grandes e checagem de 16 KB.
- Aviso legal no app (B9).
- Closed test: 12 testers por 14 dias (B6).

### Sprint 1 e 2 pendentes
- ViewModel, Repository e UseCases; quebrar `MainActivity.kt`.
- `strings.xml` (pt-BR e en).
- Hash da imagem no restore (P7).
- Testes: OCR, backup (round-trip, senha errada, arquivo corrompido, zip slip), DAO e migração 1→2.
- Regras R8 testadas em release.
- CI com `lintRelease`.

### Decisões que dependem de você
- Aceitar `allowBackup=false` (P6)?
- Qual pessoa ou canal será o encarregado (LGPD)?
- Manter o `v0.4.0` público, marcar como pré-release ou remover?
- Publicar a `v0.5.0` quando os testes no celular passarem?
