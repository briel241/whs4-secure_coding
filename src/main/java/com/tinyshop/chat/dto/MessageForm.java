package com.tinyshop.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageForm {
    @NotBlank(message = "메시지를 입력해 주세요.")
    @Size(min = 1, max = 1000, message = "메시지는 1~1,000자여야 합니다.")
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
