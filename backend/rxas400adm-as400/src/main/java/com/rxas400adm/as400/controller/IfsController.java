package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.IfsClient;
import com.rxas400adm.as400.dto.IfsWriteDTO;
import com.rxas400adm.as400.model.IfsEntry;
import com.rxas400adm.as400.service.IIfsService;
import com.rxas400adm.as400.vo.IfsContentVO;
import com.rxas400adm.as400.vo.IfsDeleteVO;
import com.rxas400adm.as400.vo.IfsPathVO;
import com.rxas400adm.as400.vo.IfsUploadVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * IFS 文件浏览（2.3.5）+ 文件管理（V32）：上传/下载/新建目录/删除。
 * 浏览/查看用 IFS_VIEW；写操作用 IFS_MANAGE（按钮级授权）。
 */
@RestController
@RequestMapping("/api/v1/ifs")
@RequiredArgsConstructor
@Tag(name = "IFS文件管理")
public class IfsController {

    /** 上传大小上限（与 application.yml spring.servlet.multipart.max-file-size 对齐，100MB） */
    private static final long MAX_UPLOAD_BYTES = 100L * 1024 * 1024;

    /**
     * S1：允许访问的 IFS 根目录白名单（逗号分隔）。默认限定平台工作目录 /QOpenSys/rxas400，
     * 防止任意路径读写连接账号可达的系统文件（/etc、/QOpenSys/usr/bin 等）。
     * 回收站 TRASH_ROOT 位于该目录之下，无需单独配置。
     */
    @Value("${rxas400.ifs.allowed-roots:/QOpenSys/rxas400}")
    private String allowedRoots;

    private final IIfsService ifsService;

    @GetMapping
    @PreAuthorize("hasAuthority('IFS_VIEW')")
    public ApiResponse<List<IfsEntry>> list(@RequestParam(defaultValue = "/QOpenSys/rxas400") String path) {
        String normalized = normalize(path);
        return ApiResponse.success(ifsService.list(normalized));
    }

    @GetMapping("/content")
    @PreAuthorize("hasAuthority('IFS_VIEW')")
    public ApiResponse<IfsContentVO> content(@RequestParam String path) {
        String normalized = normalize(path);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 路径不能为空");
        }
        return ApiResponse.success(new IfsContentVO(normalized, ifsService.read(normalized)));
    }

    /**
     * 写入 IFS 文本文件（文档管理「上传到 IFS」）：
     * 用 DOC_MANAGE 门控（文档管理按钮级权限，管理员可控）。
     */
    @PostMapping("/write")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "上传文档到 IFS")
    public ApiResponse<IfsPathVO> write(@Valid @RequestBody IfsWriteDTO dto) {
        String path = normalize(dto.getPath());
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 目标路径不能为空");
        }
        if (!ifsService.write(path, dto.getContent())) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS 文件写入失败: " + path);
        }
        return ApiResponse.success(new IfsPathVO(path));
    }

    /**
     * 上传任意文件到 IFS（IFS 页文件管理，IFS_MANAGE）：
     * multipart: file + path（目标完整路径）。
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('IFS_MANAGE')")
    @OperateLog(module = "IFS 文件", operation = "上传文件到 IFS")
    public ApiResponse<IfsUploadVO> upload(@RequestPart("file") MultipartFile file,
                                            @RequestParam String path) throws IOException {
        String normalized = normalize(path);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 目标路径不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件超出大小限制（100MB）");
        }
        if (!ifsService.writeBytes(normalized, file.getBytes())) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS 文件上传失败: " + normalized);
        }
        return ApiResponse.success(new IfsUploadVO(normalized, file.getSize()));
    }

    /** 下载 IFS 文件（IFS_MANAGE） */
    @GetMapping("/download")
    @PreAuthorize("hasAuthority('IFS_MANAGE')")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String path) {
        String normalized = normalize(path);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 路径不能为空");
        }
        // P0-3：流式下载，避免大文件整读内存；流由 Spring 在写完后关闭
        InputStream in = ifsService.readStream(normalized);
        if (in == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IFS 文件不存在或不可读: " + normalized);
        }
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        StreamingResponseBody body = outputStream -> {
            try (InputStream source = in) {
                source.transferTo(outputStream);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    /** 新建目录（IFS_MANAGE） */
    @PostMapping("/mkdir")
    @PreAuthorize("hasAuthority('IFS_MANAGE')")
    @OperateLog(module = "IFS 文件", operation = "新建 IFS 目录")
    public ApiResponse<IfsPathVO> mkdir(@Valid @RequestBody IfsWriteDTO dto) {
        String path = normalize(dto.getPath());
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 目录路径不能为空");
        }
        if (!ifsService.mkdir(path)) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS 目录创建失败: " + path);
        }
        return ApiResponse.success(new IfsPathVO(path));
    }

    /**
     * 逻辑删除 IFS 文件/目录（IFS_MANAGE）：移入回收站 .trash（保留原路径结构，可 restore）。
     * 文档删除时前端联动调用本接口，rx_doc 记录走 DB 逻辑删除。
     */
    @DeleteMapping("/file")
    @PreAuthorize("hasAuthority('IFS_MANAGE')")
    @OperateLog(module = "IFS 文件", operation = "删除 IFS 文件/目录（移入回收站）")
    public ApiResponse<IfsDeleteVO> delete(@RequestParam String path) {
        String normalized = normalize(path);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 路径不能为空");
        }
        String trashPath = ifsService.trash(normalized);
        if (trashPath == null) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS 移入回收站失败: " + normalized);
        }
        return ApiResponse.success(new IfsDeleteVO(normalized, trashPath));
    }

    /** 从回收站恢复 IFS 文件/目录（IFS_MANAGE）：移回原路径 */
    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('IFS_MANAGE')")
    @OperateLog(module = "IFS 文件", operation = "恢复 IFS 文件/目录")
    public ApiResponse<IfsDeleteVO> restore(@RequestParam String trashPath) {
        String normalized = normalize(trashPath);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "回收站路径不能为空");
        }
        // S2：仅允许恢复回收站内的路径——防止 substring(36) 构成任意文件「搬运」原语
        if (!normalized.startsWith(IfsClient.TRASH_ROOT + "/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "仅允许恢复回收站内路径（" + IfsClient.TRASH_ROOT + "/*）: " + normalized);
        }
        if (!ifsService.restore(normalized)) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS 恢复失败: " + normalized);
        }
        String original = normalized.startsWith(IfsClient.TRASH_ROOT)
                ? normalized.substring(IfsClient.TRASH_ROOT.length()) : normalized;
        return ApiResponse.success(new IfsDeleteVO(original, normalized));
    }

    /**
     * IFS 沙箱（P2-8）：统一归一化并校验路径——
     * ① 仅接受绝对路径（以 / 开头）；② 拒绝 .. 路径穿越与反斜杠混淆（先转 / 再校验）；
     * ③ 拒绝以 . 开头的隐藏段；④ 去除尾部多余斜杠。
     * 非法路径抛 BAD_REQUEST（不再直达 IFS 客户端，防止越权读写连接账号可达的任意路径）。
     */
    private String normalize(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        if (!normalized.startsWith("/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 路径必须是绝对路径（以 / 开头）: " + path);
        }
        // 逐段校验：拒绝 .. 与隐藏段（.. 可穿越到连接账号可达的任意目录，. 开头的系统文件应显式避开）
        for (String segment : normalized.split("/")) {
            if (segment.equals("..")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 路径不允许包含 .. : " + path);
            }
            if (segment.startsWith(".") && !segment.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS 路径不允许访问隐藏路径段: " + path);
            }
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // S1：根目录白名单——必须落在某个允许根之内（等于根或其子路径）
        // normalized 在上方 while 中被重赋值（非 effectively final），lambda 捕获需拷贝
        final String candidate = normalized;
        final String rootsConfig = allowedRoots;
        boolean allowed = Arrays.stream(rootsConfig.split(","))
                .map(String::trim)
                .filter(root -> !root.isEmpty())
                .anyMatch(root -> candidate.equals(root) || candidate.startsWith(root + "/"));
        if (!allowed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "IFS 路径不在允许范围内（rxas400.ifs.allowed-roots）: " + path);
        }
        return normalized;
    }
}
