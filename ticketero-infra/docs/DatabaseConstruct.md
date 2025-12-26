# DatabaseConstruct - RDS PostgreSQL

## Overview

The DatabaseConstruct creates a managed PostgreSQL 16 database using Amazon RDS with auto-generated credentials stored in AWS Secrets Manager.

## Architecture

```
Private Subnets
├── RDS PostgreSQL 16
│   ├── Instance: t3.micro (dev) / t3.small (prod)
│   ├── Storage: 20GB (dev) / 50GB (prod)
│   ├── Multi-AZ: No (dev) / Yes (prod)
│   └── Backup: 7 days retention
└── Secrets Manager
    └── Auto-generated credentials
        ├── username: postgres
        └── password: 32-char random
```

## Configuration by Environment

| Setting | Development | Production |
|---------|-------------|------------|
| Instance Type | t3.micro | t3.small |
| Storage | 20 GB | 50 GB |
| Multi-AZ | No | Yes |
| Deletion Protection | No | Yes |
| Removal Policy | DESTROY | RETAIN |
| Cost/month | ~$15 | ~$45 |

## Security Features

### Network Security
- **Private Subnets**: Database not accessible from internet
- **Security Group**: Only allows port 5432 from ECS security group
- **VPC**: Isolated network environment

### Access Control
- **Secrets Manager**: Auto-generated credentials
- **IAM Integration**: ECS tasks can access secrets via IAM roles
- **Encryption**: Data encrypted at rest and in transit

### Backup & Recovery
- **Automated Backups**: 7 days retention
- **Point-in-time Recovery**: Available for last 7 days
- **Multi-AZ**: Automatic failover in production

## Generated Resources

- 1 RDS PostgreSQL 16 instance
- 1 DB Subnet Group (auto-created)
- 1 Secrets Manager secret with auto-generated password
- 1 DB Parameter Group (default)

## Database Configuration

- **Engine**: PostgreSQL 16
- **Database Name**: `ticketero`
- **Port**: 5432
- **Character Set**: UTF-8
- **Timezone**: UTC

## Connection Details

The application connects using:
- **Endpoint**: Retrieved from `database.getEndpoint()`
- **Credentials**: Retrieved from Secrets Manager
- **JDBC URL**: `jdbc:postgresql://{endpoint}:5432/ticketero`

## Usage

```java
DatabaseConstruct database = new DatabaseConstruct(this, "Database", config, networking);

// Get connection details
String endpoint = database.getEndpoint();
Secret credentials = database.getCredentials();

// Use in ECS task environment
Map<String, Secret> secrets = Map.of(
    "DATABASE_USERNAME", Secret.fromSecretsManager(credentials, "username"),
    "DATABASE_PASSWORD", Secret.fromSecretsManager(credentials, "password")
);
```

## Monitoring

The database includes:
- **CloudWatch Metrics**: CPU, memory, connections, IOPS
- **Performance Insights**: Query performance monitoring (can be enabled)
- **Log Exports**: PostgreSQL logs to CloudWatch (can be enabled)

## Maintenance

- **Maintenance Window**: Configurable (defaults to AWS managed)
- **Minor Version Upgrades**: Automatic
- **Major Version Upgrades**: Manual
- **Backup Window**: Configurable (defaults to AWS managed)

## Cost Optimization

### Development
- Single AZ deployment
- Smaller instance type (t3.micro)
- Minimal storage (20GB)
- No deletion protection

### Production
- Multi-AZ for high availability
- Larger instance type (t3.small)
- More storage (50GB)
- Deletion protection enabled
- Backup retention for compliance