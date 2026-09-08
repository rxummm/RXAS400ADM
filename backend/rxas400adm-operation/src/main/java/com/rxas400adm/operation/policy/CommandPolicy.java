package com.rxas400adm.operation.policy;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CommandPolicy {

    private static final Set<String> BLOCKED_COMMANDS = Set.of(
        "DLTLIB", "DLTF", "DLTUSRPRF", "RGZOBJPFR", "ENDSBS *IMMED",
        "CHGSYSVAL", "STRSBS", "PWRDWNSYS"
    );

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?i)(CALL\\s+QCMDEXC|SBMJOB|CHGJOB|SNDPGMMSG|SNDUSRMSG|RTVJOBA)"
    );

    public void validateClCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "CL command cannot be empty");
        }

        String upperCmd = command.trim().toUpperCase();

        for (String blocked : BLOCKED_COMMANDS) {
            if (upperCmd.startsWith(blocked)) {
                throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN,
                    "Blocked CL command: " + blocked);
            }
        }

        if (DANGEROUS_PATTERN.matcher(upperCmd).find()) {
            throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN,
                "Dangerous CL command pattern detected");
        }
    }
}