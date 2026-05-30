package com.vibeclip.service;

import com.vibeclip.dto.subscription.SubscriptionRequestResponse;
import com.vibeclip.entity.Subscription;
import com.vibeclip.entity.SubscriptionStatus;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.exception.AlreadySubscribedException;
import com.vibeclip.exception.UserNotFoundException;
import com.vibeclip.repository.SubscriptionRepository;
import com.vibeclip.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public void subscribe(User subscriber, UUID targetId) {

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId.toString()));

        if (subscriber.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Нельзя подписаться на себя");
        }

        SubscriptionStatus desiredStatus = target.isPrivateProfile()
                ? SubscriptionStatus.PENDING
                : SubscriptionStatus.ACCEPTED;

        Optional<Subscription> existing = subscriptionRepository
                .findBySubscriberAndTarget(subscriber, target);

        if (existing.isPresent()) {
            Subscription sub = existing.get();
            if (sub.getStatus() == SubscriptionStatus.REJECTED) {
                sub.setStatus(desiredStatus);
                return;
            }
            throw new AlreadySubscribedException(targetId);
        }

        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .target(target)
                .status(desiredStatus)
                .build();

        subscriptionRepository.save(subscription);
    }

    /**
     * На публичном профиле подписка не требует одобрения — переводим устаревшие PENDING в ACCEPTED.
     */
    public void ensureAcceptedIfPublic(User subscriber, User target) {
        if (subscriber == null || target.isPrivateProfile()) {
            return;
        }
        if (subscriber.getId().equals(target.getId())) {
            return;
        }

        subscriptionRepository.findBySubscriberAndTarget(subscriber, target)
                .filter(sub -> sub.getStatus() == SubscriptionStatus.PENDING)
                .ifPresent(sub -> sub.setStatus(SubscriptionStatus.ACCEPTED));
    }

    public void unsubscribe(User subscriber, UUID targetId) {

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId.toString()));

        subscriptionRepository.deleteBySubscriberAndTarget(subscriber, target);
    }

    public boolean isSubscribed(User subscriber, User target) {

        return subscriptionRepository
                .findBySubscriberAndTarget(subscriber, target)
                .map(sub -> sub.getStatus() == SubscriptionStatus.ACCEPTED)
                .orElse(false);
    }

    public long getSubscribersCount(User user) {

        return subscriptionRepository.countByTargetAndStatus(user, SubscriptionStatus.ACCEPTED);
    }

    public long getSubscriptionsCount(User user) {

        return subscriptionRepository.countBySubscriberAndStatus(user, SubscriptionStatus.ACCEPTED);
    }

    public void acceptSubscription(User target, UUID subscriberId) {

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new UserNotFoundException(subscriberId.toString()));

        Subscription sub = subscriptionRepository
                .findBySubscriberAndTarget(subscriber, target)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        sub.setStatus(SubscriptionStatus.ACCEPTED);
    }

    public void rejectSubscription(User target, UUID subscriberId) {

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new UserNotFoundException(subscriberId.toString()));

        Subscription sub = subscriptionRepository
                .findBySubscriberAndTarget(subscriber, target)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        sub.setStatus(SubscriptionStatus.REJECTED);
    }

    public List<SubscriptionRequestResponse> getIncomingRequests(User me) {

        return subscriptionRepository
                .findByTargetAndStatus(me, SubscriptionStatus.PENDING)
                .stream()
                .map(sub -> SubscriptionRequestResponse.builder()
                        .subscriberId(sub.getSubscriber().getId())
                        .username(sub.getSubscriber().getUsername())
                        .build()
                )
                .toList();
    }

    public List<SubscriptionRequestResponse> getOutgoingRequests(User me) {

        return subscriptionRepository
                .findBySubscriberAndStatus(me, SubscriptionStatus.PENDING)
                .stream()
                .map(sub -> SubscriptionRequestResponse.builder()
                        .subscriberId(sub.getTarget().getId())
                        .username(sub.getTarget().getUsername())
                        .build()
                )
                .toList();
    }

    public List<User> getApprovedSubscriptions(User user) {
        return subscriptionRepository
                .findBySubscriberAndStatus(user, SubscriptionStatus.ACCEPTED)
                .stream()
                .map(Subscription::getTarget)
                .toList();
    }

    public List<SubscriptionRequestResponse> getAcceptedFollowing(User me) {

        return subscriptionRepository
                .findBySubscriberAndStatus(me, SubscriptionStatus.ACCEPTED)
                .stream()
                .map(sub -> SubscriptionRequestResponse.builder()
                        .subscriberId(sub.getTarget().getId())
                        .username(sub.getTarget().getUsername())
                        .build()
                )
                .toList();
    }

    public List<SubscriptionRequestResponse> getAcceptedFollowers(User me) {

        return subscriptionRepository
                .findByTargetAndStatus(me, SubscriptionStatus.ACCEPTED)
                .stream()
                .map(sub -> SubscriptionRequestResponse.builder()
                        .subscriberId(sub.getSubscriber().getId())
                        .username(sub.getSubscriber().getUsername())
                        .build()
                )
                .toList();
    }

    public boolean canViewProfile(User viewer, User author) {
        if (!author.isPrivateProfile()) {
            return true;
        }

        if (viewer != null && viewer.getId().equals(author.getId())) {
            return true;
        }

        return isSubscribed(viewer, author);
    }

    public boolean canViewVideo(User viewer, Video video) {
        return canViewProfile(viewer, video.getAuthor());
    }
}