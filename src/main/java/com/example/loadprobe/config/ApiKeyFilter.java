package com.example.loadprobe.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 查询接口鉴权：/api/** 必须携带正确的 X-API-Key 请求头。
 * loadprobe.api-key 为空时不做校验（仅限本机调试）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    public static final String HEADER = "X-API-Key";

    private final LoadProbeProperties properties;

    public ApiKeyFilter(LoadProbeProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || isExempt(path)) {
            chain.doFilter(request, response);
            return;
        }

        String expected = properties.getApiKey();
        if (expected == null || expected.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided != null && MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("API key 校验失败: method={} path={} remote={} keyPresent={}",
                request.getMethod(), path, request.getRemoteAddr(), provided != null);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"缺少或错误的 X-API-Key 请求头\"}");
    }

    /** 免鉴权路径：精确匹配或按目录前缀匹配（如 /api/v1/health 及其子路径）。 */
    private boolean isExempt(String path) {
        for (String exempt : properties.getAuthExemptPaths()) {
            if (exempt == null || exempt.isBlank()) {
                continue;
            }
            String prefix = exempt.endsWith("/") ? exempt : exempt + "/";
            if (path.equals(exempt) || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
