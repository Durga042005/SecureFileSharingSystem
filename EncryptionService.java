package com.securefileshare.services;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

public class EncryptionService {
    
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int KEY_SIZE = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int IV_SIZE = 16;
    private static final int SALT_SIZE = 16;
    
    public EncryptionResult encrypt(byte[] fileData, String password) throws Exception {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be null or empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        System.out.println("Encrypting " + fileData.length + " bytes with AES-256...");
        
        byte[] salt = generateSalt();
        System.out.println("Generated salt: " + Base64.getEncoder().encodeToString(salt).substring(0, 16) + "...");
        
        SecretKey secretKey = generateKey(password, salt);
        System.out.println("Generated AES-256 key from password");
        
        byte[] iv = generateIV();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        System.out.println("Generated IV: " + Base64.getEncoder().encodeToString(iv).substring(0, 16) + "...");
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedData = cipher.doFinal(fileData);
        
        System.out.println("Encryption complete. Original size: " + fileData.length + 
                          " bytes, Encrypted size: " + encryptedData.length + " bytes");
        
        String originalFileHash = generateSHA256(fileData);
        System.out.println("Original file SHA-256: " + originalFileHash.substring(0, 32) + "...");
        
        String encryptedFileHash = generateSHA256(encryptedData);
        System.out.println("Encrypted file SHA-256: " + encryptedFileHash.substring(0, 32) + "...");
        
        if (encryptedData.length == 0) {
            throw new SecurityException("Encryption failed - no data produced");
        }
        
        return new EncryptionResult(
            encryptedData,
            originalFileHash,
            encryptedFileHash,
            Base64.getEncoder().encodeToString(secretKey.getEncoded()),
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(salt)
        );
    }
    
    public byte[] decrypt(byte[] encryptedData, String password, String saltBase64, String ivBase64) throws Exception {
        if (encryptedData == null || encryptedData.length == 0) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        if (saltBase64 == null || ivBase64 == null) {
            throw new IllegalArgumentException("Salt and IV cannot be null");
        }
        
        System.out.println("Decrypting " + encryptedData.length + " bytes with AES-256...");
        
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            
            SecretKey secretKey = generateKey(password, salt);
            
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            byte[] dataToDecrypt = encryptedData;
            
            if (encryptedData.length > 32) {
                // Check for old format detection
            }
            
            byte[] decryptedData = cipher.doFinal(dataToDecrypt);
            
            System.out.println("Decryption complete. Size: " + decryptedData.length + " bytes");
            return decryptedData;
            
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            System.err.println("Decryption failed - Wrong password or corrupted data: " + e.getMessage());
            throw new Exception("Incorrect password", e);
        } catch (IllegalArgumentException e) {
            System.err.println("Decryption failed - Invalid salt or IV format: " + e.getMessage());
            throw new Exception("Invalid encryption metadata", e);
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            throw new Exception("Decryption failed: " + e.getMessage(), e);
        }
    }
    
    public boolean verifyFileIntegrity(byte[] fileData, String expectedHash) throws NoSuchAlgorithmException {
        String actualHash = generateSHA256(fileData);
        boolean matches = actualHash.equals(expectedHash);
        
        System.out.println("Integrity check: " + (matches ? "PASSED" : "FAILED"));
        System.out.println("Expected: " + expectedHash.substring(0, 32) + "...");
        System.out.println("Actual:   " + actualHash.substring(0, 32) + "...");
        
        return matches;
    }
    
    private SecretKey generateKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    public String generateSHA256(byte[] data) throws NoSuchAlgorithmException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null for hashing");
        }
        
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(data);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        random.nextBytes(salt);
        return salt;
    }
    
    private byte[] generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);
        return iv;
    }
    
    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    public String generateReadablePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(16);
        
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    public static class EncryptionResult {
        private byte[] encryptedData;
        private String originalFileHash;
        private String encryptedFileHash;
        private String encryptionKey;
        private String iv;
        private String salt;
        
        public EncryptionResult(byte[] encryptedData, String originalFileHash, String encryptedFileHash, 
                              String encryptionKey, String iv, String salt) {
            this.encryptedData = encryptedData;
            this.originalFileHash = originalFileHash;
            this.encryptedFileHash = encryptedFileHash;
            this.encryptionKey = encryptionKey;
            this.iv = iv;
            this.salt = salt;
        }
        
        public byte[] getEncryptedData() { return encryptedData; }
        public String getOriginalFileHash() { return originalFileHash; }
        public String getEncryptedFileHash() { return encryptedFileHash; }
        public String getEncryptionKey() { return encryptionKey; }
        public String getIv() { return iv; }
        public String getSalt() { return salt; }
        
        public String getOriginalFileHashShort() {
            return originalFileHash != null && originalFileHash.length() > 16 ? 
                   originalFileHash.substring(0, 16) + "..." : originalFileHash;
        }
        
        public String getEncryptedFileHashShort() {
            return encryptedFileHash != null && encryptedFileHash.length() > 16 ? 
                   encryptedFileHash.substring(0, 16) + "..." : encryptedFileHash;
        }
        
        public int getOriginalSize() {
            return encryptedData != null ? encryptedData.length : 0;
        }
    }

    public byte[] decryptFile(byte[] encryptedData, String password) throws Exception {
        if (encryptedData == null || encryptedData.length == 0) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        System.out.println("Decrypting " + encryptedData.length + " bytes...");
        
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        
        System.arraycopy(encryptedData, 0, salt, 0, 16);
        System.arraycopy(encryptedData, 16, iv, 0, 16);
        
        SecretKey secretKey = generateKey(password, salt);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        byte[] actualEncryptedData = new byte[encryptedData.length - 32];
        System.arraycopy(encryptedData, 32, actualEncryptedData, 0, actualEncryptedData.length);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decryptedData = cipher.doFinal(actualEncryptedData);
        
        System.out.println("Decryption complete. Decrypted size: " + decryptedData.length + " bytes");
        
        return decryptedData;
    }
}