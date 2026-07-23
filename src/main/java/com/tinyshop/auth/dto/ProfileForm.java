package com.tinyshop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다.")
    private String nickname;

    public ProfileForm() {
    }

    public ProfileForm(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
