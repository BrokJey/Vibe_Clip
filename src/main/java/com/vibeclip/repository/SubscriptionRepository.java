package com.vibeclip.repository;

import com.vibeclip.entity.Subscription;
import com.vibeclip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsBySubscriberAndTarget(User subscriber, User target);

    long countByTarget(User target);

    long countBySubscriber(User subscriber);

    void deleteBySubscriberAndTarget(User subscriber, User target);

    Optional<Subscription> findBySubscriberAndTarget(User subscriber, User target);
}