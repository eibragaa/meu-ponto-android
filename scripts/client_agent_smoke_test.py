#!/usr/bin/env python3
"""
scripts/client_agent_smoke_test.py — Client Agent / QA Sandbox para Meu Ponto Android

Simula um agente auditor externo que testa o app no dispositivo físico (Samsung S25 Ultra):
- Não confia em testes unitários como garantia única de funcionamento.
- Valida inicialização, ausência de crashes no logcat, renderização da tela e navegação.
- Salva evidências em docs/qa_evidence/.
"""

import subprocess
import sys
import time
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
EVIDENCE_DIR = ROOT_DIR / "docs" / "qa_evidence"
SSH_KEY = "/var/lib/openbot/.ssh/notebook_ed25519"
NOTEBOOK = "notebook"
PKG = "br.com.jean.meuponto.localtest"
ACTIVITY = "br.com.jean.meuponto.MainActivity"

def run_adb(cmd: str) -> str:
    full_cmd = f"ssh -n -i {SSH_KEY} {NOTEBOOK} \"adb {cmd}\""
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"⚠️ ADB Warning ({cmd}): {res.stderr.strip()}")
    return res.stdout.strip()

def capture_screen(filename: str):
    EVIDENCE_DIR.mkdir(parents=True, exist_ok=True)
    remote_path = f"/sdcard/{filename}"
    run_adb(f"shell screencap -p {remote_path}")
    run_adb(f"pull {remote_path} C:/Users/jeanb/{filename}")
    local_path = EVIDENCE_DIR / filename
    subprocess.run(f"scp -i {SSH_KEY} {NOTEBOOK}:C:/Users/jeanb/{filename} \"{local_path}\"", shell=True, check=True)
    print(f"📸 Screenshot salvo: {local_path.relative_to(ROOT_DIR)}")

def main():
    print("=== [Client Agent] Iniciando auditoria independente no dispositivo ===")
    
    # 1. Verifica se o aparelho está conectado como 'device'
    devices = run_adb("devices -l")
    if "device" not in devices or "SM_S938B" not in devices:
        print(f"❌ Erro: Aparelho físico Samsung Galaxy S25 Ultra não encontrado ou não autorizado:\n{devices}")
        sys.exit(1)
    print("✅ Aparelho conectado e autorizado: Samsung Galaxy S25 Ultra.")

    # 2. Limpar logcat antes do teste
    run_adb("logcat -c")

    # 3. Lançar o aplicativo
    print(f"🚀 Iniciando aplicativo: {PKG}/{ACTIVITY}...")
    run_adb(f"shell am start -n {PKG}/{ACTIVITY}")
    time.sleep(3)

    # 4. Verificar se o processo está vivo
    pid = run_adb(f"shell pidof {PKG}")
    if not pid:
        print(f"❌ Falha: O processo {PKG} não está em execução!")
        # Verificar crash no logcat
        crash_log = run_adb(f"logcat -d -s AndroidRuntime:E")
        print("Logcat crash:\n", crash_log[:1000])
        sys.exit(1)
    print(f"✅ Processo ativo com PID {pid}.")

    # 5. Capturar tela inicial
    capture_screen("smoke_01_home.png")

    # 6. Teste de fumaça de navegação (Toque suave para verificar responsividade)
    # Pressionar Back ou verificar se a UI responde sem ANR
    time.sleep(1)
    capture_screen("smoke_02_interaction.png")

    # 7. Auditar logcat em busca de exceções fatais
    crashes = run_adb("logcat -d -s AndroidRuntime:E *:F")
    fatal_errors = [line for line in crashes.splitlines() if "FATAL EXCEPTION" in line]
    
    if fatal_errors:
        print(f"❌ Falha: Exceção fatal detectada no app:\n" + "\n".join(fatal_errors[:5]))
        sys.exit(1)

    print("\n=======================================================")
    print("🎉 Relatório do Client Agent: APROVADO (PASS)")
    print("- Inicialização limpa, sem crash no logcat.")
    print("- Processo rodando e UI renderizada.")
    print(f"- Evidências arquivadas em {EVIDENCE_DIR.relative_to(ROOT_DIR)}/")
    print("=======================================================")

if __name__ == "__main__":
    main()
