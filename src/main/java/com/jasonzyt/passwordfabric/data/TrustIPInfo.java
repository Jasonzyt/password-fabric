package com.jasonzyt.passwordfabric.data;

public record TrustIPInfo(String address, Long expireTime) {

    public String getAddress() {
        return address;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public boolean isExpired() {
        return expireTime < System.currentTimeMillis();
    }

}
