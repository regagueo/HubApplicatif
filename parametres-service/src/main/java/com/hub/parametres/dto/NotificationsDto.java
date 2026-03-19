package com.hub.parametres.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsDto {
    private Boolean emailAlerts;
    private Boolean pushEnabled;
    private Boolean smsEnabled;
}
