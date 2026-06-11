package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.mapper.ImageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;

import static org.mockito.Mockito.*;

import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ImageServiceTest {

    @Autowired
    private ImageService imageService;

    // 실제 DB 연동 없이 비즈니스 로직만 테스트하기 위해 Mapper를 Mock(가짜 객체)으로 주입합니다.
    @MockBean
    private ImageMapper imageMapper;

    @Test
    @DisplayName("syncImages: 기존 이미지와 새 이미지를 비교하여 PENDING/CONFIRMED 처리가 정확히 나뉘는지 테스트")
    public void testSyncImages() {
        // given: 상황 세팅
        // 기존 게시글에 1번, 2번, 3번 이미지가 있었음
        String[] oldUrls = {"/images/img1.jpg", "/images/img2.jpg", "/images/img3.jpg"};
        // 유저가 글을 수정하면서 1번은 남기고, 2/3번은 지우고, 4번을 새롭게 추가함
        String[] newUrls = {"/images/img1.jpg", "/images/img4.jpg"};

        // when: 서비스 로직 실행
        imageService.syncImages(oldUrls, newUrls);

        // then: 결과 검증
        // 1. 새롭게 추가된 4번 이미지만 CONFIRMED 로 업데이트 하도록 Mapper가 호출되어야 함
        verify(imageMapper, times(1)).updateStatusToConfirmed(Arrays.asList("/images/img4.jpg"));
        
        // 2. 지워진 2번, 3번 이미지만 PENDING 으로 변경하도록 Mapper가 호출되어야 함
        verify(imageMapper, times(1)).updateStatusToPending(Arrays.asList("/images/img2.jpg", "/images/img3.jpg"));
    }

    @Test
    @DisplayName("confirmImages: 이미지 배열 확정 시 Mapper가 정상 호출되는지 테스트")
    public void testConfirmImages() {
        // given
        String[] urls = {"/images/new1.jpg", "/images/new2.jpg"};

        // when
        imageService.confirmImages(urls);

        // then
        // 넘겨준 배열이 리스트로 변환되어 Mapper에 전달되어야 함
        verify(imageMapper, times(1)).updateStatusToConfirmed(Arrays.asList("/images/new1.jpg", "/images/new2.jpg"));
    }

    @Test
    @DisplayName("uploadImage: 지원하지 않는 이미지 확장자 업로드 시 예외 발생 테스트")
    public void testUploadImage_InvalidExtension() {
        // given: txt 파일 (지원하지 않는 형식)
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "Dummy content".getBytes()
        );

        // when & then: IllegalArgumentException 이 발생해야 함
        assertThatThrownBy(() -> imageService.uploadImage(invalidFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 이미지 형식입니다");
    }
}
