package org.apache.coyote.http11.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.coyote.http11.Http11Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequest {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);
    private static final String CONTENT_LENGTH = "Content-Length";

    private final String method;
    private final String requestUrl;
    private final Map<String, String> requestParams;
    private final String protocolVersion;
    private final Map<String, String> headers;
    private final String requestBody;

    private HttpRequest(final String method, final String requestUrl, final Map<String, String> requestParams,
                        final String protocolVersion, final Map<String, String> headers, final String requestBody) {
        this.method = method;
        this.requestUrl = requestUrl;
        this.requestParams = requestParams;
        this.protocolVersion = protocolVersion;
        this.headers = headers;
        this.requestBody = requestBody;
    }

    public static HttpRequest from(final BufferedReader reader) throws IOException {
        final var startLine = reader.readLine();
        if (startLine.isEmpty()) {
            return null;
        }
        final var requestHeaders = readRequestHeaders(reader);
        var requestBody = "";
        if (requestHeaders.containsKey(CONTENT_LENGTH)) {
            requestBody = readRequestBody(reader, requestHeaders.get(CONTENT_LENGTH));
        }
        return HttpRequest.from(startLine, requestHeaders, requestBody);
    }

    private static String readRequestBody(final BufferedReader reader, final String contentLength) throws IOException {
        log.info("content=length ::: {}", contentLength);
        int length = Integer.parseInt(contentLength);
        char[] buffer = new char[length];
        reader.read(buffer, 0, length);

        return new String(buffer);
    }

    public static HttpRequest from(final String startLine, final Map<String, String> headers,
                                   final String requestBody) {
        return new HttpRequest(parseMethod(startLine), parseUrl(startLine), parseRequestParams(startLine),
                parseProtocolVersion(startLine), headers, requestBody);
    }

    private static Map<String, String> readRequestHeaders(final BufferedReader bufferedReader) throws IOException {
        StringBuilder requestHeaders = new StringBuilder();
        String line;
        while (!(line = Objects.requireNonNull(bufferedReader.readLine())).isBlank()) {
            requestHeaders.append(line)
                    .append("\r\n");
        }
        log.info("request-headers ::: {}", requestHeaders.toString());
        return parseHeaders(requestHeaders.toString());
    }

    private static Map<String, String> parseHeaders(final String headers) {
        return Arrays.stream(headers.split("\r\n"))
                .map(header -> header.split(": "))
                .collect(Collectors.toMap(header -> header[0], header -> header[1]));
    }

    private static String parseMethod(final String startLine) {
        return startLine.split(" ")[0];
    }

    private static String parseUrl(final String requestHeader) {
        log.info("requestHeader::: {}", requestHeader);
        return requestHeader.split(" ")[1]
                .split("\\?")[0];
    }

    private static Map<String, String> parseRequestParams(final String requestHeader) {
        String url = requestHeader.split(" ")[1];

        if (!url.contains("?")) {
            return Map.of();
        }

        String params = url.split("\\?")[1];

        return Arrays.stream(params.split("&"))
                .map(it -> it.split("="))
                .collect(Collectors.toMap(it -> it[0], it -> it[1]));
    }

    private static String parseProtocolVersion(final String startLine) {
        return startLine.split(" ")[2];
    }

    public String getMethod() {
        return method;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
