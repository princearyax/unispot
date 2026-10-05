package com.prince.unispot.user.presentation.controller;

import com.prince.unispot.user.application.service.UserAdminService;
import com.prince.unispot.user.presentation.dto.UpdateUserRoleRequest;
import com.prince.unispot.user.presentation.dto.UserSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserAdminService userAdminService;

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserSummaryResponse> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(userAdminService.updateRole(id, request.role()));
    }
}