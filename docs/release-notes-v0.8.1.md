# Meu Ponto v0.8.1

## Correções

- Badge "Falta saída"/"Falta entrada" não aparece mais em dias que são parte de um turno completo de madrugada. O status agora usa o pareamento global (entrada e saída podem estar em dias diferentes).

## Interno

- Novo `dayPunchStatusFromJourneys`, que calcula o status a partir das jornadas já pareadas, em vez de isolar os comprovantes do dia.
