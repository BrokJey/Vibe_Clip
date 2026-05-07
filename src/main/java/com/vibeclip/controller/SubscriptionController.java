package com.vibeclip.controller;

import com.vibeclip.entity.User;
import com.vibeclip.service.SubscriptionService;
import com.vibeclip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(UserService userService, SubscriptionService subscriptionService
    ) {
        super(userService);
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{targetId}")
    public ResponseEntity<Void> subscribe(@PathVariable UUID targetId, Authentication authentication
    ) {

        User currentUser = getCurrentUser(authentication);

        subscriptionService.subscribe(currentUser, targetId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> unsubscribe(@PathVariable UUID targetId, Authentication authentication
    ) {

        User currentUser = getCurrentUser(authentication);

        subscriptionService.unsubscribe(currentUser, targetId);

        return ResponseEntity.noContent().build();
    }
}