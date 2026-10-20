# Meu Ponto — checklist de produção

## Estado da base

- [x] `compileSdk` e `targetSdk` 36.
- [x] `versionCode` incremental e `versionName` visível.
- [x] SplashScreen compatível com Android 12+ e tema claro/escuro.
- [x] Scanner usando o fluxo de documentos do Google Play Services.
- [x] Sem permissão ampla de armazenamento.
- [x] Backup local exportável com senha opcional AES-GCM.
- [x] Tela principal com filtros e agrupamento em ação contextual.
- [ ] Keystore de produção armazenado fora do repositório.
- [ ] Build release assinada e AAB publicado em teste interno.
- [ ] Play App Signing configurado no Play Console.

## Dados e privacidade

- [ ] Hospedar `privacy-policy.md` em domínio público e HTTPS.
- [ ] Preencher Data Safety no Play Console.
- [ ] Declarar que OCR é processado localmente, se essa arquitetura permanecer.
- [ ] Declarar exportação voluntária para Drive ou outro destino escolhido pelo usuário.
- [ ] Revisar bibliotecas e SDKs a cada release.
- [ ] Não incluir analytics, anúncios ou coleta oculta sem atualizar política e Data Safety.

## Google Drive

Para backup automático futuro, usar o menor escopo possível, preferencialmente `drive.file` ou `drive.appdata`. Evitar acesso amplo a todo o Drive. A tela OAuth deve usar nome, suporte, homepage e política de privacidade reais. Se o app for público e utilizar escopos sensíveis/restritos, planejar a verificação do Google antes do lançamento.

## Qualidade antes da produção

- [ ] Testar captura e cancelamento em pelo menos três aparelhos Android.
- [ ] Testar comprovantes com data, sem data, com horários diferentes e baixa iluminação.
- [ ] Confirmar edição manual, agrupamento e PDF individual/lote.
- [ ] Testar backup protegido, senha incorreta, arquivo danificado e restauração repetida.
- [ ] Testar fonte ampliada, TalkBack, rotação, pouca memória e armazenamento cheio.
- [ ] Executar teste fechado no Play Console conforme a exigência da conta de desenvolvedor.
- [ ] Registrar feedback e corrigir crashes antes de solicitar produção.

## Assinatura no GitHub Actions

Os valores abaixo devem ser configurados como Secrets, nunca versionados:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Até esses Secrets existirem, o APK de GitHub Releases deve ser tratado como build de teste.
