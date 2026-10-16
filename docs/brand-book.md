# Meu Ponto — Brand book v1.1

## Essência

**Promessa:** guardar comprovantes de ponto com clareza, privacidade e calma.

**Posicionamento:** um arquivo pessoal confiável para momentos de trabalho que precisam ser encontrados rapidamente.

**Personalidade:** preciso, acolhedor, discreto e contemporâneo. O produto transmite segurança sem parecer burocrático.

**Frase de marca:** Clareza para sua jornada.

## Logotipo

O símbolo combina uma folha de comprovante com um ponto de atenção. A folha representa o arquivo; o ponto representa a informação importante que o usuário quer reencontrar.

- Use o símbolo em Coral sobre fundos claros ou em branco sobre fundos Ink.
- Preserve uma área livre mínima equivalente a 25% da largura do símbolo.
- Nunca aplique sombra forte, gradiente, contorno externo ou rotação.
- O nome “Meu Ponto” deve usar peso semibold/bold e nunca ser escrito todo em caixa alta.

O arquivo vetorial oficial está em `app/src/main/res/drawable/ic_meu_ponto_logo.xml`.

## Cores

| Token | Hex | Uso |
| --- | --- | --- |
| Ink | `#112032` | títulos, superfícies de destaque, navegação forte |
| Ink Soft | `#304052` | texto secundário com alta legibilidade |
| Teal | `#157C70` | ação principal, status positivo, links |
| Teal Dark | `#0B5C53` | texto sobre Mint e estados de confiança |
| Mint | `#D8F1E2` | seleção, ícones de apoio, áreas de calma |
| Coral | `#FF8A72` | assinatura da marca e pontos de atenção |
| Cream | `#F8F7F2` | fundo principal do aplicativo |
| Mist | `#E9EFEC` | chips, divisores suaves e superfícies auxiliares |

O Coral é expressivo e deve ser usado como acento. A interface deve continuar funcional em escala de cinza usando Ink e Teal para toda informação essencial.

### Tema escuro

O tema escuro é uma composição própria da marca, não uma inversão automática da paleta clara. A sensação desejada é arquivo premium: silencioso, profundo e preciso.

| Token | Hex | Uso |
| --- | --- | --- |
| Dark Canvas | `#0C141A` | fundo geral e áreas de respiro |
| Dark Surface | `#121E25` | cards, campos e folhas modais |
| Dark Raised | `#1B2A33` | controles elevados e agrupadores |
| Dark Hero Start | `#14242C` | início do card de resumo |
| Dark Hero End | `#0D4C47` | fim do card de resumo, com teal profundo |
| Dark Teal | `#74D9C6` | ação principal, números e estados positivos |
| Dark Teal Soft | `#103A38` | selo de backup e áreas de apoio |
| Dark Text | `#F1F8F5` | títulos e informação primária |
| Dark Muted | `#B8CCC7` | descrições e metadados |
| Dark Line | `#42565D` | divisores e contornos |

Regras do modo escuro:

- Mint não deve ocupar grandes superfícies; no escuro ele é substituído por Dark Teal Soft.
- O card principal usa navy/petróleo, nunca branco, menta claro ou gradiente com `onBackground`.
- Dark Text é reservado à informação principal; Dark Muted mantém a hierarquia sem competir com números.
- Coral aparece apenas como ponto de atenção ou detalhe do símbolo, nunca como segunda cor dominante.
- Ações continuam reconhecíveis por texto, ícone e estado; cor sozinha não comunica confirmação.

## Tipografia

O app usa a família sans-serif do sistema para evitar download de fontes e manter bom desempenho. A hierarquia visual segue:

- Títulos principais: semibold/bold, 20–26sp.
- Números de resumo: bold, 48sp.
- Texto de leitura: 14–16sp, entrelinha confortável.
- Metadados: 11–12sp, sempre acompanhados de contraste e não usados sozinhos para informação crítica.

## Sistema de interface

- Grade base de 8dp.
- Cards principais com raio de 22–28dp.
- Controles de busca e chips com raio de 12–18dp.
- Superfícies sem sombra pesada; profundidade vem de contraste, espaçamento e agrupamento.
- Um CTA primário por contexto. O botão “Adicionar” é sempre a ação mais evidente.
- Estados vazios devem explicar o próximo passo, não apenas informar que não há dados.

## Voz e conteúdo

Escreva como uma pessoa cuidadosa que ajuda o usuário a recuperar um documento importante. Prefira frases curtas e concretas:

- “Seu arquivo está em dia” em vez de “Operação concluída”.
- “Horário pendente de revisão” em vez de “OCR falhou”.
- “Backup exportável” quando o arquivo ZIP pode ser criado pelo seletor do Android; não sugerir sincronização automática quando ela não estiver configurada.

Não prometa sincronização automática no Drive antes que OAuth, restauração e testes de conflito existam. O backup exportável atual pode ser descrito com precisão como arquivo ZIP com imagens e metadados.

## Acessibilidade

- Toda ação de ícone recebe `contentDescription`.
- A cor não é a única forma de indicar estado.
- Áreas de toque devem ter pelo menos 44dp.
- Textos críticos usam Ink/Ink Soft no claro e Dark Text/Dark Muted no escuro; tons claros ficam restritos a fundos decorativos.
- Testar com fonte ampliada, TalkBack, modo escuro e daltonismo antes de uma release de produção.

## Próximas decisões de produto

1. Implementar OAuth e sincronização incremental automática do Google Drive.
2. Criar testes para importação, câmera, OCR, restauração e compartilhamento.
3. Preparar assinatura release protegida por GitHub Secrets.
4. Validar o fluxo em aparelhos pequenos, grandes e com fonte ampliada.
