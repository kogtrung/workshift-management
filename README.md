# 🗓️ Workshift Management System

> Hệ thống quản lý phân ca & đăng ký lịch làm việc đa nhóm (Multi-Group Workshift Management)

![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen)
![Backend](https://img.shields.io/badge/Backend-Java%2017%20%7C%20Spring%20Boot%204.x-green)
![Frontend](https://img.shields.io/badge/Frontend-React%2019%20%7C%20Vite%207-blue)
![DB](https://img.shields.io/badge/DB-MySQL-orange)
![Auth](https://img.shields.io/badge/Auth-JWT-yellow)

## ✨ Tổng quan

Workshift Management System hỗ trợ các cửa hàng/chuỗi cửa hàng (F&B) quản lý lịch làm việc cho nhân viên part-time theo mô hình **multi-group**.

- Một user có thể tham gia nhiều group (quán) với vai trò khác nhau
- Dữ liệu được cách ly theo `group_id` (multi-tenancy theo logic)
- Manager quản lý ca/nhu cầu/duyệt đăng ký; Member đăng ký ca và khai báo lịch rảnh

## 🧭 Mục lục

- [Tính năng](#-tính-năng)
- [Kiến trúc](#-kiến-trúc)
- [Cấu trúc repo](#-cấu-trúc-repo)
- [Quick Start](#-quick-start)
- [Biến môi trường](#-biến-môi-trường)
- [API nhanh](#-api-nhanh)
- [Test](#-test)
- [Git workflow](#-git-workflow)
- [Troubleshooting](#-troubleshooting)
- [Tài liệu](#-tài-liệu)
- [License](#-license)

## 🚀 Tính năng

### 👔 Manager
- Quản lý group (quán)
- Cấu hình vị trí (Position), ca mẫu (ShiftTemplate)
- Tạo ca, cấu hình nhu cầu nhân sự (ShiftRequirement)
- Duyệt/từ chối/gán nhân viên
- Báo cáo hoạt động theo tuần/tháng

### 👷 Member
- Khai báo lịch rảnh (Availability)
- Xem ca phù hợp, đăng ký/hủy đăng ký
- Xem lịch cá nhân theo tuần/tháng
- Yêu cầu đổi ca

## 🏗️ Kiến trúc

### 📦 Feature-based (chuẩn cho teamwork)

Backend tổ chức theo **feature-based**: mỗi nghiệp vụ gom trong 1 module (Auth/Group/Shift...).

- Dễ đọc: mở module là thấy controller/service/dto liên quan
- Dễ mở rộng: thêm module mới không làm rối cấu trúc chung
- Giảm conflict khi nhiều người cùng phát triển

### 🛠 Công nghệ

**Backend** (`workshift-backend`)
- Java 17, Spring Boot 4.x
- Spring Data JPA + MySQL
- Spring Security (JWT, stateless)
- Validation (jakarta validation)
- Health/metrics: Spring Boot Actuator (`/actuator/health`)

**Frontend** (`workshift-frontend`)
- React 19 + Vite 7
- `fetch` wrapper cho API, base url qua `VITE_API_BASE_URL`

## 📁 Cấu trúc repo

```
workshift-management/
├── workshift-backend/
│   ├── src/main/java/com/workshift/backend/
│   │   ├── auth/                # Feature: Auth (controller/service/dto/jwt)
│   │   │   ├── dto/
│   │   │   └── jwt/
│   │   ├── common/              # Shared: api response, exception, base entity
│   │   ├── config/              # Security, beans...
│   │   ├── domain/              # Core entities (User...)
│   │   └── repository/          # Core repositories (UserRepository...)
│   └── src/main/resources/
│       └── application.yaml
│
├── workshift-frontend/
│   ├── src/
│   │   └── api/                 # apiClient (`apiFetch`)
│   └── package.json
│
├── tasks.md
├── spec.md
└── README.md
```

## ⚡ Quick Start

### ✅ Yêu cầu
- Java 17+
- Node.js 18+
- MySQL 8+

### 1) Backend

```bash
cd workshift-backend
```

Tạo `.env` tại `workshift-backend/.env`:

```properties
DB_URL=jdbc:mysql://localhost:3306/workshift_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASS=
SERVER_PORT=8080

JWT_SECRET=change-me
JWT_ISSUER=workshift-backend
JWT_EXPIRES_IN_SECONDS=86400
```

Chạy backend:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Health check:
- `GET http://localhost:8080/actuator/health`

### 2) Frontend

```bash
cd workshift-frontend
npm install
```

Tạo `.env` từ mẫu và trỏ về backend:

```bash
copy .env.example .env
```

Trong `workshift-frontend/.env`:

```properties
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Chạy frontend:

```bash
npm run dev
```

## 🔧 Biến môi trường

### Backend (`workshift-backend/.env`)
- `DB_URL`, `DB_USER`, `DB_PASS`
- `SERVER_PORT`
- `JWT_SECRET` (bắt buộc đổi khi deploy)
- `JWT_ISSUER`, `JWT_EXPIRES_IN_SECONDS`

### Frontend (`workshift-frontend/.env`)
- `VITE_API_BASE_URL`

## 🔌 API nhanh

### Auth

- `POST /api/v1/auth/register`

```json
{
  "username": "user1",
  "email": "user1@example.com",
  "password": "secret123",
  "fullName": "User One",
  "phone": "0123456789"
}
```

- `POST /api/v1/auth/login`

```json
{
  "usernameOrEmail": "user1@example.com",
  "password": "secret123"
}
```

Khi gọi API cần auth:
- Header: `Authorization: Bearer <token>`

### Health
- `GET /actuator/health`

## 🧪 Test

Backend:

```bash
cd workshift-backend
./mvnw test
```

Ghi chú:
- Test profile dùng H2 in-memory để chạy nhanh và không phụ thuộc MySQL

## 🌿 Git workflow

- Nhánh chính: `develop`
- Làm feature: `feature/<tên-tính-năng>`
- MR vào `develop`, bật review
- Nên tick `Delete source branch` sau khi merge
- Commit message: `feat:`, `fix:`, `test:`, `chore:`

## 🧯 Troubleshooting

- Không gọi được API:
  - Kiểm tra backend đã chạy và đúng port (`SERVER_PORT`)
  - Kiểm tra `VITE_API_BASE_URL` trên frontend
- Login không ra token:
  - JSON field phải là `usernameOrEmail` và `password`
  - User phải tồn tại trong DB (đăng ký trước)
- DB connection failed:
  - Kiểm tra `DB_URL/DB_USER/DB_PASS` trong `.env`
  - Đảm bảo MySQL đang chạy

## 📚 Tài liệu

- `tasks.md`: phân chia nhiệm vụ theo thành viên
- `spec.md`: đặc tả nghiệp vụ & schema

## 📄 License

Dự án nội bộ. Không phân phối lại khi chưa được phép.
