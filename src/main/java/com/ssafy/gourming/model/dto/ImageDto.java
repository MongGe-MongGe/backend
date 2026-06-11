package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

/**
 * 이미지 라이프사이클(상태) 관리를 위한 DTO 클래스
 * DB의 'images' 테이블과 1:1로 매핑됩니다.
 */
public class ImageDto {
    private Long id;
    private String filename; // 고유하게 생성된 파일명 (예: 20260609_uuid.jpg)
    private String url;      // 프론트엔드에서 접근할 URL (예: /images/20260609_uuid.jpg)
    private String status;   // 상태값: 'PENDING'(임시), 'CONFIRMED'(확정)
    private LocalDateTime createdAt; // 생성 일시 (스케줄러 청소 기준)

    public ImageDto() {}

    public ImageDto(String filename, String url, String status) {
        this.filename = filename;
        this.url = url;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
