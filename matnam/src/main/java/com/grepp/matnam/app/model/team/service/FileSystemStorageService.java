package com.grepp.matnam.app.model.team.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    @Value("${upload.path}")
    private String uploadPath;

    private Path rootLocation;

    @PostConstruct
    public void init() throws IOException {
        this.rootLocation = Paths.get(uploadPath);
        Files.createDirectories(rootLocation);
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        // 원본 이름 정리 + UUID 붙이기
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "-" + original;
        // 디스크에 저장
        Path destination = rootLocation.resolve(filename);
        file.transferTo(destination.toFile());  // transferTo로 편리하게 저장
        // 브라우저에서 접근 가능한 URL 반환 (/download/** 로 매핑됨)
        return "/download/" + filename;
    }

}
