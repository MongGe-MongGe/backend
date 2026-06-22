package com.ssafy.gourming.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry
			.addMapping("/api/**")
			.allowedOrigins("http://localhost:5173") // 허용 도메인
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(true); // Authorization 헤더를 포함한 요청만 허용
	}
	
	@Value("${file.upload.dir}")
	private String uploadDir;
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // application.properties의 상대 경로(uploads/images/)를 안전하게 절대 경로로 변환
        String absolutePath = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        // 경로 끝에 슬래시(/)가 없으면 추가 (정적 리소스 맵핑 규칙)
        if (!absolutePath.endsWith(java.io.File.separator)) {
            absolutePath += java.io.File.separator;
        }

        // `/images/**` 로 오는 요청을 실제 물리적 경로로 연결 및 mock 이미지 제공을 위한 클래스패스 경로 추가
		registry.addResourceHandler("/images/**")
			.addResourceLocations("file:///" + absolutePath)
			.addResourceLocations("classpath:/images/");
	}
}
