package com.example.infra;

import com.example.infra.aspects.TaggingAspect;
import com.example.infra.config.EnvironmentConfig;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * Entry point de la aplicación CDK.
 */
public class TicketeroApp {
    
    public static void main(final String[] args) {
        App app = new App();
        
        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region = System.getenv("CDK_DEFAULT_REGION");
        
        if (account == null || region == null) {
            account = (String) app.getNode().tryGetContext("accountId");
            region = (String) app.getNode().tryGetContext("region");
        }
        
        Environment awsEnv = Environment.builder()
            .account(account)
            .region(region)
            .build();
        
        // Stack Dev
        EnvironmentConfig devConfig = EnvironmentConfig.dev();
        TicketeroStack devStack = new TicketeroStack(app, "ticketero-dev", 
            StackProps.builder()
                .env(awsEnv)
                .description("Ticketero - Development")
                .build(), 
            devConfig);
        TaggingAspect.applyTo(devStack, devConfig.envName());
        
        // Stack Prod
        EnvironmentConfig prodConfig = EnvironmentConfig.prod();
        TicketeroStack prodStack = new TicketeroStack(app, "ticketero-prod", 
            StackProps.builder()
                .env(awsEnv)
                .description("Ticketero - Production")
                .build(), 
            prodConfig);
        TaggingAspect.applyTo(prodStack, prodConfig.envName());
        
        app.synth();
    }
}