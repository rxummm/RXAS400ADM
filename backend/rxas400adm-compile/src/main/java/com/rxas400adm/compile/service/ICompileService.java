package com.rxas400adm.compile.service;

import com.rxas400adm.compile.dto.CompileRequest;
import com.rxas400adm.compile.entity.CompileRecord;

import java.util.List;

public interface ICompileService {

    CompileRecord compile(CompileRequest request);

    List<CompileRecord> history();
}