package com.vibeclip.controller;

import com.vibeclip.dto.user.UpdatePrivacyRequest;
import com.vibeclip.dto.user.UserProfileResponse;
import com.vibeclip.dto.user.UserResponse;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.mapper.UserMapper;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.VideoRepository;
import com.vibeclip.service.SubscriptionService;
import com.vibeclip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController {

    private final UserMapper userMapper;
    private final SubscriptionService subscriptionService;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    public UserController(UserService userService, UserMapper userMapper, SubscriptionService subscriptionService, VideoRepository videoRepository, VideoMapper videoMapper) {
        super(userService);
        this.userMapper = userMapper;
        this.subscriptionService = subscriptionService;
        this.videoRepository = videoRepository;
        this.videoMapper = videoMapper;
    }

    // Получение информации о текущем аутентифицированном пользователе
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserMe(Authentication authentication) {
        User user = getCurrentUser(authentication);
        UserResponse response = userMapper.toDTO(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String username, Authentication authentication
    ) {

        User currentUser = getCurrentUser(authentication);

        User profileUser = userService.findByUsername(username);

        boolean isMe = currentUser.getId().equals(profileUser.getId());

        boolean isAccepted = subscriptionService.isSubscribed(currentUser, profileUser);

        boolean canViewVideos =
                !profileUser.isPrivateProfile()
                        || isMe
                        || isAccepted;

        List<VideoResponse> videos = List.of();

        if (canViewVideos) {

            videos = videoRepository
                    .findByAuthorAndStatus(profileUser, VideoStatus.PUBLISHED)
                    .stream()
                    .map(videoMapper::toDTO)
                    .toList();
        }

        UserProfileResponse response = UserProfileResponse.builder()
                .id(profileUser.getId())
                .username(profileUser.getUsername())
                .privateProfile(profileUser.isPrivateProfile())
                .subscribed(isAccepted)
                .subscribersCount(subscriptionService.getSubscribersCount(profileUser))
                .subscriptionsCount(subscriptionService.getSubscriptionsCount(profileUser))
                .videos(videos)
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/privacy")
    public ResponseEntity<UserResponse> updatePrivacy(@RequestBody UpdatePrivacyRequest request, Authentication authentication
    ) {

        User user = getCurrentUser(authentication);

        User updated = userService.updatePrivacy(user, request.isPrivateProfile());

        return ResponseEntity.ok(userMapper.toDTO(updated));
    }
}