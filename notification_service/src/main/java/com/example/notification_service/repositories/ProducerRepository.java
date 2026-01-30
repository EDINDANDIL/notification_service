package com.example.notification_service.repositories;

import com.example.notification_service.models.NotificationProducer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProducerRepository extends JpaRepository<NotificationProducer, Long> {
        Optional<NotificationProducer> findByProviderId(String id);
}
