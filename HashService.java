package com.securefileshare.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashService {
    
    public String generateSHA256(byte[] fileData) {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be null or empty for hashing");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileData);
            
            return HexFormat.of().formatHex(hash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    public String generateSHA256WithLog(byte[] fileData, String fileName) {
        System.out.println("Generating SHA-256 hash for: " + fileName);
        System.out.println("Data size: " + fileData.length + " bytes");
        
        long startTime = System.currentTimeMillis();
        String hash = generateSHA256(fileData);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Hash generated in " + (endTime - startTime) + "ms");
        System.out.println("SHA-256 Hash: " + hash);
        System.out.println("Hash length: " + hash.length() + " characters");
        
        return hash;
    }
    
    public String generateMD5(byte[] fileData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(fileData);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    public String generateSHA512(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }
    
    public String generateSHA512(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }
    
    public boolean verifyFileIntegrity(byte[] fileData, String expectedHash) {
        if (expectedHash == null || expectedHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected hash cannot be null or empty");
        }
        
        System.out.println("Verifying file integrity...");
        System.out.println("Expected hash: " + expectedHash.substring(0, Math.min(32, expectedHash.length())) + "...");
        
        String actualHash = generateSHA256(fileData);
        boolean matches = actualHash.equals(expectedHash);
        
        System.out.println("Actual hash: " + actualHash.substring(0, Math.min(32, actualHash.length())) + "...");
        System.out.println("Integrity check: " + (matches ? "✅ PASSED" : "❌ FAILED"));
        
        if (!matches) {
            System.err.println("WARNING: File integrity check failed!");
            System.err.println("Expected: " + expectedHash);
            System.err.println("Actual:   " + actualHash);
        }
        
        return matches;
    }
    
    public boolean verifyFileIntegrityAdvanced(byte[] fileData, String expectedSHA256, String expectedMD5) {
        boolean sha256Valid = false;
        boolean md5Valid = false;
        
        if (expectedSHA256 != null && !expectedSHA256.trim().isEmpty()) {
            String actualSHA256 = generateSHA256(fileData);
            sha256Valid = actualSHA256.equals(expectedSHA256);
            System.out.println("SHA-256 check: " + (sha256Valid ? "✅" : "❌"));
        }
        
        if (expectedMD5 != null && !expectedMD5.trim().isEmpty()) {
            String actualMD5 = generateMD5(fileData);
            md5Valid = actualMD5.equals(expectedMD5);
            System.out.println("MD5 check: " + (md5Valid ? "✅" : "❌"));
        }
        
        if (expectedSHA256 != null && expectedMD5 != null) {
            return sha256Valid && md5Valid;
        } else if (expectedSHA256 != null) {
            return sha256Valid;
        } else if (expectedMD5 != null) {
            return md5Valid;
        }
        
        return false;
    }
    
    public String hashPassword(String password, String salt) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        if (salt == null || salt.trim().isEmpty()) {
            salt = generateRandomSalt();
        }
        
        String combined = password + salt;
        
        String hash = generateSHA512(combined);
        
        return salt + ":" + hash;
    }
    
    public boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        String[] parts = storedHash.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        
        String salt = parts[0];
        String expectedHash = parts[1];
        
        String combined = password + salt;
        String actualHash = generateSHA512(combined);
        
        return actualHash.equals(expectedHash);
    }
    
    public String generateRandomSalt() {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }
    
    public double calculateHashSimilarity(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != hash2.length()) {
            return 0.0;
        }
        
        int matchingChars = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) == hash2.charAt(i)) {
                matchingChars++;
            }
        }
        
        return (matchingChars * 100.0) / hash1.length();
    }
    
    public String generateShortHash(String fullHash, int length) {
        if (fullHash == null || fullHash.length() < length) {
            return fullHash;
        }
        return fullHash.substring(0, length) + "...";
    }
    
    public String generateFileHash(byte[] fileData) {
        return generateSHA256(fileData);
    }
}