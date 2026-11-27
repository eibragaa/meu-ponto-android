#!/usr/bin/env bash
# scripts/deploy_dogfood.sh — Deploy rápido da versão de teste no celular via ADB sobre SSH
#
# Linha de Chegada fora do código (Pilar 1 da metodologia Vibe Coding com Rigor):
# 1. Compila APK debug com applicationId isolado (-PlocalTestSuffix=.localtest)
# 2. Transfere para o notebook de Jean via SCP
# 3. Instala via ADB no Samsung Galaxy S25 Ultra conectado
# 4. Inicia a Activity e captura screenshot para validação visual

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SSH_KEY="/var/lib/openbot/.ssh/notebook_ed25519"
NOTEBOOK_HOST="notebook"
APK_LOCAL="${PROJECT_ROOT}/app/build/outputs/apk/github/debug/app-github-debug.apk"
APK_REMOTE="C:/Users/jeanb/meuponto_dogfood.apk"
PACKAGE_NAME="br.com.jean.meuponto.localtest"
ACTIVITY_NAME="br.com.jean.meuponto.MainActivity"

echo "=== [1/4] Compilando Meu Ponto com sufixo de teste isolado (.localtest) ==="
cd "${PROJECT_ROOT}"
./gradlew :app:assembleGithubDebug -PlocalTestSuffix=.localtest --no-daemon

if [[ ! -f "${APK_LOCAL}" ]]; then
    echo "❌ Erro: APK não encontrado em ${APK_LOCAL}"
    exit 1
fi

echo "=== [2/4] Transferindo APK para o notebook (${NOTEBOOK_HOST}) ==="
scp -i "${SSH_KEY}" "${APK_LOCAL}" "${NOTEBOOK_HOST}:${APK_REMOTE}"

echo "=== [3/4] Instalando e iniciando no Samsung Galaxy S25 Ultra via ADB ==="
ssh -n -i "${SSH_KEY}" "${NOTEBOOK_HOST}" "adb install -r -d \"${APK_REMOTE}\""
ssh -n -i "${SSH_KEY}" "${NOTEBOOK_HOST}" "adb shell am start -n ${PACKAGE_NAME}/${ACTIVITY_NAME}"

echo "=== [4/4] Capturando screenshot de evidência visual ==="
ssh -n -i "${SSH_KEY}" "${NOTEBOOK_HOST}" "adb shell screencap -p /sdcard/dogfood_screen.png"
ssh -n -i "${SSH_KEY}" "${NOTEBOOK_HOST}" "adb pull /sdcard/dogfood_screen.png \"C:/Users/jeanb/dogfood_screen.png\""

mkdir -p "${PROJECT_ROOT}/docs/qa_evidence"
scp -i "${SSH_KEY}" "${NOTEBOOK_HOST}:C:/Users/jeanb/dogfood_screen.png" "${PROJECT_ROOT}/docs/qa_evidence/dogfood_latest.png"

echo "✅ Sucesso! Meu Ponto (.localtest) instalado e aberto no aparelho."
echo "📸 Evidência salva em: docs/qa_evidence/dogfood_latest.png"
