# Meu Ponto v0.5.1

## Correções

- Turno noturno agora é pareado entre comprovantes e entre dias; a duração é creditada no dia da
  entrada (P16).
- Editor de horário mantém o cursor e salva apenas o tipo escolhido ("Só entrada" ou "Só saída"),
  evitando horários invertidos e batidas duplicadas (P18/P19).
- Duração de jornada unificada, com o mesmo teto de 16 horas para comprovante único e para
  comprovantes separados.

## Interno

- Código reorganizado em `domain/`, `data/`, `ui/` e `updater/`.
- 35 testes unitários cobrindo cálculo de horas, máscaras, formatação e comparação de versão.
- Padrão de engenharia e documentação do projeto atualizados.

## Observações

- Esta versão ainda não inclui as melhorias de teclado no editor (E1) nem o motivo detalhado do
  "Revisar" (E3); elas ficam para a próxima versão.
