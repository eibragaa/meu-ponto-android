# Meu Ponto v0.6.6

## Correções e melhorias

- Comprovante com só uma batida (só entrada ou só saída) não fica mais preso em "Revisar" depois de revisado.
- Importar comprovantes em sequência não sobrescreve mais a imagem de um pelo outro.
- Restaurar backup com motivo inválido não trava mais a restauração; o comprovante vira normal.

## Interno

- Batida única classificada deixa de contar como pendência; só turno acima de 16 h ou sem tipo pede revisão.
- Lint passa a rodar também nos flavors de release; `setup-java` atualizado para v5.
- Nome do APK de release sanitizado para branch com `/`.
