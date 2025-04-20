package com.fvp.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "links")
@Setting(settingPath = "es-settings.json")
public class LinkDocument {

    @Id
    private String id;

    @Field(type = FieldType.Integer)
    private Integer tenantId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Integer)
    private Integer duration;

    @Field(type = FieldType.Keyword)
    private String thumbnail;

    @Field(type = FieldType.Keyword)
    private String thumbPath;

    @Field(type = FieldType.Keyword)
    private String sheetName;

    @Field(type = FieldType.Keyword)
    private String link;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Integer)
    private Integer stars;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Date createdAt;

    @Field(type = FieldType.Keyword)
    private String trailer;

    @Field(type = FieldType.Text)
    private String searchableText;
} 