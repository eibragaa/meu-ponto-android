# Meu Ponto — Android nativo

Aplicativo Android nativo para arquivar, pesquisar e compartilhar comprovantes de ponto.

## Stack
- Kotlin + Jetpack Compose
- Room para metadados locais
- ML Kit Document Scanner para captura automática, recorte e correção de documentos
- ML Kit Text Recognition para OCR no aparelho
- GitHub Actions para compilar APK e publicar Releases por tag `v*`

## Estado atual
O app agora tem uma interface premium com identidade própria, tema claro/escuro persistente, scanner de documentos com captura automática, detecção de bordas, correção de perspectiva, filtros, importação da galeria, OCR local, revisão manual de data/horário reconhecidos, agrupamento simples por dia ou semana, persistência em Room, busca, miniaturas e exportação de um ou vários comprovantes em PDF. Na exportação em lote, dois comprovantes são organizados por página; a exportação individual continua disponível. O scanner funciona no dispositivo pelo Google Play Services e não exige que o Meu Ponto peça permissão direta da câmera. O backup exportável inclui imagens, datas, horários e OCR em um ZIP e pode ser restaurado pelo seletor do Android, inclusive escolhendo o Google Drive como destino.

Limites conhecidos para a distribuição final:
- A sincronização automática e incremental em segundo plano no Google Drive exige configurar OAuth no Google Cloud, incluindo o cliente Android e o SHA-1/SHA-256 do certificado de assinatura.
- O OCR de data/horário continua exigindo revisão humana; o app não deve ser tratado como registro trabalhista oficial sem essa conferência.
- O workflow publica uma build debug para testes. Para atualizações sem desinstalação e distribuição pública, ainda é necessário cadastrar uma chave de release permanente nos Secrets do GitHub.

## Identidade visual

O sistema de marca, logotipo, cores, tipografia, voz e regras de acessibilidade está documentado em [`docs/brand-book.md`](docs/brand-book.md). O app usa a assinatura visual “Clareza para sua jornada”, com Ink, Teal, Mint, Coral e fundo Cream.

## Desenvolvimento local
1. Instale/abra o Android Studio com JDK 17.
2. Abra a pasta raiz deste projeto (`meu-ponto-android`) no Android Studio.
3. Aguarde a sincronização do Gradle e aceite a instalação do Android SDK 35 se for solicitada.
4. Conecte um aparelho com depuração USB autorizada ou crie um emulador.
5. Execute a configuração `app` pelo botão Run.

Pelo terminal do Windows, com `ANDROID_HOME` apontando para o SDK, também é possível executar:

```powershell
.\gradlew.bat :app:assembleDebug
```

O APK ficará em `app/build/outputs/apk/debug/app-debug.apk`. O `local.properties` é específico do computador e não deve ser enviado ao GitHub.

## Publicar um APK no GitHub Releases
Depois de enviar o projeto ao GitHub, crie e envie uma tag, por exemplo:

```bash
git tag v0.3.0
git push origin v0.3.0
```

O workflow `.github/workflows/android-release.yml` usa o Gradle Wrapper, compila o APK debug, renomeia o artefato para `Meu-Ponto-vX.Y.Z.apk` e o anexa ao Release da tag. O APK é apropriado para testes; antes de distribuir publicamente, configure a chave de release permanente para preservar a assinatura entre versões.

## Backup no Google Drive
O app já exporta e restaura um backup ZIP pelo seletor de documentos do Android. Para usar o Google Drive agora, escolha-o como destino ao criar o arquivo. A sincronização automática, incremental e silenciosa no estilo WhatsApp é uma etapa separada: exige OAuth no Google Cloud (cliente Android, SHA-1/SHA-256 do certificado e escopos do Drive), política de conflitos e Secrets/configuração fora do repositório. Essas credenciais não devem ser colocadas no GitHub.
