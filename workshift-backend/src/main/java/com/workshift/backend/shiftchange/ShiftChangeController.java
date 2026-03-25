package com.workshift.backend.shiftchange;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.shiftchange.dto.CreateChangeRequestDto;
import com.workshift.backend.shiftchange.dto.RejectChangeRequestDto;
import com.workshift.backend.shiftchange.dto.ShiftChangeRequestResponse;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/shift-change-requests")
public class ShiftChangeController {

    private final ShiftChangeService shiftChangeService;

    public ShiftChangeController(ShiftChangeService shiftChangeService) {
        this.shiftChangeService = shiftChangeService;
    }

    /**
     * B21: Member tạo yêu cầu đổi ca.
     * POST /api/v1/groups/{groupId}/shift-change-requests
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ShiftChangeRequestResponse>> createChangeRequest(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @RequestBody CreateChangeRequestDto dto
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        ShiftChangeRequestResponse data = shiftChangeService
                .createChangeRequest(groupId, authentication.getName(), dto);
        return ResponseEntity.status(201).body(ApiResponse.created("Yêu cầu đổi ca đã được gửi", data));
    }

    /**
     * Manager xem danh sách PENDING.
     * GET /api/v1/groups/{groupId}/shift-change-requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ShiftChangeRequestResponse>>> listPending(
            Authentication authentication,
            @PathVariable("groupId") Long groupId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        List<ShiftChangeRequestResponse> data = shiftChangeService
                .listPendingRequests(groupId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Danh sách yêu cầu đổi ca đang chờ duyệt", data));
    }

    /**
     * B22: Manager duyệt yêu cầu đổi ca.
     * PATCH /api/v1/groups/{groupId}/shift-change-requests/{requestId}/approve
     */
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<ShiftChangeRequestResponse>> approveChangeRequest(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("requestId") Long requestId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        ShiftChangeRequestResponse data = shiftChangeService
                .approveChangeRequest(groupId, requestId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Duyệt đổi ca thành công", data));
    }

    /**
     * B22: Manager từ chối yêu cầu đổi ca.
     * PATCH /api/v1/groups/{groupId}/shift-change-requests/{requestId}/reject
     */
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<ShiftChangeRequestResponse>> rejectChangeRequest(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("requestId") Long requestId,
            @RequestBody(required = false) RejectChangeRequestDto dto
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        String reason = (dto != null) ? dto.reason() : null;
        ShiftChangeRequestResponse data = shiftChangeService
                .rejectChangeRequest(groupId, requestId, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Từ chối đổi ca thành công", data));
    }
}
