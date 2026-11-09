package com.cornelius.portfolio.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Minimal streaming client for a local Ollama server (POST /api/chat, NDJSON).
 * Plain JDK HTTP, so no extra dependencies. Disabled when no base URL is set.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final String baseUrl;
    private final String model;
    private final String keepAlive;
    private final int numPredict;
    private final int numCtx;
    private final double temperature;
    private final Duration timeout;
    private final long queueWaitSeconds;
    private final HttpClient http;
    private final Semaphore slots;
    private final long cooldownNanos;
    private volatile long downUntil;
    private volatile boolean everDown;

    /** The server is busy with another question; says nothing about its health. */
    private static final class Busy extends IOException {
        Busy() {
            super("Ollama is busy");
        }
    }

    public OllamaClient(
        @Value("${app.ollama.base-url:}") String baseUrl,
        @Value("${app.ollama.model:qwen2.5:1.5b}") String model,
        @Value("${app.ollama.keep-alive:24h}") String keepAlive,
        @Value("${app.ollama.max-tokens:350}") int numPredict,
        @Value("${app.ollama.context-tokens:3072}") int numCtx,
        @Value("${app.ollama.temperature:0.2}") double temperature,
        @Value("${app.ollama.timeout-seconds:90}") long timeoutSeconds,
        @Value("${app.ollama.max-concurrent:1}") int maxConcurrent,
        @Value("${app.ollama.queue-wait-seconds:8}") long queueWaitSeconds,
        @Value("${app.ollama.cooldown-seconds:30}") long cooldownSeconds) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        this.model = model;
        this.keepAlive = keepAlive;
        this.numPredict = numPredict;
        this.numCtx = numCtx;
        this.temperature = temperature;
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        this.queueWaitSeconds = Math.max(0, queueWaitSeconds);
        this.slots = new Semaphore(Math.max(1, maxConcurrent));
        this.cooldownNanos = TimeUnit.SECONDS.toNanos(Math.max(0, cooldownSeconds));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        if (isEnabled()) {
            log.info("Ollama enabled: {} (model {})", this.baseUrl, model);
        } else {
            log.info("Ollama disabled (set OLLAMA_BASE_URL to enable); answering from the rule-based engine only");
        }
    }

    public boolean isEnabled() {
        return !baseUrl.isBlank();
    }

    /** Enabled and not in the cool-down that follows a failure, so visitors never wait on a dead server. */
    public boolean isAvailable() {
        return isEnabled() && System.nanoTime() - downUntil >= 0;
    }

    private void markDown(Exception cause) {
        if (!everDown) {
            log.warn("Ollama unreachable ({}); using the rule-based engine for the next {}s", cause.getMessage(),
                TimeUnit.NANOSECONDS.toSeconds(cooldownNanos));
        }
        everDown = true;
        downUntil = System.nanoTime() + cooldownNanos;
    }

    /** Streams the model's reply token by token. Blocks until the reply is complete. */
    public void generate(String system, String user, Consumer<String> onToken)
        throws IOException, InterruptedException {
        if (!isEnabled()) {
            throw new IOException("Ollama is not configured");
        }
        if (!isAvailable()) {
            throw new IOException("Ollama is cooling down after a failure");
        }
        if (!slots.tryAcquire(queueWaitSeconds, TimeUnit.SECONDS)) {
            throw new Busy();
        }
        boolean[] gotToken = {false};
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(system, user), StandardCharsets.UTF_8))
                .build();
            HttpResponse<Stream<String>> response = http.send(request, HttpResponse.BodyHandlers.ofLines());
            long deadline = System.nanoTime() + timeout.toNanos();
            try (Stream<String> lines = response.body()) {
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("Ollama HTTP " + response.statusCode() + ": "
                        + lines.findFirst().orElse(""));
                }
                Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    if (System.nanoTime() > deadline) {
                        throw new IOException("Ollama took too long");
                    }
                    String line = it.next();
                    if (line.isBlank()) {
                        continue;
                    }
                    String error = extractString(line, "\"error\":\"");
                    if (error != null) {
                        throw new IOException("Ollama error: " + error);
                    }
                    String content = extractString(line, "\"content\":\"");
                    if (content != null && !content.isEmpty()) {
                        gotToken[0] = true;
                        onToken.accept(content);
                    }
                    if (line.contains("\"done\":true")) {
                        break;
                    }
                }
            }
            if (everDown) {
                log.info("Ollama is reachable again");
                everDown = false;
            }
        } catch (IOException e) {
            if (!(e instanceof Busy) && !gotToken[0] && !(e.getCause() instanceof InterruptedException)) {
                markDown(e); // connection refused, timeout or HTTP error before any output
            }
            throw e;
        } finally {
            slots.release();
        }
    }

    // ---- JSON helpers (the payloads are tiny, so no JSON library is needed) -----------------

    String requestBody(String system, String user) {
        return "{\"model\":" + quote(model)
            + ",\"stream\":true,\"keep_alive\":" + quote(keepAlive)
            + ",\"options\":{\"temperature\":" + String.format(Locale.ROOT, "%.2f", temperature)
            + ",\"num_predict\":" + numPredict + ",\"num_ctx\":" + numCtx + "}"
            + ",\"messages\":[{\"role\":\"system\",\"content\":" + quote(system)
            + "},{\"role\":\"user\",\"content\":" + quote(user) + "}]}";
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /** Reads the JSON string that follows {@code marker}; null when the marker is absent. */
    static String extractString(String json, String marker) {
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start + marker.length(); i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\' || i + 1 >= json.length()) {
                sb.append(c);
                continue;
            }
            char n = json.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (i + 4 < json.length()) {
                        try {
                            sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            sb.append("\\u");
                        }
                    }
                }
                default -> sb.append(n);
            }
        }
        return sb.toString();
    }
}
