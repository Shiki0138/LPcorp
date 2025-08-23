package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Resource entity for fine-grained access control
 */
@Entity
@Table(name = "rbac_resources", indexes = {
    @Index(name = "idx_resource_type", columnList = "resourceType"),
    @Index(name = "idx_resource_identifier", columnList = "identifier"),
    @Index(name = "idx_resource_tenant", columnList = "tenantId"),
    @Index(name = "idx_resource_owner", columnList = "ownerId"),
    @Index(name = "idx_resource_parent", columnList = "parentResourceId")
})
public class Resource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Resource type is required")
    private String resourceType;
    
    @Column(nullable = false, length = 500)
    @NotBlank(message = "Resource identifier is required")
    private String identifier;
    
    @Column(nullable = false, length = 255)
    @NotBlank(message = "Resource name is required")
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column
    private String departmentId;
    
    @Column
    private String ownerId;
    
    // Hierarchical structure
    @Column
    private String parentResourceId;
    
    @Column
    private String resourcePath; // Full hierarchical path
    
    @Column
    private Integer hierarchyLevel = 0;
    
    // Classification and security
    @Enumerated(EnumType.STRING)
    private DataClassification classification = DataClassification.PUBLIC;
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;
    
    @Column
    private String securityLabel;
    
    @Column
    private String complianceTag;
    
    // Status and lifecycle
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceStatus status = ResourceStatus.ACTIVE;
    
    @Column
    private Instant expiresAt;
    
    @Column
    private Boolean inherited = true; // Whether permissions can be inherited
    
    // Attributes for ABAC
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_resource_attributes",
        joinColumns = @JoinColumn(name = "resource_id")
    )
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value")
    private Map<String, String> attributes = new HashMap<>();
    
    // Metadata
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_resource_metadata",
        joinColumns = @JoinColumn(name = "resource_id")
    )
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata = new HashMap<>();
    
    // Location and geography
    @Column
    private String location;
    
    @Column
    private String geographicRegion;
    
    @Column
    private String dataResidencyRegion;
    
    // Business context
    @Column
    private String businessContext;
    
    @Column
    private String costCenter;
    
    @Column
    private String projectId;
    
    // Audit fields
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column
    private String createdBy;
    
    @Column
    private String updatedBy;
    
    @Column
    private Instant lastAccessedAt;
    
    @Column
    private String lastAccessedBy;
    
    // Constructors
    public Resource() {}
    
    public Resource(String resourceType, String identifier, String name, String tenantId) {
        this.resourceType = resourceType;
        this.identifier = identifier;
        this.name = name;
        this.tenantId = tenantId;
    }
    
    // Business methods
    public boolean isActive() {
        return status == ResourceStatus.ACTIVE;
    }
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
    
    public boolean isAccessible() {
        return isActive() && !isExpired();
    }
    
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }
    
    public boolean isClassified() {
        return classification != DataClassification.PUBLIC;
    }
    
    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }
    
    public String getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }
    
    public void setAttribute(String attributeName, String value) {
        attributes.put(attributeName, value);
    }
    
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
    
    public String getFullPath() {
        if (resourcePath != null) {
            return resourcePath;
        }
        return "/" + resourceType + "/" + identifier;
    }
    
    public boolean isChildOf(String parentId) {
        return parentResourceId != null && parentResourceId.equals(parentId);
    }
    
    public boolean belongsToTenant(String tenant) {
        return tenantId.equals(tenant);
    }
    
    public boolean belongsToDepartment(String department) {
        return departmentId != null && departmentId.equals(department);
    }
    
    public boolean isOwnedBy(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    
    public String getParentResourceId() { return parentResourceId; }
    public void setParentResourceId(String parentResourceId) { this.parentResourceId = parentResourceId; }
    
    public String getResourcePath() { return resourcePath; }
    public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }
    
    public Integer getHierarchyLevel() { return hierarchyLevel; }
    public void setHierarchyLevel(Integer hierarchyLevel) { this.hierarchyLevel = hierarchyLevel; }
    
    public DataClassification getClassification() { return classification; }
    public void setClassification(DataClassification classification) { this.classification = classification; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public String getSecurityLabel() { return securityLabel; }
    public void setSecurityLabel(String securityLabel) { this.securityLabel = securityLabel; }
    
    public String getComplianceTag() { return complianceTag; }
    public void setComplianceTag(String complianceTag) { this.complianceTag = complianceTag; }
    
    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Boolean getInherited() { return inherited; }
    public void setInherited(Boolean inherited) { this.inherited = inherited; }
    
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getGeographicRegion() { return geographicRegion; }
    public void setGeographicRegion(String geographicRegion) { this.geographicRegion = geographicRegion; }
    
    public String getDataResidencyRegion() { return dataResidencyRegion; }
    public void setDataResidencyRegion(String dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; }
    
    public String getBusinessContext() { return businessContext; }
    public void setBusinessContext(String businessContext) { this.businessContext = businessContext; }
    
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(Instant lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public String getLastAccessedBy() { return lastAccessedBy; }
    public void setLastAccessedBy(String lastAccessedBy) { this.lastAccessedBy = lastAccessedBy; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;
        Resource resource = (Resource) o;
        return id != null && id.equals(resource.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Resource{" +
                "id='" + id + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}