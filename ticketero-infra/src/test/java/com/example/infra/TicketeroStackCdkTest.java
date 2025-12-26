package com.example.infra;

import com.example.infra.config.EnvironmentConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import java.util.Map;

/**
 * CDK Integration Tests - Requires Node.js to be installed
 * Run these tests after installing Node.js and AWS CDK CLI
 */
class TicketeroStackCdkTest {
    
    @Test
    void devStackCreatesAllResources() {
        App app = new App();
        TicketeroStack stack = new TicketeroStack(app, "TestDev", null, EnvironmentConfig.dev());
        Template template = Template.fromStack(stack);
        
        // Networking resources
        template.resourceCountIs("AWS::EC2::VPC", 1);
        template.resourceCountIs("AWS::EC2::Subnet", 4); // 2 public + 2 private
        template.resourceCountIs("AWS::EC2::NatGateway", 1); // Dev has 1 NAT
        template.resourceCountIs("AWS::EC2::SecurityGroup", 4); // ALB, ECS, RDS, MQ
        template.resourceCountIs("AWS::EC2::InternetGateway", 1);
        
        // Database resources
        template.resourceCountIs("AWS::RDS::DBInstance", 1);
        template.resourceCountIs("AWS::SecretsManager::Secret", 3); // DB, MQ, Telegram
        
        // Messaging resources
        template.resourceCountIs("AWS::AmazonMQ::Broker", 1);
        
        // Container resources
        template.resourceCountIs("AWS::ECR::Repository", 1);
        template.resourceCountIs("AWS::ECS::Cluster", 1);
        template.resourceCountIs("AWS::ECS::Service", 1);
        template.resourceCountIs("AWS::ElasticLoadBalancingV2::LoadBalancer", 1);
        
        // Monitoring resources (dev has no alarms)
        template.resourceCountIs("AWS::CloudWatch::Alarm", 0);
        template.resourceCountIs("AWS::CloudWatch::Dashboard", 0);
        template.resourceCountIs("AWS::Logs::LogGroup", 1);
        
        // Verify VPC CIDR
        template.hasResourceProperties("AWS::EC2::VPC", Map.of(
            "CidrBlock", "10.0.0.0/16"
        ));
    }
    
    @Test
    void prodStackHasHighAvailability() {
        App app = new App();
        TicketeroStack stack = new TicketeroStack(app, "TestProd", null, EnvironmentConfig.prod());
        Template template = Template.fromStack(stack);
        
        // Prod should have 2 NAT Gateways for HA
        template.resourceCountIs("AWS::EC2::NatGateway", 2);
        
        // Prod should have Multi-AZ RDS
        template.hasResourceProperties("AWS::RDS::DBInstance", Map.of(
            "MultiAZ", true
        ));
        
        // Prod should have monitoring
        template.resourceCountIs("AWS::CloudWatch::Alarm", 4);
        template.resourceCountIs("AWS::CloudWatch::Dashboard", 1);
    }
    
    @Test
    void stackHasCorrectOutputs() {
        App app = new App();
        TicketeroStack stack = new TicketeroStack(app, "TestOutputs", null, EnvironmentConfig.dev());
        Template template = Template.fromStack(stack);
        
        // Verify key outputs exist
        template.hasOutput("LoadBalancerDNS", Map.of());
        template.hasOutput("EcrRepositoryUri", Map.of());
        template.hasOutput("DatabaseEndpoint", Map.of());
        template.hasOutput("MQEndpoint", Map.of());
    }
    
    @Test
    void securityGroupsHaveCorrectRules() {
        App app = new App();
        TicketeroStack stack = new TicketeroStack(app, "TestSG", null, EnvironmentConfig.dev());
        Template template = Template.fromStack(stack);
        
        // Verify security group count
        template.resourceCountIs("AWS::EC2::SecurityGroup", 4);
        
        // ALB should allow HTTP from anywhere
        template.hasResourceProperties("AWS::EC2::SecurityGroupIngress", Map.of(
            "IpProtocol", "tcp",
            "FromPort", 80,
            "ToPort", 80,
            "CidrIp", "0.0.0.0/0"
        ));
    }
}