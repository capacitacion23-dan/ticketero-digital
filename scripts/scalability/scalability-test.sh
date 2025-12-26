#!/bin/bash
# =============================================================================
# TICKETERO - Scalability Test
# =============================================================================
# Mide performance bajo diferentes niveles de carga
# Usage: ./scripts/scalability/scalability-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - SCALABILITY TEST (SCAL-01)                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RESULTS_FILE="$PROJECT_ROOT/results/scalability-results-$(date +%Y%m%d-%H%M%S).csv"
mkdir -p "$PROJECT_ROOT/results"

# Test scenarios: different concurrent users
SCENARIOS=(1 5 10 20)
TICKETS_PER_SCENARIO=10

echo "concurrent_users,total_tickets,duration_seconds,throughput_per_min,success_rate" > "$RESULTS_FILE"

echo -e "${YELLOW}Ejecutando tests de escalabilidad...${NC}"
echo ""

for USERS in "${SCENARIOS[@]}"; do
    echo -e "${YELLOW}Escenario: $USERS usuarios concurrentes${NC}"
    
    START_TIME=$(date +%s)
    SUCCESS=0
    
    # Create tickets concurrently
    echo "   Creando $TICKETS_PER_SCENARIO tickets con $USERS usuarios..."
    
    for ((i=1; i<=TICKETS_PER_SCENARIO; i++)); do
        (
            NATIONAL_ID="SCAL$(printf '%02d' $USERS)$(printf '%03d' $i)"
            QUEUE_INDEX=$((i % 4))
            QUEUES=("CAJA" "PERSONAL" "EMPRESAS" "GERENCIA")
            QUEUE=${QUEUES[$QUEUE_INDEX]}
            
            RESPONSE=$(curl -s -w "%{http_code}" -X POST "http://localhost:8080/api/tickets" \
                -H "Content-Type: application/json" \
                -d "{
                    \"nationalId\": \"${NATIONAL_ID}\",
                    \"telefono\": \"+56912345678\",
                    \"branchOffice\": \"Sucursal Test\",
                    \"queueType\": \"${QUEUE}\"
                }")
            
            HTTP_CODE=$(echo "$RESPONSE" | tail -c 4)
            
            if [ "$HTTP_CODE" = "201" ]; then
                echo "1" >> "/tmp/success_${USERS}.txt"
            fi
        ) &
        
        # Control concurrency
        if (( i % USERS == 0 )); then
            wait
        fi
    done
    
    wait
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    # Calculate metrics
    if [ -f "/tmp/success_${USERS}.txt" ]; then
        SUCCESS=$(wc -l < "/tmp/success_${USERS}.txt")
        rm -f "/tmp/success_${USERS}.txt"
    fi
    
    THROUGHPUT=$(echo "scale=1; $SUCCESS * 60 / $DURATION" | bc)
    SUCCESS_RATE=$(echo "scale=1; $SUCCESS * 100 / $TICKETS_PER_SCENARIO" | bc)
    
    # Record results
    echo "$USERS,$TICKETS_PER_SCENARIO,$DURATION,$THROUGHPUT,$SUCCESS_RATE" >> "$RESULTS_FILE"
    
    echo "   ✓ Completado: ${THROUGHPUT} tickets/min, ${SUCCESS_RATE}% éxito"
    echo ""
    
    sleep 2
done

# Generate summary
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  RESULTADOS SCALABILITY TEST${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Usuarios | Throughput | Éxito | Estado"
echo "---------|------------|-------|--------"

while IFS=',' read -r users tickets duration throughput success_rate; do
    if [[ "$users" != "concurrent_users" ]]; then
        if (( $(echo "$throughput >= 30" | bc -l) )) && (( $(echo "$success_rate >= 95" | bc -l) )); then
            STATUS="${GREEN}PASS${NC}"
        else
            STATUS="${RED}FAIL${NC}"
        fi
        printf "%-8s | %-10s | %-5s | %s\n" "$users" "${throughput} t/min" "${success_rate}%" "$STATUS"
    fi
done < "$RESULTS_FILE"

echo ""
echo "📁 Resultados: $RESULTS_FILE"

# Overall result
LAST_THROUGHPUT=$(tail -1 "$RESULTS_FILE" | cut -d',' -f4)
LAST_SUCCESS=$(tail -1 "$RESULTS_FILE" | cut -d',' -f5)

if (( $(echo "$LAST_THROUGHPUT >= 30" | bc -l) )) && (( $(echo "$LAST_SUCCESS >= 95" | bc -l) )); then
    echo -e "${GREEN}✅ SCALABILITY TEST PASSED${NC}"
    exit 0
else
    echo -e "${RED}❌ SCALABILITY TEST FAILED${NC}"
    exit 1
fi