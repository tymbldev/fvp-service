package com.fvp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "processed_sheet")
@NoArgsConstructor
public class ProcessedSheet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "sheet_name", nullable = false)
    private String sheetName;
    
    @Column(name = "processed_date", nullable = false)
    private LocalDateTime processedDate;
    
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved;
    
    @Column(name = "records_processed")
    private Integer recordsProcessed;
    
    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;
    
    @Column(name = "workbook_id", nullable = false)
    private String workbookId;

    public ProcessedSheet(String sheetName, Boolean isApproved, Integer recordsProcessed, Integer tenantId, String workbookId) {
        this.sheetName = sheetName;
        this.processedDate = LocalDateTime.now();
        this.isApproved = isApproved;
        this.recordsProcessed = recordsProcessed;
        this.tenantId = tenantId;
        this.workbookId = workbookId;
    }
} 