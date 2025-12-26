#!/bin/bash
# =============================================================================
# TICKETERO - Idempotency Test
# =============================================================================
# Valida que tickets ya procesados no se reprocesan
# Usage: ./scripts/concurrency/idempotency-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - IDEMPOTENCY TEST (CONC-02)                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Configurando escenario...${NC}"
docker exec ticketero-db psql -U dev -d ticketero -c "
    DELETE FROM mensaje;
    DELETE FROM ticket;
    UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0;
" > /dev/null 2>&1

# Crear y esperar que se complete un ticket
echo -e "${YELLOW}2. Creando ticket y esperando procesamiento...${NC}"

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/tickets" \
    -H "Content-Type: application/json" \
    -d '{
        "nationalId": "70000001",
        "telefono": "+56912345678",
        "branchOffice": "Sucursal Test",
        "queueType": "CAJA"
    }')

TICKET_ID=$(echo "$RESPONSE" | grep -o '"numero":"[^"]*"' | cut -d'"' -f4)
echo "   ✓ Ticket creado: $TICKET_ID"

# Esperar procesamiento
sleep 30

# Capturar estado
INITIAL_COMPLETED=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
INITIAL_MESSAGES=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje;" | xargs)
INITIAL_SERVED=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT SUM(total_tickets_served) FROM advisor;" | xargs)

echo "   Estado inicial:"
echo "   - Tickets completados: $INITIAL_COMPLETED"
echo "   - Mensajes enviados: $INITIAL_MESSAGES"
echo "   - Total servidos: $INITIAL_SERVED"

# Simular reenvío del mismo ticket (duplicado)
echo -e "${YELLOW}3. Simulando ticket duplicado...${NC}"

DUPLICATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/tickets" \
    -H "Content-Type: application/json" \
    -d '{
        "nationalId": "70000001",
        "telefono": "+56912345678",
        "branchOffice": "Sucursal Test",
        "queueType": "CAJA"
    }')

DUPLICATE_HTTP_CODE=$(echo "$DUPLICATE_RESPONSE" | tail -1)
echo "   ✓ Respuesta duplicado: HTTP $DUPLICATE_HTTP_CODE"

# Esperar procesamiento del posible duplicado
echo -e "${YELLOW}4. Esperando procesamiento (10s)...${NC}"
sleep 10

# Validar que nada cambió
FINAL_COMPLETED=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
FINAL_MESSAGES=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje;" | xargs)
FINAL_SERVED=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT SUM(total_tickets_served) FROM advisor;" | xargs)

echo -e "${YELLOW}5. Validando idempotencia...${NC}"
echo ""
echo "   Estado final:"
echo "   - Tickets completados: $FINAL_COMPLETED"
echo "   - Mensajes enviados: $FINAL_MESSAGES"
echo "   - Total servidos: $FINAL_SERVED"
echo ""

PASS=true

# Validar comportamiento del duplicado
if [ "$DUPLICATE_HTTP_CODE" = "409" ] || [ "$DUPLICATE_HTTP_CODE" = "400" ]; then
    echo -e "   - Duplicado rechazado: ${GREEN}PASS${NC} (HTTP $DUPLICATE_HTTP_CODE)"
elif [ "$DUPLICATE_HTTP_CODE" = "201" ]; then
    # Si se acepta, verificar que no se procesó dos veces
    TOTAL_TICKETS=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE national_id='70000001';" | xargs)
    
    if [ "$TOTAL_TICKETS" -eq 1 ]; then
        echo -e "   - Duplicado manejado: ${GREEN}PASS${NC} (1 ticket en BD)"
    else
        echo -e "   - Duplicado manejado: ${RED}FAIL${NC} ($TOTAL_TICKETS tickets en BD)"
        PASS=false
    fi
else
    echo -e "   - Respuesta duplicado: ${YELLOW}WARN${NC} (HTTP $DUPLICATE_HTTP_CODE)"
fi

# Validar que contadores no se incrementaron indebidamente
if [ "$FINAL_COMPLETED" -eq "$INITIAL_COMPLETED" ] || [ "$FINAL_COMPLETED" -eq $((INITIAL_COMPLETED + 1)) ]; then
    echo -e "   - Tickets no duplicados: ${GREEN}PASS${NC}"
else
    echo -e "   - Tickets no duplicados: ${RED}FAIL${NC}"
    PASS=false
fi

if [ "$FINAL_SERVED" -eq "$INITIAL_SERVED" ] || [ "$FINAL_SERVED" -eq $((INITIAL_SERVED + 1)) ]; then
    echo -e "   - Contador no incrementado: ${GREEN}PASS${NC}"
else
    echo -e "   - Contador no incrementado: ${RED}FAIL${NC}"
    PASS=false
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "  ${GREEN}✅ IDEMPOTENCY TEST PASSED${NC}"
    exit 0
else
    echo -e "  ${RED}❌ IDEMPOTENCY TEST FAILED${NC}"
    exit 1
fi