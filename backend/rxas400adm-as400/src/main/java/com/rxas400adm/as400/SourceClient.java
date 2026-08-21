package com.rxas400adm.as400;

import java.util.List;

/**
 * 源码域：库（Library）、源文件（Source File）、源成员（Source Member）。
 * <p>
 * M2：未接入真实实现的查询默认优雅降级（返回空），实现类只覆写已接入的部分；
 * 新增方法时无需同时改动 MockAS400Client / JTOpenAS400Client。
 */
public interface SourceClient {

    /** 列出库（Library）。未接入真机时返回空列表（优雅降级）。 */
    default List<String> listLibraries() {
        return List.of();
    }

    /** 列出源文件（Source File）。未接入真机时返回空列表（优雅降级）。 */
    default List<String> listSourceFiles(String library) {
        return List.of();
    }

    /** 列出源成员（Source Member）。未接入真机时返回空列表（优雅降级）。 */
    default List<String> listMembers(String library, String sourceFile) {
        return List.of();
    }

    /** 读取源成员内容。未接入真机时返回空字符串（优雅降级）。 */
    default String readMember(String library, String sourceFile, String member) {
        return "";
    }
}