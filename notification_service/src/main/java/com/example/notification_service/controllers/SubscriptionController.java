package com.example.notification_service.controllers;

import com.example.notification_service.models.BasicUser;
import com.example.notification_service.models.NotificationProducer;
import com.example.notification_service.models.NotificationSubscriber;
import com.example.notification_service.models.OAuthUser;
import com.example.notification_service.repositories.ProducerRepository;
import com.example.notification_service.repositories.SubscriberRepository;
import com.example.notification_service.services.NotificationPushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Subscription;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Controller for handling subscription operations including saving, retrieving,
 * and managing push notification subscriptions.
 */
@RestController
public class SubscriptionController {

    private final ProducerRepository producerRepository;
    private final SubscriberRepository subscriberRepository;
    private final NotificationPushService pushService;
    private final ObjectMapper mapper;

    @Autowired
    public SubscriptionController(
            ProducerRepository producerRepository,
            SubscriberRepository subscriberRepository,
            NotificationPushService pushService,
            ObjectMapper mapper) {
        this.producerRepository = producerRepository;
        this.subscriberRepository = subscriberRepository;
        this.pushService = pushService;
        this.mapper = mapper;
    }

    /**
     * Retrieves the public key for Web Push subscriptions.
     *
     * @return response containing the public key
     */
    @GetMapping("/get_key")
    public ResponseEntity<Map<String, String>> getKey() {
        return ResponseEntity.ok(Map.of("key", pushService.getPublicKey()));
    }

    /**
     * Retrieves the current authenticated user's ID and creates a producer if needed.
     *
     * @return response containing the user ID or unauthorized status
     */
    @GetMapping("/get_currentId")
    public ResponseEntity<Map<String, String>> getId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuthUser oAuthUser) {
            ensureProducerExists(oAuthUser.getProviderId());
            return ResponseEntity.ok(Map.of("id", oAuthUser.getProviderId()));
        } else if (principal instanceof BasicUser basicUser) {
            ensureProducerExists(basicUser.getUsername());
            return ResponseEntity.ok(Map.of("id", basicUser.getUsername()));
        }

        return ResponseEntity.status(401).body(Map.of("status", "unauthorized"));
    }

    /**
     * Saves a push notification subscription for a user.
     *
     * @param subscription the Web Push subscription object
     * @param id           the producer ID
     * @param name         the subscriber name
     * @return response indicating success or failure
     * @throws IOException              if I/O error occurs
     * @throws JoseException            if JOSE error occurs
     * @throws GeneralSecurityException if security error occurs
     * @throws ExecutionException       if execution error occurs
     * @throws InterruptedException     if interrupted
     */
    @PostMapping("/save-subscription/{id}")
    public ResponseEntity<String> saveSubscription(
            @RequestBody Subscription subscription,
            @PathVariable("id") String id,
            @RequestParam String name)
            throws IOException, JoseException, GeneralSecurityException, ExecutionException, InterruptedException {

        Optional<NotificationProducer> optional = producerRepository.findByProviderId(id);
        pushService.send(subscription);

        if (optional.isPresent() && name != null && !name.isEmpty()) {
            NotificationProducer producer = optional.get();

            NotificationSubscriber subscriber = new NotificationSubscriber();
            subscriber.setProducer(producer);
            subscriber.setName(name);
            subscriber.setSubscriptionJson(mapper.writeValueAsString(subscription));
            subscriberRepository.save(subscriber);

            return ResponseEntity.ok("saved");
        }

        return ResponseEntity.badRequest().body("no such user");
    }

    /**
     * Removes a push notification subscription.
     *
     * @param subscription the subscription to remove
     * @return response indicating success
     * @throws JsonProcessingException if JSON processing fails
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestBody Subscription subscription)
            throws JsonProcessingException {
        String subscriptionJson = mapper.writeValueAsString(subscription);
        Optional<NotificationSubscriber> optional = subscriberRepository.findBySubscriptionJson(subscriptionJson);
        optional.ifPresent(subscriberRepository::delete);
        return ResponseEntity.ok("deleted");
    }

    private void ensureProducerExists(String providerId) {
        if (producerRepository.findByProviderId(providerId).isEmpty()) {
            NotificationProducer producer = NotificationProducer.builder()
                    .providerId(providerId)
                    .build();
            producerRepository.save(producer);
        }
    }
}
