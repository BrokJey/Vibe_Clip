package com.vibeclip.service;

import com.vibeclip.dto.folder.FolderRequest;
import com.vibeclip.dto.folder.FolderResponse;
import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderPreference;
import com.vibeclip.entity.FolderStatus;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.FolderMapper;
import com.vibeclip.mapper.FolderPreferenceMapper;
import com.vibeclip.repository.FolderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;
    private final FolderPreferenceMapper preferenceMapper;

    @Transactional
    public FolderResponse create(FolderRequest request, User owner) {
        // Проверяем уникальность имени папки
        if (folderRepository.existsByNameAndOwner(request.getName(), owner)) {
            log.error("Папка с таким именем уже существует");
            throw new IllegalArgumentException("Папка с таким именем уже существует");
        }

        Folder folder = folderMapper.fromDTO(request);
        folder.setOwner(owner);
        folder.setStatus(FolderStatus.ACTIVE);

        // Устанавливаем настройки, если они есть
        if (request.getPreference() != null) {
            FolderPreference preference = preferenceMapper.fromDTO(request.getPreference());
            folder.setPreference(preference);
        } else {
            // Настройки по умолчанию
            folder.setPreference(FolderPreference.builder()
                    .freshnessWeight(0.5)
                    .popularityWeight(0.5)
                    .build());
        }

        Folder saved = folderRepository.save(folder);
        log.info("Папка с именем {} создана", folder.getName());
        return folderMapper.toDTO(saved);
    }

    public FolderResponse getById(UUID id, User owner) {
        Folder folder = getEntityById(id, owner);
        log.info("Папка найдена. Folder id: {}, name: {}", id, folder.getName());
        return folderMapper.toDTO(folder);
    }

    public List<FolderResponse> getByOwner(User owner) {
        List<Folder> folders = folderRepository.findByOwnerAndStatusOrderByCreatedAtDesc(owner, FolderStatus.ACTIVE);

        log.info("Найдено {} активных папок для пользователя: {}", folders.size(), owner.getUsername());

        if (folders.isEmpty()) {
            log.warn("Нет активных папок, доступных пользователю: {}", owner.getUsername());
        }

        return folders.stream()
                .map(folderMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponse update(UUID id, FolderRequest request, User owner) {

        Folder folder = getEntityById(id, owner);

        // Обновляем только переданные поля
        if (request.getName() != null && !request.getName().equals(folder.getName())) {
            // Проверяем уникальность нового имени
            if (folderRepository.existsByNameAndOwner(request.getName(), owner)) {
                throw new IllegalArgumentException("Папка с таким именем уже существует");
            }
            folder.setName(request.getName());
        }
        if (request.getDescription() != null) {
            folder.setDescription(request.getDescription());
        }
        if (request.getPreference() != null) {
            if (folder.getPreference() == null) {
                folder.setPreference(preferenceMapper.fromDTO(request.getPreference()));
            } else {
                preferenceMapper.updateEntity(folder.getPreference(), request.getPreference());
            }
        }

        Folder updated = folderRepository.save(folder);
        log.info("Папка {} обновлена", folder.getName());
        return folderMapper.toDTO(updated);
    }

    @Transactional
    public void delete(UUID id, User owner) {
        Folder folder = getEntityById(id, owner);
        
        log.info("Начинается удаление папки {} ({})", folder.getId(), folder.getName());
        
        // Полностью удаляем папку из БД
        // Каскадное удаление автоматически удалит:
        // - folder_videos (связи с видео)
        // - folder_allowed_hashtags
        // - folder_blocked_hashtags
        // - folder_allowed_authors
        // - folder_blocked_authors
        folderRepository.delete(folder);
        
        log.info("Папка {} ({}) полностью удалена", id, folder.getName());
    }

    @Transactional
    public void archive(UUID id, User owner) {
        Folder folder = getEntityById(id, owner);
        folder.setStatus(FolderStatus.ARCHIVED);
        folderRepository.save(folder);
    }

    public Folder getEntityById(UUID id, User owner) {
        return folderRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> {
                    log.error("Папка не найдена или пользователь '{}' (id={}) не является её владельцем",
                            owner.getUsername(), owner.getId());
                    return new IllegalArgumentException("Папка не найдена или вы не являетесь ее владельцем");
                });
    }

    public Folder getEntityByIdAndStatus(UUID id, User owner, FolderStatus status) {
        return folderRepository.findByIdAndOwnerAndStatus(id, owner, status)
                .orElseThrow(() -> new IllegalArgumentException("Папка не найдена или вы не владелец"));
    }
}

