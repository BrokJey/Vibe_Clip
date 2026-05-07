package com.vibeclip.service;

import com.vibeclip.entity.Subscription;
import com.vibeclip.entity.User;
import com.vibeclip.repository.SubscriptionRepository;
import com.vibeclip.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public void subscribe(User subscriber, UUID targetId) {

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя подписаться на себя
        if (subscriber.getId().equals(target.getId())) {
            throw new RuntimeException("Нельзя подписаться на самого себя");
        }

        // Уже подписан
        if (subscriptionRepository.existsBySubscriberAndTarget(subscriber, target)) {
            throw new RuntimeException("Вы уже подписаны");
        }

        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .target(target)
                .build();

        subscriptionRepository.save(subscription);
    }

    public void unsubscribe(User subscriber, UUID targetId) {

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        subscriptionRepository.deleteBySubscriberAndTarget(subscriber, target);
    }

    public boolean isSubscribed(User subscriber, User target) {

        return subscriptionRepository.existsBySubscriberAndTarget(subscriber, target);
    }

    public long getSubscribersCount(User user) {

        return subscriptionRepository.countByTarget(user);
    }

    public long getSubscriptionsCount(User user) {

        return subscriptionRepository.countBySubscriber(user);
    }
}