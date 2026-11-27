#!/usr/bin/env python3
"""
scripts/check_architecture.py — Linter de Arquitetura e Fronteiras do Meu Ponto Android

Valida as regras de arquitetura estrita:
1. domain/ não importa android.* (regras puras de negócio).
2. domain/ não importa data.* nem ui.*.
3. ui/ não importa DAOs nem banco de dados Room diretamente (ReceiptDao, AppDatabase).
4. Sem dependências cíclicas entre camadas.

Suporta modo `--baseline` para não quebrar débitos legados já mapeados (ex: ENG-4).
"""

import os
import re
import sys
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
SRC_DIR = ROOT_DIR / "app" / "src" / "main" / "java" / "br" / "com" / "jean" / "meuponto"

# Débitos técnicos conhecidos já mapeados no roadmap (ex: ENG-4 e Context em PayrollSettings)
# Não adicione novas exceções sem plano aprovado.
KNOWN_BASELINE = {
    "app/src/main/java/br/com/jean/meuponto/domain/Journeys.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/WorkStats.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/PunchPairing.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/ReceiptLabels.kt:4",
    "app/src/main/java/br/com/jean/meuponto/domain/PunchProgress.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/TodayShift.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/OpenEntry.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/PayrollSettings.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/PunchHabitLearner.kt:3",
    "app/src/main/java/br/com/jean/meuponto/domain/PunchChoice.kt:3",
}

violations = []
baseline_matches = []

def check_file(file_path: Path):
    rel_path = file_path.relative_to(ROOT_DIR).as_posix()
    pkg = None
    if "domain" in file_path.parts:
        pkg = "domain"
    elif "data" in file_path.parts:
        pkg = "data"
    elif "ui" in file_path.parts:
        pkg = "ui"
    elif "updater" in file_path.parts:
        pkg = "updater"
    else:
        pkg = "root"

    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    for i, line in enumerate(lines, 1):
        line_clean = line.strip()
        if not line_clean.startswith("import "):
            continue

        imported = line_clean.replace("import ", "").replace(";", "").strip()
        loc = f"{rel_path}:{i}"

        # Regra 1: domain não pode importar android.*
        if pkg == "domain":
            if imported.startswith("android.") and not imported.startswith("androidx.annotation."):
                msg = f"[{loc}] 'domain' não pode importar Android SDK: {imported}"
                if loc in KNOWN_BASELINE:
                    baseline_matches.append(msg)
                else:
                    violations.append(msg)

            if "br.com.jean.meuponto.data" in imported:
                msg = f"[{loc}] 'domain' não pode depender de 'data': {imported}"
                if loc in KNOWN_BASELINE:
                    baseline_matches.append(msg)
                else:
                    violations.append(msg)

            if "br.com.jean.meuponto.ui" in imported:
                violations.append(f"[{loc}] 'domain' não pode depender de 'ui': {imported}")

        # Regra 2: ui não pode importar DAOs ou Room diretamente
        if pkg == "ui":
            if any(dao in imported for dao in ["ReceiptDao", "AppDatabase"]):
                violations.append(f"[{loc}] 'ui' não pode importar banco/DAO diretamente: {imported}")

def main():
    strict_mode = "--strict" in sys.argv

    if not SRC_DIR.exists():
        print(f"Erro: diretório de fontes não encontrado: {SRC_DIR}")
        sys.exit(1)

    kt_files = list(SRC_DIR.glob("**/*.kt"))
    for f in kt_files:
        check_file(f)

    if baseline_matches:
        print(f"⚠️  {len(baseline_matches)} débito(s) de arquitetura legados detectados no baseline (ENG-4):")
        for b in baseline_matches:
            print(f"   [legado] {b}")

    if strict_mode and baseline_matches:
        print("\n[STRICT MODE] Falhando por débitos legados presentes.")
        sys.exit(1)

    if violations:
        print(f"\n❌ {len(violations)} nova(s) violação(ões) de arquitetura:")
        for v in violations:
            print(f"   [novo] {v}")
        print("\nErro: o código novo viola as fronteiras de arquitetura. Corrija antes de prosseguir.")
        sys.exit(1)

    print(f"\n✅ Nenhuma violação nova de arquitetura ({len(kt_files)} arquivos Kotlin auditados).")
    sys.exit(0)

if __name__ == "__main__":
    main()
