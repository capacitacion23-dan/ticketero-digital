// =============================================================================
// TICKETERO - K6 Stress Test
// =============================================================================
// Test de estrés con ramping de usuarios
// Usage: k6 run k6/stress-test.js
// =============================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const ticketsCreated = new Counter('tickets_created');
const ticketErrors = new Rate('ticket_errors');
const responseTime = new Trend('response_time', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const QUEUES = ['CAJA', 'PERSONAL', 'EMPRESAS', 'GERENCIA'];

export const options = {
    stages: [
        { duration: '30s', target: 5 },   // Ramp up to 5 users
        { duration: '1m', target: 10 },   // Stay at 10 users
        { duration: '30s', target: 20 },  // Ramp up to 20 users
        { duration: '1m', target: 20 },   // Stay at 20 users
        { duration: '30s', target: 50 },  // Stress test: 50 users
        { duration: '2m', target: 50 },   // Maintain stress
        { duration: '30s', target: 0 },   // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<5000'],  // 95% under 5s (stress conditions)
        ticket_errors: ['rate<0.1'],        // Less than 10% errors
        response_time: ['p(90)<3000'],       // 90% under 3s
    },
};

function generateNationalId() {
    return 'STRESS' + Math.floor(100000 + Math.random() * 900000).toString();
}

export default function () {
    const queue = QUEUES[Math.floor(Math.random() * QUEUES.length)];
    
    const payload = JSON.stringify({
        nationalId: generateNationalId(),
        telefono: '+56912345678',
        branchOffice: 'Sucursal Stress',
        queueType: queue,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        timeout: '10s',
    };

    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/tickets`, payload, params);
    const duration = Date.now() - startTime;

    responseTime.add(duration);

    const success = check(response, {
        'status is 201': (r) => r.status === 201,
        'response time < 10s': (r) => r.timings.duration < 10000,
        'has ticket number': (r) => {
            try {
                const json = r.json();
                return json && json.numero !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (success) {
        ticketsCreated.add(1);
    } else {
        ticketErrors.add(1);
        console.log(`Error: ${response.status} - ${response.body.substring(0, 100)}`);
    }

    // Variable think time based on load
    const currentVUs = __VU;
    const thinkTime = currentVUs > 30 ? 0.5 : 1; // Faster requests under high load
    sleep(Math.random() * thinkTime + 0.5);
}

export function handleSummary(data) {
    const errorRate = data.metrics.ticket_errors?.values.rate || 0;
    const avgDuration = data.metrics.http_req_duration?.values.avg || 0;
    const p95Duration = data.metrics.http_req_duration?.values['p(95)'] || 0;
    const totalRequests = data.metrics.http_reqs?.values.count || 0;
    const successfulTickets = data.metrics.tickets_created?.values.count || 0;

    let status = 'PASS';
    if (errorRate > 0.1 || p95Duration > 5000) {
        status = 'FAIL';
    }

    const summary = `
═══════════════════════════════════════════════════════════════
  TICKETERO - K6 STRESS TEST RESULTS
═══════════════════════════════════════════════════════════════

  📊 MÉTRICAS GENERALES:
  ─────────────────────────────────────────────
  Total Requests:      ${totalRequests}
  Successful Tickets:  ${successfulTickets}
  Error Rate:          ${(errorRate * 100).toFixed(2)}%
  
  ⏱️  LATENCIA:
  ─────────────────────────────────────────────
  Average:             ${avgDuration.toFixed(0)}ms
  p95:                 ${p95Duration.toFixed(0)}ms
  p99:                 ${(data.metrics.http_req_duration?.values['p(99)'] || 0).toFixed(0)}ms
  
  🎯 RESULTADO:         ${status}
  
  💡 ANÁLISIS:
  ${errorRate > 0.05 ? '⚠️  Alta tasa de errores detectada' : '✅ Tasa de errores aceptable'}
  ${p95Duration > 3000 ? '⚠️  Latencia alta bajo estrés' : '✅ Latencia aceptable'}
  ${successfulTickets < totalRequests * 0.9 ? '⚠️  Baja tasa de éxito' : '✅ Alta tasa de éxito'}

═══════════════════════════════════════════════════════════════
`;

    return {
        'stdout': summary,
        'results/k6-stress-summary.json': JSON.stringify(data, null, 2),
    };
}