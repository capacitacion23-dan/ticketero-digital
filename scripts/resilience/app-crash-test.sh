#!/bin/bash
# =============================================================================
# TICKETERO - Application Crash Test
# =============================================================================
# Simula crash de aplicación y valida recovery
# Usage: ./scripts/resilience/app-crash-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - APPLICATION CRASH TEST (RES-01)                ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Configurando escenario...${NC}"
docker exec ticketero-db psql -U dev -d ticketero -c "
    DELETE FROM mensaje;
    DELETE FROM ticket;
    UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0;
" > /dev/null 2>&1

# Crear tickets
echo -e "${YELLOW}2. Creando tickets...${NC}"
for i in $(seq 1 3); do
    curl -s -X POST "http://localhost:8080/api/tickets" \
        -H "Content-Type: application/json" \
        -d "{
            \"nationalId\": \"90000$(printf '%03d' $i)\",
            \"telefono\": \"+56912345678\",
            \"branchOffice\": \"Sucursal Test\",
            \"queueType\": \"CAJA\"
        }" > /dev/null
done

echo "   ✓ 3 tickets creados"

# Esperar que algunos estén en procesamiento
sleep 5

# Capturar estado antes del crash
BEFORE_WAITING=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='WAITING';" | xargs)
BEFORE_IN_PROGRESS=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status IN ('CALLED', 'IN_PROGRESS');" | xargs)
BEFORE_BUSY=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM advisor WHERE status='BUSY';" | xargs)

echo "   Estado antes del crash:"
echo "   - WAITING: $BEFORE_WAITING"
echo "   - IN_PROGRESS: $BEFORE_IN_PROGRESS"
echo "   - Advisors BUSY: $BEFORE_BUSY"

# Simular crash
echo -e "${YELLOW}3. Simulando crash de aplicación...${NC}"
START_TIME=$(date +%s)

docker stop ticketero-api > /dev/null 2>&1
echo "   ✓ Aplicación detenida (simulando crash)"

# Esperar un momento
sleep 10

# Reiniciar aplicación
echo -e "${YELLOW}4. Reiniciando aplicación...${NC}"
docker start ticketero-api > /dev/null 2>&1

# Esperar que la app vuelva a estar disponible
echo -e "${YELLOW}5. Esperando que la app esté disponible...${NC}"
MAX_WAIT=90
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        RESTART_TIME=$(($(date +%s) - START_TIME))
        echo "   ✓ App disponible en ${RESTART_TIME}s"
        break
    fi
    
    echo -ne "\r   Esperando... ${WAITED}s    "
    sleep 5
    WAITED=$((WAITED + 5))
done

echo ""

# Esperar procesamiento post-restart
sleep 30

# Validar estado después del restart
echo -e "${YELLOW}6. Validando estado post-restart...${NC}"
echo ""

AFTER_BUSY=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM advisor WHERE status='BUSY';" | xargs)
AFTER_COMPLETED=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
TOTAL_TICKETS=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket;" | xargs)

echo "   Estado post-restart:"
echo "   - Advisors BUSY: $AFTER_BUSY"
echo "   - Tickets COMPLETED: $AFTER_COMPLETED"
echo "   - Total tickets: $TOTAL_TICKETS"
echo ""

PASS=true

# Check 1: Advisors liberados (o procesando normalmente)
if [ "$AFTER_BUSY" -le 1 ]; then
    echo -e "   - Advisors liberados: ${GREEN}PASS${NC}"
else
    echo -e "   - Advisors liberados: ${YELLOW}WARN${NC} ($AFTER_BUSY BUSY)"
fi

# Check 2: No se perdieron tickets
if [ "$TOTAL_TICKETS" -eq 3 ]; then
    echo -e "   - Tickets preservados: ${GREEN}PASS${NC}"
else
    echo -e "   - Tickets preservados: ${RED}FAIL${NC} ($TOTAL_TICKETS/3)"
    PASS=false
fi

# Check 3: App disponible rápido
if [ "${RESTART_TIME:-999}" -lt 60 ]; then
    echo -e "   - Restart < 60s: ${GREEN}PASS${NC}"
else
    echo -e "   - Restart < 60s: ${RED}FAIL${NC}"
    PASS=false
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "  ${GREEN}✅ APPLICATION CRASH TEST PASSED${NC}"
    exit 0
else
    echo -e "  ${RED}❌ APPLICATION CRASH TEST FAILED${NC}"
    exit 1
fi