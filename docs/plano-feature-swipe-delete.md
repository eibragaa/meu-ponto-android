# Feature — apagar comprovante com swipe para a direita

_Status: implementado na branch `feat-swipe-delete` em 23/10/2026. CI verde no run `39952315886`; QA manual no aparelho pendente._

Objetivo: na lista atual de comprovantes, puxar o card para a direita e revelar a ação **Apagar**,
sem abrir o visualizador.

## 1. Estado atual

O app já tem exclusão segura:

- O visualizador (`ReceiptViewer`) tem botão de excluir.
- A exclusão abre o diálogo `Excluir comprovante?` (`MainActivity.kt:724-746`).
- Ao confirmar:
  - remove a linha do Room (`dao.delete`);
  - apaga a imagem (`File(receipt.imagePath).delete()`);
  - fecha o visualizador, se estiver aberto;
  - mostra “Comprovante excluído.”.

O que falta é um atalho na lista. Hoje o card (`ReceiptCard`, `MainActivity.kt:1162-1244`) só tem
clique, long-press, editar e compartilhar. A lista é renderizada em dois caminhos:

- agrupamento por mês/dia (`MainActivity.kt:567-583`);
- agrupamento por dia/semana/lista (`MainActivity.kt:595-610`).

## 2. UX recomendada (MVP)

**Swipe para a direita** no card:

1. O card desliza para a direita e revela um fundo vermelho com ícone de lixeira e o texto
   **“Apagar”**.
2. Ao soltar depois do limiar, o card volta ao lugar e abre o diálogo de confirmação já existente.
3. “Cancelar” mantém o comprovante.
4. “Excluir” remove o comprovante e a imagem.
5. Swipe para a esquerda não faz nada.
6. Em modo de seleção, o swipe fica desabilitado para não conflitar com o long-press/checkbox.
7. A ação também fica disponível para acessibilidade via ação customizada “Apagar comprovante”.

### Por que confirmar em vez de apagar direto

- Evita exclusão acidental.
- Reaproveita o diálogo e a lógica já testados.
- Não exige “desfazer” com restauração de imagem.
- Mantém o comportamento do visualizador consistente.

### Alternativa (não recomendada agora)

Deixar o card aberto com um botão “Apagar” permanente exigiria um `AnchoredDraggable` customizado,
controle de “qual linha está aberta” e mais estados. É mais código para pouco ganho; fica para depois
se o MVP não bastar.

## 3. Implementação proposta

### 3.1 Novo componente `ui/SwipeToDeleteBox.kt`

Wrapper genérico, sem conhecer `Receipt`:

```kotlin
@Composable
fun SwipeToDeleteBox(
    enabled: Boolean,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDeleteRequest()
                false // volta para o lugar; o diálogo assume
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = false,
        gesturesEnabled = enabled,
        backgroundContent = {
            // fundo errorContainer + ícone Delete + texto "Apagar"
        }
    ) {
        content()
    }
}
```

Detalhes:

- `StartToEnd` = arrastar da esquerda para a direita em LTR; em RTL o Compose espelha.
- `confirmValueChange` retorna `false` para o card não sair da lista; o diálogo existente assume.
- Fundo com o mesmo `RoundedCornerShape(22.dp)` do card.
- `enabled = !isSelectionMode`.
- `Modifier.semantics { customActions = listOf(CustomAccessibilityAction("Apagar comprovante") { ... }) }`
  no wrapper, para TalkBack.
- Haptic opcional no momento em que o limiar é cruzado.

### 3.2 `ReceiptCard`

Adicionar parâmetro:

```kotlin
onRequestDelete: () -> Unit
```

Envolver o card:

```kotlin
SwipeToDeleteBox(
    enabled = !isSelectionMode,
    onDeleteRequest = onRequestDelete
) {
    Card(...) { ... }
}
```

Não alterar clique, long-press, editar ou compartilhar.

### 3.3 Call sites

Nos dois `items(...)` da lista:

```kotlin
onRequestDelete = { receiptPendingDelete = receipt }
```

Assim o fluxo existente de confirmação/exclusão é reutilizado.

### 3.4 Diálogo existente

Manter `receiptPendingDelete` e o `AlertDialog` atuais. Nenhuma mudança de banco ou de arquivo.

## 4. Arquivos previstos

| Arquivo | Mudança |
|---|---|
| `app/src/main/java/br/com/jean/meuponto/ui/SwipeToDeleteBox.kt` | novo wrapper de swipe |
| `app/src/main/java/br/com/jean/meuponto/MainActivity.kt` | `onRequestDelete` no card, dois call sites e wrapper |
| `docs/plano-feature-swipe-delete.md` | este documento |

Sem dependência nova. `SwipeToDismissBox` já vem do Material3 do BOM atual.

## 5. Casos de borda

| Caso | Comportamento |
|---|---|
| Swipe para a esquerda | Nada acontece |
| Swipe curto (abaixo do limiar) | Card volta, sem diálogo |
| Swipe completo | Abre confirmação; card volta |
| Cancelar | Nada é apagado |
| Confirmar | Remove banco + imagem; lista atualiza pelo Flow |
| Modo de seleção | Swipe desabilitado |
| Busca/filtro | Funciona nos resultados |
| Agrupamento mês/dia/semana/lista | Funciona nos dois caminhos de renderização |
| TalkBack | Ação “Apagar comprovante” disponível |
| RTL | Direção espelhada pelo Compose |
| Visualizador aberto | Continua com o botão de excluir atual |

## 6. Testes

### Manual no aparelho

- [ ] Swipe para a direita revela “Apagar” e abre confirmação.
- [ ] Swipe para a esquerda não faz nada.
- [ ] Swipe curto não abre diálogo.
- [ ] Cancelar mantém o comprovante.
- [ ] Excluir remove o card e a imagem; lista atualiza.
- [ ] Funciona em mês, dia, semana e lista.
- [ ] Funciona com busca/filtro ativos.
- [ ] Modo de seleção não permite swipe.
- [ ] Clique, long-press, editar e compartilhar continuam funcionando.
- [ ] TalkBack oferece a ação de apagar.
- [ ] RTL (se aplicável) espelha a direção.

### Automatizado

Não há infraestrutura de teste de UI Compose no projeto. O wrapper é visual; a regra de exclusão já
existe. Se quisermos teste automatizado depois, criar `androidTest` com Compose UI e um fake DAO.

## 7. Fases de implementação

| Fase | Entrega |
|---|---|
| 1 | `SwipeToDeleteBox` com fundo “Apagar” e callback |
| 2 | `ReceiptCard` + dois call sites + reuso do diálogo |
| 3 | Desabilitar em seleção, ação de acessibilidade e haptic |
| 4 | QA manual, docs e PR |

Sugestão de versão: **0.7.0** (feature de UX) ou **0.6.2** se for tratada como patch. Não alterar
versão sem autorização do dono.

## 8. Critérios de aceite

- [ ] Puxar o card para a direita mostra a ação **Apagar**.
- [ ] A exclusão só acontece após confirmação.
- [ ] Swipe para a esquerda e swipe curto não apagam.
- [ ] Modo de seleção não conflita com o gesto.
- [ ] Funciona nos dois caminhos de lista.
- [ ] Clique/long-press/editar/compartilhar sem regressão.
- [ ] Build e lint das duas flavors passam no CI.
- [ ] QA manual no aparelho aprovado.

## 9. Decisões em aberto

1. MVP “swipe → diálogo” confirmado na implementação.
2. Haptic não incluído nesta rodada.
3. Versão alvo: pendente (`0.7.0` ou `0.6.3`).

## 10. Fora de escopo

- Exclusão em lote.
- “Desfazer” após excluir.
- Arrastar para a esquerda para outra ação.
- Excluir pelo visualizador (já existe).
- Testes de UI automatizados nesta rodada.

## 11. Referências

- `MainActivity.kt:567-583` — lista por mês/dia.
- `MainActivity.kt:595-610` — lista por dia/semana/lista.
- `MainActivity.kt:724-746` — diálogo e exclusão atuais.
- `MainActivity.kt:1162-1244` — `ReceiptCard`.
- `ui/ReceiptViewer.kt` — exclusão pelo visualizador.
