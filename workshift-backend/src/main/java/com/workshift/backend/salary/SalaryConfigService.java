package com.workshift.backend.salary;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupStatus;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.SalaryConfig;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.salary.dto.SalaryConfigRequest;
import com.workshift.backend.salary.dto.SalaryConfigResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SalaryConfigService {

    private final SalaryConfigRepository salaryConfigRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;

    public SalaryConfigService(
            SalaryConfigRepository salaryConfigRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            PositionRepository positionRepository,
            UserRepository userRepository) {
        this.salaryConfigRepository = salaryConfigRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
    }

    private void validateManager(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc nhóm này"));

        if (!membership.getRole().name().equals("MANAGER")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Thao tác yêu cầu quyền Quản lý (MANAGER)");
        }
    }

    public SalaryConfigResponse createSalaryConfig(Long groupId, String username, SalaryConfigRequest request) {
        validateManager(groupId, username);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        if (request.getPositionId() == null && request.getUserId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Phải cung cấp Vị trí hoặc Nhân viên để thiết lập lương");
        }
        if (request.getPositionId() != null && request.getUserId() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ được chọn 1 trong 2: Vị trí HOẶC Nhân viên");
        }

        Position position = null;
        if (request.getPositionId() != null) {
            position = positionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí"));
            if (!position.getGroup().getId().equals(groupId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Vị trí không thuộc nhóm này");
            }
        }

        User targetUser = null;
        if (request.getUserId() != null) {
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
            groupMemberRepository.findByGroupIdAndUserId(group.getId(), targetUser.getId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Nhân viên không thuộc nhóm này"));
        }

        SalaryConfig config = new SalaryConfig();
        config.setGroup(group);
        config.setPosition(position);
        config.setUser(targetUser);
        config.setHourlyRate(request.getHourlyRate());
        config.setEffectiveDate(request.getEffectiveDate());

        config = salaryConfigRepository.save(config);
        
        return mapToResponse(config);
    }

    public List<SalaryConfigResponse> getConfigs(Long groupId, String username) {
        validateManager(groupId, username);
        List<SalaryConfig> configs = salaryConfigRepository.findByGroupIdOrderByEffectiveDateDesc(groupId);
        return configs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public void deleteConfig(Long groupId, Long configId, String username) {
        validateManager(groupId, username);
        SalaryConfig config = salaryConfigRepository.findById(configId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình lương"));
        
        if (!config.getGroup().getId().equals(groupId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Cấu hình này không thuộc nhóm đang xét");
        }
        
        salaryConfigRepository.delete(config);
    }

    private SalaryConfigResponse mapToResponse(SalaryConfig entity) {
        return new SalaryConfigResponse(
                entity.getId(),
                entity.getGroup().getId(),
                entity.getPosition() != null ? entity.getPosition().getId() : null,
                entity.getPosition() != null ? entity.getPosition().getName() : null,
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getUser() != null ? entity.getUser().getFullName() : null,
                entity.getHourlyRate(),
                entity.getEffectiveDate()
        );
    }
}
