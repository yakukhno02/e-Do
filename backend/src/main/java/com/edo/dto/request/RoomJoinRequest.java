package com.edo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RoomJoinRequest {

    @NotBlank(message = "Room code is required")
    @Size(min = 6, max = 6, message = "Room code must be exactly 6 characters")
    @Pattern(regexp = "^[A-HJ-NP-Za-hj-np-z2-9]{6}$", message = "Room code contains invalid characters")
    private String code;

    public RoomJoinRequest() {
    }

    public RoomJoinRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
