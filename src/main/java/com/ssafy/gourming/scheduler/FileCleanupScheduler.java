package com.ssafy.gourming.scheduler;

import com.ssafy.gourming.model.dto.ImageDto;
import com.ssafy.gourming.model.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    @Value("${file.upload.dir}")
    private String uploadDir;
    
    private final ImageMapper imageMapper;

    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시에 실행
    @Transactional
    public void cleanupPendingImages() {
        log.info("DB 기반 고아 파일 정리 스케줄러 시작");
        
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<ImageDto> orphanImages = imageMapper.findPendingImagesOlderThan(yesterday);
        
        if (orphanImages.isEmpty()) {
            log.info("정리할 고아 파일이 없습니다.");
            return;
        }

        for (ImageDto image : orphanImages) {
            try {
                // 1. 실제 물리적 파일 삭제
                Files.deleteIfExists(Paths.get(uploadDir, image.getFilename()));
                
                // 2. DB 레코드 삭제
                imageMapper.deleteImageById(image.getId());
                
                log.info("고아 파일 삭제 완료: {}", image.getFilename());
            } catch (IOException e) {
                log.error("물리 파일 삭제 실패: {}", image.getFilename(), e);
            }
        }
        
        log.info("고아 파일 정리 스케줄러 실행 완료.");
    }
}
