package com.wade.wadeapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGuideResponse {
    private String message;
    private String generatedAt;
    private boolean cached;
    private Map<String, Boolean> activities;
}
