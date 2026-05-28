package com.vibeclip.service;

import com.vibeclip.entity.ReportStatus;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoReport;
import com.vibeclip.repository.VideoReportRepository;
import com.vibeclip.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoReportService {

    private final VideoReportRepository videoReportRepository;
    private final VideoRepository videoRepository;

    public void reportVideo(User reporter, UUID videoId, String reason) {

        if (videoReportRepository.existsByVideoIdAndReporter(videoId, reporter)) {
            throw new IllegalStateException("Вы уже жаловались на это видео");
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Видео не найдено"));

        VideoReport report = VideoReport.builder()
                .video(video)
                .reporter(reporter)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .build();

        videoReportRepository.save(report);
    }

    @Transactional
    public void resolveAllReportsForVideo(UUID videoId) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Видео не найдено"));

        List<VideoReport> reports =
                videoReportRepository.findByVideoAndStatus(
                        video,
                        ReportStatus.PENDING
                );

        reports.forEach(report ->
                report.setStatus(ReportStatus.REVIEWED)
        );

        videoReportRepository.saveAll(reports);
    }

    @Transactional
    public void rejectAllReportsForVideo(UUID videoId) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Видео не найдено"));

        List<VideoReport> reports =
                videoReportRepository.findByVideoAndStatus(
                        video,
                        ReportStatus.PENDING
                );

        reports.forEach(report ->
                report.setStatus(ReportStatus.REJECTED)
        );

        videoReportRepository.saveAll(reports);
    }

    public long countPendingReports(UUID videoId) {
        return videoReportRepository.countByVideoIdAndStatus(videoId, ReportStatus.PENDING);
    }
}
