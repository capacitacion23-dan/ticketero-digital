# ContainerConstruct - ECR, ECS Fargate & ALB

## Overview

The ContainerConstruct creates the containerized application infrastructure including ECR repository, ECS Fargate cluster, Application Load Balancer, and auto-scaling configuration.

## Architecture

```
Internet
    │
    ▼
┌─────────────┐
│     ALB     │ (Public Subnets)
│  Port 80    │
└─────────────┘
    │
    ▼
┌─────────────┐
│ ECS Fargate │ (Private Subnets)
│  Port 8080  │
│ 512 CPU     │
│ 1024 MB     │
└─────────────┘
    │
    ▼
┌─────────────┐
│     ECR     │
│ Docker Repo │
└─────────────┘
```

## Components

### 1. ECR Repository

- **Name**: `ticketero-{env}-api`
- **Image Scanning**: Enabled on push
- **Purpose**: Store Docker images for the Spring Boot application
- **Lifecycle**: Images tagged with `latest` for deployment

### 2. ECS Cluster

- **Name**: `ticketero-{env}-cluster`
- **Type**: Fargate (serverless)
- **Container Insights**: Enabled in production
- **VPC**: Private subnets only

### 3. Fargate Service

- **Service Name**: `ticketero-{env}-service`
- **Task Definition**: 512 CPU, 1024 MB memory
- **Container Port**: 8080 (Spring Boot default)
- **Desired Count**: 1 (dev), 2 (prod)
- **Network**: Private subnets with ALB

### 4. Application Load Balancer

- **Type**: Internet-facing ALB
- **Listener**: HTTP port 80
- **Health Check**: `/actuator/health`
- **Target Group**: ECS tasks on port 8080

### 5. Auto Scaling

- **Metric**: CPU utilization
- **Target**: 70% CPU
- **Min Capacity**: 1 (dev), 2 (prod)
- **Max Capacity**: 2 (dev), 4 (prod)
- **Scale Out**: 60 seconds cooldown
- **Scale In**: 300 seconds cooldown

## Environment Configuration

| Setting | Development | Production |
|---------|-------------|------------|
| Desired Tasks | 1 | 2 |
| Min Capacity | 1 | 2 |
| Max Capacity | 2 | 4 |
| Container Insights | Disabled | Enabled |
| Cost/month | ~$25 | ~$50 |

## Environment Variables

The ECS tasks receive these environment variables:

```yaml
SPRING_PROFILES_ACTIVE: dev/prod
DATABASE_URL: jdbc:postgresql://{rds-endpoint}:5432/ticketero
SPRING_RABBITMQ_PORT: 5671
SPRING_RABBITMQ_SSL_ENABLED: true
```

## Secrets Integration

The ECS tasks access these secrets from AWS Secrets Manager:

```yaml
DATABASE_USERNAME: From RDS credentials secret
DATABASE_PASSWORD: From RDS credentials secret
SPRING_RABBITMQ_USERNAME: From MQ credentials secret
SPRING_RABBITMQ_PASSWORD: From MQ credentials secret
TELEGRAM_BOT_TOKEN: From Telegram secret
```

## Health Check Configuration

- **Path**: `/actuator/health`
- **Interval**: 30 seconds
- **Timeout**: 10 seconds
- **Healthy Threshold**: 2 consecutive successes
- **Unhealthy Threshold**: 3 consecutive failures

## Logging

- **Driver**: AWS CloudWatch Logs
- **Log Group**: `/ecs/ticketero-{env}-api`
- **Stream Prefix**: `ticketero`
- **Retention**: Configured in MonitoringConstruct

## Security Features

### Network Security
- **Private Deployment**: Tasks run in private subnets
- **ALB Security**: Only HTTP/HTTPS from internet
- **Task Security**: Only port 8080 from ALB

### IAM Integration
- **Task Role**: Auto-generated with permissions for:
  - Secrets Manager access
  - CloudWatch Logs
  - ECR image pull

### Container Security
- **Image Scanning**: Vulnerability scanning on push
- **Non-root User**: Container runs as non-root
- **Read-only Root**: File system protection

## Deployment Process

1. **Build Image**: `docker build -t ticketero .`
2. **Tag Image**: `docker tag ticketero:latest {ecr-uri}:latest`
3. **Push Image**: `docker push {ecr-uri}:latest`
4. **Update Service**: ECS automatically deploys new image
5. **Health Check**: ALB validates application health
6. **Traffic Switch**: Rolling deployment with zero downtime

## Generated Resources

- 1 ECR Repository
- 1 ECS Cluster
- 1 ECS Service
- 1 ECS Task Definition
- 1 Application Load Balancer
- 1 Target Group
- 1 Auto Scaling Target
- 1 Auto Scaling Policy
- CloudWatch Log Group
- IAM Task Role and Execution Role

## Usage

```java
ContainerConstruct container = new ContainerConstruct(this, "Container", 
    config, networking, database, messaging);

// Get deployment endpoints
String albDns = container.getLoadBalancerDNS();
String ecrUri = container.getEcrRepositoryUri();
```

## Monitoring Integration

The ContainerConstruct provides metrics for:
- **CPU Utilization**: For auto-scaling decisions
- **Memory Utilization**: Task performance monitoring
- **Request Count**: ALB traffic metrics
- **Response Time**: Application performance
- **Health Check Status**: Service availability

## Cost Breakdown

| Component | Dev/Month | Prod/Month |
|-----------|-----------|------------|
| Fargate (1 task) | ~$15 | - |
| Fargate (2 tasks) | - | ~$30 |
| ALB | ~$20 | ~$20 |
| ECR Storage | ~$1 | ~$1 |
| **Total** | **~$36** | **~$51** |

## Scaling Behavior

### Scale Out Triggers
- CPU > 70% for 2 consecutive periods
- New task launched in 60 seconds
- Health check must pass before receiving traffic

### Scale In Triggers
- CPU < 70% for 5 consecutive periods (300s cooldown)
- Task gracefully drained before termination
- Minimum capacity maintained

## High Availability

- **Multi-AZ**: Tasks distributed across availability zones
- **Health Checks**: Automatic replacement of unhealthy tasks
- **Rolling Deployments**: Zero-downtime updates
- **Auto Scaling**: Automatic capacity adjustment