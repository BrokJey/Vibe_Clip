package com.vibeclip.service;

import com.vibeclip.dto.folder.FolderRequest;
import com.vibeclip.dto.folder.FolderResponse;
import com.vibeclip.entity.Comment;
import com.vibeclip.entity.Folder;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.FolderMapper;
import com.vibeclip.mapper.FolderPreferenceMapper;
import com.vibeclip.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void create_success() {
        FolderRequest request = new FolderRequest();
        request.setName("папка");

        User user = new User();
        user.setId(UUID.randomUUID());

        Folder folder = new Folder();
        folder.setName("папка");

        FolderResponse expectedResponse = new FolderResponse();
        expectedResponse.setName(folder.getName());

        when(folderRepository.existsByNameAndOwner(request.getName(), user)).thenReturn(false);
        when(folderMapper.fromDTO(request)).thenReturn(folder);
        when(folderRepository.save(any(Folder.class))).thenReturn(folder);
        when(folderMapper.toDTO(folder)).thenReturn(expectedResponse);

        FolderResponse response = folderService.create(request, user);

        assertNotNull(response);
        assertEquals(expectedResponse.getName(), response.getName());

        verify(folderRepository).existsByNameAndOwner(request.getName(), user);
        verify(folderMapper).fromDTO(request);
        verify(folderRepository).save(any(Folder.class));
        verify(folderMapper).toDTO(folder);
    }

}
