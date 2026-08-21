package com.rxas400adm.as400;

import com.rxas400adm.as400.model.UserProfileRow;

/**
 * Mock AuthClient 委托实现（认证仿真 / 用户 profile）。
 */
class MockAuthClient implements AuthClient {

    MockAuthClient(MockState state) {
    }

    @Override
    public boolean authenticate(String username, String password) {
        return username != null && !username.isBlank();
    }

    @Override
    public UserProfileRow userProfile(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String upper = username.trim().toUpperCase();
        if (upper.startsWith("GHOST")) {
            return null;
        }
        String group = switch (upper) {
            case "ADMIN", "QSECOFR" -> "GRPADM";
            case "DEVELOPER" -> "GRPDEV";
            case "OPERATOR" -> "GRPOPR";
            default -> "GRPDEV";
        };
        return new UserProfileRow(upper, group, "*ENABLED");
    }
}