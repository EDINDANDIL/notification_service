package com.example.notification_service.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationSendDTO {
    String subscriptionJson;
    String message;
}
