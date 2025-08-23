package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Customer Segment DTO for API responses
 * Maps to the CustomerSegment schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer segment information")
public class CustomerSegmentDto {
    
    @Schema(description = "Segment unique identifier", format = "uuid", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Segment name", example = "High Value Customers")
    private String name;
    
    @Schema(description = "Segment description", example = "Customers with high lifetime value and frequent purchases")
    private String description;
    
    @Schema(description = "Segment criteria as key-value pairs")
    private Map<String, Object> criteria;
    
    @Schema(description = "Number of customers in this segment", example = "1250")
    private Integer customerCount;
    
    @Schema(description = "Segment active status")
    private Boolean active;
    
    @Schema(description = "Segment priority (1-10, 1 being highest)", example = "3")
    private Integer priority;
    
    @Schema(description = "Segment color for UI display", example = "#FF5733")
    private String color;
    
    @Schema(description = "Segment tags for categorization")
    private String[] tags;
}