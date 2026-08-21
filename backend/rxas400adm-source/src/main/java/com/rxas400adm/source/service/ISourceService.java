package com.rxas400adm.source.service;

import java.util.List;
import java.util.Map;

public interface ISourceService {

    List<String> listLibraries();

    List<String> listSourceFiles(String library);

    List<String> listMembers(String library, String sourceFile);

    Map<String, String> readMember(String library, String sourceFile, String member);
}