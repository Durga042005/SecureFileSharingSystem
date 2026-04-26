# 🔐 Secure File Sharing System

## 📌 Project Overview
This project is a secure cloud-based file sharing system developed using Java. It ensures confidentiality, integrity, and access control of files using advanced security techniques.

---

## 🚀 Features
- 🔒 AES Encryption (Client-side encryption)
- 🔑 OTP-based Authentication (Two-factor authentication)
- 🛡 SHA-256 Integrity Verification
- ☁ Secure Cloud Storage (Encrypted files only)
- 👁 Zero-Knowledge Security Model
- 📜 Audit Logging (User activities tracking)

---

## 🏗 Technologies Used
- Java (Servlets & JSP)
- Apache Tomcat Server
- HTML, CSS
- MySQL Database

---

## 🔄 System Workflow
1. User Login with credentials + OTP
2. File is encrypted using AES before upload
3. SHA-256 hash is generated for integrity
4. Encrypted file is stored in cloud
5. During download:
   - File is decrypted
   - Integrity is verified using hash

---

## 🔐 Security Mechanisms
- AES Symmetric Encryption
- SHA-256 Hashing
- OTP Authentication
- Access Control
- Audit Logs

---

## ▶ How to Run
1. Install Apache Tomcat
2. Import project into IDE (Eclipse/IntelliJ)
3. Configure database (MySQL)
4. Deploy project on server
5. Run on browser: http://localhost:8080/

---

## 👩‍💻 Author
Durga 
