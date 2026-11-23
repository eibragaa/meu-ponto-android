# Relatório de QA — Meu Ponto Android

Auditoria completa do aplicativo tela por tela, fluxo por fluxo: o que funciona,
o que precisa melhorar e a prioridade de cada ponto. Base do ciclo de melhorias
de UI/UX iniciado em novembro de 2026.

Legenda de prioridade: **P0** crítico (quebra fluxo), **P1** importante,
**P2** desejável.

## 1. Tela inicial e card do dia (`TodayCard.kt`)

**Funciona**
- Status em tempo real do dia: sem registro, em turno (indicador pulsante) e finalizado.
- Cálculo das horas do dia e projeção da hora de saída com base na meta diária.
- Ação rápida (`QuickPunchFabBar`) sugerindo a próxima batida mais provável.

**Melhorias**
- ~~P1 — Intervalo em aberto: a projeção congelava/oscillava após a saída para o almoço.~~
  **Resolvido**: novo estado `ON_BREAK`, com horário de saída, volta sugerida (1h) e
  previsão de encerramento assumindo o intervalo legal até o retorno.
- P2 — Primeiro uso sem meta: o saldo aparece zerado sem explicar o motivo; convém um
  convite para configurar a carga horária.

## 2. Captura e OCR (`ReceiptImporter.kt`, `AddReceiptSheet`)

**Funciona**
- OCR 100% local (Google ML Kit), sem envio de dados.
- Reconhecimento de data `dd/MM/yyyy` e hora `HH:mm`, com data de hoje como fallback.
- Importação da galeria e Document Scanner.

**Melhorias**
- ~~P0 — Foto obrigatória: não havia como registrar um ponto sem foto.~~
  **Resolvido**: terceira opção "Digitar manualmente" (`ManualPunchSheet`), com data,
  tipo de batida, horários, ocorrência e observação. O registro é salvo com
  `imagePath` vazio e desenhado como comprovante digital no PDF.
- P2 — Comprovantes com 4 batidas: o modelo `Receipt` cobre entrada/saída; um espelho
  com 4 batidas exige edição manual. O espelho de ponto em PDF já consolida até 4
  batidas por dia a partir dos registros pareados.

## 3. Agrupamento e jornadas (`Journeys.kt`, `PunchPairing.kt`)

**Funciona**
- Pareamento cronológico em janela de 16h, com suporte a turno noturno.
- Exclusão por gesto com "Desfazer" no Snackbar.
- Detecção de pendências: saída faltante, entrada faltante, batida sem tipo.

**Melhorias**
- P2 — Busca restrita a NSR e data numérica; aceitar nome do mês e tipo de ocorrência.

## 4. Relatórios e mapa de calor (`InsightsSheet.kt`)

**Funciona**
- Calendário mensal em heatmap por horas cumpridas.
- Banco de horas com tolerância da CLT (5 min por batida, 10 min por dia).
- Respeito a `reduceMotion`.

**Melhorias**
- P1 — ~~Faltava exportar a folha consolidada.~~ **Resolvido**: botão
  "Espelho de Ponto (PDF)" no próprio Insights, e exportação por seleção na lista.
- P2 — Ciclos de folha quebrados (ex.: 21 a 20): destacar graficamente a fronteira
  do ciclo para não misturar com o mês civil.

## 5. Configuração de folha (`PayrollSettingsSheet.kt`)

**Funciona**
- Período de apuração, dias livres/DSR e validação de datas.

**Melhorias**
- P1 — Carga diferenciada por dia da semana (ex.: 8h48 de seg a sex para compensar
  sábado, ou 8h de seg a qui e 7h na sexta).

## 6. Lembretes (`ReminderSheet.kt`, `ReminderScheduler.kt`)

**Funciona**
- `PunchHabitLearner` aprende o horário habitual de entrada.
- Alarme local com antecedência de 5, 10 ou 15 min.

**Melhorias**
- P1 — Android 14/15 (One UI): orientar a marcar o app como "Sem restrições" de
  bateria para o sistema não silenciar os alarmes.

## 7. Exportação e backup (`PdfExporter.kt`, `BackupService.kt`)

**Funciona**
- Backup AES-GCM com PBKDF2 (120k iterações) e proteção contra Zip-Slip.
- Uso exclusivo do Storage Access Framework, sem permissões invasivas.

**Melhorias**
- ~~P0 — PDF era apenas a imagem do comprovante, sem formato de RH.~~
  **Resolvido**: dois modos de exportação:
  - **Canhotos**: comprovantes individuais (uma página, ou grade de 2 por página),
    incluindo cartões vetoriais para registros manuais sem foto.
  - **Espelho de Ponto**: tabela A4 consolidada (Data, Semana, Entrada 1, Saída 1,
    Entrada 2, Saída 2, Total, Observações) com cabeçalho de apuração, saldo do
    período e campo de assinatura.
- P1 — Backup agora preserva registros manuais sem imagem (manifest com
  `imageEntry = ""`), sem descartá-los.

## 8. Auto-updater (`UpdateCoordinator.kt`)

**Funciona**
- Consulta ao GitHub Releases no cold start e botão manual no rodapé.
- Nomenclatura semântica e verificação de integridade SHA-256 antes de instalar.
- Verificação automática limitada a 1x ao dia (ou ao abrir o app).

## Próximos passos priorizados

1. P1 — Carga horária diferenciada por dia da semana.
2. P1 — Aviso de otimização de bateria no Android 14/15.
3. P2 — Busca por nome do mês e tipo de ocorrência.
4. P2 — Destaque da fronteira do ciclo de folha no calendário.
