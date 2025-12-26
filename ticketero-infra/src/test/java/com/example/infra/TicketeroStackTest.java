package com.example.infra;

import com.example.infra.config.EnvironmentConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketeroStackTest {
    
    @Test
    void devConfigHasCorrectSettings() {
        EnvironmentConfig config = EnvironmentConfig.dev();
        
        assertEquals("dev", config.envName());
        assertEquals("10.0.0.0/16", config.vpcCidr());
        assertEquals(1, config.natGateways());
        assertEquals(1, config.desiredCount());
        assertEquals(1, config.minCapacity());
        assertEquals(2, config.maxCapacity());
        assertEquals(512, config.taskCpu());
        assertEquals(1024, config.taskMemory());
        assertEquals(20, config.dbAllocatedStorage());
        assertFalse(config.multiAz());
        assertEquals("mq.t3.micro", config.mqInstanceType());
        assertFalse(config.enableAlarms());
        assertEquals(7, config.logRetentionDays());
        assertFalse(config.isProd());
    }
    
    @Test
    void prodConfigHasCorrectSettings() {
        EnvironmentConfig config = EnvironmentConfig.prod();
        
        assertEquals("prod", config.envName());
        assertEquals("10.0.0.0/16", config.vpcCidr());
        assertEquals(2, config.natGateways());
        assertEquals(2, config.desiredCount());
        assertEquals(2, config.minCapacity());
        assertEquals(4, config.maxCapacity());
        assertEquals(512, config.taskCpu());
        assertEquals(1024, config.taskMemory());
        assertEquals(50, config.dbAllocatedStorage());
        assertTrue(config.multiAz());
        assertEquals("mq.t3.micro", config.mqInstanceType());
        assertTrue(config.enableAlarms());
        assertEquals(14, config.logRetentionDays());
        assertTrue(config.isProd());
    }
    
    @Test
    void resourceNameGenerationWorks() {
        EnvironmentConfig devConfig = EnvironmentConfig.dev();
        EnvironmentConfig prodConfig = EnvironmentConfig.prod();
        
        assertEquals("ticketero-dev-vpc", devConfig.resourceName("vpc"));
        assertEquals("ticketero-prod-database", prodConfig.resourceName("database"));
    }
    
    @Test
    void devEnvironmentHasCorrectCostProfile() {
        EnvironmentConfig config = EnvironmentConfig.dev();
        
        // Cost optimization settings for dev
        assertEquals(1, config.natGateways()); // Single NAT Gateway
        assertEquals(1, config.desiredCount()); // Single task
        assertFalse(config.multiAz()); // No Multi-AZ
        assertFalse(config.enableAlarms()); // No monitoring costs
        assertEquals(20, config.dbAllocatedStorage()); // Minimal storage
    }
    
    @Test
    void prodEnvironmentHasCorrectHAProfile() {
        EnvironmentConfig config = EnvironmentConfig.prod();
        
        // High availability settings for prod
        assertEquals(2, config.natGateways()); // HA NAT Gateways
        assertEquals(2, config.desiredCount()); // Multiple tasks
        assertTrue(config.multiAz()); // Multi-AZ database
        assertTrue(config.enableAlarms()); // Full monitoring
        assertEquals(50, config.dbAllocatedStorage()); // Production storage
        assertTrue(config.isProd()); // Deletion protection
    }
    
    // Note: CDK assertion tests require Node.js to be installed
    // These tests will be enabled once Node.js is available
    // For now, we validate the configuration logic above
}