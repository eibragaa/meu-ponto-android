# Template de Direção Visual de UI (Design First)

> **Regra:** Não inicie a escrita de código Jetpack Compose antes de preencher este roteiro e obter o alinhamento com Jean.

---

## 1. Identificação da Tela / Componente
- **Tela/Componente:** [Nome da tela ou folha/modal]
- **Objetivo do usuário:** [O que a pessoa precisa fazer nessa tela em 1 frase]
- **Vocabulário oficial Android:** [Ex: ModalBottomSheet, Scaffold, TopAppBar, FloatingActionButton, FilterChip, Card]

---

## 2. As 3 Direções Visuais

### Direção 1: Funcional e Compacta (Densidade Alta)
- **Foco:** Máxima informação útil com menor rolagem.
- **Estrutura:** Listas agrupadas, tipografia compacta (`bodyMedium`/`labelSmall`), ícones discretos.
- **Ideal para:** Telas de histórico, espelho mensal denso ou formulário rápido.

### Direção 2: Imersiva e Espaçada (Foco e Clareza)
- **Foco:** Respiração visual, foco na ação principal (Hero), espaçamento generoso (`16dp` a `24dp`).
- **Estrutura:** Cards elevados, feedback visual forte, visualizador em destaque.
- **Ideal para:** Visualização de comprovante, captura inicial ou dashboard semanal.

### Direção 3: Material 3 Canônica (Estrita aos Guias Google)
- **Foco:** Aderência 100% aos componentes de referência do Material You com tons tonais dinâmicos.
- **Estrutura:** `FilledTonalButton`, `ElevatedCard`, chips com seleção nativa e animações de transição do sistema.
- **Ideal para:** Configurações, seleção de períodos e filtros globais.

---

## 3. Comparativo Antes e Depois

| Elemento | Como é Hoje (Antes) | Como Ficará (Depois) |
|---|---|---|
| **Hierarquia visual** | [Texto / descrição] | [Texto / descrição] |
| **Ação primária** | [Texto / descrição] | [Texto / descrição] |
| **Tratamento de erro** | [Texto / descrição] | [Texto / descrição] |
| **Modo Escuro / Claro**| [Texto / descrição] | [Texto / descrição] |

---

## 4. Aprovação e Próximos Passos
- **Direção Selecionada:** [ ] Direção 1  |  [ ] Direção 2  |  [ ] Direção 3
- **Ajustes solicitados por Jean:** [Observações pontuais]
- **Liberação para escrita em Jetpack Compose:** [ ] Sim  |  [ ] Não
