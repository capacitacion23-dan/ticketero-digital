# =============================================================================
# TICKETERO - Simple Load Test (PowerShell)
# =============================================================================
# Ejecuta un test básico de carga para validar el sistema
# Usage: .\scripts\simple-load-test.ps1
# =============================================================================

Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        TICKETERO - SIMPLE LOAD TEST                         ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Verificar que la aplicación esté disponible
Write-Host "1. Verificando disponibilidad de la aplicación..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
    if ($healthCheck.status -eq "UP") {
        Write-Host "   ✓ Aplicación disponible" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Aplicación no disponible" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ✗ Error conectando a la aplicación: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Crear algunos tickets de prueba
Write-Host "2. Creando tickets de prueba..." -ForegroundColor Yellow
$queues = @("CAJA", "PERSONAL", "EMPRESAS", "GERENCIA")
$createdTickets = 0
$errors = 0

for ($i = 1; $i -le 10; $i++) {
    $queue = $queues[($i - 1) % 4]
    $nationalId = "100000{0:D3}" -f $i
    
    $body = @{
        nationalId = $nationalId
        telefono = "+56912345678"
        branchOffice = "Sucursal Test"
        queueType = $queue
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/tickets" -Method Post -Body $body -ContentType "application/json"
        $createdTickets++
        Write-Host "   ✓ Ticket $i creado: $($response.numero)" -ForegroundColor Green
    } catch {
        $errors++
        Write-Host "   ✗ Error creando ticket $i : $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "3. Esperando procesamiento..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Verificar estado final
Write-Host "4. Verificando resultados..." -ForegroundColor Yellow

# Simular consulta a base de datos (en un entorno real usaríamos la API o conexión directa)
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  RESULTADOS DEL TEST" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Tickets creados:    $createdTickets/10" -ForegroundColor White
Write-Host "  Errores:           $errors" -ForegroundColor White

if ($errors -eq 0 -and $createdTickets -eq 10) {
    Write-Host "  Estado:            PASS" -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ SIMPLE LOAD TEST PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  Estado:            FAIL" -ForegroundColor Red
    Write-Host ""
    Write-Host "❌ SIMPLE LOAD TEST FAILED" -ForegroundColor Red
    exit 1
}