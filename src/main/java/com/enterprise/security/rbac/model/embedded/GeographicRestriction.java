package com.enterprise.security.rbac.model.embedded;

import javax.persistence.*;
import java.util.Set;
import java.util.HashSet;

/**
 * Embeddable geographic restriction configuration
 */
@Embeddable
public class GeographicRestriction {
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_geo_allowed_countries",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @Column(name = "country_code")
    private Set<String> allowedCountries = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_geo_blocked_countries",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @Column(name = "country_code")
    private Set<String> blockedCountries = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_geo_allowed_regions",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @Column(name = "region_code")
    private Set<String> allowedRegions = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_geo_allowed_ip_ranges",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @Column(name = "ip_range")
    private Set<String> allowedIpRanges = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_geo_blocked_ip_ranges",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @Column(name = "ip_range")
    private Set<String> blockedIpRanges = new HashSet<>();
    
    @Column(name = "require_vpn")
    private Boolean requireVpn = false;
    
    @Column(name = "require_corporate_network")
    private Boolean requireCorporateNetwork = false;
    
    @Column(name = "data_residency_region")
    private String dataResidencyRegion;
    
    @Column(name = "geo_fencing_enabled")
    private Boolean geoFencingEnabled = false;
    
    // Constructors
    public GeographicRestriction() {}
    
    public GeographicRestriction(Set<String> allowedCountries) {
        this.allowedCountries = allowedCountries;
    }
    
    // Business methods
    public boolean isCountryAllowed(String countryCode) {
        if (!blockedCountries.isEmpty() && blockedCountries.contains(countryCode)) {
            return false;
        }
        
        if (!allowedCountries.isEmpty()) {
            return allowedCountries.contains(countryCode);
        }
        
        return true; // No restrictions means allowed
    }
    
    public boolean isIpAllowed(String ipAddress) {
        // Check blocked ranges first
        for (String blockedRange : blockedIpRanges) {
            if (isIpInRange(ipAddress, blockedRange)) {
                return false;
            }
        }
        
        // If allowed ranges are specified, check them
        if (!allowedIpRanges.isEmpty()) {
            for (String allowedRange : allowedIpRanges) {
                if (isIpInRange(ipAddress, allowedRange)) {
                    return true;
                }
            }
            return false; // Not in any allowed range
        }
        
        return true; // No restrictions means allowed
    }
    
    public boolean hasGeographicRestrictions() {
        return !allowedCountries.isEmpty() || !blockedCountries.isEmpty() ||
               !allowedRegions.isEmpty() || !allowedIpRanges.isEmpty() ||
               !blockedIpRanges.isEmpty() || Boolean.TRUE.equals(requireVpn) ||
               Boolean.TRUE.equals(requireCorporateNetwork);
    }
    
    public boolean requiresVpn() {
        return Boolean.TRUE.equals(requireVpn);
    }
    
    public boolean requiresCorporateNetwork() {
        return Boolean.TRUE.equals(requireCorporateNetwork);
    }
    
    private boolean isIpInRange(String ip, String range) {
        // Simple implementation - would need proper CIDR matching in production
        if (range.contains("/")) {
            // CIDR notation
            return matchesCIDR(ip, range);
        } else {
            // Single IP or simple range
            return ip.equals(range);
        }
    }
    
    private boolean matchesCIDR(String ip, String cidr) {
        // Implement CIDR matching logic
        // This is a placeholder - use proper library like Apache Commons Net
        return false;
    }
    
    // Getters and setters
    public Set<String> getAllowedCountries() { return allowedCountries; }
    public void setAllowedCountries(Set<String> allowedCountries) { this.allowedCountries = allowedCountries; }
    
    public Set<String> getBlockedCountries() { return blockedCountries; }
    public void setBlockedCountries(Set<String> blockedCountries) { this.blockedCountries = blockedCountries; }
    
    public Set<String> getAllowedRegions() { return allowedRegions; }
    public void setAllowedRegions(Set<String> allowedRegions) { this.allowedRegions = allowedRegions; }
    
    public Set<String> getAllowedIpRanges() { return allowedIpRanges; }
    public void setAllowedIpRanges(Set<String> allowedIpRanges) { this.allowedIpRanges = allowedIpRanges; }
    
    public Set<String> getBlockedIpRanges() { return blockedIpRanges; }
    public void setBlockedIpRanges(Set<String> blockedIpRanges) { this.blockedIpRanges = blockedIpRanges; }
    
    public Boolean getRequireVpn() { return requireVpn; }
    public void setRequireVpn(Boolean requireVpn) { this.requireVpn = requireVpn; }
    
    public Boolean getRequireCorporateNetwork() { return requireCorporateNetwork; }
    public void setRequireCorporateNetwork(Boolean requireCorporateNetwork) { this.requireCorporateNetwork = requireCorporateNetwork; }
    
    public String getDataResidencyRegion() { return dataResidencyRegion; }
    public void setDataResidencyRegion(String dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; }
    
    public Boolean getGeoFencingEnabled() { return geoFencingEnabled; }
    public void setGeoFencingEnabled(Boolean geoFencingEnabled) { this.geoFencingEnabled = geoFencingEnabled; }
}