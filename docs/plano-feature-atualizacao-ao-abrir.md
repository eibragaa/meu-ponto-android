# Feature — aviso de atualização ao abrir o app

_Status: planejamento. Nada implementado ainda._

Objetivo: ao abrir o Meu Ponto (flavor `github`), verificar se existe uma versão nova no GitHub e
avisar o usuário de forma clara, mostrando as correções e novidades daquela versão.

## 1. O que já existe hoje

O app **já tem parte do fluxo**:

- `MainActivity.kt:272-282` faz uma verificação automática no primeiro `LaunchedEffect` do processo.
- `AppUpdater.checkForUpdates()` consulta `/releases/latest` e devolve `AppUpdateInfo`.
- `UpdateDialog` já mostra:
  - versão atual e nova;
  - release notes vindas do corpo do release;
  - botão para baixar, progresso e instalação.
- O botão “Buscar atualizações agora” já existe na tela Sobre.

Ou seja: a base está pronta. A feature é evoluir isso para um fluxo confiável, visível e com
“novidades” bem apresentadas.

## 2. Lacunas do fluxo atual

| Lacuna | Efeito |
|---|---|
| Verifica só uma vez por processo | Se o app ficar aberto/em memória por dias, não verifica de novo ao voltar |
| Não verifica em `ON_START` | “Abrir o app” depois de voltar do background não dispara consulta |
| Não distingue “sem atualização” de erro | `checkForUpdates` devolve `null` em HTTP 403/500; a UI diz “já está na versão mais recente” mesmo sem rede |
| Modal automático sem alternativa discreta | Se o usuário toca “Mais tarde”, não fica banner/indicação na tela até reiniciar o processo |
| Não lembra o que foi dispensado | Pode reapresentar o mesmo aviso em outro processo |
| Release notes automáticas | O workflow usa `generate_release_notes`; o texto pode vir técnico e longo |
| Sem “O que há de novo” após atualizar | O usuário instala e não vê novamente o que mudou |
| Sem política de frequência | Pode gerar consultas desnecessárias ao GitHub |
| Sem teste do fluxo de decisão | Só existe teste de comparação de versão |

## 3. UX recomendada

### 3.1 Ao abrir o app

1. A tela inicial aparece normalmente; a verificação roda em background.
2. Se houver versão nova:
   - mostrar um **banner compacto** no topo da lista;
   - na primeira vez que aquela versão for vista, abrir também o diálogo de novidades.
3. Se o usuário tocar em “Depois”:
   - fecha o diálogo;
   - o banner continua visível;
   - o modal não volta para a mesma versão.
4. Se o usuário tocar em “Dispensar” no banner:
   - o banner some para aquela versão;
   - a verificação manual na tela Sobre continua disponível.
5. Se não houver atualização: nada aparece no fluxo automático.
6. Se a verificação falhar: nada aparece no automático; na verificação manual, mostrar erro
   específico com opção de tentar de novo.

### 3.2 Banner

Sugestão visual, dentro do `LazyColumn` da tela inicial:

```text
┌─────────────────────────────────────────────┐
│  ⬆  Atualização disponível · v0.5.1         │
│     Correções do turno noturno e do editor  │
│                              [Ver novidades]│
└─────────────────────────────────────────────┘
```

- Fundo `primaryContainer`, borda suave, ícone `SystemUpdate`.
- Ação principal: abrir o diálogo já existente.
- Ação secundária: dispensar aquela versão.
- Não bloquear a lista, não usar `Snackbar` para o aviso principal.

### 3.3 Diálogo de novidades

Reutilizar `UpdateDialog`, com pequenos ajustes:

- Título: “Nova versão disponível”.
- Linha “Atual: v0.5.0 → Nova: v0.5.1”.
- Seção “O que mudou nesta versão”.
- Botões:
  - “Atualizar agora” (fluxo atual);
  - “Depois” (fecha e mantém banner);
  - link “Ver no GitHub”.
- Release notes em texto simples, com rolagem e limite visual; não introduzir biblioteca de Markdown
  agora (evita dependência nova).

### 3.4 Depois de atualizar

Na primeira abertura da versão nova:

- mostrar uma folha “O que há de novo na vX.Y.Z” com as notas que já foram baixadas;
- botão “Começar a usar”;
- mostrar **uma vez por versão** e limpar o estado depois.

Se o usuário instalou o APK por fora do app, não há notas pendentes: nesse caso, não mostrar nada
(ou buscar `/releases/tags/vX` em uma fase futura).

## 4. Quando verificar

Recomendação para equilibrar “abriu o app, verificou” com o limite de 60 requisições/hora da API do
GitHub sem autenticação:

| Evento | Verifica? | Observação |
|---|---|---|
| Abertura fria (processo novo) | Sim | é o caso principal pedido |
| Volta do background após 30 min | Sim | cobre “abri o app de novo” |
| Volta do background antes de 30 min | Não | usa o último resultado em memória |
| Toque em “Buscar atualizações agora” | Sim, forçado | ignora o intervalo |
| Verificação já em andamento | Não | evita requisição duplicada |

Constantes sugeridas em `updater/`:

- `AUTO_CHECK_INTERVAL_MS = 30 * 60 * 1000L` (30 min);
- `UPDATE_REQUEST_TIMEOUT_MS = 10_000`;
- `UPDATE_READ_TIMEOUT_MS = 15_000`.

O intervalo pode ser ajustado depois sem mudar a UI.

## 5. Arquitetura proposta

Manter a regra de camadas do repositório: `updater/` faz rede, persistência e estado; `MainActivity`
só coleta estado e compõe; nada de I/O na UI.

### 5.1 Modelos

```kotlin
sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult
    data class Failed(val reason: UpdateFailure) : UpdateCheckResult
}

enum class UpdateFailure {
    OFFLINE,
    RATE_LIMITED,
    SERVER_ERROR,
    INVALID_RESPONSE,
    NO_APK
}
```

`AppUpdateInfo` continua como está, acrescentando apenas o que for necessário (ex.: `releaseUrl`).

### 5.2 Parser separado da rede

Extrair a leitura do JSON para uma função pura:

```kotlin
internal fun parseLatestRelease(
    json: String,
    currentVersion: String
): UpdateCheckResult
```

Assim o parser vira testável sem `HttpURLConnection` e sem Android.

### 5.3 Preferências

`UpdatePreferences` usando `SharedPreferences` (sem dependência nova):

| Chave | Uso |
|---|---|
| `last_check_at` | throttle entre verificações |
| `last_prompted_version` | evita reabrir o modal para a mesma versão |
| `dismissed_version` | esconde o banner daquela versão |
| `pending_notes_version` | versão cujas notas serão mostradas após instalar |
| `pending_notes` | notas salvas antes de abrir o instalador |

### 5.4 Política pura

```kotlin
object UpdatePolicy {
    fun shouldCheck(
        trigger: CheckTrigger,
        now: Long,
        lastCheckAt: Long
    ): Boolean

    fun shouldShowPrompt(
        version: String,
        lastPromptedVersion: String?
    ): Boolean
}
```

`CheckTrigger`: `ColdStart`, `Foreground`, `Manual`.

### 5.5 Coordenador

Criar `updater/UpdateCoordinator.kt` (ou `UpdateManager`) com:

- `StateFlow<UpdateUiState>` para a tela observar;
- `onColdStart()`, `onForeground()`, `checkNow()`;
- `dismissPrompt(version)`, `dismissBanner(version)`;
- `savePendingNotes(info)` antes de instalar;
- `consumeWhatsNew(installedVersion)` na abertura.

O `MainActivity` apenas:

- cria/lembra o coordenador;
- observa o estado;
- registra um `LifecycleEventObserver` para `ON_START`;
- renderiza banner/diálogo/what's new.

Não é necessário introduzir ViewModel agora (ENG-1 continua pendente); o coordenador fica em
`updater/` e não faz composição.

### 5.6 Fluxo resumido

```text
abertura/ON_START
        │
        ▼
UpdatePolicy.shouldCheck?
        │ sim
        ▼
checkForUpdates() ──► UpToDate ──► nada
        │                │
        │                └─ Failed ──► silencioso (automático)
        │                              erro específico (manual)
        ▼
    Available
        │
        ├─ banner sempre
        └─ modal só se versão ainda não foi mostrada
                │
                ▼
        “Atualizar agora” ──► download ──► salva notas pendentes ──► instalador
                │
                ▼
        próxima abertura da versão nova ──► “O que há de novo”
```

## 6. Release notes

As notas exibidas vêm do corpo do release no GitHub. Para o texto não ficar técnico:

- escrever o corpo do release em português, com seções curtas:
  - “Correções”;
  - “Novidades”;
  - “Interno” (opcional; pode ser omitido do app).
- evitar listar commits individuais;
- manter um tamanho máximo confortável (ex.: 4.000 caracteres);
- o workflow atual usa `generate_release_notes: true`; para notas curadas, definir `body`/`body_path`
  no `softprops/action-gh-release` ou editar o release depois com `gh release edit`.

Evolução opcional: manter `docs/release-notes/vX.Y.Z.md` e fazer o workflow usar esse arquivo. Isso
dá histórico versionado e evita texto automático.

## 7. Erros e casos de borda

| Caso | Comportamento |
|---|---|
| Sem internet | Automático: silencioso. Manual: “Sem conexão. Verifique a internet e tente de novo.” |
| HTTP 403/rate limit | Manual: “O GitHub limitou as consultas. Tente novamente em alguns minutos.” |
| HTTP 5xx | Manual: “O GitHub não respondeu agora. Tente de novo.” |
| JSON inválido | Manual: “Não foi possível ler a resposta do GitHub.” |
| Release sem APK | Manual: “A release existe, mas não tem APK para instalar.” |
| Versão igual/menor | `UpToDate`; não mostrar nada no automático |
| Release draft/prerelease | `/releases/latest` não retorna; comportamento atual é aceitável |
| Usuário cancelou instalação | Banner continua; o APK em cache é limpo na próxima abertura |
| Download interrompido | `.tmp` é descartado; tentar de novo baixa do zero |
| Assinatura diferente | Avisar antes de abrir o instalador (P17) — fase de hardening |
| `play` flavor | Não consulta GitHub; avaliação futura da In-App Updates API |

## 8. Segurança e privacidade

- Nenhuma permissão nova: `INTERNET` já existe.
- Só `https://api.github.com` e o CDN do GitHub; o `network_security_config` já bloqueia cleartext.
- A verificação envia apenas o `User-Agent` do app; não envia dados de comprovantes.
- Nada é baixado ou instalado sem ação explícita do usuário.
- O flavor `play` continua sem updater e sem `REQUEST_INSTALL_PACKAGES`.
- Hardening recomendado:
  - comparar a assinatura do APK baixado com a do app instalado antes de abrir o instalador;
  - validar o `digest` SHA-256 do asset, se disponível na API;
  - manter timeout e limite de redirecionamentos.

## 9. Testes

### 9.1 Unitários (JVM)

- `UpdatePolicyTest`
  - abertura fria verifica;
  - foreground dentro do intervalo não verifica;
  - foreground depois do intervalo verifica;
  - manual ignora intervalo;
  - modal não repete para a mesma versão;
  - banner dispensado não volta para a mesma versão.
- `UpdateParserTest`
  - release com APK `Meu-Ponto-*.apk`;
  - release com múltiplos APKs (preferir o nome do produto);
  - release sem APK;
  - versão igual, menor e maior;
  - tag com `v`;
  - corpo vazio;
  - JSON inválido.
- `UpdatePreferencesTest` com armazenamento fake (sem Android).
- Manter `AppUpdaterTest` de comparação de versão.

### 9.2 Manual no aparelho

- [ ] Abrir o app sem rede: não travar, não mostrar erro automático.
- [ ] Abrir com rede e versão nova: banner + modal uma vez.
- [ ] Tocar “Depois”: banner permanece, modal não reabre.
- [ ] Dispensar banner: some para aquela versão.
- [ ] Verificação manual com erro: mensagem específica.
- [ ] Baixar, instalar e reabrir: “O que há de novo” uma vez.
- [ ] Reabrir de novo: não repetir o “O que há de novo”.
- [ ] Voltar do background após 30 min: verifica de novo.
- [ ] Flavor `play`: nenhuma consulta ao GitHub.

## 10. Entregas sugeridas

| Fase | Entrega | Tamanho |
|---|---|---|
| 0 | Confirmar UX e política de release notes | P |
| 1 | Separar parser, `UpdateCheckResult` e erros tipados + testes | M |
| 2 | `UpdatePreferences` + `UpdatePolicy` + testes | P |
| 3 | `UpdateCoordinator`, lifecycle, banner e modal uma vez por versão | M |
| 4 | “O que há de novo” após atualizar | P |
| 5 | Hardening: rate limit/ETag, assinatura/checksum, mensagens de erro | M |
| 6 | QA no aparelho, docs e release | P |

Sugestão de versão: **0.6.0** (feature de UX), depois da `0.5.1` de correções. Se a prioridade for
só o aviso mínimo, dá para fazer as fases 1–3 e lançar como `0.5.2`, deixando o “O que há de novo”
para depois.

## 11. Critérios de aceite

- [ ] Abrir o flavor `github` com versão nova disponível mostra o aviso sem bloquear a tela.
- [ ] O aviso mostra versão atual, versão nova e notas legíveis.
- [ ] O modal aparece no máximo uma vez por versão; o banner continua acessível.
- [ ] Sem atualização, o fluxo automático não incomoda.
- [ ] Erro de rede não é confundido com “versão mais recente”.
- [ ] A verificação manual sempre mostra o resultado real.
- [ ] Após instalar, “O que há de novo” aparece uma vez.
- [ ] `play` não faz consulta ao GitHub.
- [ ] Testes de política, parser e preferências passam.
- [ ] Lint e build das duas flavors passam no CI.

## 12. Decisões para confirmar

1. **Aviso:** banner + modal uma vez por versão (recomendado) ou modal em toda abertura?
2. **Frequência:** abertura fria sempre + volta do background após 30 min (recomendado), ou toda
   volta ao foreground?
3. **Pós-atualização:** mostrar “O que há de novo” uma vez (recomendado) ou não?
4. **Dispensar:** “Depois” mantém o banner e “Dispensar” esconde só aquela versão (recomendado)?
5. **Notas:** corpo do release curado manualmente (recomendado) ou notas automáticas do GitHub?
6. **Play:** manter o flavor `play` sem updater por enquanto (recomendado) ou implementar a
   In-App Updates API junto?
7. **Versão alvo:** `0.6.0` para a feature completa ou `0.5.2` para o aviso mínimo?

## 13. Fora de escopo por enquanto

- Atualização obrigatória/bloqueante.
- Canal beta/prerelease.
- Download automático em background sem ação do usuário.
- Instalação silenciosa.
- In-App Updates da Play Store.
- Renderização completa de Markdown nas notas.
- Verificação de atualização com o app totalmente fechado (work manager), sem abrir o app.

## 14. Referências de código

- `app/src/main/java/br/com/jean/meuponto/MainActivity.kt:272-282` — verificação automática atual.
- `app/src/main/java/br/com/jean/meuponto/MainActivity.kt:315-328` — verificação manual.
- `app/src/main/java/br/com/jean/meuponto/updater/AppUpdater.kt:78-141` — consulta ao GitHub.
- `app/src/main/java/br/com/jean/meuponto/updater/UpdateUi.kt:70-165` — diálogo e notas.
- `app/src/main/java/br/com/jean/meuponto/updater/UpdateUi.kt:346-507` — tela Sobre.
- `.github/workflows/android-release.yml:78-85` — publicação do release.
