package com.enterprise.security.rbac.model.embedded;

import javax.persistence.*;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.Set;
import java.util.HashSet;

/**
 * Embeddable time restriction configuration
 */
@Embeddable
public class TimeRestriction {
    
    @Column(name = "time_start")
    private LocalTime startTime;
    
    @Column(name = "time_end")
    private LocalTime endTime;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_time_allowed_days",
        joinColumns = @JoinColumn(name = "entity_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> allowedDays = new HashSet<>();
    
    @Column(name = "timezone")
    private String timezone = "UTC";
    
    @Column(name = "enforce_business_hours")
    private Boolean enforceBusinessHours = false;
    
    // Holiday restrictions
    @Column(name = "block_holidays")
    private Boolean blockHolidays = false;
    
    @Column(name = "holiday_calendar")
    private String holidayCalendar; // Reference to holiday calendar
    
    // Constructors
    public TimeRestriction() {}
    
    public TimeRestriction(LocalTime startTime, LocalTime endTime, Set<DayOfWeek> allowedDays) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.allowedDays = allowedDays;
    }
    
    // Business methods
    public boolean isCurrentlyAllowed() {
        LocalTime now = LocalTime.now();
        DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        
        // Check day of week
        if (!allowedDays.isEmpty() && !allowedDays.contains(today)) {
            return false;
        }
        
        // Check time range
        if (startTime != null && endTime != null) {
            if (startTime.isBefore(endTime)) {
                // Normal time range (e.g., 9:00 - 17:00)
                return !now.isBefore(startTime) && !now.isAfter(endTime);
            } else {
                // Overnight time range (e.g., 22:00 - 06:00)
                return !now.isBefore(startTime) || !now.isAfter(endTime);
            }
        }
        
        return true;
    }
    
    public boolean isBusinessHoursOnly() {
        return Boolean.TRUE.equals(enforceBusinessHours);
    }
    
    public boolean hasTimeRestrictions() {
        return startTime != null || endTime != null || !allowedDays.isEmpty();
    }
    
    // Getters and setters
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public Set<DayOfWeek> getAllowedDays() { return allowedDays; }
    public void setAllowedDays(Set<DayOfWeek> allowedDays) { this.allowedDays = allowedDays; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public Boolean getEnforceBusinessHours() { return enforceBusinessHours; }
    public void setEnforceBusinessHours(Boolean enforceBusinessHours) { this.enforceBusinessHours = enforceBusinessHours; }
    
    public Boolean getBlockHolidays() { return blockHolidays; }
    public void setBlockHolidays(Boolean blockHolidays) { this.blockHolidays = blockHolidays; }
    
    public String getHolidayCalendar() { return holidayCalendar; }
    public void setHolidayCalendar(String holidayCalendar) { this.holidayCalendar = holidayCalendar; }
}