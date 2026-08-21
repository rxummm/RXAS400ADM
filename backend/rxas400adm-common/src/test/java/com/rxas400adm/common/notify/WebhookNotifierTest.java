package com.rxas400adm.common.notify;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookNotifierTest {

    private final WebhookNotifier notifier = new WebhookNotifier();
    // 测试缝：本地回环 HTTP 服务器做端到端验证，需绕过 SSRF 守卫（守卫单测在 SsrfGuardTest）
    private final WebhookNotifier notifierAllowAll = new WebhookNotifier(url -> true);
    private HttpServer server;
    private AtomicInteger received;

    @BeforeEach
    void startServer() throws Exception {
        received = new AtomicInteger(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            received.incrementAndGet();
            byte[] body = exchange.getRequestBody().readAllBytes();
            String response = body.length > 0 ? "ok" : "empty";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void push_reachableUrl_shouldReturnTrue() {
        boolean ok = notifierAllowAll.push("http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                "test", "hello");
        assertTrue(ok);
        assertTrue(received.get() >= 1);
    }

    @Test
    void push_blankUrl_shouldReturnFalse() {
        assertFalse(notifier.push("", "t", "m"));
        assertFalse(notifier.push(null, "t", "m"));
    }

    @Test
    void push_unreachableUrl_shouldReturnFalseWithoutThrow() {
        boolean ok = notifierAllowAll.push("http://127.0.0.1:1/nowhere", "t", "m");
        assertFalse(ok);
    }

    @Test
    void push_privateUrl_shouldBeRejectedBySsrfGuard() {
        assertFalse(notifier.push("http://127.0.0.1:" + server.getAddress().getPort() + "/hook", "t", "m"));
        assertTrue(received.get() == 0);
    }
}