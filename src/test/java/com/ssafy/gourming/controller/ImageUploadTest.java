package com.ssafy.gourming.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = "password-reset.mail.enabled=false")
@AutoConfigureMockMvc
public class ImageUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser // Spring Security 403 Forbidden 우회용
    @DisplayName("공용 이미지 업로드 테스트 - 실제 파일 업로드 정상 동작")
    public void testImageUpload() throws Exception {
        // resources/images/ 폴더 내부의 테스트 이미지 파일 읽기
        ClassPathResource resource = new ClassPathResource("images/test-image.png");

        try (InputStream is = resource.getInputStream()) {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",               
                    "test-image.png",     
                    MediaType.IMAGE_PNG_VALUE, 
                    is                    
            );

            // API 호출 및 검증
            mockMvc.perform(multipart("/api/images").file(mockFile))
                    .andExpect(status().isOk())
                    // 정상 동작 시 "/images/" 가 포함된 URL이 응답되어야 함
                    .andExpect(content().string(containsString("/images/")));
        }
    }
}
