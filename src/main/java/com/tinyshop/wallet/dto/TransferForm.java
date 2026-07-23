package com.tinyshop.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TransferForm {
    @NotBlank(message = "받는 사람의 닉네임을 입력해 주세요.")
    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
    private String recipientNickname;
    @Min(value = 1, message = "최소 1포인트를 입력해 주세요.")
    @Max(value = 1000000, message = "1회 최대 송금액은 1,000,000포인트입니다.")
    private long amount;
    @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
    private String memo;

    public String getRecipientNickname() { return recipientNickname; }
    public void setRecipientNickname(String recipientNickname) { this.recipientNickname = recipientNickname; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
