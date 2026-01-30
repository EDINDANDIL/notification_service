package com.example.worker.services;

import com.example.worker.models.NotificationSendDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.concurrent.ExecutionException;

/**
 * Service for processing and sending Web Push notifications from Kafka messages.
 */
@Service
public class WebPushService {

    private static final String WORKER_TOPIC = "worker_topic";
    private static final String KAFKA_CONCURRENCY = "60";
    private static final String NOTIFICATION_TITLE = "Новое уведомление";
    private static final String TITLE_FIELD = "title";
    private static final String BODY_FIELD = "body";

    private final ObjectMapper mapper;

    @Value("${key.private}")
    private String privateKey;

    @Getter
    @Value("${key.public}")
    private String publicKey;

    private PushService pushService;

    @Autowired
    public WebPushService(ObjectMapper mapper) {
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
     * Listens to Kafka topic and sends Web Push notifications.
     *
     * @param data the JSON string containing notification data
     * @throws IOException              if I/O error occurs
     * @throws JoseException            if JOSE error occurs
     * @throws GeneralSecurityException if security error occurs
     * @throws ExecutionException       if execution error occurs
     * @throws InterruptedException     if interrupted
     */
    @KafkaListener(topics = {WORKER_TOPIC}, concurrency = KAFKA_CONCURRENCY)
    public void sendNotification(String data)
            throws IOException, JoseException, GeneralSecurityException, ExecutionException, InterruptedException {
        NotificationSendDTO notificationDto = mapper.readValue(data, NotificationSendDTO.class);

        ObjectNode payload = mapper.createObjectNode();
        payload.put(TITLE_FIELD, NOTIFICATION_TITLE);
        payload.put(BODY_FIELD, notificationDto.getMessage());
        String jsonPayload = mapper.writeValueAsString(payload);

        Subscription subscription = mapper.readValue(
                notificationDto.getSubscriptionJson(), Subscription.class);
        Notification notification = new Notification(subscription, jsonPayload);

        pushService.send(notification);
    }
}
