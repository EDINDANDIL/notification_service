package com.example.notification_service.services;

import com.example.notification_service.models.NotificationSendDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Urgency;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service for handling Web Push notification operations and sending notifications via Kafka.
 */
@Service
public class NotificationPushService {

    private static final String WORKER_TOPIC = "worker_topic";
    private static final String TEST_NOTIFICATION_JSON = "{\"title\":\"Привет\",\"body\":\"Сообщение из Java!\"}";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    @Value("${key.private}")
    private String privateKey;

    @Getter
    @Value("${key.public}")
    private String publicKey;

    private PushService pushService;

    @Autowired
    public NotificationPushService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    /**
     * Initializes the PushService with BouncyCastle provider.
     *
     * @throws GeneralSecurityException if security initialization fails
     */
    @PostConstruct
    private void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey);
    }

    /**
     * Sends notifications to Kafka topic for asynchronous processing.
     *
     * @param notifications list of notifications to send
     * @throws JsonProcessingException if JSON serialization fails
     */
    public void sendNotifications(List<NotificationSendDTO> notifications) throws JsonProcessingException {
        for (NotificationSendDTO notification : notifications) {
            kafkaTemplate.send(WORKER_TOPIC, mapper.writeValueAsString(notification));
        }
    }

    /**
     * Sends a test notification to verify subscription.
     *
     * @param subscription the Web Push subscription
     * @throws GeneralSecurityException if security error occurs
     * @throws IOException              if I/O error occurs
     * @throws ExecutionException       if execution error occurs
     * @throws InterruptedException     if interrupted
     * @throws JoseException            if JOSE error occurs
     */
    public void send(Subscription subscription)
            throws GeneralSecurityException, IOException, ExecutionException, InterruptedException, JoseException {
        Notification notification = new Notification(
                subscription,
                TEST_NOTIFICATION_JSON,
                Urgency.NORMAL
        );
        pushService.send(notification);
    }
}
