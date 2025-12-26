# MonitoringConstruct - CloudWatch Logs, Alarms & Dashboard

## Overview

The MonitoringConstruct provides comprehensive monitoring for the Ticketero application using CloudWatch Logs, Alarms, and Dashboard. Monitoring features are environment-specific with full monitoring enabled only in production.

## Architecture

```
Development Environment:
├── CloudWatch Logs
│   ├── Log Group: /ecs/ticketero-dev-api
│   └── Retention: 1 week
└── No Alarms or Dashboard

Production Environment:
├── CloudWatch Logs
│   ├── Log Group: /ecs/ticketero-prod-api
│   └── Retention: 2 weeks
├── CloudWatch Alarms (4)
│   ├── High CPU (>80%)
│   ├── High Memory (>80%)
│   ├── HTTP 5xx Errors (>10)
│   └── DB Connections (>50)
└── CloudWatch Dashboard
    ├── ECS CPU & Memory
    └── ALB Request Count
```

## Components

### 1. CloudWatch Logs

**Purpose**: Centralized logging for ECS tasks

- **Log Group**: `/ecs/ticketero-{env}-api`
- **Stream Prefix**: `ticketero`
- **Retention**: 
  - Development: 1 week (7 days)
  - Production: 2 weeks (14 days)
- **Cost**: ~$0.50/GB ingested, ~$0.03/GB stored

### 2. CloudWatch Alarms (Production Only)

#### High CPU Alarm
- **Metric**: ECS Service CPU Utilization
- **Threshold**: 80%
- **Period**: 5 minutes
- **Evaluation**: 1 period
- **Action**: Alert when CPU consistently high

#### High Memory Alarm
- **Metric**: ECS Service Memory Utilization
- **Threshold**: 80%
- **Period**: 5 minutes
- **Evaluation**: 1 period
- **Action**: Alert when memory consistently high

#### HTTP 5xx Errors Alarm
- **Metric**: ALB Target 5xx Count
- **Threshold**: 10 errors
- **Period**: 5 minutes
- **Evaluation**: 1 period
- **Missing Data**: Not breaching
- **Action**: Alert on application errors

#### Database Connections Alarm
- **Metric**: RDS Database Connections
- **Threshold**: 50 connections
- **Period**: Default (1 minute)
- **Evaluation**: 1 period
- **Action**: Alert on connection pool exhaustion

### 3. CloudWatch Dashboard (Production Only)

**Dashboard Name**: `ticketero-prod-dashboard`

#### Widget 1: ECS CPU & Memory
- **Type**: Line graph
- **Metrics**: 
  - ECS Service CPU Utilization (%)
  - ECS Service Memory Utilization (%)
- **Width**: 12 units
- **Time Range**: Last 24 hours (default)

#### Widget 2: ALB Requests
- **Type**: Line graph
- **Metrics**: ALB Request Count
- **Width**: 12 units
- **Time Range**: Last 24 hours (default)

## Environment Differences

| Feature | Development | Production |
|---------|-------------|------------|
| Log Retention | 1 week | 2 weeks |
| CPU Alarm | ❌ | ✅ |
| Memory Alarm | ❌ | ✅ |
| HTTP 5xx Alarm | ❌ | ✅ |
| DB Connections Alarm | ❌ | ✅ |
| Dashboard | ❌ | ✅ |
| Cost/month | ~$2 | ~$8 |

## Metrics Available

### ECS Service Metrics
- **CPUUtilization**: Percentage of CPU used by tasks
- **MemoryUtilization**: Percentage of memory used by tasks
- **RunningTaskCount**: Number of running tasks
- **PendingTaskCount**: Number of pending tasks

### Application Load Balancer Metrics
- **RequestCount**: Total number of requests
- **TargetResponseTime**: Average response time
- **HTTPCode_Target_2XX_Count**: Successful responses
- **HTTPCode_Target_4XX_Count**: Client errors
- **HTTPCode_Target_5XX_Count**: Server errors
- **HealthyHostCount**: Number of healthy targets
- **UnHealthyHostCount**: Number of unhealthy targets

### RDS Database Metrics
- **DatabaseConnections**: Number of active connections
- **CPUUtilization**: Database CPU usage
- **FreeableMemory**: Available memory
- **ReadLatency**: Read operation latency
- **WriteLatency**: Write operation latency

## Alarm Actions

Currently, alarms are configured for monitoring only. To add notifications:

1. **Create SNS Topic**:
```bash
aws sns create-topic --name ticketero-alerts
```

2. **Subscribe Email**:
```bash
aws sns subscribe \
  --topic-arn arn:aws:sns:region:account:ticketero-alerts \
  --protocol email \
  --notification-endpoint admin@company.com
```

3. **Update Alarms** to include SNS topic ARN

## Log Analysis

### Common Log Queries

**Error Analysis**:
```
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100
```

**Performance Analysis**:
```
fields @timestamp, @message
| filter @message like /actuator/health/
| stats count() by bin(5m)
```

**Database Connection Issues**:
```
fields @timestamp, @message
| filter @message like /Connection/
| sort @timestamp desc
```

## Cost Breakdown

| Component | Dev/Month | Prod/Month |
|-----------|-----------|------------|
| CloudWatch Logs (1GB) | ~$0.50 | ~$0.50 |
| CloudWatch Alarms (4) | $0 | ~$2.00 |
| CloudWatch Dashboard | $0 | ~$3.00 |
| Metrics (custom) | ~$1.50 | ~$2.50 |
| **Total** | **~$2** | **~$8** |

## Generated Resources

### Development
- 1 CloudWatch Log Group

### Production
- 1 CloudWatch Log Group
- 4 CloudWatch Alarms
- 1 CloudWatch Dashboard
- Multiple metric filters (auto-created)

## Usage

```java
new MonitoringConstruct(this, "Monitoring", config, container, database);
```

The construct automatically:
- Creates log group with appropriate retention
- Sets up alarms if `config.enableAlarms()` is true
- Creates dashboard if alarms are enabled
- Configures all metric sources

## Best Practices

### Log Management
- **Structured Logging**: Use JSON format in application
- **Log Levels**: Use appropriate levels (ERROR, WARN, INFO, DEBUG)
- **Correlation IDs**: Include request IDs for tracing

### Alarm Tuning
- **Threshold Adjustment**: Monitor false positives and adjust
- **Evaluation Periods**: Balance between sensitivity and noise
- **Missing Data**: Configure appropriate handling

### Dashboard Optimization
- **Relevant Metrics**: Focus on business-critical metrics
- **Time Ranges**: Set appropriate default ranges
- **Widget Organization**: Group related metrics together

## Troubleshooting

### High CPU Alarm
1. Check application logs for performance issues
2. Review database query performance
3. Consider scaling up or out

### High Memory Alarm
1. Check for memory leaks in application
2. Review JVM heap settings
3. Monitor garbage collection metrics

### HTTP 5xx Errors
1. Check application error logs
2. Verify database connectivity
3. Check external service dependencies

### Database Connection Issues
1. Review connection pool configuration
2. Check for long-running transactions
3. Monitor database performance metrics