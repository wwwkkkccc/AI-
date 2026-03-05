package com.resumeai.dto;

public class AdminUserUpdateRequest {
    private Boolean vip;
    private Boolean blacklisted;

    public Boolean getVip() {
        return vip;
    }

    public void setVip(Boolean vip) {
        this.vip = vip;
    }

    public Boolean getBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted;
    }
}
