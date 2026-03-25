package com.workshift.backend.shiftchange;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftChangeRequest;
import com.workshift.backend.domain.ShiftChangeRequestStatus;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shiftchange.dto.CreateChangeRequestDto;
import com.workshift.backend.shiftchange.dto.ShiftChangeRequestResponse;

@Service
public class ShiftChangeService {

    private final ShiftChangeRequestRepository changeRequestRepository;
    private final RegistrationRepository registrationRepository;
    private final ShiftRepository shiftRepository;
    private final PositionRepository positionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public ShiftChangeService(
            ShiftChangeRequestRepository changeRequestRepository,
            RegistrationRepository registrationRepository,
            ShiftRepository shiftRepository,
            PositionRepository positionRepository,
            GroupMemberRepository groupMemberRepository,
            GroupRepository groupRepository,
            UserRepository userRepository) {
        this.changeRequestRepository = changeRequestRepository;
        this.registrationRepository = registrationRepository;
        this.shiftRepository = shiftRepository;
        this.positionRepository = positionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    // =====================================================================
    // B21: Tạo yêu cầu đổi ca
    // =====================================================================

    @Transactional
    public ShiftChangeRequestResponse createChangeRequest(Long groupId, String username, CreateChangeRequestDto dto) {

        // 1. Xác thực user và là MEMBER APPROVED trong group
        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, requester.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

        if (membership.getStatus() != GroupMemberStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản của bạn chưa được duyệt vào nhóm");
        }

        // 2. Load registration nguồn – phải APPROVED và thuộc requester
        Registration fromReg = registrationRepository.findById(dto.fromRegistrationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký ca gốc"));

        if (!fromReg.getUser().getId().equals(requester.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Đăng ký ca này không thuộc về bạn");
        }
        if (fromReg.getStatus() != RegistrationStatus.APPROVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể đổi ca đã được duyệt (APPROVED)");
        }
        if (!fromReg.getShift().getGroup().getId().equals(groupId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Đăng ký ca gốc không thuộc nhóm này");
        }

        // 3. Load ca đích – phải OPEN
        Shift toShift = shiftRepository.findByIdAndGroupId(dto.toShiftId(), groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca đổi sang"));

        if (toShift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ca đích phải ở trạng thái OPEN");
        }

        // 4. Không thể đổi sang chính ca mà mình đang đăng ký
        if (toShift.getId().equals(fromReg.getShift().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ca đích phải khác ca hiện tại");
        }

        // 5. Kiểm tra member chưa có registration trên ca đích
        if (registrationRepository.existsByShiftAndUser(toShift, requester)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Bạn đã có đăng ký trên ca đích này");
        }

        // 6. Load vị trí đích – phải thuộc group
        Position toPosition = positionRepository.findByIdAndGroupId(dto.toPositionId(), groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí trong nhóm này"));

        // 7. Tạo yêu cầu đổi ca
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setFromRegistration(fromReg);
        req.setToShift(toShift);
        req.setToPosition(toPosition);
        req.setRequester(requester);
        req.setStatus(ShiftChangeRequestStatus.PENDING);
        req.setReason(dto.reason());

        req = changeRequestRepository.save(req);
        return toResponse(req);
    }

    // =====================================================================
    // B22: Duyệt đổi ca (atomic: cancel cũ + tạo mới)
    // =====================================================================

    @Transactional
    public ShiftChangeRequestResponse approveChangeRequest(Long groupId, Long requestId, String username) {

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, manager.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

        if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền duyệt yêu cầu đổi ca");
        }

        ShiftChangeRequest req = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu đổi ca"));

        // Phải thuộc group này
        if (!req.getToShift().getGroup().getId().equals(groupId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Yêu cầu đổi ca không thuộc nhóm này");
        }

        if (req.getStatus() != ShiftChangeRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể duyệt yêu cầu đang ở trạng thái PENDING");
        }

        // Ca đích vẫn phải OPEN
        Shift toShift = req.getToShift();
        if (toShift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ca đích đã không còn ở trạng thái OPEN, không thể duyệt");
        }

        // Kiểm tra member chưa có registration trên ca đích (race condition guard)
        if (registrationRepository.existsByShiftAndUser(toShift, req.getRequester())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Nhân viên đã có đăng ký trên ca đích, không thể duyệt");
        }

        // === Atomic operation ===
        // 1. Cancel registration cũ
        Registration oldReg = req.getFromRegistration();
        oldReg.setStatus(RegistrationStatus.CANCELLED);
        oldReg.setManagerNote("Đã đổi ca – duyệt bởi quản lý");
        registrationRepository.save(oldReg);

        // 2. Tạo Registration APPROVED mới trên ca đích
        Registration newReg = new Registration();
        newReg.setShift(toShift);
        newReg.setUser(req.getRequester());
        newReg.setPosition(req.getToPosition());
        newReg.setStatus(RegistrationStatus.APPROVED);
        newReg.setManagerNote("Đổi ca từ #" + oldReg.getShift().getId());
        registrationRepository.save(newReg);

        // 3. Cập nhật request APPROVED
        req.setStatus(ShiftChangeRequestStatus.APPROVED);
        req = changeRequestRepository.save(req);

        return toResponse(req);
    }

    // =====================================================================
    // B22: Từ chối đổi ca
    // =====================================================================

    @Transactional
    public ShiftChangeRequestResponse rejectChangeRequest(Long groupId, Long requestId, String username, String reason) {

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, manager.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

        if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền từ chối yêu cầu đổi ca");
        }

        ShiftChangeRequest req = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu đổi ca"));

        if (!req.getToShift().getGroup().getId().equals(groupId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Yêu cầu đổi ca không thuộc nhóm này");
        }

        if (req.getStatus() != ShiftChangeRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể từ chối yêu cầu đang ở trạng thái PENDING");
        }

        req.setStatus(ShiftChangeRequestStatus.REJECTED);
        req.setManagerNote(reason);
        req = changeRequestRepository.save(req);

        return toResponse(req);
    }

    // =====================================================================
    // Xem danh sách PENDING (Manager)
    // =====================================================================

    @Transactional(readOnly = true)
    public List<ShiftChangeRequestResponse> listPendingRequests(Long groupId, String username) {

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, manager.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

        if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xem yêu cầu đổi ca");
        }

        return changeRequestRepository
                .findByToShift_Group_IdAndStatus(groupId, ShiftChangeRequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================================
    // Mapping
    // =====================================================================

    private ShiftChangeRequestResponse toResponse(ShiftChangeRequest req) {
        return new ShiftChangeRequestResponse(
                req.getId(),
                req.getFromRegistration().getId(),
                req.getFromRegistration().getShift().getId(),
                req.getToShift().getId(),
                req.getToPosition().getId(),
                req.getToPosition().getName(),
                req.getRequester().getUsername(),
                req.getRequester().getFullName(),
                req.getStatus(),
                req.getReason(),
                req.getManagerNote()
        );
    }
}
