package com.hub.frais.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class JustificatifStorageService {

    private static final List<String> ALLOWED_TYPES = List.of("application/pdf", "image/jpeg", "image/jpg", "image/png");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    private final GridFsOperations gridFsOperations;

    public JustificatifStorageService(GridFsOperations gridFsOperations) {
        this.gridFsOperations = gridFsOperations;
    }

    public String store(Long demandeFraisId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && !contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Utilisez PDF, JPG ou PNG.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Fichier trop volumineux. Maximum 5 MB.");
        }
        ObjectId id = gridFsOperations.store(
                file.getInputStream(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "justificatif",
                contentType,
                Map.<String, Object>of("demandeFraisId", demandeFraisId)
        );
        return id.toHexString();
    }

    public GridFsResource getResource(String fileId) {
        if (fileId == null || !ObjectId.isValid(fileId)) {
            return null;
        }
        GridFSFile file = gridFsOperations.findOne(Query.query(Criteria.where("_id").is(new ObjectId(fileId))));
        if (file == null) {
            return null;
        }
        return gridFsOperations.getResource(file);
    }
}
