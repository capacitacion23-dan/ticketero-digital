#!/bin/bash
# =============================================================================
# TICKETERO - Database Failure Test
# =============================================================================
# Simula caída de PostgreSQL y valida que la app maneja la falla
# Usage: ./scripts/resilience/db-failure-test.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TICKETERO - DATABASE FAILURE TEST (RES-02)                 ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Limpiando estado...${NC}"
docker exec ticketero-db psql -U dev -d ticketero -c "
    DELETE FROM mensaje;
    DELETE FROM ticket;
" > /dev/null 2>&1

# Detener PostgreSQL
echo -e "${YELLOW}2. Deteniendo PostgreSQL (30 segundos)...${NC}"
docker stop ticketero-db > /dev/null 2>&1

# Intentar crear tickets mientras DB está caída
echo -e "${YELLOW}3. Intentando crear tickets (DB caída)...${NC}"

ERRORS=0
for i in $(seq 1 5); do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/tickets" \
        -H "Content-Type: application/json" \
        -d "{
            \"nationalId\": \"91000$(printf '%03d' $i)\",
            \"telefono\": \"+56912345678\",
            \"branchOffice\": \"Sucursal Test\",
            \"queueType\": \"CAJA\"
        }")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" != "201" ]; then
        ERRORS=$((ERRORS + 1))
    fi
    
    echo -ne "\r   Intentos: $i/5, Errores: $ERRORS    "
    sleep 2
done

echo ""
echo "   ✓ Errores esperados durante caída DB: $ERRORS/5"

# Reiniciar PostgreSQL
echo -e "${YELLOW}4. Reiniciando PostgreSQL...${NC}"
docker start ticketero-db > /dev/null 2>&1

# Esperar que PostgreSQL esté listo
sleep 15
echo "   ✓ PostgreSQL reiniciado"

# Verificar que la app se recupera
echo -e "${YELLOW}5. Verificando recovery de la aplicación...${NC}"
MAX_WAIT=60
WAITED=0
RECOVERED=false

while [ $WAITED -lt $MAX_WAIT ]; do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/tickets" \
        -H "Content-Type: application/json" \
        -d '{
            "nationalId": "91999999",
            "telefono": "+56912345678",
            "branchOffice": "Sucursal Test",
            "queueType": "CAJA"
        }')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "201" ]; then
        RECOVERY_TIME=$WAITED
        RECOVERED=true
        echo "   ✓ App recuperada en ${RECOVERY_TIME}s"
        break
    fi
    
    echo -ne "\r   Esperando recovery... ${WAITED}s    "
    sleep 5
    WAITED=$((WAITED + 5))
done

echo ""

# Validar resultados
echo -e "${YELLOW}6. Validando resultados...${NC}"
echo ""

PASS=true

# Check: Errores durante caída
if [ "$ERRORS" -ge 3 ]; then
    echo -e "   - Errores durante caída: ${GREEN}PASS${NC} ($ERRORS/5 fallaron como esperado)"
else
    echo -e "   - Errores durante caída: ${YELLOW}WARN${NC} ($ERRORS/5 fallaron)"
fi

# Check: Recovery exitoso
if [ "$RECOVERED" = true ]; then
    echo -e "   - Recovery exitoso: ${GREEN}PASS${NC}"
else
    echo -e "   - Recovery exitoso: ${RED}FAIL${NC} (timeout)"
    PASS=false
fi

# Check: Tiempo de recovery
if [ "$RECOVERED" = true ] && [ "$RECOVERY_TIME" -lt 30 ]; then
    echo -e "   - Recovery < 30s: ${GREEN}PASS${NC} (${RECOVERY_TIME}s)"
else
    echo -e "   - Recovery < 30s: ${YELLOW}WARN${NC} (${RECOVERY_TIME:-timeout}s)"
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "  ${GREEN}✅ DATABASE FAILURE TEST PASSED${NC}"
    echo "  Aplicación maneja fallas de DB correctamente"
    exit 0
else
    echo -e "  ${RED}❌ DATABASE FAILURE TEST FAILED${NC}"
    exit 1
fi