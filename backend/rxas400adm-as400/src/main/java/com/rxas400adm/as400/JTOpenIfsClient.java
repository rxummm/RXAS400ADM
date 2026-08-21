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
 */
@Slf4j
class JTOpenIfsClient implements IfsClient {

    private final JTOpenConnectionState state;

    JTOpenIfsClient(JTOpenConnectionState state) {
        this.state = state;
    }

    @Override
    public List<IfsEntry> listIfsDir(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        AS400 system = state.connect();
        try {
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
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 目录读取失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return List.of();
        }
    }

    @Override
    public String readIfsFile(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        AS400 system = state.connect();
        StringBuilder content = new StringBuilder();
        try (IFSFileInputStream in = new IFSFileInputStream(system, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            return content.toString();
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 文件读取失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return "";
        }
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
        AS400 system = state.connect();
        try {
            IFSFile file = new IFSFile(system, path);
            IFSFile parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (IFSFileOutputStream out = new IFSFileOutputStream(file)) {
                out.write(content == null ? new byte[0] : content);
            }
            return true;
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 文件写入失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean mkdirIfs(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        AS400 system = state.connect();
        try {
            IFSFile dir = new IFSFile(system, path);
            if (!dir.exists()) {
                return dir.mkdirs();
            }
            return dir.isDirectory();
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 目录创建失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return false;
        }
    }

    @Override
    public String trashIfsFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        AS400 system = state.connect();
        try {
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
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 移入回收站失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return null;
        }
    }

    @Override
    public boolean restoreIfsFile(String trashPath) {
        if (trashPath == null || trashPath.isBlank()) {
            return false;
        }
        AS400 system = state.connect();
        try {
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
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 恢复失败(host={}, path={}): {}", state.host, trashPath, state.redact(e.getMessage()));
            return false;
        }
    }

    @Override
    public byte[] readIfsFileBytes(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        AS400 system = state.connect();
        try (IFSFileInputStream in = new IFSFileInputStream(system, path)) {
            return in.readAllBytes();
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 文件读取失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return null;
        }
    }

    @Override
    public InputStream readIfsFileStream(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        AS400 system = state.connect();
        try {
            return new IFSFileInputStream(system, path);
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IFS 文件打开失败(host={}, path={}): {}", state.host, path, state.redact(e.getMessage()));
            return null;
        }
    }
}