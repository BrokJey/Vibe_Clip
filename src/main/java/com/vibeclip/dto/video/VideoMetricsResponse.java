package com.vibeclip.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMetricsResponse {

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
}


