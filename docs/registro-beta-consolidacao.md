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

## Histórico

| Tag | Data | Commit | Observação |
|---|---|---|---|
| (pendente) | — | — | Primeiro beta: rodar `beta.yml` manual. |
