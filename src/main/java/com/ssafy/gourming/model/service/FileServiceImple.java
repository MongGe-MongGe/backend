package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.ImageDto;
import com.ssafy.gourming.model.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImple implements FileService {

    @Value("${file.upload.dir}")
    private String uploadDir;
    
    private final ImageMapper imageMapper;

    @Override
    @Transactional
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueFilename = timeStamp + "_" + UUID.randomUUID().toString() + extension;

        Path filePath = Paths.get(uploadDir, uniqueFilename);

        try {
            Files.createDirectories(Paths.get(uploadDir));
            file.transferTo(filePath.toFile());
            
            String finalUrl = "/images/" + uniqueFilename;
            
            ImageDto imageDto = new ImageDto(uniqueFilename, finalUrl, "PENDING");
            imageMapper.insertImage(imageDto);
            
            return finalUrl;
            
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    @Override
    @Transactional
    public void confirmImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageMapper.updateStatusToConfirmed(Collections.singletonList(imageUrl));
        }
    }

    @Override
    @Transactional
    public void confirmImages(String[] imageUrls) {
        if (imageUrls != null && imageUrls.length > 0) {
            imageMapper.updateStatusToConfirmed(Arrays.asList(imageUrls));
        }
    }

    @Override
    @Transactional
    public void syncImages(String[] oldUrls, String[] newUrls) {
        List<String> oldList = oldUrls == null ? Collections.emptyList() : Arrays.asList(oldUrls);
        List<String> newList = newUrls == null ? Collections.emptyList() : Arrays.asList(newUrls);

        // 새롭게 추가된 이미지 (newList 에는 있고 oldList 에는 없는 것) -> CONFIRMED
        List<String> toConfirm = newList.stream()
                .filter(url -> !oldList.contains(url))
                .collect(Collectors.toList());
        if (!toConfirm.isEmpty()) {
            imageMapper.updateStatusToConfirmed(toConfirm);
        }

        // 삭제된 이미지 (oldList 에는 있고 newList 에는 없는 것) -> PENDING (스케줄러 삭제 유도)
        List<String> toPending = oldList.stream()
                .filter(url -> !newList.contains(url))
                .collect(Collectors.toList());
        if (!toPending.isEmpty()) {
            imageMapper.updateStatusToPending(toPending);
        }
    }

    @Override
    @Transactional
    public void deleteImage(String url) {
        if (url == null || url.isEmpty()) return;
        String filename = url.substring(url.lastIndexOf("/") + 1);
        try {
            Files.deleteIfExists(Paths.get(uploadDir, filename));
            imageMapper.deleteImageByUrl(url);
        } catch (IOException e) {
            throw new RuntimeException("물리적 파일 삭제 실패: " + filename, e);
        }
    }

    @Override
    @Transactional
    public void deleteImages(String[] urls) {
        if (urls == null || urls.length == 0) return;
        
        for (String url : urls) {
            String filename = url.substring(url.lastIndexOf("/") + 1);
            try {
                Files.deleteIfExists(Paths.get(uploadDir, filename));
            } catch (IOException e) {
                // 특정 파일 삭제 실패 시 무시
            }
        }
        imageMapper.deleteImagesByUrls(Arrays.asList(urls));
    }
}
