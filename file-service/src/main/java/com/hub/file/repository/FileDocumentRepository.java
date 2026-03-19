package com.hub.file.repository;

import com.hub.file.document.FileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileDocumentRepository extends MongoRepository<FileDocument, String> {

    List<FileDocument> findByConversationId(String conversationId);

    List<FileDocument> findByUploaderId(Long uploaderId);
}
