# Guía de Deployment - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**DevOps Engineer:** DevOps Engineer Senior

---

## 1. Estrategia de Deployment

### 1.1 Enfoque de Infraestructura como Código

El Sistema Ticketero utiliza **AWS CDK (Cloud Development Kit) con Java** para definir y desplegar toda la infraestructura de manera declarativa y versionada.

**Principios:**
- **Infraestructura como Código:** Todo recurso AWS definido en código Java
- **Ambientes Separados:** Dev y Prod completamente aislados
- **Deployment Automatizado:** Un comando para desplegar toda la infraestructura
- **Rollback Seguro:** Capacidad de revertir cambios mediante CloudFormation
- **Configuración Externalizada:** Variables por ambiente usando records Java

### 1.2 Arquitectura de Deployment

```
┌─────────────────────────────────────────────────────────┐
│                    VPC 10.0.0.0/16                     │
│                                                         │
│  ┌─────────────┐     ┌─────────────┐                   │
│  │  Public     │     │  Public     │                   │
│  │  Subnet A   │     │  Subnet B   │                   │
│  │ 10.0.1.0/24 │     │ 10.0.2.0/24 │                   │
│  └──────┬──────┘     └──────┬──────┘                   │
│         │ NAT               │                          │
│         ▼                   ▼                          │
│  ┌─────────────┐     ┌─────────────┐                   │
│  │  Private    │     │  Private    │                   │
│  │  Subnet A   │     │  Subnet B   │                   │
│  │ 10.0.11.0/24│     │ 10.0.12.0/24│                   │
│  └─────────────┘     └─────────────┘                   │
│         │                   │                          │
│         ▼                   ▼                          │
│  ┌───────────┐       ┌───────────┐                     │
│  │    RDS    │       │ Amazon MQ │                     │
│  │ PostgreSQL│       │ RabbitMQ  │                     │
│  └───────────┘       └───────────┘                     │
└─────────────────────────────────────────────────────────┘

Internet ──► ALB ──► ECS Fargate ──► RDS + MQ
```

### 1.3 Componentes de Infraestructura

| Componente | Desarrollo | Producción |
|------------|------------|------------|
| **VPC** | 10.0.0.0/16 | 10.0.0.0/16 |
| **NAT Gateways** | 1 | 2 (HA) |
| **RDS PostgreSQL** | t3.micro, Single-AZ | t3.small, Multi-AZ |
| **Amazon MQ** | mq.t3.micro | mq.t3.micro |
| **ECS Tasks** | 1 (min=1, max=2) | 2 (min=2, max=4) |
| **CloudWatch Alarms** | 0 | 4 |
| **Costo Estimado/mes** | ~$110 USD | ~$210 USD |

---

## 2. Prerrequisitos

### 2.1 Software Requerido

**Verificar versiones:**
```bash
java --version      # Requiere: 21+
mvn --version       # Requiere: 3.8+
node --version      # Requiere: 18+
docker info         # Debe estar corriendo
aws --version       # Requiere: v2
```

**Si falta alguno:** Instalar antes de continuar.

### 2.2 Configuración AWS CLI

```bash
# 1. Configurar credenciales
aws configure
# → AWS Access Key ID: [solicitar al usuario]
# → Secret Access Key: [solicitar al usuario]
# → Default region: us-east-1
# → Default output: json

# 2. Verificar identidad
aws sts get-caller-identity
```

**Guardar:** El valor de `Account` (12 dígitos) para el siguiente paso.

### 2.3 Instalar CDK

```bash
# Instalar CDK CLI
npm install -g aws-cdk@2.170.0

# Configurar variables (CRÍTICO)
export CDK_DEFAULT_ACCOUNT=<account-id-12-digitos>
export CDK_DEFAULT_REGION=us-east-1

# Verificar
cdk --version
echo $CDK_DEFAULT_ACCOUNT
```

---

## 3. Estructura del Proyecto CDK

### 3.1 Árbol de Archivos

```
ticketero-infra/
├── src/main/java/com/example/infra/
│   ├── TicketeroApp.java              # Entry point
│   ├── TicketeroStack.java            # Stack principal
│   ├── constructs/
│   │   ├── NetworkingConstruct.java   # VPC, subnets, SGs
│   │   ├── DatabaseConstruct.java     # RDS PostgreSQL
│   │   ├── MessagingConstruct.java    # Amazon MQ + Secrets
│   │   ├── ContainerConstruct.java    # ECR, ECS, Fargate
│   │   └── MonitoringConstruct.java   # CloudWatch
│   └── config/
│       └── EnvironmentConfig.java     # Configuración por ambiente
├── src/test/java/com/example/infra/
│   └── TicketeroStackTest.java        # Tests de infraestructura
├── cdk.json
└── pom.xml
```

### 3.2 Configuración por Ambiente

**EnvironmentConfig.java** define configuraciones inmutables:

```java
public record EnvironmentConfig(
    String envName,
    String vpcCidr,
    int natGateways,
    int desiredCount,
    // ... más configuraciones
) {
    public static EnvironmentConfig dev() {
        return new EnvironmentConfig(
            "dev",
            "10.0.0.0/16",
            1,              // 1 NAT Gateway
            1,              // desired tasks
            // ... configuración optimizada para desarrollo
        );
    }
    
    public static EnvironmentConfig prod() {
        return new EnvironmentConfig(
            "prod", 
            "10.0.0.0/16",
            2,              // 2 NAT Gateways (HA)
            2,              // desired tasks
            // ... configuración optimizada para producción
        );
    }
}
```

---

## 4. Proceso de Dry-Run

### 4.1 Bootstrap y Validación

```bash
cd ticketero-infra

# Bootstrap (una vez por cuenta/región)
cdk bootstrap aws://$CDK_DEFAULT_ACCOUNT/$CDK_DEFAULT_REGION

# Validar síntesis
cdk synth

# Ver recursos a crear
cdk diff
```

### 4.2 Tests de Infraestructura

```bash
# Ejecutar tests unitarios
mvn test

# Verificar que los tests pasan
# Tests run: 2, Failures: 0
```

### 4.3 Validaciones Pre-Deploy

**Checklist de validación:**

- [ ] `cdk synth`: ✅ CloudFormation generado sin errores
- [ ] `cdk diff`: Muestra recursos a crear
- [ ] `mvn test`: Tests de infraestructura pasan
- [ ] Variables de entorno configuradas (`CDK_DEFAULT_ACCOUNT`, `CDK_DEFAULT_REGION`)
- [ ] Credenciales AWS válidas
- [ ] Docker corriendo (para build de imágenes)

### 4.4 Estimación de Costos

**⚠️ ADVERTENCIA DE COSTOS:**

| Ambiente | Costo Estimado/mes |
|----------|-------------------|
| **Desarrollo** | ~$90-120 USD |
| **Producción** | ~$180-250 USD |

**Principales componentes de costo:**
- RDS PostgreSQL: ~$15-30/mes
- Amazon MQ: ~$15/mes
- NAT Gateway: ~$45/mes (dev), ~$90/mes (prod)
- ECS Fargate: ~$15-30/mes
- ALB: ~$20/mes

---

## 5. Proceso de Deploy Real

### 5.1 Deploy de Ambiente de Desarrollo

```bash
# 1. Deploy infraestructura
cdk deploy ticketero-dev --require-approval broadening

# Tiempo estimado: 15-20 minutos
```

### 5.2 Build y Push de Imagen Docker

```bash
# 2. Obtener URI del ECR
ECR_URI=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

# 3. Login a ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI

# 4. Build y push imagen
docker build -t $ECR_URI:latest ../
docker push $ECR_URI:latest
```

### 5.3 Configuración de Secrets

```bash
# 5. Actualizar Telegram Bot Token
aws secretsmanager put-secret-value \
  --secret-id ticketero-dev-telegram \
  --secret-string '{"token":"YOUR_REAL_TELEGRAM_BOT_TOKEN"}'
```

### 5.4 Deployment de Aplicación

```bash
# 6. Forzar nuevo deployment
aws ecs update-service \
  --cluster ticketero-dev-cluster \
  --service ticketero-dev-service \
  --force-new-deployment

# 7. Esperar estabilización
aws ecs wait services-stable \
  --cluster ticketero-dev-cluster \
  --services ticketero-dev-service
```

### 5.5 Validación Post-Deploy

```bash
# 8. Obtener DNS del Load Balancer
ALB_DNS=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text)

# 9. Validar health check
curl http://$ALB_DNS/actuator/health
# Esperado: {"status":"UP"}

# 10. Validar API funcional
curl -X POST http://$ALB_DNS/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId":"12345678-9",
    "telefono":"+56912345678",
    "branchOffice":"Centro",
    "queueType":"CAJA"
  }'
# Esperado: 201 Created
```

---

## 6. Ambientes

### 6.1 Ambiente de Desarrollo

**Características:**
- **Propósito:** Testing, desarrollo, demos
- **Configuración:** Recursos mínimos para reducir costos
- **Disponibilidad:** Single-AZ, sin redundancia
- **Monitoreo:** Logs básicos, sin alarms
- **Costo:** ~$110/mes

**Recursos:**
- VPC con 1 NAT Gateway
- RDS t3.micro, Single-AZ
- ECS: 1 task (min=1, max=2)
- Sin CloudWatch Alarms
- Logs con retención de 7 días

### 6.2 Ambiente de Producción

**Características:**
- **Propósito:** Operación real, clientes finales
- **Configuración:** Alta disponibilidad y performance
- **Disponibilidad:** Multi-AZ, redundancia completa
- **Monitoreo:** Alarms completos, dashboard
- **Costo:** ~$210/mes

**Recursos:**
- VPC con 2 NAT Gateways (HA)
- RDS t3.small, Multi-AZ
- ECS: 2 tasks (min=2, max=4)
- 4 CloudWatch Alarms
- Dashboard de monitoreo
- Logs con retención de 14 días

### 6.3 Deploy a Producción

```bash
# Deploy producción (requiere aprobación manual)
cdk deploy ticketero-prod --require-approval broadening

# Configurar secrets de producción
aws secretsmanager put-secret-value \
  --secret-id ticketero-prod-telegram \
  --secret-string '{"token":"PRODUCTION_TELEGRAM_BOT_TOKEN"}'

# Build y push imagen para producción
ECR_URI_PROD=$(aws cloudformation describe-stacks --stack-name ticketero-prod \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

docker tag $ECR_URI:latest $ECR_URI_PROD:latest
docker push $ECR_URI_PROD:latest
```

---

## 7. Riesgos y Costos

### 7.1 Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| **Costos inesperados** | Media | Alto | Monitoreo de billing, alerts de costo |
| **Falla de NAT Gateway** | Baja | Alto | Multi-AZ en producción |
| **Agotamiento de RDS** | Media | Alto | Monitoring de conexiones, auto-scaling |
| **Falla de deployment** | Media | Medio | Tests automatizados, rollback CDK |
| **Secrets comprometidos** | Baja | Alto | Rotación automática, least privilege |

### 7.2 Estrategias de Mitigación

**Costos:**
- Configurar AWS Budgets con alertas
- Usar Spot instances para desarrollo (futuro)
- Scheduled scaling para reducir costos nocturnos

**Disponibilidad:**
- Multi-AZ en producción
- Health checks configurados
- Auto-scaling basado en CPU

**Seguridad:**
- Secrets Manager para credenciales
- Security Groups con mínimo privilegio
- VPC con subnets privadas

### 7.3 Desglose de Costos Detallado

**Desarrollo (~$110/mes):**
```
RDS t3.micro (Single-AZ):     $15/mes
Amazon MQ t3.micro:           $15/mes
NAT Gateway (1):              $45/mes
ECS Fargate (1 task):         $15/mes
ALB:                          $20/mes
Total:                        ~$110/mes
```

**Producción (~$210/mes):**
```
RDS t3.small (Multi-AZ):      $60/mes
Amazon MQ t3.micro:           $15/mes
NAT Gateway (2):              $90/mes
ECS Fargate (2 tasks):        $30/mes
ALB:                          $20/mes
Total:                        ~$215/mes
```

### 7.4 Optimización de Costos

**Recomendaciones:**
1. **Reserved Instances:** 30-50% descuento para RDS en producción
2. **Scheduled Scaling:** Reducir tasks ECS fuera de horario laboral
3. **Log Retention:** Ajustar retención según necesidades de auditoría
4. **Monitoring:** Configurar alertas de billing para evitar sorpresas

---

## 8. Comandos Útiles

### 8.1 Gestión de Stacks

```bash
# Listar stacks
cdk ls

# Ver CloudFormation generado
cdk synth ticketero-dev

# Ver cambios pendientes
cdk diff ticketero-dev

# Deployar stack
cdk deploy ticketero-dev

# Destruir stack (¡CUIDADO!)
cdk destroy ticketero-dev
```

### 8.2 Monitoreo y Logs

```bash
# Ver logs en tiempo real
aws logs tail /ecs/ticketero-dev-api --follow

# Ver estado del servicio ECS
aws ecs describe-services \
  --cluster ticketero-dev-cluster \
  --services ticketero-dev-service

# Ver métricas de CloudWatch
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=ticketero-dev-service \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### 8.3 Troubleshooting

| Error | Solución |
|-------|----------|
| `CDK_DEFAULT_ACCOUNT not set` | Ejecutar `export CDK_DEFAULT_ACCOUNT=...` |
| `Bootstrap required` | Ejecutar `cdk bootstrap` primero |
| `Docker not running` | Iniciar Docker Desktop |
| `Credentials expired` | Re-ejecutar `aws configure` |
| `Service unhealthy` | Verificar logs: `aws logs tail /ecs/ticketero-dev-api` |

---

## 9. Checklist de Deployment

### 9.1 Pre-Deploy

- [ ] Prerrequisitos instalados (Java 21, Maven, Node, Docker, AWS CLI)
- [ ] AWS CLI configurado con credenciales válidas
- [ ] CDK instalado y variables de entorno configuradas
- [ ] Tests de infraestructura pasan (`mvn test`)
- [ ] `cdk synth` ejecuta sin errores
- [ ] Presupuesto AWS configurado con alertas

### 9.2 Deploy

- [ ] `cdk bootstrap` ejecutado (primera vez)
- [ ] `cdk deploy` completado exitosamente
- [ ] Imagen Docker construida y pusheada a ECR
- [ ] Secrets configurados (Telegram Bot Token)
- [ ] Servicio ECS estable y healthy
- [ ] Health check responde correctamente

### 9.3 Post-Deploy

- [ ] API endpoints responden correctamente
- [ ] Base de datos accesible desde aplicación
- [ ] Mensajes Telegram funcionando
- [ ] Logs visibles en CloudWatch
- [ ] Métricas reportando en CloudWatch (prod)
- [ ] Documentación actualizada con endpoints

---

## 10. Rollback y Recuperación

### 10.1 Rollback de Infraestructura

```bash
# Ver historial de deployments
aws cloudformation describe-stack-events --stack-name ticketero-dev

# Rollback a versión anterior (automático en caso de falla)
# CloudFormation maneja rollback automáticamente si el deploy falla

# Rollback manual (si es necesario)
cdk deploy ticketero-dev --previous-parameters
```

### 10.2 Rollback de Aplicación

```bash
# Rollback a imagen anterior
docker tag $ECR_URI:previous $ECR_URI:latest
docker push $ECR_URI:latest

# Forzar nuevo deployment
aws ecs update-service \
  --cluster ticketero-dev-cluster \
  --service ticketero-dev-service \
  --force-new-deployment
```

### 10.3 Recuperación de Desastres

**Escenarios y procedimientos:**

1. **Falla completa de región:** Redeploy en región secundaria
2. **Corrupción de base de datos:** Restore desde backup automático (7 días)
3. **Compromiso de secrets:** Rotación inmediata desde Secrets Manager
4. **Falla de aplicación:** Rollback a imagen anterior conocida

---

**DOCUMENTO DE DEPLOYMENT COMPLETADO**

**Estadísticas:**
- **Ambientes:** 2 (dev, prod)
- **Recursos AWS:** 15+ tipos diferentes
- **Tiempo de deploy:** 15-20 minutos
- **Costo total:** ~$320/mes (ambos ambientes)
- **Comandos documentados:** 25+

**Este documento está listo para:**
- Ejecución de deployments por DevOps team
- Onboarding de nuevos desarrolladores
- Troubleshooting de problemas de infraestructura
- Estimación y control de costos AWS