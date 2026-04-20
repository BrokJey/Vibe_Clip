package com.vibeclip.dto.folder;

import com.vibeclip.dto.folder.preference.FolderPreferenceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderRequest {

    @Size(min = 1, max = 100, message = "Имя папки должно содержать от 1 до 100 символов.")
    private String name;

    @Size(max = 500, message = "Описание не должно превышать 500 символов.")
    private String description;

    @Valid
    private FolderPreferenceRequest preference;
}

