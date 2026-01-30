package com.example.postman.repositories;

import com.example.postman.models.NotificationProducer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationProducerRepository extends JpaRepository<NotificationProducer, Long> {}
