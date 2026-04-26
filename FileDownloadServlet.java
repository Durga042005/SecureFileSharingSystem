package com.securefileshare.servlets;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.services.CloudStorageService;
import com.securefileshare.services.EncryptionService;
import com.securefileshare.services.HashService;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.AuditDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;

import org.json.JSONObject;

public class FileDownloadServlet extends HttpServlet {
    
    private CloudStorageService cloudStorageService;
    private EncryptionService encryptionService;
    private HashService hashService;
    private FileDAO fileDAO;
    private AuditDAO auditDAO;
	private ServletRequest request;
    
    private static final java.util.Map<String, TempFileInfo> tempFileStorage = new java.util.concurrent.ConcurrentHashMap<>();
    
    private static class TempFileInfo {
        byte[] data;
        String fileName;
        long fileSize;
        long timestamp;
        
        TempFileInfo(byte[] data, String fileName) {
            this.data = data;
            this.fileName = fileName;
            this.fileSize = data.length;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    @Override
    public void init() throws ServletException {
        try {
            this.cloudStorageService = CloudStorageService.getInstance();
            this.encryptionService = new EncryptionService();
            this.hashService = new HashService();
            this.fileDAO = new FileDAO();
            this.auditDAO = new AuditDAO();
            
            startCleanupThread();
            
            System.out.println("✓ FileDownloadServlet initialized successfully");
            
        } catch (Exception e) {
            System.err.println("✗ FileDownloadServlet initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000);
                    long now = System.currentTimeMillis();
                    tempFileStorage.entrySet().removeIf(entry -> 
                        (now - entry.getValue().timestamp) > 30 * 60 * 1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
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
            String fileIdParam = request.getParameter("fileId");
            String action = request.getParameter("action");
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("=== FILE DOWNLOAD REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            System.out.println("File ID: " + fileIdParam);
            System.out.println("Action: " + (action != null ? action : "download"));
            System.out.println("=".repeat(80));
            
            if (fileIdParam == null || fileIdParam.isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "File ID is required");
                jsonResponse.put("errorCode", "MISSING_FILE_ID");
                response.setStatus(400);
                out.print(jsonResponse.toString());
                return;
            }
            
            int fileId = Integer.parseInt(fileIdParam);
            
            FileMetadata metadata = fileDAO.getFileMetadata(fileId);
            
            if (metadata == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "File not found");
                jsonResponse.put("errorCode", "FILE_NOT_FOUND");
                response.setStatus(404);
                out.print(jsonResponse.toString());
                return;
            }
            
            if (metadata.getUserId() != user.getUserId()) {
                boolean hasAccess = checkFileAccess(user.getUserId(), fileId);
                if (!hasAccess) {
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "You don't have permission to download this file");
                    jsonResponse.put("errorCode", "ACCESS_DENIED");
                    response.setStatus(403);
                    out.print(jsonResponse.toString());
                    return;
                }
            }
            
            if ("verify".equals(action)) {
                handleVerification(fileId, metadata, request, response, user);
            } else if ("password".equals(action)) {
                requestPassword(fileId, metadata, request, response, user);
            } else if ("download_token".equals(action)) {
                downloadWithToken(request.getParameter("token"), response, user);
            } else {
                handleDownload(fileId, metadata, request, response, user);
            }
            
        } catch (NumberFormatException e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Invalid file ID format");
            jsonResponse.put("errorCode", "INVALID_FILE_ID");
            response.setStatus(400);
            out.print(jsonResponse.toString());
        } catch (Exception e) {
            System.err.println("✗ Download error: " + e.getMessage());
            e.printStackTrace();
            
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Download failed: " + e.getMessage());
            jsonResponse.put("errorCode", "DOWNLOAD_FAILED");
            response.setStatus(500);
            out.print(jsonResponse.toString());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check if this is an AJAX request
        String ajaxHeader = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equals(ajaxHeader);
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                if (isAjax) {
                    sendJsonError(response, "Authentication required. Please login first.", "AUTH_REQUIRED", 401);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login.jsp");
                }
                return;
            }
            
            User user = (User) session.getAttribute("user");
            String fileIdParam = request.getParameter("fileId");
            String password = request.getParameter("password");
            String action = request.getParameter("action");
            String downloadToken = request.getParameter("token");
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("=== FILE DOWNLOAD POST REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            System.out.println("File ID: " + fileIdParam);
            System.out.println("Action: " + (action != null ? action : "decrypt_and_download"));
            System.out.println("Is AJAX: " + isAjax);
            System.out.println("=".repeat(80));
            
            if ("decrypt_and_download".equals(action)) {
                decryptAndDownload(fileIdParam, password, request, response, user);
                return;
            } else if ("get_decrypted_file".equals(action)) {
                getDecryptedFileWithToken(fileIdParam, downloadToken, request, response, user);
                return;
            } else {
                if (isAjax) {
                    sendJsonError(response, "Invalid action", "INVALID_ACTION", 400);
                } else {
                    response.sendError(400, "Invalid action");
                }
            }
            
        } catch (NumberFormatException e) {
            if (isAjax) {
                sendJsonError(response, "Invalid file ID format", "INVALID_FILE_ID", 400);
            } else {
                response.sendError(400, "Invalid file ID format");
            }
        } catch (Exception e) {
            System.err.println("✗ Download error: " + e.getMessage());
            e.printStackTrace();
            if (isAjax) {
                sendJsonError(response, "Download failed: " + e.getMessage(), "DOWNLOAD_FAILED", 500);
            } else {
                response.sendError(500, "Download failed: " + e.getMessage());
            }
        }
    }
    
    private void sendJsonError(HttpServletResponse response, String message, String errorCode, int status) 
            throws IOException {
        if (!response.isCommitted()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(status);
            
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", message);
            error.put("errorCode", errorCode);
            
            PrintWriter out = response.getWriter();
            out.print(error.toString());
            out.flush();
        }
    }
    
    private void sendJsonSuccess(HttpServletResponse response, JSONObject data) throws IOException {
        if (!response.isCommitted()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            
            PrintWriter out = response.getWriter();
            out.print(data.toString());
            out.flush();
        }
    }
    
    private void handleDownload(int fileId, FileMetadata metadata, HttpServletRequest request, 
                               HttpServletResponse response, User user) throws Exception {
        
        System.out.println("\n=== DOWNLOADING FILE ===");
        System.out.println("File: " + metadata.getOriginalFilename());
        System.out.println("Encrypted: " + metadata.isEncrypted());
        
        if (metadata.isEncrypted()) {
            response.sendRedirect(request.getContextPath() + "/jsp/user/decrypt-password.jsp?fileId=" + fileId);
            return;
        }
        
        downloadFile(metadata, null, request, response, user);
    }
    
    private void decryptAndDownload(String fileIdParam, String password, 
                                   HttpServletRequest request, HttpServletResponse response, 
                                   User user) throws Exception {
        
        int fileId = Integer.parseInt(fileIdParam);
        FileMetadata metadata = fileDAO.getFileMetadata(fileId);
        
        System.out.println("\n=== DECRYPTING AND DOWNLOADING FILE ===");
        System.out.println("File ID: " + fileId);
        System.out.println("File: " + metadata.getOriginalFilename());
        
        if (password == null || password.isEmpty()) {
            sendJsonError(response, "Password is required for encrypted file", "PASSWORD_REQUIRED", 400);
            return;
        }
        
        password = password.trim();
        
        byte[] fileData;
        try {
            fileData = cloudStorageService.downloadFile(metadata.getCloudFileId());
            System.out.println("Downloaded " + fileData.length + " bytes from Google Drive");
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            sendJsonError(response, "Failed to download file from cloud storage", "CLOUD_DOWNLOAD_FAILED", 500);
            return;
        }
        
        boolean hasSalt = metadata.getSalt() != null && !metadata.getSalt().isEmpty();
        boolean hasIv = metadata.getIv() != null && !metadata.getIv().isEmpty();
        
        System.out.println("=== FILE STATE DEBUG ===");
        System.out.println("Has Salt in DB: " + hasSalt);
        System.out.println("Has IV in DB: " + hasIv);
        System.out.println("File size: " + fileData.length + " bytes");
        System.out.println("=========================");
        
        byte[] decryptedData = null;
        String errorMessage = null;
        
        try {
            if (hasSalt && hasIv) {
                System.out.println("METHOD 1: Using DB salt and IV (new format)");
                try {
                    decryptedData = encryptionService.decrypt(fileData, password, metadata.getSalt(), metadata.getIv());
                    System.out.println("✓ METHOD 1 successful");
                } catch (Exception e1) {
                    System.out.println("METHOD 1 failed: " + e1.getMessage());
                    errorMessage = e1.getMessage();
                }
            }
            
            if (decryptedData == null && fileData.length >= 32) {
                System.out.println("METHOD 2: Old format (embedded salt+IV in file)");
                try {
                    byte[] embeddedSalt = new byte[16];
                    byte[] embeddedIv = new byte[16];
                    System.arraycopy(fileData, 0, embeddedSalt, 0, 16);
                    System.arraycopy(fileData, 16, embeddedIv, 0, 16);
                    
                    byte[] actualEncryptedData = new byte[fileData.length - 32];
                    System.arraycopy(fileData, 32, actualEncryptedData, 0, actualEncryptedData.length);
                    
                    System.out.println("Extracted salt (16 bytes), IV (16 bytes) from file header");
                    System.out.println("Actual encrypted data size: " + actualEncryptedData.length + " bytes");
                    
                    String saltBase64 = Base64.getEncoder().encodeToString(embeddedSalt);
                    String ivBase64 = Base64.getEncoder().encodeToString(embeddedIv);
                    
                    decryptedData = encryptionService.decrypt(actualEncryptedData, password, saltBase64, ivBase64);
                    System.out.println("✓ METHOD 2 successful");
                    
                    // Migrate to new format
                    try {
                        metadata.setSalt(saltBase64);
                        metadata.setIv(ivBase64);
                        fileDAO.updateEncryptionMetadata(metadata.getFileId(), saltBase64, ivBase64, metadata.getEncryptionKey());
                        System.out.println("✓ File migrated to new format");
                    } catch (Exception e) {
                        System.err.println("Migration failed (non-critical): " + e.getMessage());
                    }
                    
                } catch (Exception e3) {
                    System.out.println("METHOD 2 failed: " + e3.getMessage());
                    if (errorMessage == null) errorMessage = e3.getMessage();
                }
            }
            
            if (decryptedData == null && hasIv && !hasSalt && fileData.length >= 16) {
                System.out.println("METHOD 3: Attempting to extract salt from file (legacy support)");
                try {
                    byte[] embeddedSalt = new byte[16];
                    System.arraycopy(fileData, 0, embeddedSalt, 0, 16);
                    
                    byte[] actualEncryptedData = new byte[fileData.length - 16];
                    System.arraycopy(fileData, 16, actualEncryptedData, 0, actualEncryptedData.length);
                    
                    System.out.println("Extracted salt (16 bytes) from file");
                    System.out.println("Using IV from DB");
                    System.out.println("Actual encrypted data size: " + actualEncryptedData.length + " bytes");
                    
                    String saltBase64 = Base64.getEncoder().encodeToString(embeddedSalt);
                    
                    decryptedData = encryptionService.decrypt(actualEncryptedData, password, saltBase64, metadata.getIv());
                    System.out.println("✓ METHOD 3 successful");
                    
                    try {
                        metadata.setSalt(saltBase64);
                        fileDAO.updateEncryptionMetadata(metadata.getFileId(), saltBase64, metadata.getIv(), metadata.getEncryptionKey());
                        System.out.println("✓ File partially migrated - salt saved to DB");
                    } catch (Exception e) {
                        System.err.println("Migration failed: " + e.getMessage());
                    }
                    
                } catch (Exception e2) {
                    System.out.println("METHOD 3 failed: " + e2.getMessage());
                    if (errorMessage == null) errorMessage = e2.getMessage();
                }
            }
            
            if (decryptedData == null) {
                throw new Exception("Incorrect password or corrupted file - tried all decryption methods. Last error: " + errorMessage);
            }
            
            System.out.println("✓ Decryption successful!");
            System.out.println("Decrypted size: " + decryptedData.length + " bytes");
            
            // Verify file integrity with hash
            String downloadedHash = hashService.generateFileHash(decryptedData);
            String expectedHash = metadata.getFileHash();
            
            System.out.println("Original file hash: " + expectedHash);
            System.out.println("Downloaded file hash: " + downloadedHash);
            System.out.println("Hash match: " + downloadedHash.equals(expectedHash));
            
            // Update access count
            int newAccessCount = metadata.getAccessCount() + 1;
            fileDAO.updateAccessCount(metadata.getFileId(), newAccessCount);
            
            // ==============================================
            // LOG BOTH DECRYPT AND DOWNLOAD ACTIVITIES
            // ==============================================
            
            // Log decryption activity
            auditDAO.logActivity(
                user.getUserId(),
                "DECRYPT_SUCCESS",
                "Successfully decrypted file: " + metadata.getOriginalFilename(),
                request.getRemoteAddr(),
                "SUCCESS"
            );
            System.out.println("✓ Decrypt activity logged for user: " + user.getUsername());
            
            // ==============================================
            // FIX: ADD THIS TO LOG THE DOWNLOAD
            // ==============================================
            auditDAO.logActivity(
                user.getUserId(),
                "DOWNLOAD",
                "Downloaded file: " + metadata.getOriginalFilename(),
                request.getRemoteAddr(),
                "SUCCESS"
            );
            System.out.println("✓ Download activity logged for user: " + user.getUsername());
            
            // ==============================================
            // STORE DATA IN SESSION FOR SUCCESS PAGE
            // ==============================================
            HttpSession session = request.getSession();
            session.setAttribute("fileName", metadata.getOriginalFilename());
            session.setAttribute("fileSize", formatFileSize(decryptedData.length));
            session.setAttribute("originalHash", expectedHash);
            session.setAttribute("downloadedHash", downloadedHash);
            session.setAttribute("encryptedHash", metadata.getProcessedHash());
            session.setAttribute("encryptionAlgorithm", metadata.getEncryptionAlgorithm() != null ? 
                                metadata.getEncryptionAlgorithm() : "AES-256");
            session.setAttribute("downloadDate", new Timestamp(System.currentTimeMillis()).toString());
            session.setAttribute("fileId", fileId);
            session.setAttribute("hashMatch", downloadedHash.equals(expectedHash));
            
            // ==============================================
            // SEND FILE TO BROWSER FOR DOWNLOAD
            // ==============================================
            System.out.println("\n=== SENDING FILE TO BROWSER ===");
            
            // Set response headers for file download
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                              URLEncoder.encode(metadata.getOriginalFilename(), "UTF-8").replace("+", "%20") + "\"");
            response.setHeader("Content-Length", String.valueOf(decryptedData.length));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // Write file data to response
            try (ServletOutputStream os = response.getOutputStream()) {
                os.write(decryptedData);
                os.flush();
            }
            
            System.out.println("✓ File sent to browser successfully");
            System.out.println("✓ Download completed for user: " + user.getUsername());
            
        } catch (Exception e) {
            System.err.println("✗ Decryption failed: " + e.getMessage());
            e.printStackTrace();
            
            auditDAO.logActivity(
                user.getUserId(),
                "DECRYPT_FAILED",
                "Failed to decrypt file: " + metadata.getOriginalFilename(),
                request.getRemoteAddr(),
                "FAILED"
            );
            
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "Incorrect password: " + e.getMessage());
            errorResponse.put("errorCode", "INVALID_PASSWORD");
            sendJsonError(response, errorResponse.toString(), "INVALID_PASSWORD", 400);
        }
    }
    
    private void downloadFile(FileMetadata metadata, String password, 
                             HttpServletRequest request, HttpServletResponse response, 
                             User user) throws Exception {
        
        Path tempFile = null;
        Path decryptedFile = null;
        
        try {
            System.out.println("\n=== DOWNLOADING FROM GOOGLE DRIVE ===");
            System.out.println("Cloud File ID: " + metadata.getCloudFileId());
            System.out.println("File: " + metadata.getOriginalFilename());
            System.out.println("Encrypted: " + metadata.isEncrypted());
            
            byte[] fileData = cloudStorageService.downloadFile(metadata.getCloudFileId());
            
            if (fileData == null || fileData.length == 0) {
                throw new Exception("Downloaded file is empty");
            }
            
            System.out.println("Downloaded " + fileData.length + " bytes from Google Drive");
            
            tempFile = Files.createTempFile("download_", "_" + metadata.getOriginalFilename());
            Files.write(tempFile, fileData);
            
            byte[] finalData = fileData;
            
            if (metadata.isEncrypted()) {
                System.out.println("\n=== DECRYPTING FILE ===");
                System.out.println("File: " + metadata.getOriginalFilename());
                
                if (password == null || password.isEmpty()) {
                    throw new Exception("Password required for decryption");
                }
                
                String saltBase64 = metadata.getSalt();
                String ivBase64 = metadata.getIv();
                
                System.out.println("Salt from metadata: " + (saltBase64 != null ? "Present" : "MISSING"));
                System.out.println("IV from metadata: " + (ivBase64 != null ? "Present" : "MISSING"));
                
                if (saltBase64 == null || saltBase64.isEmpty()) {
                    throw new Exception("Salt not found in file metadata");
                }
                
                if (ivBase64 == null || ivBase64.isEmpty()) {
                    throw new Exception("IV not found in file metadata");
                }
                
                try {
                    byte[] decryptedData = encryptionService.decrypt(fileData, password, saltBase64, ivBase64);
                    finalData = decryptedData;
                    
                    String downloadedHash = hashService.generateFileHash(decryptedData);
                    String expectedHash = metadata.getFileHash();
                    
                    System.out.println("\n=== HASH VERIFICATION ===");
                    System.out.println("Expected hash: " + expectedHash);
                    System.out.println("Downloaded hash: " + downloadedHash);
                    
                    boolean hashMatch = downloadedHash.equals(expectedHash);
                    
                    if (hashMatch) {
                        System.out.println("✓ Hash verification PASSED");
                    } else {
                        System.err.println("✗ Hash verification FAILED");
                        auditDAO.logActivity(
                            user.getUserId(),
                            "DOWNLOAD_INTEGRITY_FAILED",
                            "File integrity check failed for: " + metadata.getOriginalFilename(),
                            request.getRemoteAddr(),
                            "WARNING"
                        );
                    }
                    
                    decryptedFile = Files.createTempFile("decrypted_", "_" + metadata.getOriginalFilename());
                    Files.write(decryptedFile, decryptedData);
                    
                    System.out.println("\n✓ Decryption complete!");
                    System.out.println("Decrypted size: " + decryptedData.length + " bytes");
                    
                } catch (Exception e) {
                    System.err.println("✗ Decryption failed: " + e.getMessage());
                    throw new Exception("Incorrect password", e);
                }
            }
            
            int newAccessCount = metadata.getAccessCount() + 1;
            fileDAO.updateAccessCount(metadata.getFileId(), newAccessCount);
            System.out.println("Updated access count: " + newAccessCount);
            
            // ==============================================
            // LOG DOWNLOAD ACTIVITY FOR NON-ENCRYPTED FILES
            // ==============================================
            auditDAO.logActivity(
                user.getUserId(),
                "DOWNLOAD",
                "Downloaded file: " + metadata.getOriginalFilename(),
                request.getRemoteAddr(),
                "SUCCESS"
            );
            System.out.println("✓ Download activity logged for user: " + user.getUsername());
            
            // Send file to browser for download
            sendFileToBrowser(finalData, metadata.getOriginalFilename(), response);
            System.out.println("\n✓ File download completed successfully");
            
        } catch (Exception e) {
            System.err.println("\n✗ File download failed: " + e.getMessage());
            e.printStackTrace();
            
            auditDAO.logActivity(
                user.getUserId(),
                "DOWNLOAD_ERROR",
                "Failed to download file: " + metadata.getOriginalFilename() + " - " + e.getMessage(),
                request.getRemoteAddr(),
                "FAILED"
            );
            
            throw e;
            
        } finally {
            cleanupTempFile(tempFile);
            cleanupTempFile(decryptedFile);
        }
    }
    
    private void downloadWithToken(String token, HttpServletResponse response, User user) throws Exception {
        System.out.println("\n=== DOWNLOADING FILE WITH TOKEN ===");
        System.out.println("Token: " + token);
        
        if (token == null || token.isEmpty()) {
            sendJsonError(response, "Invalid download token", "INVALID_TOKEN", 400);
            return;
        }
        
        TempFileInfo fileInfo = tempFileStorage.get(token);
        
        if (fileInfo == null) {
            System.err.println("Token not found or expired: " + token);
            sendJsonError(response, "Download link expired or invalid. Please try again.", "TOKEN_EXPIRED", 404);
            return;
        }
        
        byte[] fileData;
        String fileName = fileInfo.fileName;
        
        if (fileInfo.data != null && fileInfo.data.length > 0) {
            String dataStr = new String(fileInfo.data);
            if (dataStr.startsWith("FILE:")) {
                String tempFilePath = dataStr.substring(5);
                Path tempFile = Path.of(tempFilePath);
                if (Files.exists(tempFile)) {
                    fileData = Files.readAllBytes(tempFile);
                    System.out.println("Read " + fileData.length + " bytes from temp file");
                    
                    // Clean up temp file
                    Files.deleteIfExists(tempFile);
                    System.out.println("Temp file cleaned up");
                } else {
                    sendJsonError(response, "Temporary file not found", "FILE_NOT_FOUND", 404);
                    return;
                }
            } else {
                fileData = fileInfo.data;
            }
        } else {
            sendJsonError(response, "No data found for token", "DATA_NOT_FOUND", 404);
            return;
        }
        
        // Remove from storage
        tempFileStorage.remove(token);
        
        // Log activity
        auditDAO.logActivity(
            user.getUserId(),
            "DOWNLOAD_COMPLETE",
            "Downloaded file: " + fileName,
            request.getRemoteAddr(),
            "SUCCESS"
        );
        
        // Send file to browser
        sendFileToBrowser(fileData, fileName, response);
        System.out.println("✓ File download completed");
    }
    
    private void getDecryptedFileWithToken(String fileIdParam, String token, 
                                          HttpServletRequest request, HttpServletResponse response, 
                                          User user) throws Exception {
        
        System.out.println("\n=== GETTING DECRYPTED FILE WITH TOKEN ===");
        
        if (token == null || token.isEmpty()) {
            sendJsonError(response, "Invalid token", "INVALID_TOKEN", 400);
            return;
        }
        
        TempFileInfo fileInfo = tempFileStorage.get(token);
        
        if (fileInfo == null) {
            sendJsonError(response, "Token expired or invalid", "TOKEN_EXPIRED", 404);
            return;
        }
        
        byte[] decryptedData;
        String fileName = fileInfo.fileName;
        
        if (fileInfo.data != null && fileInfo.data.length > 0) {
            String dataStr = new String(fileInfo.data);
            if (dataStr.startsWith("FILE:")) {
                String tempFilePath = dataStr.substring(5);
                Path tempFile = Path.of(tempFilePath);
                if (Files.exists(tempFile)) {
                    decryptedData = Files.readAllBytes(tempFile);
                } else {
                    sendJsonError(response, "Temporary file not found", "FILE_NOT_FOUND", 404);
                    return;
                }
            } else {
                decryptedData = fileInfo.data;
            }
        } else {
            sendJsonError(response, "No data found", "DATA_NOT_FOUND", 404);
            return;
        }
        
        // Send file to browser
        sendFileToBrowser(decryptedData, fileName, response);
        
        // Clean up
        tempFileStorage.remove(token);
        
        if (fileInfo.data != null) {
            String dataStr = new String(fileInfo.data);
            if (dataStr.startsWith("FILE:")) {
                String tempFilePath = dataStr.substring(5);
                try {
                    Files.deleteIfExists(Path.of(tempFilePath));
                } catch (Exception e) {
                    System.err.println("Failed to delete temp file: " + e.getMessage());
                }
            }
        }
    }
    
    private void handleVerification(int fileId, FileMetadata metadata, HttpServletRequest request,
                                   HttpServletResponse response, User user) throws Exception {
        
        JSONObject jsonResponse = new JSONObject();
        
        jsonResponse.put("success", true);
        jsonResponse.put("fileId", metadata.getFileId());
        jsonResponse.put("fileName", metadata.getOriginalFilename());
        jsonResponse.put("fileSize", formatFileSize(metadata.getFileSize()));
        
        if (metadata.getUploadDate() != null) {
            jsonResponse.put("uploadDate", metadata.getUploadDate().toString());
        } else {
            jsonResponse.put("uploadDate", "Unknown");
        }
        
        jsonResponse.put("encrypted", metadata.isEncrypted());
        
        if (metadata.isEncrypted()) {
            jsonResponse.put("encryptionAlgorithm", 
                metadata.getEncryptionAlgorithm() != null ? metadata.getEncryptionAlgorithm() : "AES-256");
            jsonResponse.put("originalHash", metadata.getFileHash() != null ? metadata.getFileHash() : "");
            jsonResponse.put("encryptedHash", 
                metadata.getProcessedHash() != null ? metadata.getProcessedHash() : metadata.getFileHash());
        } else {
            jsonResponse.put("fileHash", metadata.getFileHash() != null ? metadata.getFileHash() : "");
        }
        
        jsonResponse.put("cloudStorage", "Google Drive");
        jsonResponse.put("cloudFileId", metadata.getCloudFileId() != null ? metadata.getCloudFileId() : "");
        
        response.setContentType("application/json");
        response.getWriter().write(jsonResponse.toString());
    }
    
    private void requestPassword(int fileId, FileMetadata metadata, HttpServletRequest request,
                                HttpServletResponse response, User user) throws IOException {
        
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("success", true);
        jsonResponse.put("requirePassword", true);
        jsonResponse.put("fileId", fileId);
        jsonResponse.put("fileName", metadata.getOriginalFilename());
        jsonResponse.put("fileSize", formatFileSize(metadata.getFileSize()));
        jsonResponse.put("encrypted", true);
        jsonResponse.put("encryptionAlgorithm", 
            metadata.getEncryptionAlgorithm() != null ? metadata.getEncryptionAlgorithm() : "AES-256");
        
        response.setContentType("application/json");
        response.getWriter().write(jsonResponse.toString());
    }
    
    private void sendFileToBrowser(byte[] fileData, String fileName, HttpServletResponse response) 
            throws IOException {
        
        response.reset();
        
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                          URLEncoder.encode(fileName, "UTF-8").replace("+", "%20") + "\"");
        response.setHeader("Content-Length", String.valueOf(fileData.length));
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        try (ServletOutputStream os = response.getOutputStream()) {
            os.write(fileData);
            os.flush();
        }
    }
    
    private void cleanupTempFile(Path file) {
        if (file != null && Files.exists(file)) {
            try {
                Files.delete(file);
                System.out.println("Temp file cleaned up: " + file.getFileName());
            } catch (IOException e) {
                System.err.println("Warning: Failed to delete temp file: " + e.getMessage());
            }
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private boolean checkFileAccess(int userId, int fileId) {
        // Implement your file sharing logic here
        return false;
    }
}