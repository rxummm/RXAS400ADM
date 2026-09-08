package com.rxas400adm.operation.policy;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RiskPolicy {

    public void checkPermission(String requiredPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not logged in");
        }
        boolean hasPermission = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals(requiredPermission) || a.equals("ROLE_ADMIN"));
        if (!hasPermission) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Insufficient permissions: " + requiredPermission);
        }
    }

    public void checkRiskLevel(RiskLevel riskLevel, Operation op) {
        switch (riskLevel) {
            case CRITICAL -> checkPermission("USER_CRITICAL_AUTHORITY");
            case BREAK_GLASS -> checkPermission("BREAK_GLASS_EXECUTE");
            default -> { }
        }
    }
}