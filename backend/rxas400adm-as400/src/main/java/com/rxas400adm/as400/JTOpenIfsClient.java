package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileOutputStream;
import com.rxas400adm.as400.model.IfsEntry;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JTOpen IfsClient 委托实现（IFS 目录 / 文件读写 / 回收站）。
 * 8 个方法的 connect→try/catch/invalidate/log 样板收敛为 {@link #ifsCall} 模板（中-11），
 * 各方法只保留业务主体与兜底值。
 */
@Slf4j
class JTOpenIfsClient implements IfsClient {

    private final JTOpenConnectionState state;

    JTOpenIfsClient(JTOpenConnectionState state) {
        this.state = state;
    }

    /** IFS 业务主体：允许抛出 JT400 受检异常（IOException/AS400SecurityException 等），由模板统一兜底 */
    @FunctionalInterface
    private interface IfsAction<T> {
        T run(AS400 system) throws Exception;
    }

    /**
     * IFS 调用模板：connect → 业务执行；异常时 invalidate 连接、记脱敏告警日志并返回 fallback。
     *
     * @param op       操作名（仅用于日志，如「目录读取」）
     * @param path     目标路径（日志用）
     * @param fallback 失败兜底返回值
     * @param action   以已连接 AS400 为入参的业务主体
     */
    private <T> T ifsCall(String op, String path, T fallback, IfsAction<T> action) {
        AS400 system = state.connect();
        try {
            return action.run(system);
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS {}失败(host={}, path={}): {}", op, state.host, path, state.redact(e.getMessage()));
            return fallback;
        }
    }

    @Override
    public List<IfsEntry> listIfsDir(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return ifsCall("目录读取", path, List.of(), system -> {
            IFSFile dir = new IFSFile(system, path);
            IFSFile[] children = dir.listFiles();
            List<IfsEntry> rows = new ArrayList<>();
            if (children == null) {
                return rows;
            }
            for (IFSFile child : children) {
                rows.add(new IfsEntry(child.getName(),
                        child.isDirectory() ? "DIR" : "FILE",
                        child.length(),
                        String.valueOf(child.lastModified()),
                        child.getAbsolutePath()));
            }
            return rows;
        });
    }

    @Override
    public String readIfsFile(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return ifsCall("文件读取", path, "", system -> {
            StringBuilder content = new StringBuilder();
            try (IFSFileInputStream in = new IFSFileInputStream(system, path);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }
            return content.toString();
        });
    }

    @Override
    public boolean writeIfsFile(String path, String content) {
        return writeIfsFileBytes(path, (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean writeIfsFileBytes(String path, byte[] content) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return ifsCall("文件写入", path, false, system -> {
            IFSFile file = new IFSFile(system, path);
            IFSFile parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (IFSFileOutputStream out = new IFSFileOutputStream(file)) {
                out.write(content == null ? new byte[0] : content);
            }
            return true;
        });
    }

    @Override
    public boolean mkdirIfs(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return ifsCall("目录创建", path, false, system -> {
            IFSFile dir = new IFSFile(system, path);
            if (!dir.exists()) {
                return dir.mkdirs();
            }
            return dir.isDirectory();
        });
    }

    @Override
    public String trashIfsFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return ifsCall("移入回收站", path, null, system -> {
            IFSFile file = new IFSFile(system, path);
            if (!file.exists()) {
                return null;
            }
            String trashPath = IfsClient.TRASH_ROOT + path;
            IFSFile target = new IFSFile(system, trashPath);
            IFSFile parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!file.renameTo(target)) {
                log.warn("IFS 移入回收站失败(host={}, path={})", state.host, path);
                return null;
            }
            return trashPath;
        });
    }

    @Override
    public boolean restoreIfsFile(String trashPath) {
        if (trashPath == null || trashPath.isBlank()) {
            return false;
        }
        return ifsCall("恢复", trashPath, false, system -> {
            IFSFile file = new IFSFile(system, trashPath);
            if (!file.exists()) {
                return false;
            }
            String original = trashPath.substring(IfsClient.TRASH_ROOT.length());
            IFSFile target = new IFSFile(system, original);
            IFSFile parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            return file.renameTo(target);
        });
    }

    @Override
    public byte[] readIfsFileBytes(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return ifsCall("文件读取", path, null, system -> {
            try (IFSFileInputStream in = new IFSFileInputStream(system, path)) {
                return in.readAllBytes();
            }
        });
    }

    @Override
    public InputStream readIfsFileStream(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return ifsCall("文件打开", path, null, system -> new IFSFileInputStream(system, path));
    }
}
