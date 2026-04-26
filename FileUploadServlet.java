package com.securefileshare.servlets;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.services.CloudStorageService;
import com.securefileshare.services.EncryptionService;
import com.securefileshare.services.HashService;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.AuditDAO;
import com.securefileshare.services.CloudStorageService.UploadResult;
import com.securefileshare.services.EncryptionService.EncryptionResult;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

@MultipartConfig(
    maxFileSize = 5368709120L,
    maxRequestSize = 5368709120L,
    fileSizeThreshold = 1048576
)
public class FileUploadServlet extends HttpServlet {
    
    private CloudStorageService cloudStorageService;
    private EncryptionService encryptionService;
    private HashService hashService;
    private FileDAO fileDAO;
    private AuditDAO auditDAO;
    
    private static final Map<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();
    
    @Override
    public void init() throws ServletException {
        try {
            this.cloudStorageService = CloudStorageService.getInstance();
            this.encryptionService = new EncryptionService();
            this.hashService = new HashService();
            this.fileDAO = new FileDAO();
            this.auditDAO = new AuditDAO();
            
            System.out.println("✓ FileUploadServlet initialized successfully");
            System.out.println("Cloud Storage Status: " + 
                (cloudStorageService.isServiceAvailable() ? "AVAILABLE (Google Drive)" : "UNAVAILABLE"));
            
            if (cloudStorageService.isServiceAvailable()) {
                System.out.println("Google Drive connection test: " + cloudStorageService.testConnection());
            }
            
        } catch (Exception e) {
            System.err.println("✗ FileUploadServlet initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Authentication required. Please login first.");
                jsonResponse.put("errorCode", "AUTH_REQUIRED");
                response.setStatus(401);
                out.print(jsonResponse.toString());
                return;
            }
            
            User user = (User) session.getAttribute("user");
            String action = request.getParameter("action");
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("=== FILE UPLOAD REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            System.out.println("Action: " + (action != null ? action : "direct_upload"));
            System.out.println("Cloud Storage Available: " + cloudStorageService.isServiceAvailable());
            System.out.println("=".repeat(80));
            
            if (!cloudStorageService.isServiceAvailable()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Google Drive service is not available. Please check your Google Drive configuration.");
                jsonResponse.put("errorCode", "CLOUD_SERVICE_UNAVAILABLE");
                jsonResponse.put("debugInfo", cloudStorageService.testConnection());
                response.setStatus(503);
                out.print(jsonResponse.toString());
                return;
            }
            
            if (action != null) {
                switch (action) {
                    case "init":
                        initChunkedUpload(request, response, user);
                        break;
                    case "chunk":
                        uploadChunk(request, response, user);
                        break;
                    case "complete":
                        completeChunkedUpload(request, response, user);
                        break;
                    default:
                        handleDirectUpload(request, response, user);
                }
            } else {
                handleDirectUpload(request, response, user);
            }
            
        } catch (Exception e) {
            System.err.println("✗ Upload error: " + e.getMessage());
            e.printStackTrace();
            
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Upload failed: " + e.getMessage());
            jsonResponse.put("errorCode", "UPLOAD_FAILED");
            jsonResponse.put("errorDetails", e.toString());
            
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            jsonResponse.put("stackTrace", sw.toString());
            
            response.setStatus(500);
            out.print(jsonResponse.toString());
        }
    }
    
    private void initChunkedUpload(HttpServletRequest request, HttpServletResponse response, User user) 
            throws Exception {
        
        String uploadId = UUID.randomUUID().toString();
        String filename = request.getParameter("filename");
        long totalSize = Long.parseLong(request.getParameter("totalSize"));
        boolean encrypt = "true".equals(request.getParameter("encrypt"));
        String password = request.getParameter("password");
        
        if (encrypt && (password == null || password.isEmpty())) {
            password = encryptionService.generateRandomPassword();
        }
        
        UploadSession session = new UploadSession();
        session.setUploadId(uploadId);
        session.setUserId(user.getUserId());
        session.setFilename(filename);
        session.setTotalSize(totalSize);
        session.setEncrypted(encrypt);
        session.setPassword(password);
        session.setStatus("initializing");
        session.setStartTime(System.currentTimeMillis());
        
        uploadSessions.put(uploadId, session);
        
        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("uploadId", uploadId);
        result.put("chunkSize", 5 * 1024 * 1024);
        result.put("message", "Upload session initialized");
        
        if (encrypt) {
            result.put("encryptionPassword", password);
            result.put("warning", "Save this password for decryption: " + password);
        }
        
        response.getWriter().write(result.toString());
    }
    
    private void uploadChunk(HttpServletRequest request, HttpServletResponse response, User user) 
            throws Exception {
        
        String uploadId = request.getParameter("uploadId");
        UploadSession session = uploadSessions.get(uploadId);
        
        if (session == null) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Invalid upload session");
            error.put("errorCode", "INVALID_SESSION");
            response.setStatus(400);
            response.getWriter().write(error.toString());
            return;
        }
        
        Part chunkPart = request.getPart("chunk");
        int chunkIndex = Integer.parseInt(request.getParameter("chunkIndex"));
        
        String tempDir = System.getProperty("java.io.tmpdir") + "/securefileshare_uploads/" + uploadId + "/";
        Files.createDirectories(Paths.get(tempDir));
        
        Path chunkPath = Paths.get(tempDir + "chunk_" + chunkIndex);
        try (InputStream is = chunkPart.getInputStream()) {
            Files.copy(is, chunkPath);
        }
        
        session.incrementChunks();
        session.setStatus("uploading");
        
        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("chunkIndex", chunkIndex);
        result.put("uploadedChunks", session.getUploadedChunks());
        result.put("message", "Chunk " + chunkIndex + " uploaded successfully");
        
        response.getWriter().write(result.toString());
    }
    
    private void completeChunkedUpload(HttpServletRequest request, HttpServletResponse response, User user) 
            throws Exception {
        
        String uploadId = request.getParameter("uploadId");
        UploadSession session = uploadSessions.get(uploadId);
        
        if (session == null) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Invalid upload session");
            error.put("errorCode", "INVALID_SESSION");
            response.setStatus(400);
            response.getWriter().write(error.toString());
            return;
        }
        
        session.setStatus("processing");
        
        System.out.println("\n=== COMPLETING CHUNKED UPLOAD ===");
        System.out.println("Upload ID: " + uploadId);
        System.out.println("File: " + session.getFilename());
        System.out.println("User: " + user.getUsername());
        System.out.println("Total Size: " + session.getTotalSize() + " bytes");
        
        String tempDir = System.getProperty("java.io.tmpdir") + "/securefileshare_uploads/" + uploadId + "/";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int chunkIndex = 0;
        long totalBytesRead = 0;
        while (true) {
            Path chunkPath = Paths.get(tempDir + "chunk_" + chunkIndex);
            if (!Files.exists(chunkPath)) {
                break;
            }
            
            byte[] chunkData = Files.readAllBytes(chunkPath);
            baos.write(chunkData);
            totalBytesRead += chunkData.length;
            chunkIndex++;
        }
        
        byte[] fileData = baos.toByteArray();
        System.out.println("Assembled file size: " + fileData.length + " bytes");
        System.out.println("Expected size: " + session.getTotalSize() + " bytes");
        System.out.println("Chunks assembled: " + chunkIndex);
        
        processUploadedFile(fileData, session.getFilename(), session.isEncrypted(), 
                           session.getPassword(), request, response, user);
        
        cleanupTempFiles(uploadId);
        uploadSessions.remove(uploadId);
    }
    
    private void handleDirectUpload(HttpServletRequest request, HttpServletResponse response, User user) 
            throws Exception {
        
        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "No file selected or file is empty");
            error.put("errorCode", "NO_FILE");
            response.setStatus(400);
            response.getWriter().write(error.toString());
            return;
        }
        
        String filename = getFileName(filePart);
        boolean encrypt = "true".equals(request.getParameter("encrypt")) || 
                         "on".equals(request.getParameter("encrypt"));
        String password = request.getParameter("encryptionPassword");
        String description = request.getParameter("description");
        
        System.out.println("\n=== DIRECT FILE UPLOAD ===");
        System.out.println("File: " + filename);
        System.out.println("Size: " + filePart.getSize() + " bytes");
        System.out.println("Encrypt: " + encrypt);
        System.out.println("User: " + user.getUsername());
        System.out.println("Description: " + description);
        
        byte[] fileData;
        try (InputStream is = filePart.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            fileData = baos.toByteArray();
        }
        
        processUploadedFile(fileData, filename, encrypt, password, request, response, user);
    }
    
    private void processUploadedFile(byte[] fileData, String filename, boolean encrypt, 
                                    String password, HttpServletRequest request, 
                                    HttpServletResponse response, User user) throws Exception {
        
        System.out.println("\n=== PROCESSING UPLOADED FILE ===");
        System.out.println("File: " + filename);
        System.out.println("Size: " + fileData.length + " bytes");
        System.out.println("Encrypt: " + encrypt);
        
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();
        
        boolean isUserProvidedPassword = false;
        
        if (encrypt) {
            if (password != null && !password.trim().isEmpty()) {
                isUserProvidedPassword = true;
                System.out.println("Using user-provided password");
            } else {
                password = encryptionService.generateRandomPassword();
                System.out.println("Generated random password: " + password);
            }
        }
        
        Path tempFile = null;
        String originalHash = null;
        String encryptedHash = null;
        EncryptionResult encryptionResult = null;
        byte[] processedData = fileData;
        
        try {
            tempFile = Files.createTempFile("upload_", "_" + filename);
            Files.write(tempFile, fileData);
            
            System.out.println("Temp file created: " + tempFile.toString());
            
            originalHash = hashService.generateFileHash(fileData);
            System.out.println("Original file SHA-256: " + originalHash);
            
            if (encrypt) {
                System.out.println("Encrypting file with AES-256...");
                encryptionResult = encryptionService.encrypt(fileData, password);
                processedData = encryptionResult.getEncryptedData();
                encryptedHash = hashService.generateFileHash(processedData);
                System.out.println("Encryption complete.");
                System.out.println("Original size: " + fileData.length + " bytes");
                System.out.println("Encrypted size: " + processedData.length + " bytes");
                System.out.println("Encrypted file SHA-256: " + encryptedHash);
            } else {
                encryptedHash = originalHash;
            }
            
            String folder = request.getParameter("folder");
            if (folder == null || folder.isEmpty()) {
                folder = "uploads";
            }
            
            String description = request.getParameter("description");
            String tags = request.getParameter("tags");
            
            System.out.println("\n=== ATTEMPTING GOOGLE DRIVE UPLOAD ===");
            System.out.println("Folder: " + folder);
            System.out.println("Description: " + description);
            System.out.println("Encryption Result: " + (encryptionResult != null ? "Available" : "Not Available"));
            
            UploadResult uploadResult = null;
            try {
                uploadResult = cloudStorageService.uploadFileWithEncryption(
                    fileData,
                    filename,
                    folder,
                    user.getUserId(),
                    encrypt,
                    password,
                    encryptionResult
                );
                
                System.out.println("UploadResult received: " + (uploadResult != null ? "OK" : "NULL"));
                if (uploadResult != null) {
                    System.out.println("Upload success: " + uploadResult.isSuccess());
                    System.out.println("Error message: " + uploadResult.getErrorMessage());
                }
                
            } catch (Exception e) {
                System.err.println("EXCEPTION during uploadFileWithEncryption: " + e.getMessage());
                e.printStackTrace();
                throw new Exception("Failed to upload to Google Drive: " + e.getMessage(), e);
            }
            
            if (uploadResult == null) {
                throw new Exception("Google Drive service returned null result");
            }
            
            if (!uploadResult.isSuccess()) {
                String errorMsg = uploadResult.getErrorMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "Google Drive upload failed with no error message";
                }
                throw new Exception(errorMsg);
            }
            
            System.out.println("✓ Google Drive upload successful!");
            System.out.println("Google Drive File ID: " + uploadResult.getCloudFileId());
            System.out.println("Shareable URL: " + uploadResult.getShareableUrl());
            System.out.println("Filename: " + uploadResult.getFilename());
            System.out.println("Original Filename: " + uploadResult.getOriginalFilename());
            
            FileMetadata metadata = new FileMetadata();
            metadata.setUserId(user.getUserId());
            metadata.setOriginalFilename(filename);
            metadata.setStoredFilename(uploadResult.getFilename());
            metadata.setFileHash(originalHash);
            metadata.setProcessedHash(encryptedHash);
            metadata.setFileSize(fileData.length);
            metadata.setFileType(getFileExtension(filename));
            metadata.setDescription(description);
            metadata.setCloudFileId(uploadResult.getCloudFileId());
            metadata.setStoragePath(uploadResult.getWebdavPath());
            metadata.setEncrypted(encrypt);
            metadata.setAccessCount(0);
            metadata.setUploadDate(new Timestamp(System.currentTimeMillis()));
            
            if (encrypt && encryptionResult != null) {
                metadata.setEncryptionKey(encryptionResult.getEncryptionKey());
                metadata.setIv(encryptionResult.getIv());
                metadata.setSalt(encryptionResult.getSalt());
                metadata.setEncryptionAlgorithm("AES-256");
                
                System.out.println("!!! SAVING TO DATABASE !!!");
                System.out.println("Salt: " + metadata.getSalt());
                System.out.println("IV: " + metadata.getIv());
                System.out.println("Key: " + metadata.getEncryptionKey());
                System.out.println("Password source: " + (isUserProvidedPassword ? "USER PROVIDED" : "AUTO-GENERATED"));
            }
            
            int fileId = fileDAO.saveFileMetadata(metadata);
            System.out.println("File metadata saved with ID: " + fileId);
            
            HttpSession session = request.getSession();

            if (encrypt) {
                session.setAttribute("encryptionPassword", password);
                session.setAttribute("isUserProvidedPassword", isUserProvidedPassword);
            }

            session.setAttribute("originalHash", originalHash);
            session.setAttribute("encryptedHash", encryptedHash);

            session.setAttribute("uploadedFile", filename);
            session.setAttribute("storedFile", uploadResult.getFilename());
            session.setAttribute("fileSize", formatFileSize(fileData.length));
            session.setAttribute("cloudFileId", uploadResult.getCloudFileId());
            session.setAttribute("shareableUrl", uploadResult.getShareableUrl());
            session.setAttribute("fileId", fileId);
            session.setAttribute("uploadDate", new Timestamp(System.currentTimeMillis()));
            session.setAttribute("description", description);
            session.setAttribute("encrypted", encrypt);
            session.setAttribute("passwordSource", isUserProvidedPassword ? "user" : "auto");

            System.out.println("=== STORED IN SESSION ===");
            System.out.println("encryptionPassword: " + (encrypt ? password : "N/A"));
            System.out.println("Password source: " + (isUserProvidedPassword ? "USER" : "AUTO"));
            System.out.println("originalHash: " + originalHash);
            System.out.println("encryptedHash: " + encryptedHash);
            System.out.println("uploadedFile: " + filename);
            System.out.println("fileId: " + fileId);
            System.out.println("==========================");

            auditDAO.logActivity(
                user.getUserId(),
                "UPLOAD",
                "Uploaded file: " + filename + " (Encrypted: " + encrypt + ", Password: " + 
                (isUserProvidedPassword ? "User-provided" : "Auto-generated") + ") to Google Drive",
                request.getRemoteAddr(),
                "SUCCESS"
            );

            jsonResponse.put("success", true);
            jsonResponse.put("redirect", request.getContextPath() + "/jsp/user/upload-success.jsp");
            jsonResponse.put("message", "File uploaded successfully!");
            jsonResponse.put("filename", filename);
            jsonResponse.put("fileId", fileId);
            jsonResponse.put("encrypted", encrypt);
            if (encrypt) {
                jsonResponse.put("encryptionPassword", password);
                jsonResponse.put("isUserProvidedPassword", isUserProvidedPassword);
            }

            JSONObject hashes = new JSONObject();
            hashes.put("original", originalHash);
            hashes.put("encrypted", encryptedHash);
            jsonResponse.put("sha256", hashes);

            out.print(jsonResponse.toString());
            return;
            
        } finally {
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                    System.out.println("Temp file cleaned up");
                } catch (IOException e) {
                    System.err.println("Warning: Failed to delete temp file: " + e.getMessage());
                }
            }
        }
    }
    
    private void cleanupTempFiles(String uploadId) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir") + "/securefileshare_uploads/" + uploadId + "/";
            Path dirPath = Paths.get(tempDir);
            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                     .sorted((a, b) -> -a.compareTo(b))
                     .forEach(path -> {
                         try { 
                             Files.delete(path); 
                         } catch (IOException e) {
                             System.err.println("Failed to delete temp file: " + path);
                         }
                     });
                System.out.println("Cleaned up temp files for upload: " + uploadId);
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up temp files: " + e.getMessage());
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            String[] items = contentDisposition.split(";");
            for (String item : items) {
                if (item.trim().startsWith("filename")) {
                    String filename = item.substring(item.indexOf('=') + 2, item.length() - 1);
                    int lastSlash = filename.lastIndexOf('\\');
                    if (lastSlash != -1) {
                        filename = filename.substring(lastSlash + 1);
                    }
                    lastSlash = filename.lastIndexOf('/');
                    if (lastSlash != -1) {
                        filename = filename.substring(lastSlash + 1);
                    }
                    return filename;
                }
            }
        }
        return "file_" + System.currentTimeMillis();
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot != -1 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject info = new JSONObject();
        info.put("service", "File Upload Service");
        info.put("status", "active");
        info.put("maxFileSize", "5GB");
        info.put("supportedEncryption", "AES-256");
        info.put("cloudStorage", "Google Drive");
        info.put("chunkedUpload", true);
        
        if (cloudStorageService != null) {
            info.put("cloudStorageAvailable", cloudStorageService.isServiceAvailable());
            info.put("cloudStorageStatus", cloudStorageService.testConnection());
        }
        
        response.getWriter().write(info.toString());
    }
    
    private static class UploadSession {
        private String uploadId;
        private int userId;
        private String filename;
        private long totalSize;
        private int uploadedChunks;
        private String status;
        private boolean encrypted;
        private String password;
        private long startTime;
        
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        
        public int getUploadedChunks() { return uploadedChunks; }
        public void setUploadedChunks(int uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public void incrementChunks() { this.uploadedChunks++; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
    }
}