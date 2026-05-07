package com.vibeclip.dto.subscription;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SubscriptionRequestResponse {

    private UUID subscriberId;
    private String username;
}