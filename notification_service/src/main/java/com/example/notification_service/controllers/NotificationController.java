package com.example.notification_service.controllers;

import com.example.notification_service.models.NotificationProducer;
import com.example.notification_service.models.NotificationRequestDTO;
import com.example.notification_service.models.NotificationSendDTO;
import com.example.notification_service.models.NotificationSubscriber;
import com.example.notification_service.repositories.ProducerRepository;
import com.example.notification_service.repositories.SubscriberRepository;
import com.example.notification_service.services.NotificationPushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for handling notification sending operations.
 */
@RestController
public class NotificationController {

    private final ProducerRepository producerRepository;
    private final SubscriberRepository subscriberRepository;
    private final NotificationPushService pushService;

    @Autowired
    public NotificationController(
            ProducerRepository producerRepository,
            SubscriberRepository subscriberRepository,
            ObjectMapper mapper,
            NotificationPushService pushService) {
        this.producerRepository = producerRepository;
        this.subscriberRepository = subscriberRepository;
        this.pushService = pushService;
    }

    /**
     * Sends notifications to subscribers matching the provided names.
     *
     * @param dto the notification request containing message and subscriber names
     * @param id  the producer ID
     * @return response with success status
     * @throws JsonProcessingException if JSON processing fails
     */
    @PostMapping("/notificate")
    public ResponseEntity<Map<String, String>> notificate(
            @RequestBody NotificationRequestDTO dto,
            @RequestParam("id") String id) throws JsonProcessingException {

        Optional<NotificationProducer> optional = producerRepository.findByProviderId(id);

        if (optional.isPresent()) {
            NotificationProducer producer = optional.get();
            List<NotificationSubscriber> subscribers = subscriberRepository.getAllByProducer(producer);
            List<NotificationSubscriber> filteredSubscribers = filterSubscribersByName(
                    subscribers, dto.getNames());

            List<NotificationSendDTO> toSend = filteredSubscribers.stream()
                    .map(subscriber -> new NotificationSendDTO(
                            subscriber.getSubscriptionJson(),
                            dto.getMessage()))
                    .collect(Collectors.toList());

            if (!toSend.isEmpty()) {
                pushService.sendNotifications(toSend);
            }
        }

        return ResponseEntity.ok(Map.of("status", "successfully sent to all available users"));
    }

    private List<NotificationSubscriber> filterSubscribersByName(
            List<NotificationSubscriber> subscribers, String[] names) {
        List<String> nameList = Arrays.asList(names);
        return subscribers.stream()
                .filter(subscriber -> nameList.contains(subscriber.getName()))
                .collect(Collectors.toList());
    }
}
