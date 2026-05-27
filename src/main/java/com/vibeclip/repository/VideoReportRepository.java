package com.vibeclip.repository;

import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoReport;
import com.vibeclip.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VideoReportRepository extends JpaRepository<VideoReport, UUID> {

    long countByVideoAndStatus(Video video, ReportStatus status);

    List<VideoReport> findByStatus(ReportStatus status);

    @Query("SELECT DISTINCT vr.video FROM VideoReport vr WHERE vr.status = :status")
    List<Video> findReportedVideos(@Param("status") ReportStatus status);

    boolean existsByVideoIdAndReporter(UUID videoId, User reporter);
}