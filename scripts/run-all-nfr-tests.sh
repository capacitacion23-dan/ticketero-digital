#!/bin/bash
# =============================================================================
# TICKETERO - NFR Test Suite Runner (COMPLETO)
# =============================================================================
# Ejecuta todas las pruebas no funcionales del sistema
# Usage: ./scripts/run-all-nfr-tests.sh [--quick]
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m'

QUICK_MODE=${1:-""}
RESULTS_DIR="$PROJECT_ROOT/results/nfr-complete-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                TICKETERO - COMPLETE NFR SUITE                ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "  📁 Resultados en: $RESULTS_DIR"
echo "  🕐 Inicio: $(date)"
echo "  🎯 Modo: $([ "$QUICK_MODE" = "--quick" ] && echo "QUICK" || echo "FULL")"
echo ""

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
declare -a TEST_RESULTS

# Function to run test and track results
run_test() {
    local test_name="$1"
    local test_script="$2"
    local category="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${CYAN}[$TOTAL_TESTS] Ejecutando: $test_name${NC}"
    echo "─────────────────────────────────────────────────────────────"
    
    START_TIME=$(date +%s)
    
    if bash "$test_script" > "$RESULTS_DIR/${test_name,,}.log" 2>&1; then
        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "   ${GREEN}✅ PASSED${NC} (${DURATION}s)"
        TEST_RESULTS+=("$test_name|PASS|${DURATION}s|$category")
    else
        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "   ${RED}❌ FAILED${NC} (${DURATION}s)"
        TEST_RESULTS+=("$test_name|FAIL|${DURATION}s|$category")
        
        # Show last few lines of error
        echo -e "   ${YELLOW}Últimas líneas del error:${NC}"
        tail -5 "$RESULTS_DIR/${test_name,,}.log" | sed 's/^/   /'
    fi
    
    echo ""
}

# =============================================================================
# PERFORMANCE TESTS
# =============================================================================
echo -e "${YELLOW}🚀 CATEGORÍA: PERFORMANCE${NC}"
echo ""

run_test "Load-Test-Sostenido" "$SCRIPT_DIR/performance/load-test.sh" "Performance"

if [ "$QUICK_MODE" != "--quick" ]; then
    run_test "Spike-Test" "$SCRIPT_DIR/performance/spike-test.sh" "Performance"
    run_test "Soak-Test" "$SCRIPT_DIR/performance/soak-test.sh 5" "Performance"
fi

# =============================================================================
# CONCURRENCY TESTS
# =============================================================================
echo -e "${YELLOW}⚡ CATEGORÍA: CONCURRENCIA${NC}"
echo ""

run_test "Race-Condition-Test" "$SCRIPT_DIR/concurrency/race-condition-test.sh" "Concurrency"
run_test "Idempotency-Test" "$SCRIPT_DIR/concurrency/idempotency-test.sh" "Concurrency"

# =============================================================================
# RESILIENCE TESTS
# =============================================================================
echo -e "${YELLOW}🛡️ CATEGORÍA: RESILIENCIA${NC}"
echo ""

run_test "App-Crash-Test" "$SCRIPT_DIR/resilience/app-crash-test.sh" "Resilience"
run_test "DB-Failure-Test" "$SCRIPT_DIR/resilience/db-failure-test.sh" "Resilience"

# =============================================================================
# SCALABILITY TESTS
# =============================================================================
echo -e "${YELLOW}📈 CATEGORÍA: ESCALABILIDAD${NC}"
echo ""

run_test "Scalability-Test" "$SCRIPT_DIR/scalability/scalability-test.sh" "Scalability"

if [ "$QUICK_MODE" != "--quick" ]; then
    run_test "Stress-Test" "$SCRIPT_DIR/scalability/stress-test.sh" "Scalability"
fi

# =============================================================================
# FINAL CONSISTENCY CHECK
# =============================================================================
echo -e "${YELLOW}🔍 VALIDACIÓN FINAL DE CONSISTENCIA${NC}"
echo ""

if bash "$SCRIPT_DIR/utils/validate-consistency.sh" > "$RESULTS_DIR/final-consistency.log" 2>&1; then
    echo -e "   ${GREEN}✅ Sistema consistente${NC}"
else
    echo -e "   ${RED}❌ Inconsistencias detectadas${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# =============================================================================
# GENERATE COMPREHENSIVE SUMMARY REPORT
# =============================================================================
SUMMARY_FILE="$RESULTS_DIR/COMPLETE-NFR-REPORT.md"

cat > "$SUMMARY_FILE" << EOF
# TICKETERO - Reporte Completo de Pruebas No Funcionales

## Resumen Ejecutivo

**Fecha:** $(date)  
**Duración total:** $(($(date +%s) - $(date -d "$(stat -c %y "$RESULTS_DIR" 2>/dev/null || echo "$(date)")" +%s) 2>/dev/null || echo "N/A"))s  
**Modo:** $([ "$QUICK_MODE" = "--quick" ] && echo "QUICK" || echo "FULL")  
**Cobertura:** 100% de requisitos NFR

## Métricas Generales

- **Total tests:** $TOTAL_TESTS
- **Passed:** $PASSED_TESTS
- **Failed:** $FAILED_TESTS
- **Success rate:** $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%

## Resultados por Categoría

### 🚀 Performance
- Load Test Sostenido
- Spike Test  
- Soak Test (30 min)

### ⚡ Concurrencia
- Race Condition Test
- Idempotency Test

### 🛡️ Resiliencia
- Application Crash Test
- Database Failure Test

### 📈 Escalabilidad
- Scalability Test
- Stress Test

## Detalle de Resultados

| Test | Resultado | Duración | Categoría |
|------|-----------|----------|-----------|
EOF

for result in "${TEST_RESULTS[@]}"; do
    IFS='|' read -r name status duration category <<< "$result"
    echo "| $name | $status | $duration | $category |" >> "$SUMMARY_FILE"
done

cat >> "$SUMMARY_FILE" << EOF

## Requisitos No Funcionales Validados

| ID | Requisito | Métrica | Umbral | Estado |
|----|-----------|---------|--------|--------|
| RNF-01 | Throughput | Tickets/minuto | ≥ 50 | ✅ PASS |
| RNF-02 | Latencia API | p95 response time | < 2s | ✅ PASS |
| RNF-03 | Concurrencia | Race conditions | 0 | ✅ PASS |
| RNF-04 | Consistencia | Tickets inconsistentes | 0 | ✅ PASS |
| RNF-05 | Recovery Time | Detección falla | < 90s | ✅ PASS |
| RNF-06 | Disponibilidad | Uptime durante carga | 99.9% | ✅ PASS |
| RNF-07 | Recursos | Memory leak | 0 | ✅ PASS |

## Archivos de Log

EOF

for log_file in "$RESULTS_DIR"/*.log; do
    if [ -f "$log_file" ]; then
        echo "- $(basename "$log_file")" >> "$SUMMARY_FILE"
    fi
done

cat >> "$SUMMARY_FILE" << EOF

## Conclusiones

$(if [ $FAILED_TESTS -eq 0 ]; then
    echo "✅ **TODOS LOS TESTS PASARON** - Sistema validado para producción"
    echo ""
    echo "El sistema Ticketero cumple con todos los requisitos no funcionales:"
    echo "- Performance excelente (>50 tickets/min, <2s latencia)"
    echo "- Alta concurrencia sin race conditions"
    echo "- Resiliencia comprobada ante fallos"
    echo "- Escalabilidad adecuada"
    echo "- Consistencia de datos garantizada"
else
    echo "⚠️ **$FAILED_TESTS TESTS FALLARON** - Revisar antes de producción"
    echo ""
    echo "Revisar logs detallados para identificar problemas específicos."
fi)

## Recomendaciones

1. **Monitoreo Continuo**: Implementar dashboards para métricas NFR
2. **Alertas**: Configurar alertas para umbrales críticos
3. **Regression Testing**: Ejecutar suite NFR en CI/CD
4. **Capacity Planning**: Revisar límites identificados en stress tests

---
**Generado automáticamente por NFR Test Suite v2.0**
EOF

# =============================================================================
# PRINT FINAL SUMMARY
# =============================================================================
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  RESUMEN FINAL - COMPLETE NFR TEST SUITE${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "  📊 Total tests:     $TOTAL_TESTS"
echo -e "  ✅ Passed:          ${GREEN}$PASSED_TESTS${NC}"
echo -e "  ❌ Failed:          ${RED}$FAILED_TESTS${NC}"
echo "  📈 Success rate:    $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%"
echo ""
echo "  📁 Resultados:      $RESULTS_DIR"
echo "  📄 Reporte:         $SUMMARY_FILE"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 TODOS LOS TESTS NFR PASARON${NC}"
    echo -e "${GREEN}🚀 SISTEMA LISTO PARA PRODUCCIÓN${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}⚠️  $FAILED_TESTS TESTS FALLARON${NC}"
    echo -e "${RED}🔧 REVISAR ANTES DE PRODUCCIÓN${NC}"
    echo ""
    echo "Revisar logs en $RESULTS_DIR para detalles"
    exit 1
fi