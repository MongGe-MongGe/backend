package com.ssafy.gourming.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.gourming.model.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
	
	private final FileService fileService;
	
	@PostMapping
	public ResponseEntity<String> uploadImage(@RequestParam MultipartFile file) {
		String tempUrl = fileService.uploadImage(file);
		return ResponseEntity.ok(tempUrl);
	}
}
