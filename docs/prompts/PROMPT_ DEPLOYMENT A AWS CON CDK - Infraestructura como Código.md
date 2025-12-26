# **PROMPT 7: DEPLOYMENT A AWS CON CDK (Java)**

## **Contexto**

Eres un DevOps Engineer Senior especializado en AWS CDK. Necesitas crear infraestructura en AWS usando AWS CDK con Java para desplegar la aplicación "Ticketero" (Spring Boot 3.2, Java 21).

**Características del proyecto:**

* API REST con PostgreSQL y RabbitMQ  
* Dockerfile funcional existente  
* 3 tipos de notificaciones vía Telegram  
* Ambientes: desarrollo y producción

**IMPORTANTE:** Después de completar CADA paso, debes DETENERTE y solicitar una **revisión exhaustiva** antes de continuar.

---

## **Documentos de Entrada**

**Lee estos archivos del proyecto:**

1. `docs/ARQUITECTURA.md` \- Stack tecnológico  
2. `docker-compose.yml` \- Configuración local (PostgreSQL \+ RabbitMQ)  
3. `Dockerfile` \- Build de la aplicación  
4. **Sistema testeado:** Suite de tests E2E pasando (PROMPT 6\)

---

## **Metodología de Trabajo**

### **Principio:**

**"Codificar → Sintetizar → Validar → Confirmar → Continuar"**

Después de CADA paso:

1. ✅ Codifica el construct CDK  
2. ✅ Ejecuta `cdk synth` y valida CloudFormation  
3. ✅ Ejecuta tests unitarios  
4. ⏸️ **DETENTE y solicita revisión**  
5. ✅ Espera confirmación antes de continuar

---

## **Recursos AWS Requeridos**

### **Networking**

* VPC con CIDR 10.0.0.0/16  
* 2 subnets públicas (para ALB)  
* 2 subnets privadas (para ECS, RDS, MQ)  
* NAT Gateway (1 para dev, 2 para prod)  
* Security Groups con principio de mínimo privilegio

### **Base de Datos**

* RDS PostgreSQL 16  
* Instancia: t3.micro (dev), t3.small (prod)  
* Multi-AZ solo en producción  
* Automated backups 7 días  
* Credentials en Secrets Manager (auto-generado)

### **Mensajería**

* Amazon MQ (RabbitMQ)  
* Instancia: mq.t3.micro  
* Credentials en Secrets Manager

### **Aplicación**

* ECR repository para imágenes Docker  
* ECS Cluster con Fargate  
* Task Definition: 512 CPU, 1024 MB  
* Fargate Service con desired=2  
* Auto-scaling: min=1, max=4, target CPU 70%

### **Load Balancer**

* Application Load Balancer  
* Health check a /actuator/health  
* HTTP listener (puerto 80\)

### **Seguridad**

* Secrets Manager para TELEGRAM\_BOT\_TOKEN  
* IAM roles automáticos (CDK los genera)  
* Security groups con referencias cruzadas

### **Monitoreo**

* CloudWatch Log Group (retención 14 días)  
* CloudWatch Alarms para CPU \> 80%  
* Dashboard básico

---

## **Estructura del Proyecto CDK**

ticketero-infra/  
├── src/main/java/com/example/infra/  
│   ├── TicketeroApp.java              \# Entry point  
│   ├── TicketeroStack.java            \# Stack principal  
│   ├── constructs/  
│   │   ├── NetworkingConstruct.java   \# VPC, subnets, SGs  
│   │   ├── DatabaseConstruct.java     \# RDS PostgreSQL  
│   │   ├── MessagingConstruct.java    \# Amazon MQ \+ Secrets  
│   │   ├── ContainerConstruct.java    \# ECR, ECS, Fargate  
│   │   └── MonitoringConstruct.java   \# CloudWatch  
│   └── config/  
│       └── EnvironmentConfig.java     \# Configuración por ambiente  
├── src/test/java/com/example/infra/  
│   └── TicketeroStackTest.java        \# Tests de infraestructura  
├── cdk.json  
└── pom.xml

---

## **Requisitos Técnicos**

* **AWS CDK:** \>= 2.100.0  
* **Java:** 21  
* **Maven:** 3.9+  
* **Constructs:** L2/L3 (alto nivel) preferidos  
* **Props pattern** para configuración de constructs  
* **Tests:** CDK assertions  
* **Documentación:** JavaDoc en clases principales

---

## **Buenas Prácticas Requeridas**

* ✅ Constructs personalizados para cada componente  
* ✅ Configuración externalizada por ambiente (dev/prod)  
* ✅ Builder pattern para props complejos  
* ✅ Tags automáticos con Aspects  
* ✅ Outputs para endpoints importantes  
* ✅ Tests con assertions de CDK  
* ✅ Documentación Javadoc

---

## **Tu Tarea: 9 Pasos**

**PASO 1:** Setup Proyecto CDK \+ Arquitectura  
**PASO 2:** EnvironmentConfig \+ TicketeroApp  
**PASO 3:** NetworkingConstruct (VPC, Subnets, Security Groups)  
**PASO 4:** DatabaseConstruct (RDS PostgreSQL)  
**PASO 5:** MessagingConstruct (Amazon MQ \+ Secrets)  
**PASO 6:** ContainerConstruct (ECR, ECS Fargate, ALB)  
**PASO 7:** MonitoringConstruct (CloudWatch Logs, Alarms, Dashboard)  
**PASO 8:** Tags con Aspects \+ Tests de Stack  
**PASO 9:** Deploy y Validación Final

---

## **PASO 1: Setup Proyecto CDK \+ Arquitectura**

**Objetivo:** Configurar proyecto Maven CDK y definir arquitectura.

**Arquitectura:**

                        ┌─────────────────────────────────────────────┐  
                         │              VPC 10.0.0.0/16                │  
                         │                                             │  
    Internet ────────────┤  ┌─────────────┐     ┌─────────────┐       │  
         │               │  │  Public     │     │  Public     │       │  
         ▼               │  │  Subnet A   │     │  Subnet B   │       │  
    ┌─────────┐          │  │ 10.0.1.0/24 │     │ 10.0.2.0/24 │       │  
    │   ALB   │──────────┤  └──────┬──────┘     └──────┬──────┘       │  
    └─────────┘          │         │ NAT               │              │  
         │               │         ▼                   ▼              │  
         ▼               │  ┌─────────────┐     ┌─────────────┐       │  
    ┌─────────┐          │  │  Private    │     │  Private    │       │  
    │   ECS   │◄─────────┤  │  Subnet A   │     │  Subnet B   │       │  
    │ Fargate │          │  │ 10.0.11.0/24│     │ 10.0.12.0/24│       │  
    └─────────┘          │  └─────────────┘     └─────────────┘       │  
         │               │         │                   │              │  
    ┌────┴────┐          │         ▼                   ▼              │  
    ▼         ▼          │  ┌───────────┐       ┌───────────┐         │  
┌──────┐  ┌──────┐       │  │    RDS    │       │ Amazon MQ │         │  
│Secrets│  │ ECR  │       │  │ PostgreSQL│       │ RabbitMQ  │         │  
│Manager│  │      │       │  └───────────┘       └───────────┘         │  
└──────┘  └──────┘       └─────────────────────────────────────────────┘

**Crear pom.xml:**

\<?xml version="1.0" encoding="UTF-8"?\>  
\<project xmlns="http://maven.apache.org/POM/4.0.0"  
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0   
         https://maven.apache.org/xsd/maven-4.0.0.xsd"\>  
    \<modelVersion\>4.0.0\</modelVersion\>

    \<groupId\>com.example\</groupId\>  
    \<artifactId\>ticketero-infra\</artifactId\>  
    \<version\>1.0.0\</version\>  
    \<packaging\>jar\</packaging\>  
    \<description\>AWS CDK Infrastructure for Ticketero\</description\>

    \<properties\>  
        \<java.version\>21\</java.version\>  
        \<cdk.version\>2.150.0\</cdk.version\>  
        \<constructs.version\>10.3.0\</constructs.version\>  
        \<junit.version\>5.10.0\</junit.version\>  
        \<maven.compiler.source\>21\</maven.compiler.source\>  
        \<maven.compiler.target\>21\</maven.compiler.target\>  
        \<project.build.sourceEncoding\>UTF-8\</project.build.sourceEncoding\>  
    \</properties\>

    \<dependencies\>  
        \<dependency\>  
            \<groupId\>software.amazon.awscdk\</groupId\>  
            \<artifactId\>aws-cdk-lib\</artifactId\>  
            \<version\>${cdk.version}\</version\>  
        \</dependency\>  
        \<dependency\>  
            \<groupId\>software.constructs\</groupId\>  
            \<artifactId\>constructs\</artifactId\>  
            \<version\>${constructs.version}\</version\>  
        \</dependency\>  
        \<dependency\>  
            \<groupId\>org.junit.jupiter\</groupId\>  
            \<artifactId\>junit-jupiter\</artifactId\>  
            \<version\>${junit.version}\</version\>  
            \<scope\>test\</scope\>  
        \</dependency\>  
    \</dependencies\>

    \<build\>  
        \<plugins\>  
            \<plugin\>  
                \<groupId\>org.apache.maven.plugins\</groupId\>  
                \<artifactId\>maven-compiler-plugin\</artifactId\>  
                \<version\>3.11.0\</version\>  
            \</plugin\>  
            \<plugin\>  
                \<groupId\>org.codehaus.mojo\</groupId\>  
                \<artifactId\>exec-maven-plugin\</artifactId\>  
                \<version\>3.1.0\</version\>  
                \<configuration\>  
                    \<mainClass\>com.example.infra.TicketeroApp\</mainClass\>  
                \</configuration\>  
            \</plugin\>  
        \</plugins\>  
    \</build\>  
\</project\>

**Crear cdk.json:**

{  
  "app": "mvn \-e \-q compile exec:java",  
  "watch": {  
    "include": \["\*\*"\],  
    "exclude": \["README.md", "cdk\*.json", "target", "pom.xml"\]  
  },  
  "context": {  
    "@aws-cdk/aws-ec2:restrictDefaultSecurityGroup": true,  
    "@aws-cdk/aws-rds:lowercaseDbIdentifier": true,  
    "@aws-cdk/core:stackRelativeExports": true  
  }  
}

**Validaciones:**

mkdir \-p ticketero-infra/src/main/java/com/example/infra/{constructs,config}  
mkdir \-p ticketero-infra/src/test/java/com/example/infra  
cd ticketero-infra  
mvn clean compile  
cdk \--version  \# \>= 2.100.0

**🔍 PUNTO DE REVISIÓN 1:** Verificar proyecto compila y CDK instalado.

---

## **PASO 2: EnvironmentConfig \+ TicketeroApp**

**Objetivo:** Crear configuración por ambiente usando record pattern.

**EnvironmentConfig.java:**

package com.example.infra.config;

import software.amazon.awscdk.services.ec2.InstanceClass;  
import software.amazon.awscdk.services.ec2.InstanceSize;

/\*\*  
 \* Configuración inmutable por ambiente para infraestructura Ticketero.  
 \*/  
public record EnvironmentConfig(  
    String envName,  
    String vpcCidr,  
    int natGateways,  
    int desiredCount,  
    int minCapacity,  
    int maxCapacity,  
    int taskCpu,  
    int taskMemory,  
    InstanceClass dbInstanceClass,  
    InstanceSize dbInstanceSize,  
    int dbAllocatedStorage,  
    boolean multiAz,  
    String mqInstanceType,  
    boolean enableAlarms,  
    int logRetentionDays  
) {  
    /\*\*  
     \* Configuración para ambiente de desarrollo.  
     \* Costo estimado: \~$110/mes  
     \*/  
    public static EnvironmentConfig dev() {  
        return new EnvironmentConfig(  
            "dev",  
            "10.0.0.0/16",  
            1,              // 1 NAT Gateway  
            1,              // desired tasks  
            1,              // min capacity  
            2,              // max capacity  
            512,            // CPU units  
            1024,           // Memory MB  
            InstanceClass.T3,  
            InstanceSize.MICRO,  
            20,             // GB storage  
            false,          // no Multi-AZ  
            "mq.t3.micro",  
            false,          // no alarms  
            7               // log retention days  
        );  
    }  
      
    /\*\*  
     \* Configuración para ambiente de producción.  
     \* Costo estimado: \~$210/mes  
     \*/  
    public static EnvironmentConfig prod() {  
        return new EnvironmentConfig(  
            "prod",  
            "10.0.0.0/16",  
            2,              // 2 NAT Gateways (HA)  
            2,              // desired tasks  
            2,              // min capacity  
            4,              // max capacity  
            512,            // CPU units  
            1024,           // Memory MB  
            InstanceClass.T3,  
            InstanceSize.SMALL,  
            50,             // GB storage  
            true,           // Multi-AZ  
            "mq.t3.micro",  
            true,           // alarms enabled  
            14              // log retention days  
        );  
    }  
      
    public String resourceName(String baseName) {  
        return "ticketero-" \+ envName \+ "-" \+ baseName;  
    }  
      
    public boolean isProd() {  
        return "prod".equals(envName);  
    }  
}

**TicketeroApp.java:**

package com.example.infra;

import com.example.infra.config.EnvironmentConfig;  
import software.amazon.awscdk.App;  
import software.amazon.awscdk.Environment;  
import software.amazon.awscdk.StackProps;  
import software.amazon.awscdk.Tags;

/\*\*  
 \* Entry point de la aplicación CDK.  
 \*/  
public class TicketeroApp {  
      
    public static void main(final String\[\] args) {  
        App app \= new App();  
          
        String account \= System.getenv("CDK\_DEFAULT\_ACCOUNT");  
        String region \= System.getenv("CDK\_DEFAULT\_REGION");  
          
        if (account \== null || region \== null) {  
            account \= (String) app.getNode().tryGetContext("accountId");  
            region \= (String) app.getNode().tryGetContext("region");  
        }  
          
        Environment awsEnv \= Environment.builder()  
            .account(account)  
            .region(region)  
            .build();  
          
        // Stack Dev  
        EnvironmentConfig devConfig \= EnvironmentConfig.dev();  
        TicketeroStack devStack \= new TicketeroStack(app, "ticketero-dev",   
            StackProps.builder()  
                .env(awsEnv)  
                .description("Ticketero \- Development")  
                .build(),   
            devConfig);  
        applyTags(devStack, devConfig);  
          
        // Stack Prod  
        EnvironmentConfig prodConfig \= EnvironmentConfig.prod();  
        TicketeroStack prodStack \= new TicketeroStack(app, "ticketero-prod",   
            StackProps.builder()  
                .env(awsEnv)  
                .description("Ticketero \- Production")  
                .build(),   
            prodConfig);  
        applyTags(prodStack, prodConfig);  
          
        app.synth();  
    }  
      
    private static void applyTags(TicketeroStack stack, EnvironmentConfig config) {  
        Tags.of(stack).add("Environment", config.envName());  
        Tags.of(stack).add("Project", "Ticketero");  
        Tags.of(stack).add("ManagedBy", "CDK");  
        Tags.of(stack).add("CostCenter", "ticketero-" \+ config.envName());  
    }  
}

**TicketeroStack.java:**

package com.example.infra;

import com.example.infra.config.EnvironmentConfig;  
import com.example.infra.constructs.\*;  
import software.amazon.awscdk.CfnOutput;  
import software.amazon.awscdk.Stack;  
import software.amazon.awscdk.StackProps;  
import software.constructs.Construct;

/\*\*  
 \* Stack principal que orquesta todos los constructs.  
 \*/  
public class TicketeroStack extends Stack {  
      
    public TicketeroStack(final Construct scope, final String id,   
                          final StackProps props, final EnvironmentConfig config) {  
        super(scope, id, props);  
          
        // 1\. Networking  
        NetworkingConstruct networking \= new NetworkingConstruct(this, "Networking", config);  
          
        // 2\. Database  
        DatabaseConstruct database \= new DatabaseConstruct(this, "Database", config, networking);  
          
        // 3\. Messaging  
        MessagingConstruct messaging \= new MessagingConstruct(this, "Messaging", config, networking);  
          
        // 4\. Container  
        ContainerConstruct container \= new ContainerConstruct(this, "Container",   
            config, networking, database, messaging);  
          
        // 5\. Monitoring  
        new MonitoringConstruct(this, "Monitoring", config, container, database);  
          
        // Outputs  
        CfnOutput.Builder.create(this, "LoadBalancerDNS")  
            .value(container.getLoadBalancerDNS())  
            .description("ALB DNS Name")  
            .exportName(config.resourceName("alb-dns"))  
            .build();  
              
        CfnOutput.Builder.create(this, "EcrRepositoryUri")  
            .value(container.getEcrRepositoryUri())  
            .description("ECR Repository URI")  
            .exportName(config.resourceName("ecr-uri"))  
            .build();  
              
        CfnOutput.Builder.create(this, "DatabaseEndpoint")  
            .value(database.getEndpoint())  
            .description("RDS Endpoint")  
            .exportName(config.resourceName("db-endpoint"))  
            .build();  
              
        CfnOutput.Builder.create(this, "MQEndpoint")  
            .value(messaging.getMqEndpoint())  
            .description("Amazon MQ Endpoint")  
            .exportName(config.resourceName("mq-endpoint"))  
            .build();  
    }  
}

**Validaciones:**

cdk ls  
\# ticketero-dev  
\# ticketero-prod

**🔍 PUNTO DE REVISIÓN 2:** Verificar 2 stacks listados con tags.

---

## **PASO 3: NetworkingConstruct**

**Objetivo:** Crear VPC 10.0.0.0/16 con subnets y Security Groups.

**NetworkingConstruct.java:**

package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;  
import software.amazon.awscdk.services.ec2.\*;  
import software.constructs.Construct;  
import java.util.List;

/\*\*  
 \* VPC, Subnets y Security Groups.  
 \*/  
public class NetworkingConstruct extends Construct {  
      
    private final Vpc vpc;  
    private final SecurityGroup albSg;  
    private final SecurityGroup ecsSg;  
    private final SecurityGroup rdsSg;  
    private final SecurityGroup mqSg;  
      
    public NetworkingConstruct(final Construct scope, final String id,   
                                final EnvironmentConfig config) {  
        super(scope, id);  
          
        // VPC  
        this.vpc \= Vpc.Builder.create(this, "Vpc")  
            .vpcName(config.resourceName("vpc"))  
            .ipAddresses(IpAddresses.cidr(config.vpcCidr()))  
            .maxAzs(2)  
            .natGateways(config.natGateways())  
            .subnetConfiguration(List.of(  
                SubnetConfiguration.builder()  
                    .name("Public")  
                    .subnetType(SubnetType.PUBLIC)  
                    .cidrMask(24)  
                    .build(),  
                SubnetConfiguration.builder()  
                    .name("Private")  
                    .subnetType(SubnetType.PRIVATE\_WITH\_EGRESS)  
                    .cidrMask(24)  
                    .build()  
            ))  
            .build();  
          
        // Security Group \- ALB  
        this.albSg \= SecurityGroup.Builder.create(this, "AlbSg")  
            .vpc(vpc)  
            .securityGroupName(config.resourceName("alb-sg"))  
            .description("ALB Security Group")  
            .allowAllOutbound(true)  
            .build();  
        albSg.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "HTTP");  
        albSg.addIngressRule(Peer.anyIpv4(), Port.tcp(443), "HTTPS");  
          
        // Security Group \- ECS  
        this.ecsSg \= SecurityGroup.Builder.create(this, "EcsSg")  
            .vpc(vpc)  
            .securityGroupName(config.resourceName("ecs-sg"))  
            .description("ECS Tasks Security Group")  
            .allowAllOutbound(true)  
            .build();  
        ecsSg.addIngressRule(albSg, Port.tcp(8080), "From ALB");  
          
        // Security Group \- RDS  
        this.rdsSg \= SecurityGroup.Builder.create(this, "RdsSg")  
            .vpc(vpc)  
            .securityGroupName(config.resourceName("rds-sg"))  
            .description("RDS Security Group")  
            .allowAllOutbound(false)  
            .build();  
        rdsSg.addIngressRule(ecsSg, Port.tcp(5432), "PostgreSQL from ECS");  
          
        // Security Group \- Amazon MQ  
        this.mqSg \= SecurityGroup.Builder.create(this, "MqSg")  
            .vpc(vpc)  
            .securityGroupName(config.resourceName("mq-sg"))  
            .description("Amazon MQ Security Group")  
            .allowAllOutbound(false)  
            .build();  
        mqSg.addIngressRule(ecsSg, Port.tcp(5671), "AMQPS from ECS");  
        mqSg.addIngressRule(ecsSg, Port.tcp(443), "HTTPS Console");  
    }  
      
    public Vpc getVpc() { return vpc; }  
    public SecurityGroup getAlbSg() { return albSg; }  
    public SecurityGroup getEcsSg() { return ecsSg; }  
    public SecurityGroup getRdsSg() { return rdsSg; }  
    public SecurityGroup getMqSg() { return mqSg; }  
}

**🔍 PUNTO DE REVISIÓN 3:** VPC \+ 4 Security Groups con mínimo privilegio.

---

## **PASO 4: DatabaseConstruct**

**Objetivo:** Crear RDS PostgreSQL 16 con credenciales en Secrets Manager.

**DatabaseConstruct.java:**

package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;  
import software.amazon.awscdk.Duration;  
import software.amazon.awscdk.RemovalPolicy;  
import software.amazon.awscdk.services.ec2.\*;  
import software.amazon.awscdk.services.rds.\*;  
import software.amazon.awscdk.services.secretsmanager.\*;  
import software.constructs.Construct;  
import java.util.List;

/\*\*  
 \* RDS PostgreSQL con credenciales auto-generadas.  
 \*/  
public class DatabaseConstruct extends Construct {  
      
    private final DatabaseInstance database;  
    private final Secret credentials;  
      
    public DatabaseConstruct(final Construct scope, final String id,  
                              final EnvironmentConfig config,  
                              final NetworkingConstruct networking) {  
        super(scope, id);  
          
        // Credenciales auto-generadas  
        this.credentials \= Secret.Builder.create(this, "DbCredentials")  
            .secretName(config.resourceName("db-credentials"))  
            .description("RDS PostgreSQL Credentials")  
            .generateSecretString(SecretStringGenerator.builder()  
                .secretStringTemplate("{\\"username\\": \\"postgres\\"}")  
                .generateStringKey("password")  
                .excludePunctuation(true)  
                .passwordLength(32)  
                .build())  
            .build();  
          
        // RDS PostgreSQL  
        this.database \= DatabaseInstance.Builder.create(this, "Postgres")  
            .instanceIdentifier(config.resourceName("db"))  
            .engine(DatabaseInstanceEngine.postgres(  
                PostgresInstanceEngineProps.builder()  
                    .version(PostgresEngineVersion.VER\_16)  
                    .build()))  
            .instanceType(InstanceType.of(  
                config.dbInstanceClass(), config.dbInstanceSize()))  
            .vpc(networking.getVpc())  
            .vpcSubnets(SubnetSelection.builder()  
                .subnetType(SubnetType.PRIVATE\_WITH\_EGRESS)  
                .build())  
            .securityGroups(List.of(networking.getRdsSg()))  
            .credentials(Credentials.fromSecret(credentials))  
            .databaseName("ticketero")  
            .allocatedStorage(config.dbAllocatedStorage())  
            .multiAz(config.multiAz())  
            .backupRetention(Duration.days(7))  
            .deletionProtection(config.isProd())  
            .removalPolicy(config.isProd() ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY)  
            .build();  
    }  
      
    public String getEndpoint() {  
        return database.getDbInstanceEndpointAddress();  
    }  
      
    public Secret getCredentials() { return credentials; }  
    public DatabaseInstance getDatabase() { return database; }  
}

**🔍 PUNTO DE REVISIÓN 4:** RDS PostgreSQL 16 \+ Secret auto-generado.

---

## **PASO 5: MessagingConstruct**

**Objetivo:** Crear Amazon MQ (RabbitMQ) y secrets.

**MessagingConstruct.java:**

package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;  
import software.amazon.awscdk.services.amazonmq.\*;  
import software.amazon.awscdk.services.secretsmanager.\*;  
import software.constructs.Construct;  
import java.util.List;

/\*\*  
 \* Amazon MQ RabbitMQ y secrets para Telegram.  
 \*/  
public class MessagingConstruct extends Construct {  
      
    private final CfnBroker mqBroker;  
    private final Secret mqCredentials;  
    private final Secret telegramSecret;  
      
    public MessagingConstruct(final Construct scope, final String id,  
                               final EnvironmentConfig config,  
                               final NetworkingConstruct networking) {  
        super(scope, id);  
          
        // Credenciales MQ auto-generadas  
        this.mqCredentials \= Secret.Builder.create(this, "MqCredentials")  
            .secretName(config.resourceName("mq-credentials"))  
            .description("Amazon MQ Credentials")  
            .generateSecretString(SecretStringGenerator.builder()  
                .secretStringTemplate("{\\"username\\": \\"ticketero\\"}")  
                .generateStringKey("password")  
                .excludePunctuation(true)  
                .excludeCharacters("/@\\"\\\\")  
                .passwordLength(32)  
                .build())  
            .build();  
          
        // Amazon MQ Broker  
        this.mqBroker \= CfnBroker.Builder.create(this, "MqBroker")  
            .brokerName(config.resourceName("mq"))  
            .engineType("RABBITMQ")  
            .engineVersion("3.11.20")  
            .hostInstanceType(config.mqInstanceType())  
            .deploymentMode("SINGLE\_INSTANCE")  
            .publiclyAccessible(false)  
            .autoMinorVersionUpgrade(true)  
            .subnetIds(List.of(  
                networking.getVpc().getPrivateSubnets().get(0).getSubnetId()  
            ))  
            .securityGroups(List.of(  
                networking.getMqSg().getSecurityGroupId()  
            ))  
            .users(List.of(  
                CfnBroker.UserProperty.builder()  
                    .username("ticketero")  
                    .password(mqCredentials.secretValueFromJson("password").unsafeUnwrap())  
                    .build()  
            ))  
            .logs(CfnBroker.LogListProperty.builder()  
                .general(true)  
                .build())  
            .build();  
          
        // Secret Telegram (placeholder)  
        this.telegramSecret \= Secret.Builder.create(this, "TelegramSecret")  
            .secretName(config.resourceName("telegram"))  
            .description("Telegram Bot Token \- UPDATE AFTER DEPLOY")  
            .generateSecretString(SecretStringGenerator.builder()  
                .secretStringTemplate("{\\"token\\": \\"PLACEHOLDER\\"}")  
                .generateStringKey("dummy")  
                .build())  
            .build();  
    }  
      
    public String getMqEndpoint() {  
        return mqBroker.getAttrAmqpEndpoints().toString();  
    }  
      
    public Secret getMqCredentials() { return mqCredentials; }  
    public Secret getTelegramSecret() { return telegramSecret; }  
    public CfnBroker getMqBroker() { return mqBroker; }  
}

**🔍 PUNTO DE REVISIÓN 5:** Amazon MQ \+ 2 secrets creados.

---

## **PASO 6: ContainerConstruct**

**Objetivo:** Crear ECR, ECS Fargate con ALB y auto-scaling.

**ContainerConstruct.java:**

package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;  
import software.amazon.awscdk.Duration;  
import software.amazon.awscdk.services.ecr.Repository;  
import software.amazon.awscdk.services.ecs.\*;  
import software.amazon.awscdk.services.ecs.patterns.\*;  
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;  
import software.constructs.Construct;  
import java.util.List;  
import java.util.Map;

/\*\*  
 \* ECR, ECS Fargate, ALB con auto-scaling.  
 \*/  
public class ContainerConstruct extends Construct {  
      
    private final Repository ecr;  
    private final Cluster cluster;  
    private final ApplicationLoadBalancedFargateService service;  
      
    public ContainerConstruct(final Construct scope, final String id,  
                               final EnvironmentConfig config,  
                               final NetworkingConstruct networking,  
                               final DatabaseConstruct database,  
                               final MessagingConstruct messaging) {  
        super(scope, id);  
          
        // ECR  
        this.ecr \= Repository.Builder.create(this, "Ecr")  
            .repositoryName(config.resourceName("api"))  
            .imageScanOnPush(true)  
            .build();  
          
        // ECS Cluster  
        this.cluster \= Cluster.Builder.create(this, "Cluster")  
            .vpc(networking.getVpc())  
            .clusterName(config.resourceName("cluster"))  
            .containerInsights(config.enableAlarms())  
            .build();  
          
        // Environment variables  
        Map\<String, String\> environment \= Map.of(  
            "SPRING\_PROFILES\_ACTIVE", config.envName(),  
            "DATABASE\_URL", "jdbc:postgresql://" \+ database.getEndpoint() \+ ":5432/ticketero",  
            "SPRING\_RABBITMQ\_PORT", "5671",  
            "SPRING\_RABBITMQ\_SSL\_ENABLED", "true"  
        );  
          
        // Secrets  
        Map\<String, Secret\> secrets \= Map.of(  
            "DATABASE\_USERNAME", Secret.fromSecretsManager(database.getCredentials(), "username"),  
            "DATABASE\_PASSWORD", Secret.fromSecretsManager(database.getCredentials(), "password"),  
            "SPRING\_RABBITMQ\_USERNAME", Secret.fromSecretsManager(messaging.getMqCredentials(), "username"),  
            "SPRING\_RABBITMQ\_PASSWORD", Secret.fromSecretsManager(messaging.getMqCredentials(), "password"),  
            "TELEGRAM\_BOT\_TOKEN", Secret.fromSecretsManager(messaging.getTelegramSecret(), "token")  
        );  
          
        // Fargate \+ ALB  
        this.service \= ApplicationLoadBalancedFargateService.Builder  
            .create(this, "Service")  
            .cluster(cluster)  
            .serviceName(config.resourceName("service"))  
            .desiredCount(config.desiredCount())  
            .cpu(config.taskCpu())  
            .memoryLimitMiB(config.taskMemory())  
            .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()  
                .image(ContainerImage.fromEcrRepository(ecr, "latest"))  
                .containerPort(8080)  
                .environment(environment)  
                .secrets(secrets)  
                .logDriver(LogDrivers.awsLogs(AwsLogDriverProps.builder()  
                    .streamPrefix("ticketero")  
                    .build()))  
                .build())  
            .publicLoadBalancer(true)  
            .securityGroups(List.of(networking.getEcsSg()))  
            .assignPublicIp(false)  
            .build();  
          
        // ALB Security Group  
        service.getLoadBalancer().addSecurityGroup(networking.getAlbSg());  
          
        // Health Check  
        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()  
            .path("/actuator/health")  
            .interval(Duration.seconds(30))  
            .timeout(Duration.seconds(10))  
            .healthyThresholdCount(2)  
            .unhealthyThresholdCount(3)  
            .build());  
          
        // Auto-scaling  
        ScalableTaskCount scaling \= service.getService().autoScaleTaskCount(  
            EnableScalingProps.builder()  
                .minCapacity(config.minCapacity())  
                .maxCapacity(config.maxCapacity())  
                .build());  
          
        scaling.scaleOnCpuUtilization("CpuScaling",  
            CpuUtilizationScalingProps.builder()  
                .targetUtilizationPercent(70)  
                .scaleInCooldown(Duration.seconds(300))  
                .scaleOutCooldown(Duration.seconds(60))  
                .build());  
    }  
      
    public String getLoadBalancerDNS() {  
        return service.getLoadBalancer().getLoadBalancerDnsName();  
    }  
      
    public String getEcrRepositoryUri() {  
        return ecr.getRepositoryUri();  
    }  
      
    public ApplicationLoadBalancedFargateService getService() { return service; }  
    public Repository getEcr() { return ecr; }  
}

**🔍 PUNTO DE REVISIÓN 6:** ECR \+ ECS \+ ALB \+ auto-scaling.

---

## **PASO 7: MonitoringConstruct**

**Objetivo:** CloudWatch Logs, Alarms y Dashboard (solo prod).

**MonitoringConstruct.java:**

package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;  
import software.amazon.awscdk.Duration;  
import software.amazon.awscdk.services.cloudwatch.\*;  
import software.amazon.awscdk.services.logs.\*;  
import software.constructs.Construct;  
import java.util.List;

/\*\*  
 \* CloudWatch Logs, Alarms y Dashboard.  
 \*/  
public class MonitoringConstruct extends Construct {  
      
    public MonitoringConstruct(final Construct scope, final String id,  
                                final EnvironmentConfig config,  
                                final ContainerConstruct container,  
                                final DatabaseConstruct database) {  
        super(scope, id);  
          
        // Log Group  
        LogGroup.Builder.create(this, "LogGroup")  
            .logGroupName("/ecs/" \+ config.resourceName("api"))  
            .retention(RetentionDays.of(config.logRetentionDays()))  
            .build();  
          
        if (\!config.enableAlarms()) {  
            return;  
        }  
          
        var service \= container.getService();  
          
        // Alarm: CPU \> 80%  
        Alarm.Builder.create(this, "CpuAlarm")  
            .alarmName(config.resourceName("high-cpu"))  
            .alarmDescription("CPU \> 80%")  
            .metric(service.getService().metricCpuUtilization(  
                MetricOptions.builder().period(Duration.minutes(5)).build()))  
            .threshold(80)  
            .evaluationPeriods(1)  
            .comparisonOperator(ComparisonOperator.GREATER\_THAN\_THRESHOLD)  
            .build();  
          
        // Alarm: Memory \> 80%  
        Alarm.Builder.create(this, "MemoryAlarm")  
            .alarmName(config.resourceName("high-memory"))  
            .metric(service.getService().metricMemoryUtilization(  
                MetricOptions.builder().period(Duration.minutes(5)).build()))  
            .threshold(80)  
            .evaluationPeriods(1)  
            .build();  
          
        // Alarm: HTTP 5xx  
        Alarm.Builder.create(this, "Http5xxAlarm")  
            .alarmName(config.resourceName("http-5xx"))  
            .metric(service.getLoadBalancer().metricHttpCodeTarget(  
                HttpCodeTarget.TARGET\_5XX\_COUNT,  
                MetricOptions.builder().period(Duration.minutes(5)).build()))  
            .threshold(10)  
            .evaluationPeriods(1)  
            .treatMissingData(TreatMissingData.NOT\_BREACHING)  
            .build();  
          
        // Alarm: DB Connections  
        Alarm.Builder.create(this, "DbConnectionsAlarm")  
            .alarmName(config.resourceName("db-connections"))  
            .metric(database.getDatabase().metricDatabaseConnections())  
            .threshold(50)  
            .evaluationPeriods(1)  
            .build();  
          
        // Dashboard  
        Dashboard dashboard \= Dashboard.Builder.create(this, "Dashboard")  
            .dashboardName(config.resourceName("dashboard"))  
            .build();  
          
        dashboard.addWidgets(  
            GraphWidget.Builder.create()  
                .title("ECS CPU & Memory")  
                .left(List.of(  
                    service.getService().metricCpuUtilization(),  
                    service.getService().metricMemoryUtilization()))  
                .width(12).build(),  
            GraphWidget.Builder.create()  
                .title("ALB Requests")  
                .left(List.of(service.getLoadBalancer().metricRequestCount()))  
                .width(12).build()  
        );  
    }  
}

**🔍 PUNTO DE REVISIÓN 7:** 4 alarms \+ dashboard en prod, 0 en dev.

---

## **PASO 8: Tests de Infraestructura**

**Objetivo:** Crear tests con CDK assertions.

**TicketeroStackTest.java:**

package com.example.infra;

import com.example.infra.config.EnvironmentConfig;  
import org.junit.jupiter.api.Test;  
import software.amazon.awscdk.App;  
import software.amazon.awscdk.assertions.Template;  
import java.util.Map;

class TicketeroStackTest {  
      
    @Test  
    void devStackCreatesAllResources() {  
        App app \= new App();  
        TicketeroStack stack \= new TicketeroStack(app, "Test", null, EnvironmentConfig.dev());  
        Template template \= Template.fromStack(stack);  
          
        template.resourceCountIs("AWS::EC2::VPC", 1);  
        template.resourceCountIs("AWS::EC2::Subnet", 4);  
        template.resourceCountIs("AWS::EC2::NatGateway", 1);  
        template.resourceCountIs("AWS::EC2::SecurityGroup", 4);  
        template.resourceCountIs("AWS::RDS::DBInstance", 1);  
        template.resourceCountIs("AWS::AmazonMQ::Broker", 1);  
        template.resourceCountIs("AWS::ECR::Repository", 1);  
        template.resourceCountIs("AWS::ECS::Cluster", 1);  
        template.resourceCountIs("AWS::ECS::Service", 1);  
        template.resourceCountIs("AWS::ElasticLoadBalancingV2::LoadBalancer", 1);  
        template.resourceCountIs("AWS::SecretsManager::Secret", 3);  
        template.resourceCountIs("AWS::CloudWatch::Alarm", 0);  
    }  
      
    @Test  
    void prodHasHighAvailability() {  
        App app \= new App();  
        TicketeroStack stack \= new TicketeroStack(app, "Test", null, EnvironmentConfig.prod());  
        Template template \= Template.fromStack(stack);  
          
        template.resourceCountIs("AWS::EC2::NatGateway", 2);  
        template.hasResourceProperties("AWS::RDS::DBInstance", Map.of("MultiAZ", true));  
        template.resourceCountIs("AWS::CloudWatch::Alarm", 4);  
        template.resourceCountIs("AWS::CloudWatch::Dashboard", 1);  
    }  
}

**Ejecutar tests:**

mvn test  
\# Tests run: 2, Failures: 0

**🔍 PUNTO DE REVISIÓN 8:** Tests pasando.

---

## **PASO 9: Deploy y Validación**

**Objetivo:** Deployar infraestructura y validar.

**Comandos:**

\# 1\. Bootstrap (primera vez)  
export CDK\_DEFAULT\_ACCOUNT=$(aws sts get-caller-identity \--query Account \--output text)  
export CDK\_DEFAULT\_REGION=us-east-1  
cdk bootstrap

\# 2\. Deploy dev  
cdk deploy ticketero-dev \--require-approval never

\# 3\. Build y push imagen  
ECR\_URI=$(aws cloudformation describe-stacks \--stack-name ticketero-dev \\  
  \--query 'Stacks\[0\].Outputs\[?OutputKey==\`EcrRepositoryUri\`\].OutputValue' \--output text)

aws ecr get-login-password | docker login \--username AWS \--password-stdin $ECR\_URI  
docker build \-t $ECR\_URI:latest ../ticketero/  
docker push $ECR\_URI:latest

\# 4\. Actualizar Telegram token  
aws secretsmanager put-secret-value \\  
  \--secret-id ticketero-dev-telegram \\  
  \--secret-string '{"token":"YOUR\_REAL\_TOKEN"}'

\# 5\. Forzar deployment  
aws ecs update-service \\  
  \--cluster ticketero-dev-cluster \\  
  \--service ticketero-dev-service \\  
  \--force-new-deployment

\# 6\. Esperar estabilización  
aws ecs wait services-stable \\  
  \--cluster ticketero-dev-cluster \\  
  \--services ticketero-dev-service

\# 7\. Validar  
ALB\_DNS=$(aws cloudformation describe-stacks \--stack-name ticketero-dev \\  
  \--query 'Stacks\[0\].Outputs\[?OutputKey==\`LoadBalancerDNS\`\].OutputValue' \--output text)

curl http://$ALB\_DNS/actuator/health  
\# {"status":"UP"}

curl \-X POST http://$ALB\_DNS/api/tickets \\  
  \-H "Content-Type: application/json" \\  
  \-d '{"nationalId":"12345678","telefono":"+56912345678","branchOffice":"Centro","queueType":"CAJA"}'  
\# 201 Created

**🔍 PUNTO DE REVISIÓN FINAL 9:**

✅ PASO 9 COMPLETADO \- INFRAESTRUCTURA AWS COMPLETA

Recursos creados:  
\- ✅ VPC 10.0.0.0/16 (2 AZs, 4 subnets)  
\- ✅ 4 Security Groups  
\- ✅ RDS PostgreSQL 16  
\- ✅ Amazon MQ RabbitMQ  
\- ✅ ECR Repository  
\- ✅ ECS Cluster \+ Fargate Service  
\- ✅ Application Load Balancer  
\- ✅ 3 Secrets (DB, MQ, Telegram)  
\- ✅ CloudWatch Logs  
\- ✅ 4 Alarms \+ Dashboard (prod)

Validaciones:  
\- ✅ Health check: UP  
\- ✅ POST /api/tickets: 201  
\- ✅ Tests: 2/2 pasando

⏸️ DEPLOYMENT COMPLETADO\!

---

## **Resumen de Recursos por Ambiente**

| Recurso | Dev | Prod |
| ----- | ----- | ----- |
| NAT Gateways | 1 | 2 |
| RDS Multi-AZ | No | Sí |
| ECS Tasks | 1 | 2 |
| Auto-scaling | 1-2 | 2-4 |
| CloudWatch Alarms | 0 | 4 |
| Dashboard | No | Sí |
| **Costo/mes** | \~$110 | \~$210 |

---

## **Comandos Útiles**

cdk ls                          \# Listar stacks  
cdk synth ticketero-dev         \# Ver CloudFormation  
cdk diff ticketero-dev          \# Ver cambios  
cdk deploy ticketero-dev        \# Deployar  
cdk destroy ticketero-dev       \# Destruir  
aws logs tail /ecs/ticketero-dev-api \--follow  \# Ver logs

---

