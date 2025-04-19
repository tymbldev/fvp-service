package com.fvp.batch;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.sheets.v4.model.Sheet;
import com.fvp.model.SheetData;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableBatchProcessing
public class GoogleSheetsBatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private Sheets sheetsService;

    @Value("${google.sheets.spreadsheet.id}")
    private String spreadsheetId;

    @Bean
    public ItemReader<SheetData> sheetDataReader() throws IOException {
        List<SheetData> data = new ArrayList<>();
        
        // Get all sheets
        List<Sheet> sheets = sheetsService.spreadsheets().get(spreadsheetId).execute().getSheets();
        
        // Filter sheets with date format (dd.MM.yy)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        List<Sheet> dateSheets = sheets.stream()
            .filter(sheet -> {
                try {
                    LocalDate.parse(sheet.getProperties().getTitle(), formatter);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList());
        
        // Read data from each sheet
        for (Sheet sheet : dateSheets) {
            String range = sheet.getProperties().getTitle() + "!A2:G"; // Skip header row
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
            
            List<List<Object>> values = response.getValues();
            if (values != null) {
                for (List<Object> row : values) {
                    if (row.size() >= 7) { // Ensure we have all required columns
                        SheetData sheetData = new SheetData();
                        sheetData.setLink(row.get(0).toString());
                        sheetData.setSource(row.get(1).toString());
                        sheetData.setThumbnail(row.get(2).toString());
                        sheetData.setTitle(row.get(3).toString());
                        sheetData.setCategories(row.get(4).toString());
                        sheetData.setPornstar(row.get(5).toString());
                        sheetData.setDuration(Integer.parseInt(row.get(6).toString()));
                        data.add(sheetData);
                    }
                }
            }
        }
        
        return new ListItemReader<>(data);
    }

    @Bean
    public Step importSheetDataStep(ItemReader<SheetData> reader,
                                  CategoryProcessor processor,
                                  ItemWriter<SheetData> writer) {
        return stepBuilderFactory.get("importSheetDataStep")
            .<SheetData, SheetData>chunk(100)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job importSheetDataJob(Step importSheetDataStep) {
        return jobBuilderFactory.get("importSheetDataJob")
            .incrementer(new RunIdIncrementer())
            .flow(importSheetDataStep)
            .end()
            .build();
    }
} 