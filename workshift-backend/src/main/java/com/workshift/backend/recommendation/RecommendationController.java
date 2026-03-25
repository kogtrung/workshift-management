package com.workshift.backend.recommendation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.recommendation.dto.CandidateResponse;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/shifts/{shiftId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * GET /api/v1/groups/{groupId}/shifts/{shiftId}/recommendations?positionId={positionId}
     * Trả danh sách nhân viên phù hợp (Rảnh + Đúng vị trí + Chưa có lịch).
     * Chỉ MANAGER của group được phép gọi.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidateResponse>>> recommend(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("shiftId") Long shiftId,
            @RequestParam("positionId") Long positionId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        List<CandidateResponse> candidates = recommendationService
                .recommend(groupId, shiftId, positionId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok("Danh sách nhân viên được gợi ý", candidates));
    }
}
