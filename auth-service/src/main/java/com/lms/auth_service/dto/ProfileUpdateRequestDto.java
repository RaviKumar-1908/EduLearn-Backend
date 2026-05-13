package com.lms.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequestDto {
    private String fullName;
    private Long mobile;
    private String bio;
    private String profilePicUrl;
    private String gender;
}
