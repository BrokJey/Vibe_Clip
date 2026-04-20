package com.vibeclip.service;

import com.vibeclip.dto.folder.FolderRequest;
import com.vibeclip.dto.folder.FolderResponse;
import com.vibeclip.dto.folder.preference.FolderPreferenceRequest;
import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderPreference;
import com.vibeclip.entity.FolderStatus;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.FolderMapper;
import com.vibeclip.mapper.FolderPreferenceMapper;
import com.vibeclip.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FolderServiceTest {
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private FolderMapper folderMapper;
    @Mock
    private FolderPreferenceMapper preferenceMapper;

    @InjectMocks
    private FolderService folderService;

    @Test
    void create_success_withPreference() {
        User owner = new User();
        owner.setId(UUID.randomUUID());

        FolderPreferenceRequest preferenceRequest = new FolderPreferenceRequest();
        preferenceRequest.setFreshnessWeight(0.7);
        preferenceRequest.setPopularityWeight(0.3);

        FolderRequest request = new FolderRequest();
        request.setName("Моя папка");
        request.setPreference(preferenceRequest);

        Folder folder = new Folder();

        FolderPreference pref = new FolderPreference();
        pref.setFreshnessWeight(0.7);
        pref.setPopularityWeight(0.3);

        Folder saved = new Folder();

        FolderResponse response = new FolderResponse();
        response.setName("Моя папка");

        when(folderRepository.existsByNameAndOwner("Моя папка", owner)).thenReturn(false);
        when(folderMapper.fromDTO(request)).thenReturn(folder);
        when(preferenceMapper.fromDTO(preferenceRequest)).thenReturn(pref);
        when(folderRepository.save(folder)).thenReturn(saved);
        when(folderMapper.toDTO(saved)).thenReturn(response);

        FolderResponse result = folderService.create(request, owner);

        assertNotNull(result);
        assertEquals(owner, folder.getOwner());
        assertEquals(FolderStatus.ACTIVE, folder.getStatus());
        assertEquals(pref, folder.getPreference());

        verify(preferenceMapper).fromDTO(preferenceRequest);
    }

    @Test
    void create_success_withoutPreference_shouldSetDefault() {
        User owner = new User();
        owner.setId(UUID.randomUUID());

        FolderRequest request = new FolderRequest();
        request.setName("Моя папка");
        request.setPreference(null);

        Folder folder = new Folder();
        Folder saved = new Folder();
        FolderResponse response = new FolderResponse();

        when(folderRepository.existsByNameAndOwner("Моя папка", owner)).thenReturn(false);
        when(folderMapper.fromDTO(request)).thenReturn(folder);
        when(folderRepository.save(folder)).thenReturn(saved);
        when(folderMapper.toDTO(saved)).thenReturn(response);

        FolderResponse result = folderService.create(request, owner);

        assertNotNull(result);
        assertEquals(owner, folder.getOwner());
        assertEquals(FolderStatus.ACTIVE, folder.getStatus());

        assertNotNull(folder.getPreference());
        assertEquals(owner, folder.getOwner());
        assertEquals(FolderStatus.ACTIVE, folder.getStatus());

        assertNotNull(folder.getPreference());
        assertEquals(0.5, folder.getPreference().getFreshnessWeight());
        assertEquals(0.5, folder.getPreference().getPopularityWeight());

        verify(preferenceMapper, never()).fromDTO(any());
    }

    @Test
    void create_folderAlreadyExists_shouldThrow() {
        User owner = new User();
        owner.setId(UUID.randomUUID());

        FolderRequest request = new FolderRequest();
        request.setName("Моя папка");

        when(folderRepository.existsByNameAndOwner("Моя папка", owner)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> folderService.create(request, owner));

        assertTrue(ex.getMessage().contains("уже существует"));

        verify(folderRepository).existsByNameAndOwner("Моя папка", owner);
        verify(folderMapper, never()).fromDTO(any());
        verify(folderRepository, never()).save(any());
        verify(preferenceMapper, never()).fromDTO(any());
    }

    @Test
    void getById_success() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setName("Моя папка");
        folder.setOwner(owner);

        FolderResponse response = new FolderResponse();
        response.setId(folderId);
        response.setName("Моя папка");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderMapper.toDTO(folder)).thenReturn(response);

        FolderResponse result = folderService.getById(folderId, owner);

        assertNotNull(result);
        assertEquals(folderId, result.getId());
        assertEquals("Моя папка", result.getName());

        verify(folderRepository).findByIdAndOwner(folderId, owner);
        verify(folderMapper).toDTO(folder);
    }

    @Test
    void getById_folderNotFound_shouldThrow() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> folderService.getById(folderId, owner));

        assertTrue(ex.getMessage().contains("не найдена"));

        verify(folderRepository).findByIdAndOwner(folderId, owner);
        verify(folderMapper, never()).toDTO(any());
    }

    @Test
    void getByOwner_success() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        Folder folder1 = new Folder();
        folder1.setName("Папка 1");
        Folder folder2 = new Folder();
        folder2.setName("Папка 2");

        FolderResponse response1 = new FolderResponse();
        response1.setName("Папка 1");
        FolderResponse response2 = new FolderResponse();
        response2.setName("Папка 2");

        List<Folder> folders = List.of(folder2, folder1);

        when(folderRepository.findByOwnerAndStatusOrderByCreatedAtDesc(owner, FolderStatus.ACTIVE)).thenReturn(folders);
        when(folderMapper.toDTO(folder1)).thenReturn(response1);
        when(folderMapper.toDTO(folder2)).thenReturn(response2);

        List<FolderResponse> result = folderService.getByOwner(owner);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Папка 2", result.get(0).getName());
        assertEquals("Папка 1", result.get(1).getName());

        verify(folderRepository).findByOwnerAndStatusOrderByCreatedAtDesc(owner, FolderStatus.ACTIVE);
        verify(folderMapper).toDTO(folder1);
        verify(folderMapper).toDTO(folder2);
    }

    @Test
    void getByOwner_emptyList_shouldReturnEmpry() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        when(folderRepository.findByOwnerAndStatusOrderByCreatedAtDesc(owner, FolderStatus.ACTIVE)).thenReturn(List.of());
        List<FolderResponse> result = folderService.getByOwner(owner);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(folderRepository).findByOwnerAndStatusOrderByCreatedAtDesc(owner, FolderStatus.ACTIVE);
        verify(folderMapper, never()).toDTO(any());
    }

    @Test
    void update_success_allFields() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setName("old");
        folder.setDescription("old desc");
        folder.setOwner(owner);
        folder. setPreference(new FolderPreference());

        FolderRequest request = new FolderRequest();
        request.setName("new");
        request.setDescription("new desc");
        request.setPreference(new FolderPreferenceRequest());

        FolderResponse response = new FolderResponse();
        response.setName("new");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderRepository.existsByNameAndOwner("new", owner)).thenReturn(false);
        when(folderRepository.save(folder)).thenReturn(folder);
        when(folderMapper.toDTO(folder)).thenReturn(response);

        FolderResponse result = folderService.update(folderId, request, owner);

        assertEquals("new", folder.getName());
        assertEquals("new desc", folder.getDescription());

        verify(preferenceMapper).updateEntity(any(), any());
        verify(folderRepository).save(folder);
    }

    @Test
    void update_nameAlreadyExists_shouldThrow() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();

        Folder folder = new Folder();
        folder.setName("old");
        folder.setOwner(owner);

        FolderRequest request = new FolderRequest();
        request.setName("new");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderRepository.existsByNameAndOwner("new", owner)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> folderService.update(folderId, request, owner));

        verify(folderRepository, never()).save(any());
    }

    @Test
    void update_nameSame_shouldNotCheckUniqueness() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();

        Folder folder = new Folder();
        folder.setName("тот же");
        folder.setOwner(owner);

        FolderRequest request = new FolderRequest();
        request.setName("тот же");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderRepository.save(folder)).thenReturn(folder);
        when(folderMapper.toDTO(folder)).thenReturn(new FolderResponse());

        folderService.update(folderId, request, owner);

        verify(folderRepository, never()).existsByNameAndOwner(any(), any());
        verify(folderRepository).save(folder);
    }

    @Test
    void update_onlyDescription() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();

        Folder folder = new Folder();
        folder.setName("name");
        folder.setDescription("old");

        FolderRequest request = new FolderRequest();
        request.setDescription("new desc");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderRepository.save(folder)).thenReturn(folder);
        when(folderMapper.toDTO(folder)).thenReturn(new FolderResponse());

        folderService.update(folderId, request, owner);

        assertEquals("name", folder.getName());
        assertEquals("new desc", folder.getDescription());
    }

    @Test
    void update_preferenceNull_shouldCreateNew() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();

        Folder folder = new Folder();
        folder.setPreference(null);

        FolderPreferenceRequest preferenceRequest = new FolderPreferenceRequest();
        FolderPreference pref = new FolderPreference();

        FolderRequest request = new FolderRequest();
        request.setPreference(preferenceRequest);

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(preferenceMapper.fromDTO(preferenceRequest)).thenReturn(pref);
        when(folderRepository.save(folder)).thenReturn(folder);
        when(folderMapper.toDTO(folder)).thenReturn(new FolderResponse());

        folderService.update(folderId, request, owner);

        assertNotNull(folder.getPreference());
        verify(preferenceMapper).fromDTO(preferenceRequest);
    }

    @Test
    void update_preferenceExists_shouldUpdate() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();

        FolderPreference existing = new FolderPreference();

        Folder folder = new Folder();
        folder.setPreference(existing);

        FolderPreferenceRequest prefRequest = new FolderPreferenceRequest();

        FolderRequest request = new FolderRequest();
        request.setPreference(prefRequest);

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderRepository.save(folder)).thenReturn(folder);
        when(folderMapper.toDTO(folder)).thenReturn(new FolderResponse());

        folderService.update(folderId, request, owner);

        verify(preferenceMapper).updateEntity(existing, prefRequest);
    }

    @Test
    void delete_success() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setName("Папка");
        folder.setOwner(owner);

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));

        folderService.delete(folderId, owner);

        verify(folderRepository).findByIdAndOwner(folderId, owner);
        verify(folderRepository).delete(folder);
    }

    @Test
    void delete_folderNotFound_shouldThrow() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> folderService.delete(folderId, owner));

        assertTrue(ex.getMessage().contains("не найдена"));

        verify(folderRepository).findByIdAndOwner(folderId, owner);
        verify(folderRepository, never()).delete(any());
    }

    @Test
    void archive_success() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setOwner(owner);
        folder.setStatus(FolderStatus.ACTIVE);

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));
        when(folderRepository.save(folder)).thenReturn(folder);

        folderService.archive(folderId, owner);

        assertEquals(FolderStatus.ARCHIVED, folder.getStatus());

        verify(folderRepository).findByIdAndOwner(folderId, owner);
        verify(folderRepository).save(folder);
    }

    @Test
    void archive_folderNotFound_shouldThrow() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> folderService.archive(folderId, owner));

        verify(folderRepository).findByIdAndOwner(folderId, owner);
        verify(folderRepository, never()).save(any());
    }

    @Test
    void getEntityById_success() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setOwner(owner);

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.of(folder));

        Folder result = folderService.getEntityById(folderId, owner);

        assertNotNull(result);
        assertEquals(folderId, result.getId());

        verify(folderRepository).findByIdAndOwner(folderId, owner);
    }

    @Test
    void getEntityById_notFound_shouldThrow() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user");

        when(folderRepository.findByIdAndOwner(folderId, owner)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> folderService.getEntityById(folderId, owner)
        );

        assertTrue(ex.getMessage().contains("не найдена"));

        verify(folderRepository).findByIdAndOwner(folderId, owner);
    }

    @Test
    void getEntityByIdAndStatus_success() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setOwner(owner);
        folder.setStatus(FolderStatus.ACTIVE);

        when(folderRepository.findByIdAndOwnerAndStatus(folderId, owner, FolderStatus.ACTIVE)).thenReturn(Optional.of(folder));

        Folder result = folderService.getEntityByIdAndStatus(folderId, owner, FolderStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(folderId, result.getId());

        verify(folderRepository).findByIdAndOwnerAndStatus(folderId, owner, FolderStatus.ACTIVE);
    }

    @Test
    void getEntityByIdAndStatus_notFound_shouldThrow() {
        UUID folderId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        when(folderRepository.findByIdAndOwnerAndStatus(folderId, owner, FolderStatus.ACTIVE)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> folderService.getEntityByIdAndStatus(folderId, owner, FolderStatus.ACTIVE)
        );

        assertTrue(ex.getMessage().contains("не найдена"));

        verify(folderRepository).findByIdAndOwnerAndStatus(folderId, owner, FolderStatus.ACTIVE);
    }
}