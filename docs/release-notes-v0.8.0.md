# Meu Ponto v0.8.0

## Novidades

- Cadastro em 3 toques: escolha Entrada ou Saída antes de capturar a foto; o OCR já salva com o tipo.
- Classificação de comprovante não tipado em 1 toque: chips Entrada/Saída no card, sem abrir a folha de edição.
- Status salvo/falta na lista: badge no cabeçalho do dia ("Falta saída"/"Falta entrada") e chips no card.
- Card simplificado: menos ruído, compartilhamento apenas no visualizador.

## Interno

- Novo `domain/PunchProgress.kt` com `DayPunchStatus` e `classifyUntyped`, reusando `PunchPairing`. 12 testes novos.
- `recognizeAndSave` aceita tipo preferido escolhido pelo usuário.
