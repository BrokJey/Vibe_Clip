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
import org.springframework.web.multipart.MultipartFile;

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

        response.setAvatarUrl(userService.getAvatarUrl(user));

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String username, Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        return ResponseEntity.ok(
                userService.getProfile(username, currentUser)
        );
    }

    @PatchMapping("/privacy")
    public ResponseEntity<UserResponse> updatePrivacy(@RequestBody UpdatePrivacyRequest request, Authentication authentication) {

        User user = getCurrentUser(authentication);

        User updated = userService.updatePrivacy(user, request.isPrivateProfile());

        return ResponseEntity.ok(userMapper.toDTO(updated));
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@RequestPart("avatar") MultipartFile avatar, Authentication authentication) {
        User user = getCurrentUser(authentication);

        UserResponse response =
                userService.uploadAvatar(user, avatar);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        User user = getCurrentUser(authentication);

        userService.deleteAvatar(user);

        return ResponseEntity.noContent().build();
    }
}