package com.rxas400adm.as400.vo;

/**
 * 数据库表信息（BusinessController.tables 返回）。
 */
public record TableInfoVO(String name, String text, long rows) {
}
