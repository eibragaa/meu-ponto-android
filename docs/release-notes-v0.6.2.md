# Meu Ponto v0.6.2

## Novidades

- O selo "Revisar" agora explica o motivo no cabeçalho do dia (ex.: "saída sem entrada").

## Correções e melhorias

- Dias pendentes mostram o motivo: data não reconhecida, jornada acima de 16h, horário sem
  entrada/saída, entrada sem saída e saída sem entrada.
- "Comprovante marcado para revisão" só aparece quando não há um motivo mais específico, evitando
  repetir a mesma dúvida no cabeçalho.

## Interno

- `WorkDayStats.issues` (`DayIssue`) com `pending` derivado de uma única fonte; 16 testes novos
  (108 no total).
