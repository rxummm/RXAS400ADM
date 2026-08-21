package com.rxas400adm.source.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SourceService implements ISourceService {

    private final AS400ClientProvider clientProvider;

    private AS400Client client() {
        return clientProvider.current();
    }

    public List<String> listLibraries() {
        return client().listLibraries();
    }

    public List<String> listSourceFiles(String library) {
        return client().listSourceFiles(library);
    }

    public List<String> listMembers(String library, String sourceFile) {
        return client().listMembers(library, sourceFile);
    }

    public Map<String, String> readMember(String library, String sourceFile, String member) {
        return Map.of(
                "library", library,
                "sourceFile", sourceFile,
                "member", member,
                "content", client().readMember(library, sourceFile, member)
        );
    }
}