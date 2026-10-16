# Meu Ponto — Android nativo

Aplicativo Android nativo para arquivar, pesquisar e compartilhar comprovantes de ponto.

## Stack
- Kotlin + Jetpack Compose
- Room para metadados locais
- ML Kit Document Scanner para captura automática, recorte e correção de documentos
- ML Kit Text Recognition para OCR no aparelho
- GitHub Actions para compilar APK e publicar Releases por tag `v*`

## Estado atual
O app agora tem uma interface premium com identidade própria, scanner de documentos com captura automática, detecção de bordas, correção de perspectiva, filtros, importação da galeria, OCR local, persistência em Room, busca, filtros, miniaturas e compartilhamento em PDF. O scanner funciona no dispositivo pelo Google Play Services e não exige que o Meu Ponto peça permissão direta da câmera. Ainda não é uma versão pronta para uso trabalhista: OCR de data/horário exige revisão manual e o backup do Drive ainda está em desenvolvimento.

Ainda precisam ser concluídos e testados antes de chamar de MVP confiável:
- Implementação completa de backup incremental no Google Drive com OAuth, restauração, verificação de integridade e política de conflitos.
- Testes automatizados, validação em aparelho físico e cobertura do fluxo de edição do scanner.
- Ajustes de PDF para imagens grandes, rotação e comprovantes multipágina.
- Assinatura de release e proteção da chave de assinatura; o workflow atual publica APK debug para facilitar o primeiro teste.

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
git tag v0.1.0
git push origin v0.1.0
```

O workflow `.github/workflows/android-release.yml` usa o Gradle Wrapper, compila o APK debug, guarda o artefato da execução e anexa `app-debug.apk` ao Release da tag. Não distribua esse APK como versão final sem assinar uma build de release e testar restauração/backup.

## Backup no Google Drive
O backup ainda não está configurado no aplicativo. Para concluí-lo será necessário implementar sincronização/restauração, definir a política de conflitos e configurar OAuth no Google Cloud (cliente Android, SHA-1/SHA-256 do certificado e escopos do Drive). Essas credenciais não devem ser colocadas no repositório.
