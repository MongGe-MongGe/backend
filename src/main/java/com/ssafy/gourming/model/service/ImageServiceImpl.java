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

/**
 * 이미지 업로드 및 생명주기 관리를 담당하는 서비스 클래스입니다.
 * 
 * [생명주기 흐름]
 * 1. 업로드 (uploadImage): 이미지는 먼저 물리 저장소에 저장되고, DB에는 상태가 'PENDING'으로 기록됩니다.
 * 2. 확정 (confirmImage/confirmImages): 비즈니스 로직(예: 게시글 작성 완료)에서 해당 이미지가 실제로 사용됨이 확인되면 상태를 'CONFIRMED'로 변경합니다.
 * 3. 동기화 (syncImages): 게시글 수정 시, 기존 이미지 배열과 새 이미지 배열을 비교하여 추가된 것은 'CONFIRMED', 삭제된 것은 'PENDING'으로 상태를 전환합니다.
 * 4. 삭제 (deleteImage/deleteImages): 명시적으로 물리 파일과 DB 레코드를 함께 삭제할 때 사용합니다.
 * 
 * 참고: 'PENDING' 상태로 방치된 이미지(고아 객체)는 별도의 스케줄러(FileCleanupScheduler)에 의해 주기적으로 일괄 삭제됩니다.
 */
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    // application.properties 에 설정된 파일 업로드 물리 경로
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    // 데이터베이스(images 테이블)와의 통신을 담당하는 MyBatis 매퍼
    private final ImageMapper imageMapper;

    /**
     * 클라이언트로부터 전달받은 단일 이미지 파일을 물리 저장소에 저장하고 DB에 'PENDING' 상태로 등록합니다.
     * 
     * @param file 업로드할 MultipartFile 객체
     * @return 저장된 이미지의 웹 접근용 상대 URL (예: /images/2026..._uuid.png)
     * @throws IllegalArgumentException 파일이 비어있는 경우
     * @throws RuntimeException 물리 파일 저장 중 입출력 오류가 발생한 경우
     */
    @Override
    @Transactional
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 1. 파일 확장자 추출 및 검증
        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        List<String> allowedExtensions = List.of(".jpg", ".jpeg", ".png", ".webp");
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, webp만 가능)");
        }

        // 2. 파일명 충돌 방지를 위해 '현재시간_UUID.확장자' 형식으로 고유 파일명 생성
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueFilename = timeStamp + "_" + UUID.randomUUID().toString() + extension;

        // 물리적인 저장 경로 (예: uploads/images/2026..._uuid.png)
        Path filePath = Paths.get(uploadDir, uniqueFilename);

        try {
            // 3. 업로드 디렉토리가 없으면 생성
            Files.createDirectories(Paths.get(uploadDir).toAbsolutePath());
            
            // 4. 지정한 경로로 실제 파일 복사(저장)
            file.transferTo(filePath.toAbsolutePath().toFile());
            
            // 5. 클라이언트가 접근할 수 있는 Web URL 경로 생성
            String finalUrl = "/images/" + uniqueFilename;
            
            // 6. DB에 'PENDING'(대기) 상태로 레코드 삽입 (이후 비즈니스 로직에서 CONFIRMED 처리를 해야 최종 반영됨)
            ImageDto imageDto = new ImageDto(uniqueFilename, finalUrl, "PENDING");
            imageMapper.insertImage(imageDto);
            
            return finalUrl;
            
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    /**
     * 특정 이미지의 상태를 사용 확정('CONFIRMED')으로 변경합니다.
     * 
     * @param imageUrl 확정할 이미지의 웹 URL
     */
    @Override
    @Transactional
    public void confirmImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageMapper.updateStatusToConfirmed(Collections.singletonList(imageUrl));
        }
    }

    /**
     * 다중 이미지의 상태를 한 번에 사용 확정('CONFIRMED')으로 변경합니다.
     * 
     * @param imageUrls 확정할 이미지 웹 URL 배열
     */
    @Override
    @Transactional
    public void confirmImages(String[] imageUrls) {
        if (imageUrls != null && imageUrls.length > 0) {
            imageMapper.updateStatusToConfirmed(Arrays.asList(imageUrls));
        }
    }

    /**
     * 자원(예: 리뷰, 게시글) 수정 시 기존 이미지와 새로 전달된 이미지 목록을 비교(Sync)하여 상태를 동기화합니다.
     * 
     * - 새로 추가된 이미지: PENDING -> CONFIRMED
     * - 화면에서 삭제된 이미지: CONFIRMED -> PENDING (이후 스케줄러가 수거함)
     * 
     * @param oldUrls 기존에 DB에 저장되어 있던 이미지 URL 배열
     * @param newUrls 클라이언트가 수정한 최종 이미지 URL 배열
     */
    @Override
    @Transactional
    public void syncImages(String[] oldUrls, String[] newUrls) {
        // Null-safe 방어 로직 (null 이 들어오면 빈 리스트로 초기화)
        List<String> oldList = oldUrls == null ? Collections.emptyList() : Arrays.asList(oldUrls);
        List<String> newList = newUrls == null ? Collections.emptyList() : Arrays.asList(newUrls);

        // [추가된 이미지] newList 에는 존재하지만 oldList 에는 없는 이미지들을 찾아 상태를 CONFIRMED 로 변경
        List<String> toConfirm = newList.stream()
                .filter(url -> !oldList.contains(url))
                .collect(Collectors.toList());
        if (!toConfirm.isEmpty()) {
            imageMapper.updateStatusToConfirmed(toConfirm);
        }

        // [삭제된 이미지] oldList 에는 존재하지만 newList 에서는 빠진 이미지들을 찾아 상태를 PENDING 으로 강등
        // (즉시 삭제하지 않고 PENDING 으로 돌려놓으면 나중에 스케줄러가 안전하게 일괄 삭제 처리함)
        List<String> toPending = oldList.stream()
                .filter(url -> !newList.contains(url))
                .collect(Collectors.toList());
        if (!toPending.isEmpty()) {
            imageMapper.updateStatusToPending(toPending);
        }
    }

    /**
     * 특정 이미지의 물리적 파일과 DB 레코드를 즉시 영구 삭제합니다.
     * (일반적으로는 스케줄러에 위임하지만, 명시적 삭제가 필요할 때 사용)
     * 
     * @param url 삭제할 이미지 웹 URL
     */
    @Override
    @Transactional
    public void deleteImage(String url) {
        if (url == null || url.isEmpty()) return;
        
        // URL에서 순수 파일명만 추출 (예: /images/abc.png -> abc.png)
        String filename = url.substring(url.lastIndexOf("/") + 1);
        try {
            // 1. 물리 서버에서 파일 삭제
            Files.deleteIfExists(Paths.get(uploadDir, filename));
            // 2. DB 레코드 삭제
            imageMapper.deleteImageByUrl(url);
        } catch (IOException e) {
            throw new RuntimeException("물리적 파일 삭제 실패: " + filename, e);
        }
    }

    /**
     * 다중 이미지의 물리적 파일과 DB 레코드를 한 번에 즉시 영구 삭제합니다.
     * 
     * @param urls 삭제할 이미지 웹 URL 배열
     */
    @Override
    @Transactional
    public void deleteImages(String[] urls) {
        if (urls == null || urls.length == 0) return;
        
        for (String url : urls) {
            String filename = url.substring(url.lastIndexOf("/") + 1);
            try {
                // 물리 파일 삭제 시도 (일부 파일이 없더라도 에러 무시하고 다음 파일 진행)
                Files.deleteIfExists(Paths.get(uploadDir, filename));
            } catch (IOException e) {
                // 특정 파일 물리적 삭제 실패 시 로그만 남기거나 무시 (DB 정합성을 우선시함)
            }
        }
        // 반복문이 끝난 후 DB 레코드는 in 쿼리를 활용해 한 번에(일괄) 삭제
        imageMapper.deleteImagesByUrls(Arrays.asList(urls));
    }
}
