package com.rxas400adm.inspection;

import java.util.Map;

public interface IInspectionService {

    Map<String, Object> generate(Long serverId);

    byte[] export(String format, Long serverId);
}