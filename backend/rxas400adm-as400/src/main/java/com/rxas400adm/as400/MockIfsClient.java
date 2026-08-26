package com.rxas400adm.as400;

import com.rxas400adm.as400.model.IfsEntry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Mock IfsClient 委托实现（IFS 目录 / 文件读写 / 回收站 / 恢复）。
 */
class MockIfsClient implements IfsClient {

    private final MockState state;

    MockIfsClient(MockState state) {
        this.state = state;
    }

    /** 路径规范化：反斜杠统一为正斜杠，null 视为空串（中-12：11 处重复 replace 收敛于此） */
    private static String normalizePath(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    @Override
    public List<IfsEntry> listIfsDir(String path) {
        String dir = path == null || path.isBlank() ? "/QOpenSys/rxas400" : normalizePath(path);
        while (dir.length() > 1 && dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        if (dir.toUpperCase().startsWith(IfsClient.TRASH_ROOT.toUpperCase())) {
            return listTrashDir(dir);
        }
        String base = dir.toUpperCase();
        List<IfsEntry> rows = new ArrayList<>();
        if (base.endsWith("/RXAS400") || base.endsWith("/QOPEN")) {
            rows.add(new IfsEntry("document", "DIR", 0L, "2026-07-10 10:00:00", dir + "/document"));
            rows.add(new IfsEntry("log", "DIR", 0L, "2026-07-11 11:00:00", dir + "/log"));
            rows.add(new IfsEntry("README.txt", "FILE", 512L, "2026-07-01 09:00:00", dir + "/README.txt"));
        } else if (base.endsWith("/DOCUMENT")) {
            rows.add(new IfsEntry("operation_guide.md", "FILE", 2048L, "2026-07-15 09:30:00", dir + "/operation_guide.md"));
            rows.add(new IfsEntry("backup_plan.txt", "FILE", 1024L, "2026-07-16 10:10:00", dir + "/backup_plan.txt"));
        } else if (base.endsWith("/LOG")) {
            rows.add(new IfsEntry("joblog_20260812.log", "FILE", 8192L, "2026-08-12 08:00:00", dir + "/joblog_20260812.log"));
        } else if (base.endsWith("/DEPLOY")) {
            rows.add(new IfsEntry("package_v1.0.zip", "FILE", 1024 * 1024L, "2026-08-01 14:00:00", dir + "/package_v1.0.zip"));
        }
        String targetDirForDirs = dir;
        state.mockIfsDirs.forEach(dirPath -> {
            String normalized = normalizePath(dirPath);
            int slash = normalized.lastIndexOf('/');
            String parent = slash <= 0 ? "/" : normalized.substring(0, slash);
            if (parent.equalsIgnoreCase(targetDirForDirs)) {
                rows.add(new IfsEntry(normalized.substring(slash + 1), "DIR", 0L, "2026-08-13 12:00:00", normalized));
            }
        });
        String targetDir = dir;
        state.mockIfsFiles.forEach((filePath, content) -> {
            String normalized = normalizePath(filePath);
            int slash = normalized.lastIndexOf('/');
            String parent = slash <= 0 ? "/" : normalized.substring(0, slash);
            if (parent.equalsIgnoreCase(targetDir)) {
                rows.add(new IfsEntry(normalized.substring(slash + 1), "FILE", content.length, "2026-08-13 12:00:00", normalized));
            }
        });
        return rows;
    }

    private List<IfsEntry> listTrashDir(String dir) {
        String relDir = dir.length() > IfsClient.TRASH_ROOT.length()
                ? dir.substring(IfsClient.TRASH_ROOT.length()) : "";
        while (relDir.startsWith("/")) {
            relDir = relDir.substring(1);
        }
        String prefix = relDir.isEmpty() ? "" : relDir + "/";
        List<IfsEntry> rows = new ArrayList<>();
        TreeSet<String> seenDirs = new TreeSet<>();
        state.mockIfsFiles.forEach((filePath, content) -> {
            String normalized = normalizePath(filePath);
            if (!normalized.toUpperCase().startsWith(IfsClient.TRASH_ROOT.toUpperCase())) {
                return;
            }
            String rel = normalized.substring(IfsClient.TRASH_ROOT.length());
            while (rel.startsWith("/")) {
                rel = rel.substring(1);
            }
            if (!rel.startsWith(prefix)) {
                return;
            }
            String rest = rel.substring(prefix.length());
            int slash = rest.indexOf('/');
            if (slash < 0) {
                rows.add(new IfsEntry(rest, "FILE", content.length, "2026-08-13 12:00:00", normalized));
            } else {
                seenDirs.add(rest.substring(0, slash));
            }
        });
        state.mockIfsDirs.forEach(dirPath -> {
            String normalized = normalizePath(dirPath);
            if (!normalized.toUpperCase().startsWith(IfsClient.TRASH_ROOT.toUpperCase())) {
                return;
            }
            String rel = normalized.substring(IfsClient.TRASH_ROOT.length());
            while (rel.startsWith("/")) {
                rel = rel.substring(1);
            }
            if (!rel.startsWith(prefix)) {
                return;
            }
            String rest = rel.substring(prefix.length());
            int slash = rest.indexOf('/');
            String seg = slash < 0 ? rest : rest.substring(0, slash);
            if (!seg.isBlank()) {
                seenDirs.add(seg);
            }
        });
        for (String seg : seenDirs) {
            rows.add(new IfsEntry(seg, "DIR", 0L, "2026-08-13 12:00:00",
                    IfsClient.TRASH_ROOT + "/" + (relDir.isEmpty() ? "" : relDir + "/") + seg));
        }
        return rows;
    }

    @Override
    public String readIfsFile(String path) {
        String name = normalizePath(path);
        String file = name.substring(name.lastIndexOf('/') + 1);
        if (state.mockIfsFiles.containsKey(name)) {
            return new String(state.mockIfsFiles.get(name), StandardCharsets.UTF_8);
        }
        return switch (file.toLowerCase()) {
            case "readme.txt" -> "RXAS400ADM 部署说明（模拟内容）\n1. 解压包至 /QOpenSys/rxas400\n2. 运行 install.sh\n";
            case "operation_guide.md" -> "# 运维手册（模拟）\n\n## 每日检查\n- 作业状态 MSGW\n- 磁盘使用率\n";
            case "backup_plan.txt" -> "备份计划（模拟）：每日 02:00 SAVLIB，每周日 SAVSYS。\n";
            case "joblog_20260812.log" -> "2026-08-12 08:00:01 JOB QSECOFR/BATCH01 启动\n2026-08-12 08:00:05 JOB QSECOFR/BATCH01 正常结束\n";
            default -> "（模拟文件内容）文件: " + file;
        };
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
        String file = normalizePath(path);
        while (file.length() > 1 && file.endsWith("/")) {
            file = file.substring(0, file.length() - 1);
        }
        state.mockIfsFiles.put(file, content == null ? new byte[0] : content);
        return true;
    }

    @Override
    public boolean mkdirIfs(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String dir = normalizePath(path);
        while (dir.length() > 1 && dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        state.mockIfsDirs.add(dir.toUpperCase());
        return true;
    }

    @Override
    public String trashIfsFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String file = normalizePath(path);
        while (file.length() > 1 && file.endsWith("/")) {
            file = file.substring(0, file.length() - 1);
        }
        String trashPath = IfsClient.TRASH_ROOT + file;
        /* C16：get 判空后再移除+放入，替代 remove 结果直 put 的非原子写法 */
        byte[] content = state.mockIfsFiles.get(file);
        if (content != null) {
            state.mockIfsFiles.remove(file);
            state.mockIfsFiles.put(trashPath, content);
            return trashPath;
        }
        if (state.mockIfsDirs.contains(file.toUpperCase())) {
            state.mockIfsDirs.remove(file.toUpperCase());
            state.mockIfsDirs.add(trashPath.toUpperCase());
            List<String> moved = new ArrayList<>();
            for (String key : state.mockIfsFiles.keySet()) {
                if (key.toUpperCase().startsWith(file.toUpperCase() + "/")) {
                    moved.add(key);
                }
            }
            for (String key : moved) {
                /* C16：get 判空后再移除+放入，key 消失时跳过而非写入 null */
                byte[] childContent = state.mockIfsFiles.get(key);
                if (childContent == null) {
                    continue;
                }
                state.mockIfsFiles.remove(key);
                state.mockIfsFiles.put(IfsClient.TRASH_ROOT + key, childContent);
            }
            return trashPath;
        }
        return null;
    }

    @Override
    public boolean restoreIfsFile(String trashPath) {
        if (trashPath == null || trashPath.isBlank()) {
            return false;
        }
        String t = normalizePath(trashPath);
        while (t.length() > 1 && t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        if (!t.toUpperCase().startsWith(IfsClient.TRASH_ROOT.toUpperCase())) {
            return false;
        }
        String original = t.substring(IfsClient.TRASH_ROOT.length());
        /* C16：get 判空后再移除+放入，替代 remove 结果直 put 的非原子写法 */
        byte[] content = state.mockIfsFiles.get(t);
        if (content != null) {
            state.mockIfsFiles.remove(t);
            state.mockIfsFiles.put(original, content);
            return true;
        }
        if (state.mockIfsDirs.contains(t.toUpperCase())) {
            state.mockIfsDirs.remove(t.toUpperCase());
            state.mockIfsDirs.add(original.toUpperCase());
            List<String> moved = new ArrayList<>();
            for (String key : state.mockIfsFiles.keySet()) {
                if (key.toUpperCase().startsWith(t.toUpperCase() + "/")) {
                    moved.add(key);
                }
            }
            for (String key : moved) {
                String restored = key.substring(IfsClient.TRASH_ROOT.length());
                /* C16：get 判空后再移除+放入，key 消失时跳过而非写入 null */
                byte[] restoredContent = state.mockIfsFiles.get(key);
                if (restoredContent == null) {
                    continue;
                }
                state.mockIfsFiles.remove(key);
                state.mockIfsFiles.put(restored, restoredContent);
            }
            return true;
        }
        return false;
    }

    @Override
    public byte[] readIfsFileBytes(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String name = normalizePath(path);
        if (state.mockIfsFiles.containsKey(name)) {
            return state.mockIfsFiles.get(name);
        }
        return readIfsFile(name).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public InputStream readIfsFileStream(String path) {
        byte[] data = readIfsFileBytes(path);
        return data == null ? null : new ByteArrayInputStream(data);
    }
}