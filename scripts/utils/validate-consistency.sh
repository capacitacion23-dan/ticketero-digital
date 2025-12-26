#!/bin/bash
# =============================================================================
# TICKETERO - Consistency Validator
# =============================================================================
# Valida consistencia del sistema después de pruebas de carga
# Usage: ./scripts/utils/validate-consistency.sh
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "═══════════════════════════════════════════════════════════════"
echo "  TICKETERO - VALIDACIÓN DE CONSISTENCIA"
echo "═══════════════════════════════════════════════════════════════"
echo ""

ERRORS=0

# 1. Tickets en estado inconsistente
echo -n "1. Tickets en estado inconsistente... "
INCONSISTENT=$(docker exec ticketero-db psql -U dev -d ticketero -t -c "
    SELECT COUNT(*) FROM ticket t
    WHERE (t.status = 'IN_PROGRESS' AND t.started_at IS NULL)
       OR (t.status = 'COMPLETED' AND t.completed_at IS NULL)
       OR (t.status = 'CALLED' AND t.assigned_advisor_id IS NULL);
" | xargs)

if [ "$INCONSISTENT" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 encontrados)"
else
    echo -e "${RED}FAIL${NC} ($INCONSISTENT encontrados)"
    ERRORS=$((ERRORS + 1))
fi

# 2. Asesores en estado inconsistente
echo -n "2. Asesores BUSY sin ticket activo... "
BUSY_NO_TICKET=$(docker exec ticketero-db psql -U dev -d ticketero -t -c "
    SELECT COUNT(*) FROM advisor a
    WHERE a.status = 'BUSY'
    AND NOT EXISTS (
        SELECT 1 FROM ticket t 
        WHERE t.assigned_advisor_id = a.id 
        AND t.status IN ('CALLED', 'IN_PROGRESS')
    );
" | xargs)

if [ "$BUSY_NO_TICKET" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 encontrados)"
else
    echo -e "${YELLOW}WARN${NC} ($BUSY_NO_TICKET encontrados - recovery pendiente)"
fi

# 3. Tickets duplicados (mismo nationalId + cola en estado activo)
echo -n "3. Tickets potencialmente duplicados... "
DUPLICATES=$(docker exec ticketero-db psql -U dev -d ticketero -t -c "
    SELECT COUNT(*) FROM (
        SELECT national_id, queue_type, COUNT(*) as cnt
        FROM ticket
        WHERE status IN ('WAITING', 'CALLED', 'IN_PROGRESS')
        GROUP BY national_id, queue_type
        HAVING COUNT(*) > 1
    ) dups;
" | xargs)

if [ "$DUPLICATES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 duplicados)"
else
    echo -e "${YELLOW}WARN${NC} ($DUPLICATES posibles duplicados)"
fi

# 4. Conexiones DB abiertas
echo -n "4. Conexiones PostgreSQL... "
DB_CONN=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" | xargs)

if [ "$DB_CONN" -lt 20 ]; then
    echo -e "${GREEN}OK${NC} ($DB_CONN conexiones)"
else
    echo -e "${YELLOW}WARN${NC} ($DB_CONN conexiones - revisar pool)"
fi

# 5. Tickets sin posición en cola
echo -n "5. Tickets WAITING sin posición... "
NO_POSITION=$(docker exec ticketero-db psql -U dev -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='WAITING' AND position_in_queue IS NULL;" | xargs)

if [ "$NO_POSITION" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 sin posición)"
else
    echo -e "${RED}FAIL${NC} ($NO_POSITION sin posición)"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
if [ $ERRORS -eq 0 ]; then
    echo -e "  RESULTADO: ${GREEN}SISTEMA CONSISTENTE${NC}"
else
    echo -e "  RESULTADO: ${RED}$ERRORS ERRORES DE CONSISTENCIA${NC}"
fi
echo "═══════════════════════════════════════════════════════════════"

exit $ERRORS