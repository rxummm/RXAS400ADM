package com.rxas400adm.compile.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.compile.dto.CompileRequest;
import com.rxas400adm.compile.entity.CompileRecord;
import com.rxas400adm.compile.mapper.CompileRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CompileService implements ICompileService {

    private final AS400ClientProvider clientProvider;
    private final CompileRecordMapper recordMapper;

    // P3：权限校验收敛到 CompileController（@PreAuthorize COMPILE_EXECUTE），服务层不再重复注解
    public CompileRecord compile(CompileRequest request) {
        String command = buildCommand(request);
        CommandResult result = clientProvider.current().execute(command);

        CompileRecord record = new CompileRecord();
        record.setLibrary(request.getLibrary());
        record.setSourceFile(request.getSourceFile());
        record.setMember(request.getMember());
        record.setCommand(request.getCommand());
        record.setStatus(result.success() ? "SUCCESS" : "FAILED");
        record.setMessage(result.message());
        record.setOperator(currentUsername());
        record.setCreatedTime(LocalDateTime.now());
        recordMapper.insert(record);
        return record;
    }

    public List<CompileRecord> history() {
        return recordMapper.selectList(null);
    }

    /** IBM i 标识符白名单（库/源文件/成员）：字母数字与 $#@_ 下划线，禁止空格/引号/分号等注入字符（P1-6） */
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Z0-9_$#@]+$");

    /** 允许的编译命令（P1-6）：仅 CRT* 编译类命令，杜绝任意 CL 拼接 */
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "CRTBNDRPG", "CRTSQLRPGI", "CRTCBLMOD", "CRTPGM", "CRTBNDCL", "CRTBNDCBL");

    /** 组装 CL 编译命令，如 CRTBNDRPG PGM(APP/ORDERMAINT) SRCFILE(APP/QRPGLESRC) */
    private String buildCommand(CompileRequest request) {
        String command = request.getCommand() == null || request.getCommand().isBlank()
                ? "CRTBNDRPG" : request.getCommand().trim().toUpperCase();
        if (!ALLOWED_COMMANDS.contains(command)) {
            throw new BusinessException(ErrorCode.COMPILE_UNSUPPORTED, "不支持的编译命令: " + request.getCommand());
        }
        String library = requireIdentifier(request.getLibrary(), "库名");
        String sourceFile = requireIdentifier(request.getSourceFile(), "源文件");
        String member = requireIdentifier(request.getMember(), "成员名");
        return command + " PGM(" + library + "/" + member
                + ") SRCFILE(" + library + "/" + sourceFile + ")";
    }

    private String requireIdentifier(String value, String label) {
        String v = value == null ? "" : value.trim().toUpperCase();
        if (!IDENTIFIER.matcher(v).matches()) {
            throw new BusinessException(label + "只能是字母/数字/下划线等合法标识符: " + value);
        }
        return v;
    }

    private String currentUsername() {
        return com.rxas400adm.common.util.SecurityUtils.currentUsername();
    }
}