package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopologyServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private TopologyService service;

    @BeforeEach
    void setUp() {
        service = new TopologyService(clientProvider);
        when(clientProvider.current()).thenReturn(client);
    }

    @Test
    void graph_shouldReturnNodesAndLinks() {
        GraphData graph = new GraphData(
                List.of(new GraphNode("APP.ORDERMAINT", "ORDERMAINT", "PGM", "APP")),
                List.of(new GraphLink("APP.ORDERMAINT", "APP.ORDFILE")));
        when(client.objectGraph("APP")).thenReturn(graph);

        GraphData result = service.graph("APP");
        assertEquals(1, result.nodes().size());
        assertEquals(1, result.links().size());
        assertEquals("PGM", result.nodes().get(0).type());
    }
}
