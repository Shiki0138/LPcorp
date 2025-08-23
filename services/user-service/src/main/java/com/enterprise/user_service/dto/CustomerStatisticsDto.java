package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer Statistics DTO for API responses
 * Maps to the CustomerStatistics schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer statistics and metrics")
public class CustomerStatisticsDto {
    
    @Schema(description = "Total number of orders", example = "42")
    private Integer totalOrders;
    
    @Schema(description = "Total amount spent", example = "2549.99")
    private BigDecimal totalSpent;
    
    @Schema(description = "Average order value", example = "60.71")
    private BigDecimal averageOrderValue;
    
    @Schema(description = "Last order date", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastOrderDate;
    
    @Schema(description = "Customer lifetime value", example = "3200.50")
    private BigDecimal lifetimeValue;
    
    // Additional statistics
    @Schema(description = "Number of returned orders", example = "3")
    private Integer returnedOrders;
    
    @Schema(description = "Return rate percentage", example = "7.14")
    private BigDecimal returnRate;
    
    @Schema(description = "Number of support tickets", example = "5")
    private Integer supportTickets;
    
    @Schema(description = "Customer satisfaction score (1-10)", example = "8.5")
    private BigDecimal satisfactionScore;
    
    @Schema(description = "Number of referrals made", example = "2")
    private Integer referralsMade;
    
    @Schema(description = "Days since first order", example = "365")
    private Integer daysSinceFirstOrder;
    
    @Schema(description = "Days since last order", example = "30")
    private Integer daysSinceLastOrder;
    
    @Schema(description = "Most frequently ordered category", example = "Electronics")
    private String favoriteCategory;
    
    @Schema(description = "Preferred payment method", example = "Credit Card")
    private String preferredPaymentMethod;
    
    @Schema(description = "Number of addresses", example = "2")
    private Integer addressCount;
    
    @Schema(description = "Email open rate percentage", example = "65.5")
    private BigDecimal emailOpenRate;
    
    @Schema(description = "Email click-through rate percentage", example = "12.3")
    private BigDecimal emailClickRate;
    
    @Schema(description = "Website visit count", example = "127")
    private Integer websiteVisits;
    
    @Schema(description = "Mobile app usage count", example = "89")
    private Integer mobileAppUsage;
    
    @Schema(description = "Social media engagement score", example = "45.2")
    private BigDecimal socialEngagementScore;
}