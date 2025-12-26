# AWS CDK Infrastructure Architecture - Ticketero

## Architecture Overview

```
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
```

## Resources by Environment

| Resource | Dev | Prod |
|----------|-----|------|
| NAT Gateways | 1 | 2 |
| RDS Multi-AZ | No | Yes |
| ECS Tasks | 1 | 2 |
| Auto-scaling | 1-2 | 2-4 |
| CloudWatch Alarms | 0 | 4 |
| Dashboard | No | Yes |
| **Estimated Cost/month** | ~$110 | ~$210 |

## CDK Constructs

1. **NetworkingConstruct** - VPC, Subnets, Security Groups
2. **DatabaseConstruct** - RDS PostgreSQL with Secrets Manager
3. **MessagingConstruct** - Amazon MQ + Telegram secrets
4. **ContainerConstruct** - ECR, ECS Fargate, ALB, Auto-scaling
5. **MonitoringConstruct** - CloudWatch Logs, Alarms, Dashboard

## Implementation Steps

1. Setup CDK project + Architecture ✅
2. EnvironmentConfig + TicketeroApp
3. NetworkingConstruct (VPC, Subnets, Security Groups)
4. DatabaseConstruct (RDS PostgreSQL)
5. MessagingConstruct (Amazon MQ + Secrets)
6. ContainerConstruct (ECR, ECS Fargate, ALB)
7. MonitoringConstruct (CloudWatch Logs, Alarms, Dashboard)
8. Tags with Aspects + Tests
9. Deploy and Validation