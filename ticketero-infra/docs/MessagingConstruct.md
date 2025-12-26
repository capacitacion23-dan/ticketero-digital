# MessagingConstruct - Amazon MQ RabbitMQ & Secrets

## Overview

The MessagingConstruct creates Amazon MQ RabbitMQ broker for message queuing and AWS Secrets Manager secrets for secure credential storage, including Telegram bot token.

## Architecture

```
Private Subnet
├── Amazon MQ RabbitMQ 3.11.20
│   ├── Instance: mq.t3.micro
│   ├── Deployment: Single Instance
│   ├── User: ticketero
│   └── Logs: General logging enabled
└── Secrets Manager
    ├── MQ Credentials (auto-generated)
    │   ├── username: ticketero
    │   └── password: 32-char random
    └── Telegram Secret (placeholder)
        └── token: PLACEHOLDER (update after deploy)
```

## Components

### 1. Amazon MQ RabbitMQ Broker

- **Engine**: RabbitMQ 3.11.20
- **Instance Type**: mq.t3.micro (~$13/month)
- **Deployment Mode**: Single Instance
- **Network**: Private subnet only
- **Security**: MQ security group (ports 5671, 443)
- **Auto Minor Version Upgrade**: Enabled
- **Logging**: General logs enabled

### 2. MQ Credentials Secret

- **Purpose**: Store RabbitMQ broker credentials
- **Username**: `ticketero` (fixed)
- **Password**: 32-character auto-generated
- **Exclusions**: Special characters that could cause issues
- **Format**: JSON with username/password keys

### 3. Telegram Bot Secret

- **Purpose**: Store Telegram bot token for notifications
- **Initial Value**: Placeholder (must be updated after deployment)
- **Format**: JSON with token key
- **Usage**: ECS tasks retrieve token for Telegram API calls

## Security Features

### Network Security
- **Private Deployment**: Not publicly accessible
- **Security Group**: Only allows traffic from ECS security group
- **VPC Isolation**: Runs within private subnet

### Access Control
- **IAM Integration**: ECS tasks access secrets via IAM roles
- **Secrets Manager**: Encrypted credential storage
- **Auto-rotation**: Can be enabled for MQ credentials

### Connection Security
- **AMQPS**: Encrypted connection (port 5671)
- **TLS**: All connections encrypted in transit
- **Authentication**: Username/password authentication

## Usage in Application

### Spring Boot Configuration

```yaml
spring:
  rabbitmq:
    host: ${MQ_ENDPOINT}
    port: 5671
    username: ${SPRING_RABBITMQ_USERNAME}
    password: ${SPRING_RABBITMQ_PASSWORD}
    ssl:
      enabled: true
```

### ECS Task Environment

```java
Map<String, Secret> secrets = Map.of(
    "SPRING_RABBITMQ_USERNAME", Secret.fromSecretsManager(mqCredentials, "username"),
    "SPRING_RABBITMQ_PASSWORD", Secret.fromSecretsManager(mqCredentials, "password"),
    "TELEGRAM_BOT_TOKEN", Secret.fromSecretsManager(telegramSecret, "token")
);
```

## Post-Deployment Setup

After deploying the infrastructure, update the Telegram secret:

```bash
# Get the secret ARN from CloudFormation outputs
aws secretsmanager put-secret-value \
  --secret-id ticketero-{env}-telegram \
  --secret-string '{"token":"YOUR_ACTUAL_TELEGRAM_BOT_TOKEN"}'
```

## Cost Breakdown

| Component | Cost/Month |
|-----------|------------|
| Amazon MQ (mq.t3.micro) | ~$13 |
| Secrets Manager (2 secrets) | ~$0.80 |
| **Total** | **~$14** |

## Monitoring

Available metrics:
- **Connection Count**: Active connections to broker
- **Message Count**: Messages in queues
- **Consumer Count**: Active consumers
- **Memory Usage**: Broker memory utilization
- **CPU Usage**: Broker CPU utilization

## Generated Resources

- 1 Amazon MQ RabbitMQ broker
- 2 Secrets Manager secrets
- CloudWatch log group (for MQ logs)
- IAM service role (auto-created by MQ)

## Usage

```java
MessagingConstruct messaging = new MessagingConstruct(this, "Messaging", config, networking);

// Get connection details
String mqEndpoint = messaging.getMqEndpoint();
Secret mqCredentials = messaging.getMqCredentials();
Secret telegramSecret = messaging.getTelegramSecret();
```

## Queue Configuration

The application will create these queues:
- `ticket.notifications` - For Telegram notifications
- `ticket.processing` - For ticket processing
- Dead letter queues for error handling

## High Availability

Current setup uses single instance for cost optimization. For production high availability:
- Consider `ACTIVE_STANDBY_MULTI_AZ` deployment mode
- Use larger instance types (mq.m5.large+)
- Enable cross-AZ backup