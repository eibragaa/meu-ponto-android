# Política de Privacidade — Meu Ponto

_Esta política será publicada em URL HTTPS antes do envio para a Play Store; enquanto isso, o texto abaixo fica referenciado no app (tela Sobre)._

## Controlador e encarregado

O Meu Ponto é um aplicativo pessoal operado como controlador dos dados descritos nesta política. O encarregado pelo tratamento de dados e o canal oficial de contato devem ser preenchidos antes da publicação na Play Store:

- Controlador: **_preencher com nome/RG do controlador_**
- Contato do encarregado: **_preencher com e-mail de suporte real_**

## Dados processados e base legal

| Dado | Finalidade | Base legal (LGPD) |
| --- | --- | --- |
| Imagens dos comprovantes | Arquivar e organizar os registros digitados | Execução de serviço solicitado pelo titular |
| Texto reconhecido por OCR (extraído da imagem do comprovante) | Preenchimento automático de data e horário | Execução de serviço solicitado pelo titular |
| Datas e horários digitados | Cálculo de horas e resumos | Execução de serviço solicitado pelo titular |
| Configurações da folha (períodos e resultados) | Cálculo de salário e de insights | Execução de serviço solicitado pelo titular |
| Dados técnicos do aparelho (versão do app/sistema no updater) | Verificação de atualização (somente flavor `github`) | Legítimo interesse em manter o app seguro e atualizado |

O app **não** vende dados, não usa os comprovantes para publicidade e não envia documentos automaticamente para um servidor próprio.

## Segurança e retenção

- Todos os dados ficam no espaço privado do app no aparelho (armazenamento interno).
- O Google Auto Backup / transferência de aparelho está **desativado**: o banco Room, as imagens em `files/receipts/` e o cache não são copiados para a nuvem nem transferidos entre aparelhos pelo Android.
- Backups criados pelo app com senha são criptografados no aparelho (AES-GCM, chave derivada da senha com PBKDF2). A senha nunca é armazenada; se for perdida, o backup não pode ser recuperado.
- Retenção: os dados ficam até o titular excluí-los ou desinstalar o app. Desinstalação remove o espaço privado correspondente (exceto backups exportados pelo usuário).

## Exportação e backup

Quando o usuário cria um backup, o app gera um ZIP e o envia somente ao destino escolhido no seletor do Android (ex.: Google Drive, armazenamento local). Esse destino externo passa a ter a própria política de privacidade.

## Permissões

- O scanner de documentos é executado por um componente do Google Play Services; não é solicitado acesso amplo à galeria ou ao armazenamento.
- A permissão `REQUEST_INSTALL_PACKAGES` existe somente na distribuição `github` para instalar atualizações baixadas sob ação explícita do usuário. A distribuição `play` não possui essa permissão.

## Direitos do titular (LGPD)

O titular pode, a qualquer momento: acessar, corrigir, exportar (backup/PDF) e apagar seus dados, direto pelo app. Pedidos de esclarecimento ou reclamação podem ser enviados ao contato do encarregado; o titular pode recorrer à ANPD.

## Contato

Antes da publicação, substitua o placeholder do início deste documento pelo canal REAL de suporte e hospede este arquivo em URL HTTPS pública associada ao aplicativo (Play Console → política de privacidade).
