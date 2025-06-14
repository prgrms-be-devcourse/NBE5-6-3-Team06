package com.grepp.matnam.app.model.team.service;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file) throws IOException;
}
