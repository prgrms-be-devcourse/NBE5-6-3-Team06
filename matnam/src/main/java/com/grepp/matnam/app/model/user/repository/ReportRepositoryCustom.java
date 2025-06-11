package com.grepp.matnam.app.model.user.repository;

import com.grepp.matnam.app.model.user.dto.ReportDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepositoryCustom {

    Page<ReportDto> findAllReports(Pageable pageable);

    Page<ReportDto> findByStatusAndKeywordContaining(Boolean status, String keyword, Pageable pageable);

    Page<ReportDto> findByStatus(Boolean status, Pageable pageable);

    Page<ReportDto> findByKeywordContaining(String keyword, Pageable pageable);

}
