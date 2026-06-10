package com.ssafy.gourming.model.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    
    // 이미지 업로드 후 PENDING 상태로 DB 및 로컬 저장
    String uploadImage(MultipartFile file);
    
    // 단일 이미지 상태를 CONFIRMED로 변경
    void confirmImage(String imageUrl);
    
    // 다중 이미지 상태를 CONFIRMED로 변경
    void confirmImages(String[] imageUrls);
    
    // 수정 시 삭제된 이미지는 PENDING, 새 이미지는 CONFIRMED 처리
    void syncImages(String[] oldUrls, String[] newUrls);
    
    // 단일 이미지 즉시 삭제 (물리적 파일 + DB)
    void deleteImage(String url);
    
    // 다중 이미지 즉시 삭제 (물리적 파일 + DB)
    void deleteImages(String[] urls);
}
