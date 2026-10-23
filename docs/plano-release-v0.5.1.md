# Plano do próximo release — v0.5.1

_Análise feita em 23/10/2026. Base: `main` no commit `af365ba` (sincronizado com `origin/main`)._

Este documento registra o que falta para publicar o próximo release e serve de checklist para
retomar o trabalho depois. Ele **não autoriza** alterar versão, criar tag ou publicar release: isso
continua sendo decisão do dono do projeto.

## 1. Situação no momento da análise

| Item | Estado |
|---|---|
| Última release publicada | `v0.5.0` (`versionCode 8`, `versionName 0.5.0`) |
| Commit da tag `v0.5.0` | `49d66b6` |
| HEAD analisado | `af365ba` — `refactor+docs: organiza o código por camadas e cria o padrão de engenharia do repo (#1)` |
| Commits depois da tag | 12 |
| Testes | 35 testes em 4 arquivos |
| CI no HEAD | Run `39891919429` concluído com **sucesso** em 23/10/2026 |
| O que o CI validou | testes das duas flavors, lint, APK `githubRelease` assinado e AAB `playRelease` assinado |
| Build local | Não executado neste ambiente (sem Android SDK); a evidência é o run do CI no commit exato |

### Mudanças já presentes no `main` desde a `v0.5.0`

- `d3f9527` — pareamento de entrada e saída de turno noturno entre comprovantes e entre dias (`P16`).
- `f8f6a46` — editor de horário mantém o cursor e salva só o tipo escolhido (`P18`/`P19`).
- `ee11cba` — regra de duração unificada com teto de 16h.
- `0ed074a` — extração de tema, formatação, máscaras, importação, backup e PDF do `MainActivity`.
- `4acd34d` — testes de máscaras, formatação e comparação de versão.
- Documentação: padrão de engenharia, `AGENTS.md`, PR template, README e status atualizados.

## 2. Bloqueadores para publicar a `v0.5.1`

### 2.1 Subir a versão

Hoje `app/build.gradle.kts` ainda está em:

```kotlin
versionCode = 8
versionName = "0.5.0"
```

O workflow compara a tag com o `versionName` (`.github/workflows/android-release.yml:40-47`).
Criar a tag `v0.5.1` sem o bump faz o workflow falhar antes do build.

Valores do próximo release:

```kotlin
versionCode = 9
versionName = "0.5.1"
```

### 2.2 Validar no aparelho

Não há registro no repositório de que a migração e a validação no celular foram concluídas; o
`docs/status-e-proximos-passos.md` ainda trata isso como pendente.

Antes da tag, confirmar:

- [ ] O aparelho está com a `v0.5.0` assinada pelo keystore novo (`SHA-256 4E:F4:64:D5:...`).
- [ ] Os comprovantes restaurados continuam íntegros (quantidade e horas).
- [ ] O turno noturno (`22:45 → 06:55` ou o caso real) mostra a duração correta no dia da entrada.
- [ ] Os comprovantes noturnos foram classificados como **“Só entrada”** e **“Só saída”**.
- [ ] O editor não inverte dígitos ao digitar (`22:43` permanece `22:43`).
- [ ] “Só entrada”/“Só saída” salva apenas o horário escolhido.
- [ ] O updater está sendo testado no flavor `github` (o flavor `play` não tem updater).

Se o aparelho ainda estiver com a assinatura antiga, a migração é obrigatória antes do release:
backup com senha → desinstalar → instalar a `v0.5.0` assinada → restaurar. Sem isso, o updater baixa
o APK, mas a instalação falha por assinatura diferente (`P12`/`P17`).

### 2.3 Criar a tag e publicar

Depois do bump e da validação:

```bash
git add app/build.gradle.kts docs/
git commit -m "chore(release): prepara v0.5.1"
git push origin main

git tag -a v0.5.1 -m "Meu Ponto v0.5.1"
git push origin v0.5.1
```

A tag dispara o workflow e publica:

- `Meu-Ponto-v0.5.1.apk` — flavor `github`, assinado, com updater;
- `app-play-release.aab` — flavor `play`, assinado.

Conferir depois:

```bash
gh run list -L 3
gh release view v0.5.1
```

O release precisa ficar **Latest**, não draft e não pré-release, para o updater encontrá-lo em
`/releases/latest`.

## 3. Recomendado antes de publicar

### 3.1 Release notes

O workflow usa `generate_release_notes: true` (`.github/workflows/android-release.yml:78-85`), então
o GitHub gera notas automáticas com todos os commits — inclusive os internos. Para o updater mostrar
um texto melhor, publicar ou editar o release com notas curadas:

```markdown
## Correções

- Turno noturno agora é pareado entre comprovantes e entre dias; a duração é creditada no dia da
  entrada (P16).
- Editor de horário mantém o cursor e salva apenas o tipo escolhido ("Só entrada" ou "Só saída"),
  evitando horários invertidos e batidas duplicadas (P18/P19).

## Interno

- Código reorganizado em `domain/`, `data/`, `ui/` e `updater/`.
- 35 testes unitários e padrão de engenharia documentado.
```

### 3.2 Atualizar a documentação depois do release

- [ ] Atualizar o cabeçalho de `docs/status-e-proximos-passos.md` para `v0.5.1` (`versionCode 9`).
- [ ] Adicionar a `v0.5.1` à tabela de releases publicadas.
- [ ] Marcar `P16`, `P18` e `P19` como corrigidos e publicados.
- [ ] Marcar `P14` (teste ponta a ponta do updater) conforme o resultado no aparelho.
- [ ] Registrar que testes/lint/build foram validados pelo CI no `main`.
- [ ] Manter `E1`/`P21` e `E3`/`P20` como pendentes, se ainda não entrarem neste release.

## 4. Pendências que não bloqueiam a release GitHub

| Item | Situação | Ação |
|---|---|---|
| `E1`/`P21` — teclado cobre o editor | `EditReceiptSheet` usa `navigationBarsPadding()`, mas não `imePadding()` | Implementar antes de anunciar E1 |
| `E3`/`P20` — motivo do “Revisar” | Cabeçalho mostra “Revisar” sem explicar o motivo | Implementar antes de anunciar E3 |
| `ENG-7` — workflow manual com `/` no branch | `android-release.yml:64-66` usa `GITHUB_REF_NAME` direto | Corrigir com `${GITHUB_REF_NAME//\//-}` |
| `P15` — actions desatualizadas | `setup-java@v4` e Node 20 emitem avisos | Migrar para `setup-java@v5` e versões compatíveis |
| CI em push/PR | Workflow só roda em tag ou manualmente | Criar workflow de validação em PR/push |
| `P3` — `v0.4.0` debug público | Ainda disponível | Remover ou marcar como pré-release |
| Política de privacidade | Placeholders de controlador e encarregado | Preencher antes da Play |
| Play App Signing / Data Safety | Pendentes | Tratar no checklist da Play |
| Updater | Falha de rede vira “já está na versão mais recente”; sem aviso prévio de assinatura | Melhorar em `ENG-5`/`P17` |

### Importante sobre o escopo da `v0.5.1`

O plano `docs/plan-correcoes-editor.md` menciona release notes com E1–E3, mas **E1 e E3 ainda não
estão implementados** no `main`. A `v0.5.1` pode ser publicada com o que já está pronto (turno
noturno + editor P18/P19 + refatoração/testes), mas as notas não devem afirmar que E1/E3 entraram.

## 5. Critérios de aceite do release

- [ ] `versionName = "0.5.1"` e `versionCode = 9` no commit da tag.
- [ ] Testes e lint passam no CI da tag.
- [ ] APK `Meu-Ponto-v0.5.1.apk` e AAB são anexados ao release.
- [ ] Release marcado como **Latest**.
- [ ] APK assinado com o mesmo certificado da `v0.5.0`.
- [ ] Instalação por cima da `v0.5.0` preserva os dados.
- [ ] Updater no flavor `github` oferece a `v0.5.1` e instala.
- [ ] Turno noturno e editor validados no aparelho.

## 6. Referências

- Workflow: `.github/workflows/android-release.yml`
- Versão: `app/build.gradle.kts`
- Status e problemas conhecidos: `docs/status-e-proximos-passos.md`
- Plano do editor: `docs/plan-correcoes-editor.md`
- Padrão de engenharia: `docs/engineering-standards.md`
- Updater: `app/src/main/java/br/com/jean/meuponto/updater/AppUpdater.kt`
