## O que muda

<!-- Uma frase: o que o usuário/desenvolvedor ganha com este PR. -->

## Tipo

- [ ] `feat` — funcionalidade nova
- [ ] `fix` — correção de regra/comportamento
- [ ] `refactor` — reorganização sem mudar comportamento
- [ ] `docs` / `test` / `chore` / `style`

## Checklist (ver `docs/engineering-standards.md`, seção 10)

- [ ] Está na camada certa (UI não faz I/O; regra em `domain/`)
- [ ] Nenhuma regra duplicada e nenhum número mágico novo sem constante + KDoc
- [ ] Nenhum `String` mágico novo para estado/modo/filtro
- [ ] Regra pura nova tem teste; testes e lint rodaram (`./gradlew :app:testPlayDebugUnitTest :app:lintPlayDebug`)
- [ ] Erros produzem mensagem útil (sem `catch` mudo)
- [ ] Diff sem reformatação alheia; commit `style` separado
- [ ] `versionName`/`versionCode`/tag **não** foram alterados (a menos que o PR seja de release, com OK do dono)

## Como testar

<!-- Passos objetivos: comando, aparelho/flavor, o que observar. -->

## Não testado / riscos

<!-- Seja explícito. Ex.: "não compilado neste ambiente (sem SDK)", "updater não testado ponta a ponta (P14)". -->
