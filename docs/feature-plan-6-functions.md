## Kế hoạch 6 chức năng nâng cao (Workshift Management)

Tài liệu này mô tả chi tiết kế hoạch logic/back-end cho 6 chức năng:

1. Phân ca tự động (auto-assign)
2. Đổi ca giữa 2 người (swap)
3. Xác thực thông tin cá nhân (ảnh + CCCD qua API ngoài)
4. Cấu hình hệ số lương (ngày lễ, thưởng, tăng ca)
5. Khóa tất cả ca tuần + gửi mail lịch cá nhân
6. Xác nhận thanh toán lương theo tuần bằng OTP + xuất file

Toàn bộ thiết kế bám sát kiến trúc hiện tại:

- Spring Boot 4, Java 17, JWT stateless
- Layers: Controller → Service (@Transactional) → Repository → Entity/DTO
- Phân quyền bằng `GroupMember.role` (MANAGER/MEMBER) + `GlobalRole` (ADMIN)

---

## 1. Phân ca tự động (auto-assign công bằng theo tuần)

### Mục tiêu

- Tự gán nhân viên vào các `ShiftRequirement` (position + quantity) còn thiếu slot.
- Công bằng theo:
  - **Tuần hiện tại**: ai ít ca hơn (theo position) thì được ưu tiên trước.
  - **Tuần trước**: nếu tuần hiện tại bằng nhau, tuần trước ai ít ca hơn được ưu tiên.
- Chỉ gán cho người:
  - Thuộc group, đã APPROVED.
  - Có `MemberPosition` đúng vị trí.
  - Rảnh theo `Availability`.
  - Chưa có registration APPROVED trùng ca.

### API đề xuất

- `POST /api/v1/groups/{groupId}/shifts/auto-assign`
  - Auth: MANAGER của group.
  - Body ví dụ:
    ```json
    {
      "from": "2026-04-06",
      "to": "2026-04-12",
      "shiftIds": [123, 124],
      "positionIds": [10, 11],
      "strategy": "FAIR_WEEKLY"
    }
    ```

### Dữ liệu & Repository

- Dùng lại entity hiện có:
  - `Shift`, `ShiftRequirement`, `Registration`, `MemberPosition`, `Availability`.
- Mở rộng `RegistrationRepository` (nếu cần) cho count per user/per position:
  - `countApprovedByUserAndPositionAndShiftDateBetween(...)`
  - (hoặc dùng query hiện có + groupBy trong service, chấp nhận overhead khi dữ liệu chưa lớn).

### Thuật toán (Service)

1. Xác thực MANAGER trong group.
2. Lấy danh sách `Shift` trong `[from, to]` (hoặc giới hạn theo `shiftIds`).
3. Với mỗi shift:
   - Lấy `ShiftRequirement` (mỗi requirement là (position, quantity)).
4. Với mỗi `(shift, requirement.position)`:
   - Lấy candidate:
     - MEMBER APPROVED của group.
     - Có `MemberPosition` tương ứng position.
     - Rảnh theo `Availability`.
     - Không có `Registration` APPROVED trên shift này.
   - Tính thống kê:
     - `currentPosCount(u)` = số `Registration.APPROVED` của user đó trong **tuần hiện tại** với cùng `position`.
     - `prevPosCount(u)` = số `Registration.APPROVED` trong **tuần trước** với cùng `position`.
   - Sắp xếp candidate theo:
     - `currentPosCount ASC`,
     - rồi `prevPosCount ASC`,
     - rồi `userId ASC` (tie-break).
   - Duyệt list đã sort:
     - Trước khi gán, kiểm quota:
       - `approvedCount = countByShiftIdAndPositionIdAndStatus(shiftId, positionId, APPROVED)`
       - `remaining = requirement.quantity - approvedCount`
       - Nếu `remaining <= 0` thì dừng.
     - Tạo `Registration` mới:
       - `shift`, `user`, `position`, `status = APPROVED`, `managerNote = "AUTO_ASSIGNED"`.
     - Cập nhật `currentPosCount(u)` trong bộ nhớ để các lần gán sau vẫn công bằng.

### Ràng buộc logic

- Không bao giờ vượt quá `ShiftRequirement.quantity`.
- Không tạo `Registration` trùng `(shift, user)` (đã có check `existsByShiftAndUser`).
- Chạy trong `@Transactional` để bảo đảm atomic cho từng batch auto-assign.

---

## 2. Đổi ca giữa 2 người (swap A ↔ B, cùng vị trí, không qua manager)

### Mục tiêu

- A muốn đổi ca với B (ca A và ca B).
- Điều kiện:
  - Cả hai đang có `Registration.APPROVED`.
  - `positionId` của A và B trong 2 ca này **giống nhau**.
  - Ca ở trạng thái cho phép (khuyến nghị: `ShiftStatus.OPEN`).
- Quy trình:
  - A tạo request.
  - B xác nhận (confirm) là đủ.
  - Sau khi confirm, hệ thống hoán đổi registration một cách atomic.

### Mô hình dữ liệu

- Entity mới `ShiftSwapRequest` (gợi ý):
  - `id`
  - `fromRegistration` (Registration của A trên ca A).
  - `toRegistration` (Registration của B trên ca B).
  - `requester` (User A).
  - `targetUser` (User B).
  - `status`: `PENDING_TARGET_CONFIRM`, `CONFIRMED_EXECUTED`, `REJECTED`.
  - `reason?`, `createdAt`, `updatedAt` (kế thừa `BaseEntity`).

### API

- `POST /api/v1/groups/{groupId}/shift-swaps`
  - Auth: A (member).
  - Body: `{ "fromRegistrationId": Long, "toRegistrationId": Long, "reason": "..." }`.
- `PATCH /api/v1/groups/{groupId}/shift-swaps/{id}/confirm`
  - Auth: B (targetUser).
- `PATCH /api/v1/groups/{groupId}/shift-swaps/{id}/reject`
  - Auth: B.

### Logic confirm (atomic)

1. Xác thực:
   - User gọi API là `targetUser` của request.
2. Load `ShiftSwapRequest` và 2 `Registration` liên quan.
3. Kiểm tra:
   - Cả hai `Registration.status == APPROVED`.
   - `fromRegistration.position.id == toRegistration.position.id`.
   - Cả hai `shift.status` là `OPEN` (hoặc rule bạn chấp nhận).
   - A chưa có registration trên ca B, B chưa có registration trên ca A:
     - dùng `registrationRepository.existsByShiftAndUser`.
4. Transaction:
   - Đặt `status` của 2 registration cũ = `CANCELLED`, thêm `managerNote`/`note` mô tả đổi ca.
   - Tạo 2 registration mới:
     - A → `toShift`, giữ cùng `position`.
     - B → `fromShift`, giữ cùng `position`.
     - Trạng thái `APPROVED`.
   - Cập nhật `ShiftSwapRequest.status = CONFIRMED_EXECUTED`.

---

## 3. Xác thực thông tin cá nhân (ảnh + CCCD qua API ngoài)

### Mục tiêu

- Lưu thông tin cá nhân nhân viên:
  - Ảnh chân dung.
  - Ảnh CCCD (căn cước công dân).
- Gọi API ngoài (image verification/OCR) để xác nhận **đây là CCCD thật**, không phải ảnh giả.
- Chỉ nhân viên đã VERIFIED mới được:
  - Đăng ký ca (`registerShift`).
  - Được manager gán vào ca (`assignShift`).

### Mô hình dữ liệu

- Mở rộng `User` hoặc tạo entity `IdentityVerification`:
  - `userId`
  - `status`: `NOT_VERIFIED | PENDING | VERIFIED | REJECTED`
  - `idNumber?`, `fullName?`
  - `portraitImageUrl`, `idCardImageUrl`
  - `providerName`, `providerResultRaw?`
  - `verifiedAt`, `lastAttemptAt`, `rejectReason?`

### API

- `POST /api/v1/me/identity-verification`
  - multipart:
    - `portraitImage`
    - `idCardImage`
    - `idNumber?` (nếu muốn check khớp với OCR)
- `GET /api/v1/me/identity-verification/status`

### Logic

1. Nhận multipart, upload ảnh lên storage (hoặc giữ local path).
2. Gọi API ngoài (HTTP client) theo spec provider:
   - Gửi ảnh CCCD, nhận kết quả `isValidImage`, `docType`, `extractedIdNumber`, `extractedName`, `confidence`, ...
3. Quy tắc xác thực:
   - Nếu provider trả `isValidImage == true` và (nếu có `idNumber` nhập tay) thì phải khớp với `extractedIdNumber`.
   - Nếu đạt điều kiện: đặt `status = VERIFIED`, lưu `verifiedAt`.
   - Nếu không: đặt `status = REJECTED`, lưu rejectReason, cho phép user thử lại sau.
4. Enforcement:
   - Trong `RegistrationService.registerShift` & `RegistrationService.assignShift`, trước khi tạo registration:
     - Load user; nếu `idVerificationStatus != VERIFIED` → ném `BusinessException` với HTTP 403/400 (thông báo “Tài khoản chưa xác minh CCCD”).

---

## 4. Cấu hình hệ số lương (ngày lễ, thưởng, tăng ca theo giờ làm thêm ngoài đăng ký)

### Mục tiêu

- Cho phép admin/manager cấu hình:
  - Hệ số lương tăng ca (overtimeMultiplier).
  - Hệ số lương ngày lễ (holidayMultiplier).
  - Hệ số/tham số lương thưởng dịp đặc biệt.
  - Khung giờ làm chuẩn trong ngày.
- Hệ thống Payroll tự động áp dụng các hệ số này khi tính lương kỳ sau.

### Giải thích tăng ca (theo yêu cầu)

- “Tăng ca sẽ tính khi có ca làm thêm vào khoảng giờ **ngoài giờ đăng ký**”:
  - Đặt `normalWorkStart` / `normalWorkEnd` (ví dụ 08:00–17:00).
  - Mỗi ca:
    - `normalHours` = phần thời gian nằm trong khung chuẩn.
    - `overtimeHours` = phần nằm ngoài khung chuẩn.
  - Overtime được tính bằng `overtimeHours * hourlyRate * overtimeMultiplier`.

### Mô hình đề xuất

- Entity `CompensationPolicy` (per group):
  - `groupId`
  - `effectiveDate`
  - `normalWorkStart`, `normalWorkEnd`
  - `overtimeMultiplier`
  - `holidayMultiplier`
  - `bonusFlat?`, `bonusMultiplier?`
- Entity `GroupHoliday`:
  - `groupId`
  - `date`
  - `type`: `HOLIDAY | SPECIAL_BONUS | ...`

### API

- `PUT /api/v1/groups/{groupId}/compensation-policy` (MANAGER).
- `POST /api/v1/groups/{groupId}/holidays` (MANAGER).
- `GET /api/v1/groups/{groupId}/compensation-policy` (xem lại).

### Tích hợp vào `PayrollService.getPayroll`

1. Lấy policy & holiday trong khoảng [from, to] của kỳ lương.
2. Với mỗi `Registration` APPROVED:
   - Lấy `shift.date`, `startTime`, `endTime`.
   - Tách:
     - `normalHours` (trong `[normalWorkStart, normalWorkEnd]`).
     - `overtimeHours` (ngoài khoảng này).
   - Check:
     - `isHoliday` nếu `shift.date` ∈ holiday.
     - `isSpecialBonus` nếu có config tương ứng.
3. Tính:
   - `basePay = normalHours * hourlyRate`.
   - `otPay = overtimeHours * hourlyRate * overtimeMultiplier`.
   - Nếu holiday: nhân thêm hoặc cộng thêm phần `holidayBonus`.
   - Nếu special bonus: cộng `bonusFlat` hoặc `bonusMultiplier * basePay`.
4. Mở rộng `PayrollResponse.PayrollEntry`:
   - `normalHours`, `overtimeHours`, `holidayHours?`, `holidayBonus`, `specialBonus`, `totalPay`.

---

## 5. Khóa tất cả ca tuần + gửi mail “lịch cá nhân” về Gmail

### Mục tiêu

- Tại trang quản lý, manager có nút:
  - **“Khóa tất cả ca tuần này”**.
- Sau khi khóa:
  - Các ca trong tuần đó không cho đăng ký/đổi/assign nữa.
  - Hệ thống gửi email lịch làm việc tuần cho từng nhân viên (lịch cá nhân).

### API

- `POST /api/v1/groups/{groupId}/weeks/{year}/{week}/lock-and-send-schedule`
  - Auth: MANAGER.

### Logic

1. Tính `weekStart` / `weekEnd` theo ISO week (dùng logic tương tự `ActivityReportService.getWeeklyReport`).
2. Transaction phần **khóa ca**:
   - Tìm tất cả `Shift` thuộc group và có `date` ∈ [weekStart, weekEnd].
   - Với mỗi shift `status == OPEN`:
     - Set `status = LOCKED`.
     - REJECT mọi `Registration` còn `PENDING` (giống logic `ShiftService.lockShift` hiện có).
3. Sau khi transaction commit:
   - Lấy toàn bộ `Registration.APPROVED` trong tuần đó (theo `RegistrationRepository.findApprovedByGroupAndDateRange`).
   - Group theo `userId` để tạo lịch cá nhân (có thể reuse model `MyCalendarItemResponse` từ `MeService`).
   - Dùng `User.email` để gửi mail:
     - tiêu đề: “Lịch làm việc tuần {weekStart} - {weekEnd}”.
     - nội dung: danh sách ca (ngày, giờ, vị trí, group).
4. Idempotency:
   - Tạo entity `WeeklyScheduleEmailSent(groupId, userId, year, week)`:
     - Nếu record đã tồn tại thì không gửi lại (hoặc chỉ gửi lại khi `force=true`).

### Yêu cầu thêm

- Thêm `spring-boot-starter-mail` + cấu hình SMTP bằng env (không hardcode).

---

## 6. Thanh toán lương theo tuần bằng OTP + xuất file “đã thanh toán”

### Mục tiêu

- Tại trang “Tổng quan” (GroupHomePage) manager bấm **“Xác nhận thanh toán lương theo tuần”**.
- Flow:
  1. Manager yêu cầu OTP.
  2. Nhập OTP để xác nhận.
  3. Hệ thống đánh dấu tuần đó “đã thanh toán” + tạo file thanh toán (txt/csv).

### Mô hình dữ liệu

- Entity `WeeklyPayment`:
  - `groupId`
  - `year`, `week`
  - `status`: `PENDING_OTP`, `PAID`
  - `otpHash`, `otpExpiresAt`, `otpAttempts`
  - `paidAt`
  - `paidByUserId`
  - snapshot tổng số tiền / số người để file không thay đổi nếu payroll sau này đổi.

### API

- `POST /api/v1/groups/{groupId}/weekly-payments/{year}/{week}/otp/request`
  - Auth: MANAGER.
  - Kết quả:
    - Tạo hoặc cập nhật `WeeklyPayment` cho tuần đó.
    - Gen OTP (6 số), lưu `otpHash`, `otpExpiresAt`, `otpAttempts=0`.
    - Trả OTP về response để hiển thị cho manager trên UI (không gửi qua email).
- `POST /api/v1/groups/{groupId}/weekly-payments/{year}/{week}/otp/confirm`
  - Body: `{ "otp": "123456" }`.
  - Nếu OTP đúng + chưa hết hạn:
    - set `status=PAID`, `paidAt`, `paidByUserId`.
- `GET /api/v1/groups/{groupId}/weekly-payments/{year}/{week}/receipt`
  - Trả file `text/csv` chứa thông tin thanh toán tuần (user, tổng giờ, tổng tiền, ...).

### Logic confirm OTP

1. Load `WeeklyPayment` theo `(groupId, year, week)`.
2. Nếu `status == PAID` → trả lỗi `409` (đã thanh toán).
3. Check:
   - `now <= otpExpiresAt`.
   - `hash(inputOtp) == otpHash`.
   - `otpAttempts` chưa vượt limit (nếu có).
4. Nếu pass:
   - set `status = PAID`, `paidAt = now`, `paidByUserId` = manager hiện tại.
   - Lưu snapshot bảng lương tuần (nếu chưa có) để file export luôn ổn định.

### Xuất file thanh toán

- Dùng `PayrollService` để tính lương **tuần** (có thể reuse logic monthly, chỉ đổi range):
  - Bao gồm cả hệ số ngày lễ/tăng ca/thưởng ở chức năng 4.
- Tạo file `text/csv` trong memory:
  - Cột: userId, fullName, totalShifts, totalHours, overtimeHours, holidayBonus, specialBonus, totalPay.
  - Trả về dưới dạng `ResponseEntity<byte[]>` với header:
    - `Content-Type: text/csv`
    - `Content-Disposition: attachment; filename="weekly-payment-{groupId}-{year}-W{week}.csv"`

---

## Lộ trình triển khai gợi ý

1. **Nền tảng & dữ liệu mới**
   - Thêm entity: `CompensationPolicy`, `GroupHoliday`, `ShiftSwapRequest`, `WeeklyPayment`, (nếu cần) `IdentityVerification`, `WeeklyScheduleEmailSent`.
   - Thêm repository + migration SQL (nếu dùng flyway/liquibase).

2. **Core shift logic**
   - Implement auto-assign (chức năng 1).
   - Implement swap A ↔ B (chức năng 2).

3. **Identity & enforcement**
   - Thêm API xác thực CCCD (chức năng 3).
   - Chặn đăng ký/gán ca cho user chưa VERIFIED.

4. **Compensation & payroll**
   - Thêm config hệ số lương/ngày lễ/thưởng (chức năng 4).
   - Mở rộng `PayrollService` để áp dụng các hệ số.

5. **Khóa tuần & thông báo**
   - Thêm API khóa tất cả ca tuần + gửi mail lịch cá nhân (chức năng 5).

6. **Thanh toán tuần & OTP**
   - Thêm flow OTP + mark PAID + export file thanh toán tuần (chức năng 6).

