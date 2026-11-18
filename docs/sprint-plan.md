# Plano de sprints — Meu Ponto

_Atualizado em 27/10/2026. Válido a partir da v0.6.6 (versionCode 16)._

Orquestração: o agente principal (fronteira) **planeja e revisa**; cada sprint é implementada por
**subagentes** (execução) e **verificada** pelo principal antes de fechar. Subagente implementa
**sem commit** — o principal revisa (diff + testes + lint) e decide o commit.

## Sprint 1 — Jornada agrupada · status: concluída

Objetivo: no modo "Mês", cada turno vira **um** card (entrada+saída pareadas, inclusive madrugada),
mostrando `23:50 → 06:57 · 7h07`, creditado no dia da entrada. Remove os textos "saída/entrada
pendente" enganosos e reduz a lista.

- [x] Domínio: expor o pareamento via `journeys(receipts)` + `data class Journey`, **sem** mudar
      `dayStats` (mesmo passe único; horas/pendências idênticas).
- [x] UI: `JourneyCard` (expansível para turno com 2 comprovantes); modos Dia/Semana/Lista intactos.
- [x] Testes: par completo; madrugada 23:50→06:57 (=427 min no dia da entrada); entrada solta; saída
      solta; horário sem tipo; ocorrência; jornada >16h.

**Aceite:** testes existentes de `dayStats`/`DayOccurrence` + novos verdes; `lint` sem erro.

**Follow-up implementado (issue #13, branch `feat/beta-issues-12-13`):** ao tocar dia no Insights, `onSelectDay` limpa a busca textual (preservando turnos noturnos), seleciona filtro `Todos` e modo `Mês`, e usa `focusDayInMonthView` + `collapsedDays` (dias abertos por padrão, focando apenas o dia escolhido).

## Sprint 2 — Mapa do mês (Insights) · status: concluída

Objetivo: grade do mês no `InsightsSheet` (estilo Samsung Health) — cada dia preenche conforme as
horas, com a jornada esperada (`expectedDailyMinutes`) como meta; pendente em Coral; ocorrência
neutra; toque no dia filtra a lista por aquela data.

- [x] UI: grade 7 colunas no topo do `InsightsSheet`, mantendo os cards de números atuais.
- [x] Navegação: `onSelectDay` → navega para o dia agrupado no modo "Mês" via `focusDayInMonthView` e fecha o sheet.
- [x] Regra pura: cor/legenda por célula como função pura testável (se introduzir lógica nova).

**Aceite:** usa só `PeriodStats.days` (já calculado); sem duplicar regra de negócio; sem I/O na UI.

## Sprint 3 — Lembretes proativos · status: implementada para beta (issue #12)

Objetivo: aprender o horário típico de **entrada** e os dias de trabalho, e notificar para lembrar
de "bater o ponto"; regra pura `hasOpenEntry` pronta para identificar entrada sem saída.

- [x] 3a Domínio: `PunchHabitLearner`, `ReminderSettings` (`nextReminderAt`/`isReminderDay`) e `OpenEntry` (`hasOpenEntry`), puros e cobertos por 23 testes unitários.
- [x] 3b Config: `ReminderSettingsStore` em `SharedPreferences` + `ReminderSheet` + botão em `InsightsSheet`.
- [x] 3c Notificação: `ReminderScheduler` (`AlarmManager` **inexato** + `ReminderNotifier`/`ReminderReceiver`),
      receiver `exported=false` no manifest, permissão `POST_NOTIFICATIONS` (Android 13+) e reagendamento
      automático na `MainActivity` quando comprovantes ou config mudam.

**Pendências opcionais futuras:** reagendamento pós-reboot (`RECEIVE_BOOT_COMPLETED`/`WorkManager`) e notificação dedicada para saída pendente usando `hasOpenEntry` (mantidos fora desta entrega para não introduzir permissões/dependências sem aprovação explícita).

**Aceite:** notificação 100% local (sem servidor). Alarme inexato (sem permissão especial).
`WorkManager`/alarme exato ficam como opção futura, só com OK do dono (dependência/permissão nova).

## Validação e merge (fluxo)

1. Cada sprint entrega **sem commit**, em arquivos disjuntos no working tree.
2. O principal revisa cada entrega **individualmente** (diff + testes daquela sprint).
3. Um **subagente de integração** consolida numa branch isolada (worktree) e:
   - commita docs/sprints em commits separados (Conventional Commits);
   - faz a fiação adiada no `MainActivity.kt` (ex.: `onSelectDay` → filtro, `expectedDailyMinutes`);
   - roda a suíte completa: testes (play + github) + lint + compile.
4. Só com a suíte **verde** o principal aprova o merge para `main`.

**Áreas correlatas** (tocadas por mais de uma sprint, verificação obrigatória): `MainActivity.kt`
e o pareamento em `domain/WorkStats.kt`.

## Restrições (todas as sprints)

- Regra de negócio em `domain/`, pura e testável; `MainActivity.kt`/`ui/` **sem I/O**.
- **Não** alterar `versionName`/`versionCode`; **não** criar tag/release; **não** adicionar
  dependência (salvo OK explícito do dono).
- Conventional Commits; feature em um commit (ou `feat(domain)`/`feat(ui)` separados).
- Sem `String` mágica para estado/modo; número mágico vira `const val` com KDoc.
