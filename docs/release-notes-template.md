# Modelo de release notes

Use este formato no corpo do release do GitHub. O banner do app usa a seção **Novidades** para
destacar até dois itens; **Correções e melhorias** aparece de forma genérica, e o diálogo de
atualização mostra o texto completo.

```markdown
# Meu Ponto vX.Y.Z

## Novidades

- Recurso principal que o usuário percebe.
- Segundo destaque, se houver.

## Correções e melhorias

- Correção relevante (detalhe para quem abrir o diálogo).
- Ajuste de estabilidade ou desempenho.

## Interno

- Refatoração, testes ou mudanças que não precisam aparecer no banner.
```

Regras:

- O banner mostra no máximo **dois** itens da seção **Novidades**.
- Se não houver seção **Novidades**, o banner mostra apenas **“Correções e melhorias”**.
- Se houver seção **Novidades**, o banner usa o rótulo **“Novidades”** e destaca os itens.
- A seção **Interno** não aparece no banner; pode ser mantida para desenvolvedores.
- Mantenha o texto curto no primeiro item; o banner corta em duas linhas e trunca com `…`.
- O diálogo de atualização exibe as notas completas, então detalhes podem ficar nas seções.
