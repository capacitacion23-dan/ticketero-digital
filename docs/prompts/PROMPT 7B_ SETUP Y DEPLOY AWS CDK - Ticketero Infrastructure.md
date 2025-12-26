# **PROMPT 7B: SETUP Y DEPLOY AWS CDK \- Ticketero Infrastructure**

## **Contexto**

Eres un DevOps Engineer configurando el entorno AWS CDK para desplegar la infraestructura del sistema Ticketero.

**IMPORTANTE:** Ejecuta cada paso secuencialmente. DETENTE y solicita confirmación antes de ejecutar `cdk deploy`.

---

## **Tu Tarea: 5 Pasos**

### **PASO 1: Verificar Prerrequisitos**

\# Ejecutar todos y reportar versiones  
java \--version      \# Requiere: 21+  
mvn \--version       \# Requiere: 3.8+  
node \--version      \# Requiere: 18+  
docker info         \# Debe estar corriendo  
aws \--version       \# Requiere: v2

**Si falta alguno:** Indicar cuál instalar antes de continuar.

---

### **PASO 2: Configurar AWS CLI**

\# 1\. Configurar credenciales  
aws configure  
\# → AWS Access Key ID: \[solicitar al usuario\]  
\# → Secret Access Key: \[solicitar al usuario\]  
\# → Default region: us-east-1  
\# → Default output: json

\# 2\. Verificar identidad  
aws sts get-caller-identity

**Guardar:** El valor de `Account` (12 dígitos) para el siguiente paso.

---

### **PASO 3: Instalar CDK y Variables**

\# Instalar CDK CLI  
npm install \-g aws-cdk@2.170.0

\# Configurar variables (CRÍTICO)  
export CDK\_DEFAULT\_ACCOUNT=\<account-id-12-digitos\>  
export CDK\_DEFAULT\_REGION=us-east-1

\# Verificar  
cdk \--version  
echo $CDK\_DEFAULT\_ACCOUNT

---

### **PASO 4: Bootstrap y Validar**

cd ticketero-infra

\# Bootstrap (una vez por cuenta/región)  
cdk bootstrap aws://$CDK\_DEFAULT\_ACCOUNT/$CDK\_DEFAULT\_REGION

\# Validar síntesis  
cdk synth

\# Ver recursos a crear  
cdk diff

**🔍 PUNTO DE REVISIÓN:**

✅ PASO 4 COMPLETADO

Validaciones:  
\- cdk synth: ✅ CloudFormation generado  
\- cdk diff: X recursos a crear

⚠️ ADVERTENCIA DE COSTOS:  
Costo estimado: \~$90-120 USD/mes en ambiente dev

🔍 SOLICITO CONFIRMACIÓN:  
¿Procedo con el deploy? (sí/no)

⏸️ ESPERANDO CONFIRMACIÓN...

---

### **PASO 5: Deploy (Solo con Confirmación)**

\# SOLO ejecutar si el usuario confirmó  
cdk deploy \--all \--require-approval broadening

**Tiempo estimado:** 15-20 minutos

**Al completar:** Mostrar outputs (endpoints, URLs).

---

## **Troubleshooting Rápido**

| Error | Solución |
| ----- | ----- |
| `CDK_DEFAULT_ACCOUNT not set` | Ejecutar `export CDK_DEFAULT_ACCOUNT=...` |
| `Bootstrap required` | Ejecutar `cdk bootstrap` primero |
| `Docker not running` | Iniciar Docker Desktop |
| `Credentials expired` | Re-ejecutar `aws configure` |

---

## **Comandos Útiles Post-Deploy**

\# Ver stacks desplegados  
cdk list

\# Destruir infraestructura (¡cuidado\!)  
cdk destroy \--all

