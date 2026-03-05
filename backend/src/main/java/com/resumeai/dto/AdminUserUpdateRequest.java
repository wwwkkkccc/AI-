package com.resumeai.dto;

/**
 * 管理员更新用户信息请求 DTO，用于管理员修改用户 VIP 状态或拉黑状态的接口请求体
 */
public class AdminUserUpdateRequest {
    /** 是否设为 VIP 用户 */
    private Boolean vip;
    /** 是否拉黑该用户 */
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
