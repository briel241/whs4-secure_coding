package com.tinyshop.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TransferConfirmForm {
    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    @Size(max = 128, message = "비밀번호 입력이 너무 깁니다.")
    private String currentPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}
