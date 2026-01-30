package com.example.notification_service.models;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class NotificationRequestDTO {
    private String[] names;
    private String message;
}
