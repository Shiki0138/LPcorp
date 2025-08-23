package com.enterprise.security.rbac.model.embedded;

import com.enterprise.security.rbac.model.enums.ClearanceLevel;
import com.enterprise.security.rbac.model.enums.RiskLevel;

import javax.persistence.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Embeddable role template configuration
 */
@Embeddable
public class RoleTemplate {
    
    @Column(name = "template_name")
    private String templateName;
    
    @Column(name = "template_description")
    private String templateDescription;
    
    @Column(name = "template_category")
    private String templateCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "required_clearance_level")
    private ClearanceLevel requiredClearanceLevel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "template_risk_level")
    private RiskLevel riskLevel = RiskLevel.LOW;
    
    @Column(name = "requires_manager_approval")
    private Boolean requiresManagerApproval = false;
    
    @Column(name = "requires_security_review")
    private Boolean requiresSecurityReview = false;
    
    @Column(name = "max_assignment_duration_days")
    private Integer maxAssignmentDurationDays;
    
    @Column(name = "auto_expire_assignments")
    private Boolean autoExpireAssignments = false;
    
    @Column(name = "default_department_restriction")
    private Boolean defaultDepartmentRestriction = false;
    
    @Column(name = "inheritance_allowed")
    private Boolean inheritanceAllowed = true;
    
    @Column(name = "delegation_allowed")
    private Boolean delegationAllowed = false;
    
    // Template-specific constraints
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_role_template_constraints",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @MapKeyColumn(name = "constraint_name")
    @Column(name = "constraint_value")
    private Map<String, String> constraints = new HashMap<>();
    
    // Required attributes for users
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_role_template_required_attrs",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value")
    private Map<String, String> requiredUserAttributes = new HashMap<>();
    
    @Column(name = "notification_settings")
    private String notificationSettings; // JSON format
    
    @Column(name = "compliance_requirements")
    private String complianceRequirements; // JSON format
    
    // Constructors
    public RoleTemplate() {}
    
    public RoleTemplate(String templateName, String templateDescription, ClearanceLevel requiredClearanceLevel) {
        this.templateName = templateName;
        this.templateDescription = templateDescription;
        this.requiredClearanceLevel = requiredClearanceLevel;
    }
    
    // Business methods
    public boolean requiresApproval() {
        return Boolean.TRUE.equals(requiresManagerApproval) || Boolean.TRUE.equals(requiresSecurityReview);
    }
    
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }
    
    public boolean hasConstraints() {
        return !constraints.isEmpty();
    }
    
    public boolean hasRequiredAttributes() {
        return !requiredUserAttributes.isEmpty();
    }
    
    public String getConstraint(String constraintName) {
        return constraints.get(constraintName);
    }
    
    public void setConstraint(String constraintName, String value) {
        constraints.put(constraintName, value);
    }
    
    public String getRequiredAttribute(String attributeName) {
        return requiredUserAttributes.get(attributeName);
    }
    
    public void setRequiredAttribute(String attributeName, String value) {
        requiredUserAttributes.put(attributeName, value);
    }
    
    // Getters and setters
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public String getTemplateDescription() { return templateDescription; }
    public void setTemplateDescription(String templateDescription) { this.templateDescription = templateDescription; }
    
    public String getTemplateCategory() { return templateCategory; }
    public void setTemplateCategory(String templateCategory) { this.templateCategory = templateCategory; }
    
    public ClearanceLevel getRequiredClearanceLevel() { return requiredClearanceLevel; }
    public void setRequiredClearanceLevel(ClearanceLevel requiredClearanceLevel) { this.requiredClearanceLevel = requiredClearanceLevel; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public Boolean getRequiresManagerApproval() { return requiresManagerApproval; }
    public void setRequiresManagerApproval(Boolean requiresManagerApproval) { this.requiresManagerApproval = requiresManagerApproval; }
    
    public Boolean getRequiresSecurityReview() { return requiresSecurityReview; }
    public void setRequiresSecurityReview(Boolean requiresSecurityReview) { this.requiresSecurityReview = requiresSecurityReview; }
    
    public Integer getMaxAssignmentDurationDays() { return maxAssignmentDurationDays; }
    public void setMaxAssignmentDurationDays(Integer maxAssignmentDurationDays) { this.maxAssignmentDurationDays = maxAssignmentDurationDays; }
    
    public Boolean getAutoExpireAssignments() { return autoExpireAssignments; }
    public void setAutoExpireAssignments(Boolean autoExpireAssignments) { this.autoExpireAssignments = autoExpireAssignments; }
    
    public Boolean getDefaultDepartmentRestriction() { return defaultDepartmentRestriction; }
    public void setDefaultDepartmentRestriction(Boolean defaultDepartmentRestriction) { this.defaultDepartmentRestriction = defaultDepartmentRestriction; }
    
    public Boolean getInheritanceAllowed() { return inheritanceAllowed; }
    public void setInheritanceAllowed(Boolean inheritanceAllowed) { this.inheritanceAllowed = inheritanceAllowed; }
    
    public Boolean getDelegationAllowed() { return delegationAllowed; }
    public void setDelegationAllowed(Boolean delegationAllowed) { this.delegationAllowed = delegationAllowed; }
    
    public Map<String, String> getConstraints() { return constraints; }
    public void setConstraints(Map<String, String> constraints) { this.constraints = constraints; }
    
    public Map<String, String> getRequiredUserAttributes() { return requiredUserAttributes; }
    public void setRequiredUserAttributes(Map<String, String> requiredUserAttributes) { this.requiredUserAttributes = requiredUserAttributes; }
    
    public String getNotificationSettings() { return notificationSettings; }
    public void setNotificationSettings(String notificationSettings) { this.notificationSettings = notificationSettings; }
    
    public String getComplianceRequirements() { return complianceRequirements; }
    public void setComplianceRequirements(String complianceRequirements) { this.complianceRequirements = complianceRequirements; }
}