package com.example.infra.aspects;

import software.amazon.awscdk.Aspects;
import software.amazon.awscdk.IAspect;
import software.amazon.awscdk.Tags;
import software.constructs.IConstruct;

/**
 * Aspect para aplicar tags automáticamente a todos los recursos.
 */
public class TaggingAspect implements IAspect {
    
    private final String environment;
    private final String project;
    private final String owner;
    private final String costCenter;
    
    public TaggingAspect(String environment, String project, String owner, String costCenter) {
        this.environment = environment;
        this.project = project;
        this.owner = owner;
        this.costCenter = costCenter;
    }
    
    @Override
    public void visit(IConstruct node) {
        Tags.of(node).add("Environment", environment);
        Tags.of(node).add("Project", project);
        Tags.of(node).add("Owner", owner);
        Tags.of(node).add("CostCenter", costCenter);
        Tags.of(node).add("ManagedBy", "CDK");
        Tags.of(node).add("CreatedBy", "TicketeroInfrastructure");
    }
    
    /**
     * Aplica tags a un construct y todos sus hijos.
     */
    public static void applyTo(IConstruct construct, String environment) {
        Aspects.of(construct).add(new TaggingAspect(
            environment,
            "Ticketero",
            "DevOps-Team",
            "ticketero-" + environment
        ));
    }
}