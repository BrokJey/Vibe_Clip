package com.vibeclip.service;

import com.vibeclip.dto.user.UserProfileResponse;
import com.vibeclip.dto.user.UserResponse;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.mapper.UserMapper;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import com.vibeclip.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private static final String DEFAULT_AVATAR = "/images/default-avatar.png";
    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;
    private final SubscriptionService subscriptionService;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .toArray(String[]::new))
                        .accountLocked(false)
                        .accountExpired(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
    }

    public User updatePrivacy(User user, boolean privateProfile) {
        user.setPrivateProfile(privateProfile);
        return userRepository.save(user);
    }

    public String getAvatarUrl(User user) {
        return user.getAvatarUrl() != null
                ? user.getAvatarUrl()
                : DEFAULT_AVATAR;
    }

    public UserProfileResponse getProfile(String username, User currentUser) {

        User profileUser = findByUsername(username);

        boolean isMe = currentUser.getId().equals(profileUser.getId());

        boolean isAccepted =
                subscriptionService.isSubscribed(currentUser, profileUser);

        boolean canViewVideos =
                !profileUser.isPrivateProfile()
                        || isMe
                        || isAccepted;

        List<VideoResponse> videos = List.of();

        if (canViewVideos) {

            videos = videoRepository
                    .findByAuthorAndStatus(
                            profileUser,
                            VideoStatus.PUBLISHED
                    )
                    .stream()
                    .map(videoMapper::toDTO)
                    .toList();
        }

        return UserProfileResponse.builder()
                .id(profileUser.getId())
                .avatarUrl(getAvatarUrl(profileUser))
                .username(profileUser.getUsername())
                .privateProfile(profileUser.isPrivateProfile())
                .subscribed(isAccepted)
                .subscribersCount(
                        subscriptionService.getSubscribersCount(profileUser)
                )
                .subscriptionsCount(
                        subscriptionService.getSubscriptionsCount(profileUser)
                )
                .videos(videos)
                .build();
    }

    @Transactional
    public UserResponse uploadAvatar(User user, MultipartFile avatar) {

        if (avatar.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        if (avatar.getContentType() == null ||
                !avatar.getContentType().startsWith("image/")) {

            throw new IllegalArgumentException("Разрешены только изображения");
        }

        // удалить старый
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteFile(user.getAvatarUrl());
        }

        String avatarUrl =
                fileStorageService.storeFile(avatar, "avatar");

        user.setAvatarUrl(avatarUrl);

        User updated = userRepository.save(user);

        UserResponse response = userMapper.toDTO(updated);

        response.setAvatarUrl(getAvatarUrl(updated));

        return response;
    }

    @Transactional
    public void deleteAvatar(User user) {

        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteFile(user.getAvatarUrl());
        }

        user.setAvatarUrl(null);

        userRepository.save(user);
    }
}