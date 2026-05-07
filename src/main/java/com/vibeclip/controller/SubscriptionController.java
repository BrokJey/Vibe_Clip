package com.vibeclip.controller;

import com.vibeclip.dto.subscription.SubscriptionRequestResponse;
import com.vibeclip.entity.User;
import com.vibeclip.service.SubscriptionService;
import com.vibeclip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(UserService userService, SubscriptionService subscriptionService) {
        super(userService);
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{targetId}")
    public ResponseEntity<Void> subscribe(@PathVariable UUID targetId, Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        subscriptionService.subscribe(currentUser, targetId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> unsubscribe(@PathVariable UUID targetId, Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        subscriptionService.unsubscribe(currentUser, targetId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{subscriberId}/accept")
    public ResponseEntity<Void> accept(@PathVariable UUID subscriberId, Authentication authentication) {
        User me = getCurrentUser(authentication);
        subscriptionService.acceptSubscription(me, subscriberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriberId}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID subscriberId, Authentication authentication) {
        User me = getCurrentUser(authentication);
        subscriptionService.rejectSubscription(me, subscriberId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<SubscriptionRequestResponse>> getIncomingRequests(Authentication authentication) {
        User me = getCurrentUser(authentication);

        return ResponseEntity.ok(subscriptionService.getIncomingRequests(me));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<SubscriptionRequestResponse>> getOutgoingRequests(Authentication authentication) {
        User me = getCurrentUser(authentication);

        return ResponseEntity.ok(subscriptionService.getOutgoingRequests(me));
    }
}