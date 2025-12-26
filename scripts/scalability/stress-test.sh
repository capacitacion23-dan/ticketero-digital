#!/bin/bash
# =============================================================================
# TICKETERO - Stress Test
# =============================================================================
# Identifica el punto de quiebre del sistema
# Usage: ./scripts/scalability/stress-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - STRESS TEST (SCAL-02)                          ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Stress test: incrementar carga hasta encontrar límite
MAX_CONCURRENT=100
TICKETS_PER_BATCH=50
CURRENT_LOAD=10

echo -e "${YELLOW}Iniciando stress test...${NC}"
echo "Objetivo: Encontrar punto de quiebre del sistema"
echo ""

while [ $CURRENT_LOAD -le $MAX_CONCURRENT ]; do
    echo -e "${YELLOW}Probando carga: $CURRENT_LOAD usuarios concurrentes${NC}"
    
    START_TIME=$(date +%s)
    SUCCESS=0
    ERRORS=0
    
    # Create batch of tickets
    for ((i=1; i<=TICKETS_PER_BATCH; i++)); do
        (
            NATIONAL_ID="STRESS$(printf '%03d' $CURRENT_LOAD)$(printf '%03d' $i)"
            
            RESPONSE=$(curl -s -w "%{http_code}" -X POST "http://localhost:8080/api/tickets" \
                -H "Content-Type: application/json" \
                -d "{
                    \"nationalId\": \"${NATIONAL_ID}\",
                    \"telefono\": \"+56912345678\",
                    \"branchOffice\": \"Sucursal Stress\",
                    \"queueType\": \"CAJA\"
                }" 2>/dev/null)
            
            HTTP_CODE=$(echo "$RESPONSE" | tail -c 4)
            
            if [ "$HTTP_CODE" = "201" ]; then
                echo "OK" >> "/tmp/stress_results.txt"
            else
                echo "ERROR" >> "/tmp/stress_results.txt"
            fi
        ) &
        
        # Control concurrency
        if (( i % CURRENT_LOAD == 0 )); then
            wait
        fi
    done
    
    wait
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    # Analyze results
    if [ -f "/tmp/stress_results.txt" ]; then
        SUCCESS=$(grep -c "OK" "/tmp/stress_results.txt" 2>/dev/null || echo "0")
        ERRORS=$(grep -c "ERROR" "/tmp/stress_results.txt" 2>/dev/null || echo "0")
        rm -f "/tmp/stress_results.txt"
    fi
    
    TOTAL_REQUESTS=$((SUCCESS + ERRORS))
    ERROR_RATE=$(echo "scale=1; $ERRORS * 100 / $TOTAL_REQUESTS" | bc 2>/dev/null || echo "0")
    THROUGHPUT=$(echo "scale=1; $SUCCESS * 60 / $DURATION" | bc 2>/dev/null || echo "0")
    
    echo "   Resultados: $SUCCESS éxitos, $ERRORS errores (${ERROR_RATE}% error rate)"
    echo "   Throughput: ${THROUGHPUT} tickets/min"
    
    # Check if system is breaking
    if (( $(echo "$ERROR_RATE > 10" | bc -l) )) || (( $(echo "$THROUGHPUT < 20" | bc -l) )); then
        echo -e "   ${RED}⚠️  Sistema bajo estrés significativo${NC}"
        
        if (( $(echo "$ERROR_RATE > 25" | bc -l) )); then
            echo -e "   ${RED}🔥 PUNTO DE QUIEBRE ENCONTRADO${NC}"
            echo ""
            echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
            echo -e "${CYAN}  RESULTADOS STRESS TEST${NC}"
            echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
            echo ""
            echo "  Límite del sistema: ~$((CURRENT_LOAD - 10)) usuarios concurrentes"
            echo "  Error rate crítico: ${ERROR_RATE}%"
            echo "  Throughput degradado: ${THROUGHPUT} tickets/min"
            echo ""
            echo -e "${YELLOW}⚠️  STRESS TEST COMPLETED - LÍMITE IDENTIFICADO${NC}"
            exit 0
        fi
    else
        echo -e "   ${GREEN}✅ Sistema estable${NC}"
    fi
    
    echo ""
    
    # Increment load
    CURRENT_LOAD=$((CURRENT_LOAD + 10))
    
    # Brief recovery time
    sleep 5
done

echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  RESULTADOS STRESS TEST${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "  Sistema soportó hasta: $MAX_CONCURRENT usuarios concurrentes"
echo "  Sin degradación significativa detectada"
echo ""
echo -e "${GREEN}✅ STRESS TEST PASSED - SISTEMA ROBUSTO${NC}"