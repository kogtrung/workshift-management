# Deploy bằng Docker image trên Render (monorepo)

Tài liệu này mô tả cách tạo 2 service trên Render cho:
- Backend: `workshift-backend`
- Frontend: `workshift-frontend`

Repo dùng GitHub Actions để build/push image lên Docker Hub và có thể kích hoạt deploy bằng *Render Deploy Hook*.

## 1) Chuẩn bị trước

1. Có sẵn MySQL bên ngoài Render (do bạn chọn “external” cho DB).
2. Chuẩn bị các secret backend:
   - `DB_URL`, `DB_USER`, `DB_PASS`
   - `JWT_SECRET`, `JWT_REFRESH_SECRET` (bắt buộc để app khởi động)
3. Xác định tên service bạn sẽ đặt trên Render để dùng cho CORS:
   - Frontend service (ví dụ: `workshift-frontend`)
   - Domain mẫu: `https://<frontend-service-name>.onrender.com`

## 2) Image cần dùng (Docker Hub)

Workflow GitHub Actions sẽ push 2 image tag `latest`:

- Backend: `<DOCKERHUB_USERNAME>/workshift-backend:latest`
- Frontend: `<DOCKERHUB_USERNAME>/workshift-frontend:latest`

Trong phần tạo service của Render, bạn chọn **Existing Image (Docker)** và nhập đúng URL image như trên.

## 3) Tạo Backend service trên Render

1. Render Dashboard -> `New` -> `Web Service`
2. Chọn source **Existing Image**
3. Nhập image:
   - `docker.io/<DOCKERHUB_USERNAME>/workshift-backend:latest`
4. Region: chọn cùng region với Frontend (để gọi qua private network nhanh hơn)
5. Vào `Advanced` để set:
   - **Environment variables**:
     - `SERVER_PORT=10000` (Render mặc định yêu cầu web service bind port `10000`)
     - `DB_URL=jdbc:mysql://<host>:3306/workshift_db?createDatabaseIfNotExist=true&useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh`
     - `DB_USER=<mysql_user>`
     - `DB_PASS=<mysql_password>`
     - `JWT_SECRET=<your_access_token_secret>`
     - `JWT_REFRESH_SECRET=<your_refresh_token_secret>`
     - `JWT_ISSUER=workshift-backend` (có thể để mặc định nếu không cần khác)
     - `CORS_ALLOWED_ORIGINS=https://<frontend-service-name>.onrender.com`
   - **Health check path**: `/actuator/health`

Sau khi create, Render sẽ pull image và deploy lần đầu.

## 4) Tạo Frontend service trên Render

1. Render Dashboard -> `New` -> `Web Service`
2. Chọn source **Existing Image**
3. Nhập image:
   - `docker.io/<DOCKERHUB_USERNAME>/workshift-frontend:latest`
4. Region: chọn cùng region với Backend
5. Vào `Advanced` để set:
   - **Health check path**: `/`

Frontend Docker image đã được cấu hình Nginx bind port `10000` để tương thích Render.

## 5) Lấy Deploy Hook để GitHub Actions kích hoạt deploy

Với mỗi service, vào `Settings` -> phần *Deploy* để lấy:
- Backend: `Deploy Hook URL`
- Frontend: `Deploy Hook URL`

Sau đó bạn có thể lưu 2 giá trị này vào GitHub repo secrets (tùy chọn):
- `RENDER_BACKEND_DEPLOY_HOOK_URL`
- `RENDER_FRONTEND_DEPLOY_HOOK_URL`

Nếu bạn chưa set 2 secret trên, workflow vẫn build/push image lên registry, nhưng sẽ không tự `curl` để redeploy (bạn deploy thủ công từ Render Dashboard).

## 6) Thiết lập `VITE_API_BASE_URL`

Frontend build-time phụ thuộc biến:
- `VITE_API_BASE_URL=https://<backend-service-name>.onrender.com/api/v1`

Bạn sẽ set giá trị này trong GitHub repo:
- GitHub `Variables` (khuyến nghị): `VITE_API_BASE_URL`

Khi workflow chạy, Docker build của frontend sẽ bake `VITE_API_BASE_URL` vào bundle.

