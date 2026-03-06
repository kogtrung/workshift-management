# BẢNG ĐẶC TẢ HOÀN CHỈNH (SPECIFICATION)

> **Hệ thống quản lý đăng ký & phân ca lao động thời vụ (Multi-group Workshift Management)**
>
> Phiên bản: 1.1 (Cập nhật chuẩn hóa Enterprise)
> Ngày cập nhật: 2026-02-24

---

## I. BẢNG NGHIỆP VỤ (BUSINESS REQUIREMENTS)

| ID | Nghiệp vụ | Role | Mô tả chi tiết (Business Logic) | Input/Output |
| :--- | :--- | :--- | :--- | :--- |
| **B01** | **Đăng ký tài khoản** | User | Tạo tài khoản mới. Email & Username phải duy nhất. Password phải được hash. | Input: Email, User, Pass<br>Output: User ID, Token |
| **B02** | **Đăng nhập** | User | Xác thực user. Trả về token (JWT/Session) kèm thông tin user cơ bản. | Input: Email/User, Pass<br>Output: Token |
| **B03** | **Tạo Group (Quán)** | Manager | User tạo group mới. Người tạo tự động trở thành MANAGER của group đó. | Input: Name, Description<br>Output: Group Info |
| **B04** | **Join Group** | User | User gửi yêu cầu tham gia vào một group đã biết (qua ID hoặc Code). Trạng thái: `PENDING`. | Input: Group ID<br>Output: Request Status |
| **B05** | **Duyệt thành viên** | Manager | Manager xem danh sách yêu cầu `PENDING`. Duyệt (`APPROVED`) hoặc từ chối (`REJECTED`). | Input: Member ID, Action<br>Output: Updated Status |
| **B06** | **Quản lý Vị trí** | Manager | Định nghĩa các vị trí làm việc trong quán (VD: Phục vụ, Pha chế, Bảo vệ). | Input: Name<br>Output: Position ID |
| **B07** | **Cấu hình Ca Mẫu (Shift Template)** | Manager | Tạo các khung giờ làm việc mẫu (VD: Ca Sáng 7-12h, Ca Chiều 12-17h). Giúp tạo lịch nhanh hơn. | Input: Name, Start, End<br>Output: Template ID |
| **B08** | **Khai báo Lịch rảnh** | Member | Member khai báo khung giờ rảnh theo thứ trong tuần (T2-CN). Hệ thống sẽ so khớp với giờ của Ca để gợi ý. | Input: Day, Start, End<br>Output: Availability ID |
| **B09** | **Tạo Ca làm việc** | Manager | Tạo ca làm việc cho một ngày cụ thể. Có thể chọn từ `ShiftTemplate` hoặc nhập thủ công. Hỗ trợ tạo hàng loạt cho cả tuần. | Input: Date, Template ID (opt), Start, End<br>Output: Shift ID |
| **B10** | **Cấu hình Nhu cầu** | Manager | Xác định mỗi ca cần bao nhiêu người cho từng vị trí (VD: 2 Phục vụ, 1 Pha chế). Có thể cấu hình mặc định theo Template. | Input: Shift, Position, Quantity<br>Output: Requirement ID |
| **B11** | **Xem Ca phù hợp** | Member | Hệ thống hiển thị các ca `OPEN` chưa đủ người, member có lịch rảnh bao trùm ca (`Avail.Start <= Shift.Start` && `Avail.End >= Shift.End`). | Input: Date Range<br>Output: List Shifts |
| **B12** | **Đăng ký Ca** | Member | Member đăng ký vào 1 vị trí trong ca. Tạo `Registration` (`PENDING`). | Input: Shift ID, Position ID<br>Output: Registration ID |
| **B13** | **Hủy Đăng ký** | Member | Member hủy đăng ký. Chỉ được hủy khi ca chưa `LOCKED` và chưa bắt đầu (theo quy định giờ). | Input: Reg ID, Reason<br>Output: Status CANCELLED |
| **B14** | **Duyệt Ca** | Manager | Manager duyệt đăng ký của member. Kiểm tra `ShiftRequirement` còn slot không. | Input: Reg ID<br>Output: Status APPROVED |
| **B15** | **Từ chối Ca** | Manager | Manager từ chối đăng ký (VD: member không phù hợp, ưu tiên người khác). | Input: Reg ID, Reason<br>Output: Status REJECTED |
| **B16** | **Gán nhân viên** | Manager | Manager bỏ qua quy trình đăng ký, gán trực tiếp member vào vị trí trong ca. | Input: User ID, Shift ID, Pos ID<br>Output: Reg APPROVED |
| **B17** | **Cảnh báo Thiếu người** | System | Tự động đánh dấu các ca sắp đến giờ làm mà chưa đủ số lượng `APPROVED` so với `Requirement`. | Output: Alert/Highlight UI |
| **B18** | **Gợi ý Nhân viên** | System | Tìm member có `Availability` bao trùm khung giờ ca và chưa có lịch trùng. | Output: List Suggested Users |
| **B19** | **Xem Lịch cá nhân** | Member | Xem danh sách các ca đã được `APPROVED` của bản thân theo tuần/tháng. | Output: Calendar View |
| **B20** | **Khóa Ca (Lock)** | System | Tự động hoặc Manager thủ công chuyển trạng thái ca sang `LOCKED` (không cho sửa đổi). | Input: Shift ID<br>Output: Status LOCKED |
| **B21** | **Yêu cầu Đổi ca** | Member | Member muốn đổi ca đã `APPROVED` sang một ca khác (hoặc chỉ xin hủy có lý do đặc biệt). | Input: From Shift, To Shift<br>Output: Request PENDING |
| **B22** | **Duyệt Đổi ca** | Manager | Manager xem xét yêu cầu đổi. Nếu duyệt: Hủy ca cũ, Đăng ký ca mới (nếu có). | Input: Request ID, Action<br>Output: Updated Regs |

| **B23** | **Quản lý Hệ thống (Admin)** | Admin | Quản trị toàn hệ thống: Xem danh sách User, Group; Khóa tài khoản/Group vi phạm. | Input: Action, Target ID<br>Output: Updated Status |
| **B24** | **Cấu hình Lương** | Manager | Thiết lập mức lương theo giờ cho từng Vị trí hoặc từng Nhân viên. | Input: Position/User, Rate<br>Output: Salary Config ID |
| **B25** | **Xem Bảng lương (Payroll)** | Manager | Xem thống kê tổng giờ làm và lương dự kiến của nhân viên theo tháng. | Input: Month, Year<br>Output: Payroll Report |
| **B26** | **Báo cáo Hoạt động (Performance Report)** | Manager | Thống kê số ca, tổng giờ làm theo tuần/tháng để so sánh hiệu suất. | Input: Date Range<br>Output: Chart/Table Data |

---

## II. MÔ HÌNH THỰC THỂ (DATABASE SCHEMA)

> **Quy ước chung**: Tất cả bảng đều có các trường Audit:
> - `created_at` (datetime, default NOW)
> - `updated_at` (datetime, on update NOW)

### 1. User (Người dùng hệ thống)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | ID định danh |
| `username` | String | Unique, Not Null | Tên đăng nhập |
| `email` | String | Unique, Not Null | Email liên hệ |
| `password` | String | Not Null | Mật khẩu (Hashed) |
| `full_name` | String | Not Null | Họ tên hiển thị |
| `phone` | String | Nullable | Số điện thoại |
| `status` | Enum | ACTIVE, BANNED | Trạng thái tài khoản |
| `global_role` | Enum | ADMIN, USER | Vai trò hệ thống (Admin quản trị web, User là người dùng) |

### 2. Group (Quán/Cửa hàng)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | ID nhóm |
| `name` | String | Not Null | Tên quán |
| `description` | String | Nullable | Mô tả thêm |
| `created_by` | User ID | FK | Người tạo (Owner) |
| `status` | Enum | ACTIVE, INACTIVE | Trạng thái hoạt động |

### 3. GroupMember (Thành viên nhóm)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `group_id` | Group ID | FK | Thuộc group nào |
| `user_id` | User ID | FK | User nào |
| `role` | Enum | MANAGER, MEMBER | Vai trò trong group này |
| `status` | Enum | PENDING, APPROVED, REJECTED, BANNED | Trạng thái tham gia |
| `joined_at` | DateTime | | Ngày gia nhập chính thức |

### 4. Position (Vị trí làm việc)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `group_id` | Group ID | FK | Vị trí thuộc quán nào |
| `name` | String | Not Null | Tên vị trí (Pha chế, Thu ngân...) |
| `color_code` | String | Nullable | Mã màu hiển thị trên lịch (VD: #FF0000) |

### 5. Availability (Lịch rảnh)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `user_id` | User ID | FK | Của user nào |
| `group_id` | Group ID | FK | Trong group nào |
| `day_of_week` | Enum/Int | 1-7 (Mon-Sun) | Thứ trong tuần |
| `start_time` | Time | Not Null | Giờ bắt đầu rảnh |
| `end_time` | Time | Not Null | Giờ kết thúc rảnh |

### 6. ShiftTemplate (Ca Mẫu) - Mới
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `group_id` | Group ID | FK | Thuộc group nào |
| `name` | String | Not Null | Tên ca mẫu (Sáng, Chiều...) |
| `start_time` | Time | Not Null | Giờ bắt đầu chuẩn |
| `end_time` | Time | Not Null | Giờ kết thúc chuẩn |
| `description` | String | Nullable | Mô tả |

### 7. Shift (Ca làm việc)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `group_id` | Group ID | FK | Ca của quán nào |
| `template_id` | Template ID | Nullable FK | Link đến template (nếu có) |
| `name` | String | Nullable | Tên ca (Sáng, Chiều, Tối, Ca gãy...) |
| `date` | Date | Not Null | Ngày làm việc |
| `start_time` | Time | Not Null | Giờ bắt đầu |
| `end_time` | Time | Not Null | Giờ kết thúc |
| `status` | Enum | OPEN, LOCKED, COMPLETED | Trạng thái ca |
| `note` | String | Nullable | Ghi chú cho nhân viên (VD: "Đông khách, cần tập trung") |

### 8. ShiftRequirement (Nhu cầu nhân sự)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `shift_id` | Shift ID | FK | Thuộc ca nào |
| `position_id` | Position ID | FK | Cần vị trí nào |
| `quantity` | Int | Min 1 | Số lượng cần thiết |

### 9. Registration (Đăng ký ca) ⭐
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `shift_id` | Shift ID | FK | Đăng ký ca nào |
| `user_id` | User ID | FK | Ai đăng ký |
| `position_id` | Position ID | FK | Đăng ký vị trí nào |
| `status` | Enum | PENDING, APPROVED, REJECTED, CANCELLED | Trạng thái đăng ký |
| `note` | String | Nullable | Ghi chú của member khi đăng ký |
| `manager_note`| String | Nullable | Ghi chú của manager khi duyệt/từ chối |

### 10. ShiftChangeRequest (Yêu cầu đổi ca)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `user_id` | User ID | FK | Người yêu cầu |
| `group_id` | Group ID | FK | Trong group nào |
| `from_shift_id`| Shift ID | FK | Ca hiện tại (muốn bỏ) |
| `to_shift_id` | Shift ID | Nullable FK | Ca mong muốn (muốn vào) - Null nếu chỉ xin nghỉ |
| `reason` | String | Nullable | Lý do đổi |
| `status` | Enum | PENDING, APPROVED, REJECTED | Trạng thái yêu cầu |
| `manager_note`| String | Nullable | Phản hồi của quản lý |

### 11. SalaryConfig (Cấu hình Lương)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID/Long | PK | |
| `group_id` | Group ID | FK | |
| `position_id` | Position ID | Nullable FK | Lương theo vị trí (VD: Phục vụ 20k/h) |
| `user_id` | User ID | Nullable FK | Lương riêng cho nhân viên (VD: A làm tốt 25k/h) |
| `hourly_rate` | Decimal | Min 0 | Mức lương theo giờ |
| `effective_date`| Date | Not Null | Ngày bắt đầu áp dụng |

---

## III. LUỒNG NGƯỜI DÙNG (USER FLOW)

### 1. Luồng Nhân viên (MEMBER)
1.  **Đăng nhập** -> Dashboard (Hiện danh sách Group).
2.  **Chọn Group** -> Trang chủ Group.
3.  **Khai báo Lịch rảnh**: Vào menu "Lịch rảnh" -> Chọn thứ/giờ -> Lưu.
4.  **Đăng ký Ca**:
    *   Vào menu "Đăng ký ca" (Lịch tuần).
    *   Thấy các ô ca màu Xanh (Còn trống) / Xám (Đã đủ/Khóa).
    *   Click vào ca -> Chọn vị trí -> Bấm "Đăng ký".
    *   Trạng thái chuyển sang "Chờ duyệt" (Vàng).
5.  **Xem Lịch làm**:
    *   Vào menu "Lịch của tôi".
    *   Thấy các ca đã "Duyệt" (Xanh lá).
    *   Nếu bận: Click vào ca -> Chọn "Xin đổi/Hủy" -> Nhập lý do -> Gửi.

### 2. Luồng Quản lý (MANAGER)
1.  **Đăng nhập** -> Dashboard -> Chọn Group quản lý.
2.  **Cấu hình** (Lần đầu): Tạo Vị trí (Position).
3.  **Lên lịch**:
    *   Vào "Quản lý ca".
    *   Tạo ca mới (Ngày, Giờ).
    *   Thêm nhu cầu (Cần 2 Pha chế, 3 Phục vụ).
4.  **Duyệt Ca**:
    *   Trên lịch tuần, thấy ô ca có icon báo hiệu "Có đăng ký mới".
    *   Click vào ca -> Xem danh sách đăng ký.
    *   Bấm "Duyệt" hoặc "Từ chối".
5.  **Phân ca chủ động**:
    *   Nếu thiếu người: Click vào nút "Gợi ý".
    *   Hệ thống list ra nhân viên rảnh khung giờ đó.
    *   Chọn nhân viên -> Gán vào ca.
6.  **Xử lý Đổi ca**:
    *   Vào menu "Yêu cầu đổi ca".
    *   Xem lý do -> Duyệt (Hệ thống tự động cập nhật lại lịch) hoặc Từ chối.

---

## IV. NGUYÊN TẮC KỸ THUẬT & RÀNG BUỘC (CONSTRAINTS)

1.  **Unique Constraint**:
    *   Một nhân viên không thể có 2 `Registration` trạng thái `APPROVED` trùng hoặc giao nhau về thời gian (trong cùng 1 Group hoặc khác Group - *Tùy chọn nâng cao, ở đây scope MVP chỉ check trong cùng Group*).
    *   `GroupMember`: Cặp `(user_id, group_id)` phải duy nhất.
2.  **Data Integrity**:
    *   Số lượng `Registration (APPROVED)` của một `Position` trong `Shift` không được vượt quá `ShiftRequirement.quantity` (trừ khi Manager force assign).
3.  **Business Rules**:
    *   Không thể thao tác (Đăng ký/Hủy/Duyệt) trên `Shift` có status `LOCKED` hoặc `COMPLETED`.
    *   Manager chỉ được duyệt nhân viên thuộc Group mình quản lý.
    *   Dữ liệu `Availability`, `Shift`, `Registration` phải luôn gắn với `group_id` để đảm bảo tính cách ly dữ liệu (Multi-tenancy logic).

---
*Tài liệu này dùng làm căn cứ chính xác nhất để phát triển Database và API.*
