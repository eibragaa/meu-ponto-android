# Meu Ponto v0.7.0

## Novidades

- Modo "Mês" agrupa cada turno em um único card: entrada e saída aparecem juntas, inclusive turno
  que cruza a meia-noite (ex.: 23:50 → 06:57), com a duração creditada no dia da entrada.
- Mapa do mês nos Insights: grade do mês mostra o preenchimento de cada dia contra a jornada
  esperada; tocar num dia filtra a lista por aquela data.

## Interno

- Pareamento de batidas extraído para `PunchPairing` (uma única regra); `dayStats` e `journeys`
  usam o mesmo passe. 154 testes sem regressão.
