# Deployment Guide - AWS CDK Infrastructure

## Overview

This guide provides step-by-step instructions for deploying the Ticketero infrastructure to AWS using CDK.

## Prerequisites

### 1. Install Node.js
```bash
# Download and install Node.js 18+ from https://nodejs.org/
node --version  # Should be 18+
npm --version
```

### 2. Install AWS CDK CLI
```bash
npm install -g aws-cdk@latest
cdk --version  # Should be 2.100.0+
```

### 3. Configure AWS Credentials
```bash
aws configure
# Enter your AWS Access Key ID
# Enter your Secret Access Key  
# Enter your default region (e.g., us-east-1)
# Enter output format (json)
```

### 4. Verify Java 21
```bash
java --version  # Should be 21+
mvn --version   # Should be 3.9+
```

## Deployment Steps

### Step 1: Bootstrap CDK (First Time Only)
```bash
cd ticketero-infra

# Set environment variables
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1

# Bootstrap CDK
cdk bootstrap
```

### Step 2: Compile and Test
```bash
# Compile the CDK project
mvn clean compile

# Run configuration tests
mvn test -Dtest=TicketeroStackTest

# Run CDK integration tests (requires Node.js)
mvn test -Dtest=TicketeroStackCdkTest
```

### Step 3: Synthesize CloudFormation
```bash
# List available stacks
cdk ls

# Synthesize templates (verify without deploying)
cdk synth ticketero-dev
cdk synth ticketero-prod
```

### Step 4: Deploy Development Environment
```bash
# Deploy dev stack
cdk deploy ticketero-dev --require-approval never

# Wait for deployment to complete (~15-20 minutes)
```

### Step 5: Build and Push Docker Image
```bash
# Get ECR repository URI from stack outputs
ECR_URI=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

echo "ECR URI: $ECR_URI"

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_URI

# Build and push image (from main project directory)
cd ../
docker build -t ticketero-api .
docker tag ticketero-api:latest $ECR_URI:latest
docker push $ECR_URI:latest
```

### Step 6: Update Telegram Secret
```bash
# Update the Telegram bot token (replace with your actual token)
aws secretsmanager put-secret-value \
  --secret-id ticketero-dev-telegram \
  --secret-string '{"token":"YOUR_ACTUAL_TELEGRAM_BOT_TOKEN"}'
```

### Step 7: Force ECS Deployment
```bash
# Force ECS service to use new image
aws ecs update-service \
  --cluster ticketero-dev-cluster \
  --service ticketero-dev-service \
  --force-new-deployment

# Wait for service to stabilize
aws ecs wait services-stable \
  --cluster ticketero-dev-cluster \
  --services ticketero-dev-service
```

### Step 8: Verify Deployment
```bash
# Get ALB DNS name
ALB_DNS=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text)

echo "Application URL: http://$ALB_DNS"

# Test health endpoint
curl http://$ALB_DNS/actuator/health

# Test API endpoint
curl -X POST http://$ALB_DNS/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"nationalId":"12345678","telefono":"+56912345678","branchOffice":"Centro","queueType":"CAJA"}'
```

## Production Deployment

### Deploy Production Stack
```bash
# Deploy production (similar process)
cdk deploy ticketero-prod --require-approval never

# Follow same steps 5-8 but with 'prod' instead of 'dev'
```

## Post-Deployment Configuration

### Database Migration
```bash
# Connect to RDS and run Flyway migrations
# This is typically done by the application on startup
```

### Monitoring Setup
```bash
# Production monitoring is automatically configured
# Access CloudWatch dashboard: ticketero-prod-dashboard
```

## Troubleshooting

### Common Issues

#### 1. CDK Bootstrap Failed
```bash
# Ensure you have proper AWS permissions
aws sts get-caller-identity

# Try bootstrapping with explicit account/region
cdk bootstrap aws://ACCOUNT-NUMBER/REGION
```

#### 2. Docker Push Failed
```bash
# Ensure ECR repository exists
aws ecr describe-repositories --repository-names ticketero-dev-api

# Re-authenticate with ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_URI
```

#### 3. ECS Service Not Starting
```bash
# Check ECS service events
aws ecs describe-services --cluster ticketero-dev-cluster --services ticketero-dev-service

# Check CloudWatch logs
aws logs tail /ecs/ticketero-dev-api --follow
```

#### 4. Health Check Failing
```bash
# Check application logs
aws logs filter-log-events --log-group-name /ecs/ticketero-dev-api --filter-pattern "ERROR"

# Verify secrets are accessible
aws secretsmanager get-secret-value --secret-id ticketero-dev-db-credentials
```

## Resource Cleanup

### Delete Development Environment
```bash
# Delete the stack (this will remove all resources)
cdk destroy ticketero-dev

# Confirm deletion when prompted
```

### Delete Production Environment
```bash
# Production has deletion protection enabled
# First disable deletion protection, then destroy
cdk destroy ticketero-prod
```

## Cost Monitoring

### Estimated Monthly Costs

| Environment | Cost |
|-------------|------|
| Development | ~$110 |
| Production | ~$210 |

### Cost Breakdown by Service
- **ECS Fargate**: ~$25-50
- **RDS PostgreSQL**: ~$15-45  
- **Amazon MQ**: ~$13
- **ALB**: ~$20
- **NAT Gateway**: ~$45-90
- **CloudWatch**: ~$2-8

### Cost Optimization Tips
1. **Stop dev environment** when not in use
2. **Use Spot instances** for non-critical workloads
3. **Monitor CloudWatch costs** and adjust retention
4. **Review RDS instance sizes** periodically

## Security Checklist

- [ ] AWS credentials properly configured
- [ ] Secrets Manager contains real credentials
- [ ] Security groups follow least privilege
- [ ] RDS is in private subnets
- [ ] ALB has proper SSL certificate (if HTTPS)
- [ ] CloudWatch logs retention configured
- [ ] IAM roles have minimal permissions

## Support

For issues with this deployment:
1. Check CloudWatch logs first
2. Verify all prerequisites are met
3. Ensure AWS permissions are correct
4. Review the troubleshooting section above