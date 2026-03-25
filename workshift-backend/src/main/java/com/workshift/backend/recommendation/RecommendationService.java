package com.workshift.backend.recommendation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Availability;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.MemberPosition;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.User;
import com.workshift.backend.recommendation.dto.CandidateResponse;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.AvailabilityRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.MemberPositionRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class RecommendationService {

    private final ShiftRepository shiftRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final AvailabilityRepository availabilityRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;

    public RecommendationService(
            ShiftRepository shiftRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            MemberPositionRepository memberPositionRepository,
            AvailabilityRepository availabilityRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            PositionRepository positionRepository) {
        this.shiftRepository = shiftRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.memberPositionRepository = memberPositionRepository;
        this.availabilityRepository = availabilityRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> recommend(Long groupId, Long shiftId, Long positionId, String username) {

        // 1. Xác thực user
        User caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        // 2. Kiểm tra group tồn tại
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        // 3. Kiểm tra quyền MANAGER
        GroupMember callerMember = groupMemberRepository.findByGroupIdAndUserId(groupId, caller.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

        if (callerMember.getRole() != GroupRole.MANAGER || callerMember.getStatus() != GroupMemberStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xem gợi ý nhân viên");
        }

        // 4. Load shift
        Shift shift = shiftRepository.findByIdAndGroupId(shiftId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

        // 5. Kiểm tra position tồn tại trong group
        positionRepository.findByIdAndGroupId(positionId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí trong nhóm này"));

        LocalDate shiftDate = shift.getDate();
        DayOfWeek shiftDay = shiftDate.getDayOfWeek();

        // 6. Lấy tất cả MEMBER đã APPROVED trong group
        List<GroupMember> approvedMembers = groupMemberRepository
                .findAllByGroupIdAndStatus(groupId, GroupMemberStatus.APPROVED)
                .stream()
                .filter(gm -> gm.getRole() == GroupRole.MEMBER)
                .toList();

        if (approvedMembers.isEmpty()) {
            return List.of();
        }

        // --- Tiêu chí 2: Đúng vị trí ---
        // Lấy danh sách groupMemberId có positionId tương ứng
        Set<Long> memberIdsWithPosition = approvedMembers.stream()
                .filter(gm -> memberPositionRepository.existsByGroupMemberIdAndPositionId(gm.getId(), positionId))
                .map(gm -> gm.getUser().getId())
                .collect(Collectors.toSet());

        if (memberIdsWithPosition.isEmpty()) {
            return List.of();
        }

        // --- Tiêu chí 3: Chưa có lịch APPROVED cùng ngày ---
        Set<Long> busyUserIds = Set.copyOf(
                registrationRepository.findApprovedUserIdsByGroupAndDate(groupId, shiftDate));

        // Lọc ra các userId still eligible
        List<Long> eligibleUserIds = memberIdsWithPosition.stream()
                .filter(uid -> !busyUserIds.contains(uid))
                .toList();

        if (eligibleUserIds.isEmpty()) {
            return List.of();
        }

        // --- Tiêu chí 1: Rảnh (Availability overlap với giờ ca) ---
        List<Availability> availabilities = availabilityRepository
                .findByUserIdInAndDayOfWeek(eligibleUserIds, shiftDay);

        // Group availability theo userId
        Map<Long, List<Availability>> availByUser = availabilities.stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()));

        // Load user info bằng cách lấy từ approvedMembers
        Map<Long, User> userMap = approvedMembers.stream()
                .collect(Collectors.toMap(gm -> gm.getUser().getId(), GroupMember::getUser));

        List<CandidateResponse> result = new ArrayList<>();

        for (Long uid : eligibleUserIds) {
            List<Availability> userAvail = availByUser.getOrDefault(uid, List.of());

            boolean isFree = userAvail.stream().anyMatch(a ->
                    // Availability phải cover hoàn toàn hoặc giao với giờ ca:
                    // a.start <= shift.end AND a.end >= shift.start
                    !a.getStartTime().isAfter(shift.getEndTime())
                    && !a.getEndTime().isBefore(shift.getStartTime())
            );

            if (isFree) {
                User u = userMap.get(uid);
                if (u != null) {
                    result.add(new CandidateResponse(u.getId(), u.getFullName(), u.getUsername()));
                }
            }
        }

        return result;
    }
}
