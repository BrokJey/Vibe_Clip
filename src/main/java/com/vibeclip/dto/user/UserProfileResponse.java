package com.vibeclip.dto.user;

import com.vibeclip.dto.video.VideoResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {

    private UUID id;

    private String username;

    private boolean privateProfile;

    private boolean subscribed;

    private long subscribersCount;

    private long subscriptionsCount;

    private List<VideoResponse> videos;
}