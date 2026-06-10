package com.ssafy.gourming.model.mapper;

import com.ssafy.gourming.model.dto.ImageDto;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DB의 images 테이블과 통신하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface ImageMapper {
    
    // 1. 새로운 이미지 업로드 시 DB에 정보 등록 (PENDING 상태)
    void insertImage(ImageDto imageDto);
    
    // 2. 리뷰/피드 최종 등록 시 여러 이미지의 상태를 한 번에 CONFIRMED로 변경
    void updateStatusToConfirmed(List<String> urls);
    
    // 3. 리뷰/피드 수정 시 삭제된 이미지들을 다시 PENDING으로 변경하여 스케줄러 삭제 유도
    void updateStatusToPending(List<String> urls);
    
    // 4. 스케줄러가 특정 시간(예: 24시간) 이전에 업로드되었으나 아직 PENDING인 이미지 목록 조회
    List<ImageDto> findPendingImagesOlderThan(LocalDateTime time);
    
    // 5. 물리적 파일 삭제 완료 후 DB 레코드 완전 삭제 (ID 기반)
    void deleteImageById(Long id);
    
    // 6. 단일 URL 기반 즉시 삭제 (리뷰 전체 삭제 등)
    void deleteImageByUrl(String url);
    
    // 7. 여러 URL 기반 즉시 삭제
    void deleteImagesByUrls(List<String> urls);
}
