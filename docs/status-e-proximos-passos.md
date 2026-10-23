# Meu Ponto — status atual e próximos passos

_Atualizado em 23/10/2026. Versão publicada: `v0.5.1` (`versionCode 9`)._

Este documento registra o que foi feito, o estado atual, os problemas conhecidos e os próximos passos. Complementa o [`roadmap-aaa.md`](roadmap-aaa.md), que é o plano por sprints.

---

## 0. Resumo do dia (22/10/2026)

- Análise completa para publicação na Play Store e roadmap em sprints.
- Verificação do estado das releases e do código.
- Release local para teste (R8 + proguard), testada no WSA.
- Política de privacidade corrigida (termos LGPD).
- Keystore de distribuição criado e cadastrado no GitHub (Secrets).
- Workflow de release corrigido (assina `github`, valida tag, publica APK do updater).
- Tag `v0.5.0` criada e publicada: **Latest**, com APK assinado pelo keystore novo.
- Docs e código no `main` sincronizados com o remote.

**Pendente para amanhã:** migrar o celular e testar. Ver seção 7.

---

## 1. Histórico de trabalho

### 1.1 Análise e planejamento
- Análise completa do projeto para publicação na Play Store.
- Lista de bloqueadores (B1 a B9) e melhorias por área (build, arquitetura, dados, segurança, testes, UX e acessibilidade).
- Roadmap em sprints (0 a 7) e matriz de testes em [`roadmap-aaa.md`](roadmap-aaa.md).

### 1.2 Trabalho que já estava no working copy (commitado em 22/10)
Parte do Sprint 0 e 1 já estava implementada antes desta rodada e foi commitada em grupos:
- **Flavors** `play` e `github`. A `play` não tem auto-update; a `github` mantém o updater para sideload. Flag `BuildConfig.UPDATER_ENABLED`.
- **Permissão** `REQUEST_INSTALL_PACKAGES` removida da flavor `play` (`app/src/play/AndroidManifest.xml`).
- **Backup**: `allowBackup="false"`, `dataExtractionRules`, `fullBackupContent` (excluem banco, `receipts/` e `cache/`).
- **Network security config** com `cleartextTrafficPermitted="false"`.
- **Predictive back**: `enableOnBackInvokedCallback="true"`.
- **Room**: schema exportado para `app/schemas/` (`room.schemaLocation` configurado).
- **Restore sem duplicar**: checagem por `capturedAt` antes de inserir (`getByCapturedAt`).
- **Limpeza de cache** de PDFs, APKs e backups temporários (`cleanupUpdateAndPdfCaches`).
- **Novos módulos**: `domain/` (cálculo de folha e horas), `ui/` (insights, configuração de folha, visualizador de comprovante).
- **Política de privacidade** reescrita com estrutura LGPD (seção 4).

### 1.3 Release local para teste
- `app/build.gradle.kts`: release com R8 (`isMinifyEnabled`), shrink de recursos e `proguard-android-optimize.txt` + `proguard-rules.pro`.
- Sem keystore de produção, o release é assinado com a chave debug (só teste local).
- `app/proguard-rules.pro` criado, sem regras extras por enquanto.
- README: seção "Testar a versão release localmente".
- Build validado: `assemblePlayRelease` gerou `app-play-release.apk` (42,5 MB). Assinatura conferida com `apksigner`. Mapping do R8 gerado.

### 1.4 Teste no WSA (Windows Subsystem for Android)
- Conexão via `adb connect 127.0.0.1:58526`.
- Primeiro release instalado por cima do `br.com.jean.meuponto` existente (mesma assinatura debug, dados mantidos). App abriu sem crash.
- Tela inicial com lista vazia (0 comprovantes). Nenhum `AndroidRuntime` no logcat.
- O WSA **não tem Google Play Services nem Play Store**. O scanner de câmera não funciona nele.
- Observado: o período de folha mudou para 05/10 a 06/10 com o aviso "Período de folha atualizado" e a sheet "Configurar folha" ficou aberta. Nenhum toque foi enviado para isso. Confirmar com o usuário (P9).
- Na tela Sobre: versão 0.5.0, aviso "não é registro oficial de ponto" e botão "Política de privacidade" presentes.

### 1.5 Keystore de distribuição e Secrets
- Keystore criado fora do repo, Secrets cadastrados no GitHub (seção 3.1).
- Um erro de script foi corrigido na hora: senha da chave diferente da senha do store. Secret e arquivo de credenciais foram alinhados, e o build passou.

### 1.6 Migração do WSA
- `br.com.jean.meuponto` desinstalado e reinstalado com o APK `github` assinado pelo keystore novo (`4ef464d5…`). Abre sem crash, versão 0.5.0.
- Teste do updater no WSA: **inconclusivo** (ver P13). O botão "Buscar atualizações agora" está na Sobre, mas não consegui capturar a mensagem de retorno.

### 1.7 Workflow, tag e release v0.5.0
- Workflow corrigido (seção 3.2) e validado localmente (YAML, caminhos, `assembleGithubRelease` e `bundlePlayRelease` assinados).
- Tag `v0.5.0` criada e enviada. Run `39811970961` concluído com **sucesso** (todas as etapas).
- Release `v0.5.0` publicada, marcada como **Latest**, não é draft nem pré-release.

### 1.8 Git
- Commits do dia no `main` (seção 5), tag `v0.5.0` no commit `49d66b6`. Árvore local sincronizada com o remote.

### 1.9 Turno noturno zerado no celular (pós-release)
- Caso real: turno 22:45 → 06:55 do dia seguinte. No celular (Galaxy S25, Android 17), a Outubro mostrava 0h e "Revisar" em todos os dias.
- Causa 1 (dados): comprovantes restaurados do backup têm só `workTime` (ex.: "Horário 22:43") e **sem tipo** (entrada ou saída). Sem tipo, o app marca como pendente e soma 0.
- Causa 2 (lógica): o cálculo pareava entrada e saída só dentro do mesmo dia, pela hora do dia. Turno que cruza a meia-noite nunca era pareado, mesmo com tipo definido.
- Correção no código: pareamento cronológico entre dias (`WorkStatsCalculator.pairPunches`). A duração vai para o dia da entrada. Entrada sem saída em até 16h fica pendente. Saída sem entrada fica pendente.
- Testes novos (5): turno que cruza a meia-noite, sequência de turnos noturnos, saída sem entrada, entrada sem saída, horário sem tipo. Todos os 16 testes da suíte passam (rodados com JUnit direto, ver P1).
- APK `github` com a correção instalado no celular por cima da 0.5.0 (mesma assinatura; dados preservados: 12 comprovantes).
- **Pendente:** classificar os comprovantes no celular (Editar → "Só entrada" ou "Só saída"). Sem isso, o app continua pendente, mesmo com a lógica corrigida.
- **Publicado:** correção incluída na release `v0.5.1` (ver 1.11).

---

### 1.10 Reorganização do código (22/10, noite)
- `MainActivity.kt` passou de 2.152 para **1.567 linhas**: tema, formatação de datas, agrupamento da lista, máscaras, OCR, backup, PDF e limpeza de cache saíram para `ui/` e `data/` (nada de lógica alterada nesse commit).
- Regra de duração unificada: `durationMinutes` passou a aplicar o mesmo teto de 16h do pareamento (antes, entrada+saída no mesmo comprovante aceitava jornada irreal).
- Testes: 16 → **35** (novos: máscaras/P18, formatação de minutos, bordas de comparação de versão, consistência de jornada).
- Padrão de engenharia do repositório criado em [`engineering-standards.md`](engineering-standards.md) (+ `AGENTS.md` e PR template) — vale para pessoa e para qualquer agente de IA.
- **Validado no CI:** testes, lint e builds das duas flavors passaram no run `39891919429`; extração do `MeuPontoApp` (504 linhas) segue no roadmap do padrão.

### 1.11 Release v0.5.1 (23/10/2026)
- `versionName` `0.5.1` / `versionCode` 9 no commit `215c8b3`.
- Tag `v0.5.1` criada e enviada; run `39910556532` concluído com sucesso.
- Release publicada como **Latest**, com `Meu-Ponto-v0.5.1.apk` e `app-play-release.aab`.
- Release notes curadas aplicadas no GitHub com as correções de turno noturno e editor.
- **Pendente:** validar no aparelho a instalação por cima da `0.5.0`, o updater e a preservação dos dados.

---

## 2. Estado atual do código

| Área | Estado |
|---|---|
| Flavors `play` / `github` | Implementado |
| Auto-update | Só na `github`. Na `play` não existe (In-App Updates API da Play ainda não implementada) |
| Permissões | `INTERNET` em ambas. `REQUEST_INSTALL_PACKAGES` só na `github` |
| Backup automático do Android | Desativado (`allowBackup=false` + regras) |
| Backup do app (ZIP, AES-GCM com senha) | Implementado (PBKDF2 com 120k iterações) |
| Restore | Implementado, com checagem de duplicata por `capturedAt` |
| Room | v2, schema exportado. Migração 1→2 sem teste automatizado |
| R8 em release | Ativado, sem regras extras. Testado só no WSA (fluxo completo não rodado) |
| Testes unitários | 35 testes em 4 arquivos: `WorkStatsCalculatorTest`, `FormattingTest`, `MaskedInputTest`, `AppUpdaterTest`. Backup/PDF/DAO ainda sem teste |
| Strings | Hardcoded em pt-BR. Sem `strings.xml` |
| Arquitetura | `MainActivity.kt` com 1.567 linhas + camadas `domain/`, `data/`, `ui/`, `updater/`. Sem ViewModel nem Repository |
| Padrão de engenharia | [`engineering-standards.md`](engineering-standards.md) + [`AGENTS.md`](../AGENTS.md). Pendências `ENG-1` a `ENG-6` |
| Acessibilidade | Não auditada. Seleção de comprovantes só por long-press |

---

## 3. Releases e pipeline no GitHub

### 3.0 Releases publicadas
| Versão | Data | Estado | Assets | Assinatura |
|---|---|---|---|---|
| **`v0.5.1`** | 23/10/2026 06:17 UTC | **Latest** | `Meu-Ponto-v0.5.1.apk`, `app-play-release.aab` | Keystore novo (`4ef464d5…`) |
| `v0.5.0` | 22/10/2026 16:19 UTC | Anterior | `Meu-Ponto-v0.5.0.apk` (44,5 MB), `app-play-release.aab` (22,8 MB) | Keystore novo (`4ef464d5…`) |
| `v0.4.0` | 20/10/2026 | Anterior | `Meu-Ponto-v0.4.0.apk` (61 MB), **build debug** (debuggable) | Chave do runner (`2aa251…`) |
| `v0.3.3` | 16/10/2026 | Anterior | `Meu-Ponto-v0.3.3.apk` | Chave do runner (`8f347d…`) |

- Run da tag `v0.5.1` concluído com sucesso; release marcado como **Latest**.
- Efeito no updater: quem está na `0.5.0` passa a receber a `0.5.1`. Falta validar a instalação no aparelho.

### 3.1 Keystore de distribuição (criado em 22/10/2026)
- Arquivo: `C:\Users\jeanb\.meuponto\meu-ponto-release.jks` (fora do repo; `.gitignore` bloqueia `*.jks`).
- Credenciais: `C:\Users\jeanb\.meuponto\credenciais-release.txt`. Guardar uma cópia do `.jks` e das senhas fora deste PC. **Sem eles não há atualização futura.**
- Alias: `meuponto-release`. SHA-256 do certificado: `4E:F4:64:D5:F9:EC:8E:6C:15:6A:48:26:FB:2A:0C:0F:BA:34:4C:7B:15:CD:C7:E7:6A:55:C8:CD:C2:BB:36:83`.
- Secrets no GitHub: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Mesma chave é usada para a flavor `github` e para o AAB da `play`. Quando for para a Play com Play App Signing, a chave de upload deve ser avaliada (B3).

### 3.2 Workflow de release (`.github/workflows/android-release.yml`)
- Tag exige todos os Secrets de assinatura (falha cedo se faltar).
- Tag precisa bater com `versionName` em `app/build.gradle.kts`.
- Testes unitários e lint por flavor antes do build.
- APK publicado: `assembleGithubRelease` assinado, renomeado para `Meu-Ponto-<tag>.apk` (é o que o updater procura).
- AAB da flavor `play` anexado à release quando os Secrets existem.
- APK debug não é mais publicado.
- Warnings do run (não bloqueiam): `setup-java@v4` deprecated (migrar para v5) e `ubuntu-latest` migrando para Ubuntu 26.

---

## 4. Política de privacidade (`docs/privacy-policy.md`)

Reescrita com estrutura LGPD (controlador, base legal por dado, segurança e retenção, direitos do titular, permissões).

- [x] Item do OCR agora diz "extraído da imagem do comprovante".
- [x] "DVT" trocado por **encarregado** em todo o texto.
- [ ] **Preencher placeholders:** nome do controlador e e-mail de contato real.
- [ ] **Definir o encarregado** (pessoa ou canal).
- [ ] **Publicar em URL HTTPS** pública antes da submissão na Play.
- [ ] **Conferir** se o texto bate com o comportamento real (Auto Backup desativado, updater só na `github`).

---

## 5. Commits do dia

| Hash | Mensagem |
|---|---|
| `579fe06` | build: adiciona flavors play/github, R8 e schema Room |
| `f80cdd3` | chore(security): desativa backup automático e restringe permissões por flavor |
| `15d6d91` | feat(app): adiciona folha, insights, visualizador e restore sem duplicar |
| `e9b9af7` | test: adiciona testes do cálculo de horas e período |
| `51bfbb5` | ci: roda testes e lint por flavor e gera builds play |
| `7c5b513` | docs: atualiza política de privacidade, roadmap e status do projeto |
| `74ee901` | docs: corrige termos da política de privacidade |
| `69ef2e4` | chore: protege keystores e registra keystore de distribuição |
| `49d66b6` | ci: publica APK github assinado e valida secrets e versão na tag |
| tag `v0.5.0` | → `49d66b6`. Release publicada |

---

## 6. Problemas conhecidos

| # | Problema | Impacto | Ação |
|---|---|---|---|
| P1 | Testes unitários não rodam pelo Gradle neste PC: o executor falha porque o `JAVA_HOME` tem espaço (`C:\Program Files\...`). Compilação funciona. | Não dá para validar testes pelo Gradle localmente | **Contornado**: `:app:compilePlayDebugUnitTestKotlin` + JUnit direto com `java -cp <classes>;junit;hamcrest;kotlin-stdlib org.junit.runner.JUnitCore <classe>`. Definitivo: mover o JDK ou rodar no CI |
| P2 | Lint e testes unitários só rodaram no CI (run da tag). Lint local não foi executado | Falhas de lint só aparecem no CI | Rodar `lintPlayDebug` e `lintGithubDebug` localmente |
| P3 | Release `v0.4.0` continua pública, é APK debug e debuggable | Exposição de build de teste e confusão para o usuário | Marcar como pré-release ou remover (decisão sua) |
| P4 | ~~Não existe `v0.5.0`~~ | — | **Resolvido**: `v0.5.0` publicada (seção 3.0) |
| P5 | Scanner de câmera não funciona no WSA (sem Play Services) | Fluxo de câmera só pode ser testado no celular | Testar no celular |
| P6 | `allowBackup=false` | Reinstalar sem backup manual perde os dados | Confirmar se é a decisão desejada |
| P7 | Restore deduplica por `capturedAt` | Pode deixar passar comprovantes diferentes com o mesmo timestamp | Trocar por hash da imagem (Sprint 1) |
| P8 | Política: correções de termos feitas, faltam placeholders e encarregado (seção 4) | Não pode ser publicada ainda | Preencher antes da Play |
| P9 | Período de folha alterado no WSA sem ação registrada | Dúvida sobre dados de teste | Confirmar com o usuário |
| P10 | Versão instalada no celular é desconhecida | Não sabemos o estado de partida da migração | Informar a versão do app no celular |
| P11 | ~~Workflow de release quebrado~~ | — | **Resolvido** (seção 3.2). Rodou com sucesso na tag `v0.5.0` |
| P12 | Releases anteriores assinadas com certificados diferentes (`v0.3.3`, `v0.4.0`) | Update por cima entre releases antigas e a nova falha | **Mitigado**: keystore fixo. Falta migrar o celular (seção 7) |
| P13 | Teste de rede do updater no WSA inconclusivo (`wget` sem resposta; mensagem não capturada) | Não confirmado que o updater alcança o GitHub a partir do dispositivo de teste | Testar no celular |
| P14 | Updater precisa ser testado de ponta a ponta no aparelho | Fluxo "baixar e instalar" ainda não confirmado no celular | `v0.5.1` publicada; testar a atualização no aparelho |
| P15 | Setup Java v4 e `ubuntu-latest` com avisos de depreciação | Pode quebrar em breve | Atualizar `setup-java` para v5 e fixar `ubuntu-24.04` |
| P16 | Turno noturno sem pareamento entre dias e comprovantes restaurados sem tipo (entrada/saída) | Horas zeradas para quem trabalha à noite (ver 1.9) | **Corrigido e publicado na `v0.5.1`**. Classificar comprovantes no celular |
| P17 | Updater: instalação falha com assinatura diferente, e o erro aparece só depois do Play Protect | Usuário sem clareza sobre a migração | Mostrar mensagem de assinatura diferente antes de abrir o instalador (melhoria no updater) |
| P18 | Editor de horário: ao digitar, o cursor voltava e os dígitos entravam fora de ordem (ex.: "22:34" em vez de "22:43") | Dados errados ao editar | **Corrigido e publicado na `v0.5.1`**: campo guarda o cursor e vai para o fim após a máscara |
| P19 | Editor: "Só entrada" ou "Só saída" salvava também o outro horário (ex.: entrada e saída iguais para um comprovante só com horário) | Horas e pendências erradas | **Corrigido e publicado na `v0.5.1`**: só salva o tipo escolhido |
| P20 | "Revisar" no cabeçalho do dia não explica o motivo (ex.: saída sem entrada no dia anterior). O aviso no card some ao salvar, mas o dia continua pendente | Usuário acha que não salvou | Mostrar o motivo no cabeçalho (ex.: "saída sem entrada") ou por comprovante |

---

## 7. Próximos passos

### 7.1 Migração do celular (passo a passo, fazer uma vez)
Motivo: o celular tem APK com assinatura antiga. Sem a migração, nenhuma atualização pelo GitHub instala.

1. No app atual do celular: **Backup** (ícone de nuvem no topo) → criar backup **com senha** → salvar o ZIP em um lugar seguro (Drive ou PC).
2. Confirmar que o ZIP abre e que a senha está anotada.
3. Desinstalar o Meu Ponto do celular.
4. Instalar o **`Meu-Ponto-v0.5.0.apk`** da release Latest (https://github.com/eibragaa/meu-ponto-android/releases/tag/v0.5.0).
5. Abrir o app → Backup → **Restaurar backup** → escolher o ZIP → digitar a senha.
6. Conferir a quantidade de comprovantes e as horas.
7. Em Sobre → "Buscar atualizações agora": deve dizer que está na versão mais recente (`v0.5.0`). Isso confirma o acesso ao GitHub.

### 7.1b Classificar comprovantes do turno noturno (no celular)
1. Abrir o comprovante (toque no card) → **Editar**.
2. Escolher **"Só entrada"** para os horários de início (ex.: 22:43) e **"Só saída"** para os de fim (ex.: 07:00 ou 06:57).
3. Confirmar e salvar. O app passa a parear entrada e saída entre dias.
4. Conferir se o dia da entrada mostra as horas (ex.: 22/10 → 8h17).

### 7.2 Depois da migração
Checklist do release publicado: [`plano-release-v0.5.1.md`](plano-release-v0.5.1.md).

1. Testar no celular: scanner, galeria com OCR, edição, PDF individual e em lote, backup com e sem senha, restore, tema claro e escuro.
2. Diagnosticar o updater no celular: informar a versão instalada antes da migração (P10).
3. Testar o fluxo de atualização real (P14): a `v0.5.1` já está publicada; conferir se o celular oferece a atualização e instala.
4. Revisar a política de privacidade (seção 4): preencher placeholders e definir o encarregado.
5. Decidir sobre o `v0.4.0` (P3).
6. Resolver P1 e P2 (testes e lint locais).

### 7.3 Sprint 0 pendente (ver roadmap)
- Play App Signing e chave de upload (B3).
- Data Safety, IARC, público-alvo, declaração de anúncios e AD_ID (B5).
- Adaptive icon em `mipmap` e assets de loja (B8).
- `BackHandler` / predictive back nos sheets e telas (B7), layout para telas grandes e checagem de 16 KB.
- Onboarding com aviso legal (B9; a Sobre já tem o texto).
- Closed test: 12 testers por 14 dias (B6).

### 7.4 Sprint 1 e 2 pendentes
- ViewModel, Repository e UseCases; quebrar `MainActivity.kt`.
- `strings.xml` (pt-BR e en).
- Hash da imagem no restore (P7).
- Testes: OCR, backup (round-trip, senha errada, arquivo corrompido, zip slip), DAO e migração 1→2.
- Regras R8 testadas em release.
- CI com `lintRelease`, `setup-java` v5 (P15).

---

## 8. Decisões que dependem de você
- Aceitar `allowBackup=false` (P6)?
- Qual pessoa ou canal será o encarregado (LGPD)?
- Manter o `v0.4.0` público, marcar como pré-release ou remover (P3)?
- Quando liberar a `v0.6.0` com o novo fluxo de atualização ao abrir?
