# Meu Ponto — Android nativo

Aplicativo Android nativo para arquivar, pesquisar e compartilhar comprovantes de ponto.

## Stack
- Kotlin + Jetpack Compose
- Room para metadados locais
- ML Kit Document Scanner para captura automática, recorte e correção de documentos
- ML Kit Text Recognition para OCR no aparelho
- Android SplashScreen para uma entrada rápida e consistente em light/dark
- Atualizador in-app integrado via GitHub Releases apenas no flavor `github`; a build `play` segue a política da Play e não instala APK fora da loja
- GitHub Actions para validar builds e publicar Releases por tag `v*`

## Estado atual
O app agora tem uma interface premium com identidade própria, tema claro/escuro persistente, splash nativa, scanner de documentos com captura automática, detecção de bordas, correção de perspectiva, importação da galeria, OCR local, revisão manual de data/horário reconhecidos, agrupamento por mês, dia ou semana, persistência em Room, busca, miniaturas e exportação de um ou vários comprovantes em PDF. Cada comprovante pode registrar entrada e saída, ou apenas uma batida; as horas são calculadas por dia quando há pares completos. O card principal mostra o período de folha configurável, horas trabalhadas, dias restantes para o fechamento e pendências, com uma tela de insights calculada localmente. A lista é hierárquica por mês e dia, e o toque no comprovante abre um visualizador com zoom, metadados, edição, PDF e exclusão. Quando o OCR não encontra data ou horário, os campos de edição exibem exemplos como `22/10/2026` e `10:20`, com máscara automática, sem gravar valores fictícios. Na exportação em lote, dois comprovantes são organizados por página; a exportação individual continua disponível. Filtros e agrupamento ficam dentro de “Organizar” para manter a tela principal limpa. O scanner funciona no dispositivo pelo Google Play Services e não exige que o Meu Ponto peça permissão direta da câmera. O backup exportável inclui imagens, datas, horários, entrada/saída e OCR em um ZIP; agora pode ser protegido por senha e restaurado pelo seletor do Android, inclusive escolhendo o Google Drive como destino.

Limites conhecidos para a distribuição final:
- A build local e o pipeline estão preparados para API 36, mas a assinatura de produção ainda depende de um keystore seu configurado nos Secrets do GitHub.
- A sincronização automática e incremental em segundo plano no Google Drive exige configurar OAuth no Google Cloud, incluindo o cliente Android e o SHA-1/SHA-256 do certificado de assinatura.
- O OCR de data/horário continua exigindo revisão humana; o app não deve ser tratado como registro trabalhista oficial sem essa conferência.
- O workflow continua publicando uma build debug nomeada para testes; a publicação na Play Store exige gerar um AAB release assinado.

## Identidade visual

O sistema de marca, logotipo, cores, tipografia, voz e regras de acessibilidade está documentado em [`docs/brand-book.md`](docs/brand-book.md). O app usa a assinatura visual “Clareza para sua jornada”, com Ink, Teal, Mint, Coral e fundo Cream.

## Desenvolvimento local
1. Instale/abra o Android Studio com JDK 17.
2. Abra a pasta raiz deste projeto (`meu-ponto-android`) no Android Studio.
3. Aguarde a sincronização do Gradle e aceite a instalação do Android SDK 36 se for solicitada.
4. Conecte um aparelho com depuração USB autorizada ou crie um emulador.
5. Selecione a variante `playDebug`, usada como base da Play Store, e execute a configuração `app`.

Pelo terminal do Windows, com `ANDROID_HOME` apontando para o SDK, também é possível executar:

```powershell
.\gradlew.bat :app:assemblePlayDebug
```

Os builds quentes agora usam dois flavors de distribuição:
- `play`: build da Play Store, sem instabilidade de sideload e sem `REQUEST_INSTALL_PACKAGES`.
- `github`: build com atualizador in-app pelo GitHub Releases.

O APK ficará em `app/build/outputs/apk/play/debug/app-play-debug.apk`. O `local.properties` é específico do computador e não deve ser enviado ao GitHub. Use este caminho para instalar uma variante de teste sem conflitar com a instalação oficial:

```powershell
.\gradlew.bat :app:assemblePlayDebug "-PlocalTestSuffix=.localtest" --no-daemon
adb install -r app/build/outputs/apk/play/debug/app-play-debug.apk
```

## Dados no aparelho e backup automático
O Meu Ponto não salva nem sincroniza dados automaticamente via Android Auto Backup/Drive Backup. O comprovante, o banco Room, cache e miniaturas ficam fora do backup de sistema. Isso evita que imagens sensíveis voltem em outra conta ou aparelho. O usuário é responsável por exportar/restaurar manualmente pelo backup ZIP do app, que pode ser protegido por senha.

## Testar a versão release localmente
Gera o build com R8 e recursos reduzidos, igual ao que vai para a Play, antes de enviar ao Git:

```powershell
.\gradlew.bat :app:assemblePlayRelease "-PlocalTestSuffix=.localtest" --no-daemon
adb install -r app/build/outputs/apk/play/release/app-play-release.apk
```

Sem o keystore de produção, o APK sai assinado com a chave debug. Isso serve só para teste local: nunca envie esse APK à Play. Com o keystore configurado (variáveis `ANDROID_KEYSTORE_*`), o mesmo comando gera o APK assinado para produção.

## Publicar um APK no GitHub Releases
Depois de enviar o projeto ao GitHub, crie e envie uma tag, por exemplo:

```bash
git tag v0.5.0
git push origin v0.5.0
```

O pipeline `.github/workflows/android-release.yml` usa o Gradle Wrapper, compila o APK debug do flavor `play`, renomeia o artefato para `Meu-Ponto-vX.Y.Z.apk` e o anexa ao Release da tag. O APK é apropriado para testes; antes de distribuir publicamente, configure a chave de release permanente e o bundle `playRelease` para preservar a assinatura entre versões e usar o Play App Signing.

## Publicação e privacidade

O checklist técnico e de dados está em [`docs/google-play-readiness.md`](docs/google-play-readiness.md). A política de privacidade que deve ser hospedada em uma URL pública está em [`docs/privacy-policy.md`](docs/privacy-policy.md) como base revisável; ela não substitui a configuração final no Play Console.

## Backup no Google Drive
O app já exporta e restaura um backup ZIP pelo seletor de documentos do Android. Para usar o Google Drive agora, escolha-o como destino ao criar o arquivo. A sincronização automática, incremental e silenciosa no estilo WhatsApp é uma etapa separada: exige OAuth no Google Cloud (cliente Android, SHA-1/SHA-256 do certificado e escopos do Drive), política de conflitos e Secrets/configuração fora do repositório. Essas credenciais não devem ser colocadas no GitHub.
