# Registro Beta -> Consolidação

Beta pode ser criado a qualquer momento (manual ou push na main).
Consolidação roda sexta 22:00 BRT (cron `0 1 * * 6`) e publica a latest oficial.

## Como usar

| Quando | Ação |
|---|---|
| Semana | GitHub → Actions → "Beta Semanal" → Run workflow → tag `beta-v190001` (prerelease) |
| Sexta 22:00 BRT | `consolidacao.yml` roda sozinho → tag `v0.8.2` (latest) |
| Urgente | GitHub → Actions → "Consolidar Beta -> Release Oficial" → Run workflow |

## Regras

- `beta-v*` = prerelease, `applicationId = br.com.jean.meuponto.beta`, `versionCode >= 190001`
- `v*` oficial = latest, `applicationId = br.com.jean.meuponto`, `versionCode = base + 1`
- Mesmo keystore para beta e oficial (assinatura idêntica).
- Updater oficial usa `/releases/latest` e ignora `prerelease:true` e tag `beta-v*`.
- App beta mostra **Meu Ponto Beta** no launcher (manifest por flavor).
- Release beta: nome `Meu Ponto Beta • 0.8.1`, assets `Meu-Ponto-Beta-0.8.1.apk` (+ `-updater.apk`)
- Release oficial: nome `Meu Ponto • v0.8.2`, asset `Meu-Ponto-v0.8.2.apk`

## Modelo de descrição (inspirado no Duolingo — curto, consistente)

Toda release segue o mesmo esqueleto, em pt-BR, tom quente e direto.

**Beta (prerelease):**
```
## ⚡️ Novo beta disponível

**Meu Ponto Beta • 0.8.1** — pré-view do que vem sexta. Instale, teste e conte o que achou.

## Novidades
- (2–3 itens user-facing, sem jargão)

## Correções e melhorias
- (o que mudou no uso diário)

## Como instalar
- Baixe o APK abaixo (`Meu-Ponto-Beta-0.8.1-updater.apk`) e instale.
- Aparece como **Meu Ponto Beta** na tela inicial, ao lado do app oficial.
- Este beta **não** vira atualização automática. A versão oficial sai sexta 22:00 BRT.

## Qual arquivo baixar?
- `Meu-Ponto-Beta-0.8.1-updater.apk` — em geral é este: busca sozinho a próxima versão.
- `Meu-Ponto-Beta-0.8.1.apk` — sem updater, só teste manual.

**Obrigado por testar!** Qualquer bug, conta aqui.
```

**Oficial (latest):**
```
## ✨ Nova versão disponível

**Meu Ponto • v0.8.2** — tudo que foi testado no beta da semana, agora oficial.

## Novidades
- (2–3 itens user-facing)

## Correções e melhorias
- (o que ficou melhor)

## Como atualizar
- O app **já busca sozinho** esta versão (updater automático).
- Quer baixar manual? Pegue `Meu-Ponto-v0.8.2.apk` abaixo.
- Betas (`beta-v*`) não aparecem como atualização oficial.

**Obrigado!** Qualquer feedback ajuda a próxima versão.
```

## Histórico

| Tag | Data | Commit | Observação |
|---|---|---|---|
| `beta-v190001` | 06/11/2026 | `48b127d` | Primeiro beta publicado (pre-release). Instalado no celular ✅ |
