package com.vibeclip.dto.folder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для ленты папки (список видео с пагинацией)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderFeedResponse {

    private UUID folderId;
    private String folderName;
    private List<FolderVideoResponse> videos;
    private Integer totalCount;
    private Integer page;
    private Integer pageSize;
    private Boolean hasMore;
}


