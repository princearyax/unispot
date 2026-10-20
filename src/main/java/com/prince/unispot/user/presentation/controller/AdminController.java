package com.prince.unispot.user.presentation.controller;

import com.prince.unispot.user.application.service.UserAdminService;
import com.prince.unispot.user.presentation.dto.UpdateUserRoleRequest;
import com.prince.unispot.user.presentation.dto.UserSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "4. Admin Management", description = "Privileged user role management (requires ROLE_ADMIN)")
public class AdminController {

    private final UserAdminService userAdminService;

    @PatchMapping("/{id}/role")
    @Operation(summary = "Promote or demote a user's role (ADMIN only)")
    public ResponseEntity<UserSummaryResponse> updateRole(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(userAdminService.updateRole(id, request.role()));
    }
}