<div align="center">

<img src="docs/assets/meu-ponto-logo.svg" width="128" height="128" alt="Logo do Meu Ponto" />

# Meu Ponto

**Clareza para sua jornada.**

[![Version](https://img.shields.io/badge/Vers%C3%A3o-0.6.5-157C70?style=for-the-badge&logo=android&logoColor=white)](app/build.gradle.kts)
[![Platform](https://img.shields.io/badge/Plataforma-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-26-FF8A72?style=for-the-badge)](app/build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-112032?style=for-the-badge)](app/build.gradle.kts)
[![Privacy](https://img.shields.io/badge/Privacidade-100%25%20off--line%20%7C%20Sem%20conta%20%7C%20Sem%20an%C3%BAncios-0B5C53?style=for-the-badge)](docs/privacy-policy.md)

<p align="center">
  <i>Um arquivo pessoal, confiável e discreto para guardar, organizar e reencontrar seus comprovantes de ponto com rapidez.</i>
</p>

</div>

## ✨ Por que usar o Meu Ponto?

Tudo fica **no seu aparelho**. Sem conta, sem nuvem, sem anúncios. Feito para momentos em que você precisa reencontrar um comprovante com clareza e sem distrações.

## 🚀 Recursos principais

| Recurso | Descrição |
| --- | --- |
| **📸 Captura inteligente** | Escaneie com o *Document Scanner* do ML Kit (recorte e correção de perspectiva) ou importe direto da galeria. |
| **🧠 OCR local** | Reconhecimento de texto on-device para preencher data e horários automaticamente. |
| **⏱️ Cálculo de horas preciso** | Pareamento entre comprovantes e entre dias (incluindo turnos noturnos), resumo por período da folha e tela de *Insights*. |
| **🗂️ Organização hierárquica** | Navegue por mês, dia ou semana. Busca, filtros, miniaturas e visualizador com zoom. |
| **📄 Exportação e backup** | Gere PDFs (um ou vários comprovantes). Exporte um backup `.ZIP` com senha opcional e restaure quando precisar. |
| **🎨 Experiência refinada** | Tema claro/escuro persistente, *SplashScreen* nativa e interface fiel à identidade da marca. |
| **🔄 Atualização in-app** | No flavor `github`, atualize direto dos *GitHub Releases*. |

## 🛠️ Stack Tecnológica

| Tecnologia | Uso |
| --- | --- |
| **[Kotlin](https://kotlinlang.org/)** | Linguagem nativa, segura e concisa. |
| **[Jetpack Compose](https://developer.android.com/jetpack/compose)** | UI declarativa com [Material 3](https://m3.material.io/). |
| **[Room](https://developer.android.com/training/data-storage/room)** | Persistência local robusta e relacional. |
| **[ML Kit](https://developers.google.com/ml-kit)** | *Document Scanner* e *Text Recognition* 100% locais. |
| **[AndroidX SplashScreen](https://developer.android.com/guide/topics/ui/splash-screen)** | Transição inicial nativa. |
| **[GitHub Actions](https://github.com/features/actions)** | CI/CD para testes, lint e releases automatizados. |

> **Suporte:** `minSdk 26` • `targetSdk 36` • `compileSdk 36`

## 🏗️ Arquitetura

O projeto segue uma separação clara, previsível e orientada à testabilidade:

| Camada | Responsabilidade |
| --- | --- |
| **`ui/` + `MainActivity.kt`** | Composição de telas. Sem regras de negócio ou I/O direto. |
| **`domain/`** | Regras puras, cálculos de horas e lógica testada. |
| **`data/`** | Fontes de dados: Room, OCR, backup/restore, PDF e integrações locais. |
| **`updater/`** | Lógica de atualização in-app (restrita ao flavor `github`). |

- **Padrão completo:** [`docs/engineering-standards.md`](docs/engineering-standards.md)
- **Guia para agentes de IA:** [`AGENTS.md`](AGENTS.md)
- **Identidade visual:** [`docs/brand-book.md`](docs/brand-book.md)

## ⚡ Começando

### Pré-requisitos

- [JDK 17](https://adoptium.net/)
- [Android SDK 36](https://developer.android.com/studio)
- `local.properties` apontando para o seu Android SDK (não versionado)

### Abrir e rodar

1. Abra a raiz do projeto no **Android Studio**
2. Aguarde a sincronização do Gradle
3. Rode a variante `playDebug` no emulador ou dispositivo

### Via terminal

```bash
# Build de desenvolvimento
./gradlew :app:assemblePlayDebug

# Instalar no dispositivo conectado
adb install -r app/build/outputs/apk/play/debug/app-play-debug.apk
```

## 🔧 Comandos úteis

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:assemblePlayDebug` | APK de desenvolvimento (Play) |
| `./gradlew :app:assembleGithubRelease` | APK release com atualizador in-app |
| `./gradlew :app:bundlePlayRelease` | AAB assinado para Google Play |
| `./gradlew :app:testPlayDebugUnitTest` | Testes unitários JVM (sem emulador) |
| `./gradlew :app:lintPlayDebug` | Verificação estática com Lint |

## 🍦 Flavors de distribuição

| Flavor | Atualizador | Permissões | Uso |
| --- | --- | --- | --- |
| **`play`** | Desativado | Sem `REQUEST_INSTALL_PACKAGES` | Distribuição na Google Play |
| **`github`** | Ativado (GitHub Releases) | Conforme necessário para instalação direta | Distribuição via GitHub Releases |

**Instalação paralela para testes:** use `-PlocalTestSuffix=.localtest`. O `applicationId` ganhará o sufixo e os dados ficarão completamente isolados da versão oficial.

## 🚢 Publicação de release

Empurre uma tag `vX.Y.Z` **exatamente igual** ao `versionName` definido em `app/build.gradle.kts`:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

O workflow [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml) será acionado automaticamente (tag `v*` ou execução manual). Ele executa testes, lint, assina com as chaves dos *GitHub Secrets*, publica o **APK** do flavor `github` e o **AAB** do flavor `play` no *GitHub Release*.

> **Nota:** Sem keystore configurado, o build local usa a chave *debug* — **apenas para testes locais, nunca para distribuição.** Versão, tag e publicação são decisões do mantenedor do projeto.

## 🔐 Dados, privacidade e backup

- **100% local.** Sem *Android Auto Backup*/*Drive Backup*. Banco Room, imagens e cache ficam fora do backup do sistema.
- **Exportável pelo app.** Faça backup/restaure via arquivo `.ZIP` gerado pelo app, com senha opcional.
- **Sincronização com Drive:** não implementada no momento. Requer OAuth (Google Cloud), SHA-1/SHA-256 do certificado e credenciais fora do repositório.
- **Transparência:** veja a [Política de Privacidade](docs/privacy-policy.md) e o [Checklist de Prontidão para Play Store](docs/google-play-readiness.md).

## 📚 Documentação

| Documento | Descrição |
| --- | --- |
| [**Engineering Standards**](docs/engineering-standards.md) | Camadas, nomenclatura, testes, Git e processo de release. |
| [**Brand Book**](docs/brand-book.md) | Identidade, paleta, tipografia, voz e acessibilidade. |
| [**Roadmap AAA**](docs/roadmap-aaa.md) | Direcionamento para uma experiência premium na Play Store. |
| [**Status e Próximos Passos**](docs/status-e-proximos-passos.md) | Estado atual e pendências priorizadas. |
| [**Política de Privacidade**](docs/privacy-policy.md) | Compromisso com privacidade e dados locais. |
| [**Checklist Play Store**](docs/google-play-readiness.md) | Validações para publicação. |

<div align="center">

**Meu Ponto** — Arquive com clareza. Reencontre com confiança.

</div>
