package com.fvp.repository;

import com.fvp.entity.ProcessedSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedSheetRepository extends JpaRepository<ProcessedSheet, Integer> {
    
    Optional<ProcessedSheet> findBySheetNameAndWorkbookId(String sheetName, String workbookId);
    
    List<ProcessedSheet> findByWorkbookId(String workbookId);
    
    boolean existsBySheetNameAndWorkbookId(String sheetName, String workbookId);
} 