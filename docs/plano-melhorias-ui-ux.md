# Plano de Melhorias de UI/UX — ciclo v0.9.0

Registro do que foi implementado neste ciclo e do que fica para os próximos.

## Entregue neste ciclo

### 1. Lançamento manual de ponto (sem foto)
- Nova fonte "Digitar manualmente" na folha de adicionar comprovante.
- `ManualPunchSheet`: data, tipo de registro (entrada e saída, só entrada, só saída,
  sem horário), horários com atalho "Agora", ocorrência (atestado, folga, falta,
  esquecimento) e observação.
- Registros manuais ficam com `imagePath` vazio, aparecem na lista com ícone de
  relógio e são preservados no backup e na exportação.

### 2. Dois formatos de PDF
- **Canhotos**: exporta comprovantes individuais. Seleção múltipla gera uma grade de
  dois por página; registros manuais viram cartões vetoriais.
- **Espelho de Ponto**: tabela A4 consolidada, respeitando o período de apuração
  configurado, com:
  - cabeçalho (período, dias com ponto, total apurado, saldo);
  - colunas Data, Semana, Entrada 1, Saída 1, Entrada 2, Saída 2, Total e Observações;
  - ocorrências (atestado, folga, falta, esquecimento) por dia;
  - rodapé com campo de assinatura do trabalhador.
- O espelho pode ser gerado pela seleção na lista ou pelo botão no Insights.

### 3. Intervalo em aberto (estado "Em intervalo")
- Novo estado `ON_BREAK` no card do dia: exibe a saída para o almoço, a volta
  sugerida (1h) e a previsão de saída com as horas restantes.
- Botão direto "Bater Volta do Almoço"; o dia só é dado como finalizado quando a meta
  é cumprida ou quando passam mais de 4h da última saída.

### 4. Backup com registros manuais
- O manifest do backup passa a preservar comprovantes sem imagem (`imageEntry = ""`),
  que antes eram descartados.

## Próximos (backlog)

| Prioridade | Item |
| --- | --- |
| P1 | Carga horária diferenciada por dia da semana |
| P1 | Card de otimização de bateria (Android 14/15 / One UI) |
| P2 | Busca por nome do mês e tipo de ocorrência |
| P2 | Destaque da fronteira do ciclo de folha no calendário |
| P2 | Convite para configurar carga horária no primeiro uso |
