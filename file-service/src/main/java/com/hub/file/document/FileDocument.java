package com.hub.file.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDocument {

    @Id
    private String id;
    private Long uploaderId;
    private String filename;
    private String contentType;
    private long size;
    private String conversationId;
    private Instant createdAt;
}
