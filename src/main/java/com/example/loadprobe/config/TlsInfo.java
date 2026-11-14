package com.example.loadprobe.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 读取证书信息用于启动日志与 /health 暴露，方便判断何时需要轮换证书。
 * 读取失败不阻断启动（Tomcat 侧会先报错），仅记录告警。
 */
@Component
public class TlsInfo {

    private static final Logger log = LoggerFactory.getLogger(TlsInfo.class);
    private static final long WARN_THRESHOLD_DAYS = 30;

    private final boolean enabled;
    private final String subject;
    private final Instant notAfter;
    private final long daysRemaining;

    public TlsInfo(Environment env) {
        boolean tlsEnabled = Boolean.parseBoolean(env.getProperty("server.ssl.enabled", "false"));
        this.enabled = tlsEnabled;

        String keyStorePath = env.getProperty("server.ssl.key-store");
        String password = env.getProperty("server.ssl.key-store-password", "");
        String alias = env.getProperty("server.ssl.key-alias");

        String resolvedSubject = null;
        Instant expiry = null;
        long days = -1;

        if (tlsEnabled && keyStorePath != null && !keyStorePath.isBlank()) {
            Path path = Paths.get(keyStorePath).toAbsolutePath();
            try (InputStream in = Files.newInputStream(path)) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(in, password.toCharArray());

                String target = alias;
                if (target == null || !keyStore.containsAlias(target)) {
                    Enumeration<String> aliases = keyStore.aliases();
                    target = aliases.hasMoreElements() ? aliases.nextElement() : null;
                }
                Certificate certificate = target == null ? null : keyStore.getCertificate(target);
                if (certificate instanceof X509Certificate x509) {
                    resolvedSubject = x509.getSubjectX500Principal().getName();
                    expiry = x509.getNotAfter().toInstant();
                    days = Duration.between(Instant.now(), expiry).toDays();
                } else {
                    log.warn("证书库 {} 中未找到 X.509 证书（alias={}）", path, target);
                }
            } catch (Exception e) {
                log.warn("读取证书信息失败（{}）：{}", path, e.toString());
            }
        }

        this.subject = resolvedSubject;
        this.notAfter = expiry;
        this.daysRemaining = days;

        if (enabled) {
            log.info("TLS 已启用，证书 subject={}，到期时间={}，剩余 {} 天", resolvedSubject, expiry, days);
            if (days >= 0 && days < WARN_THRESHOLD_DAYS) {
                log.warn("TLS 证书将在 {} 天后过期，请执行 scripts\\gen-cert.cmd -Force 重新生成并重启", days);
            }
        } else {
            log.info("TLS 未启用（LOADPROBE_TLS_ENABLED=false），当前以 HTTP 运行");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSubject() {
        return subject;
    }

    public Instant getNotAfter() {
        return notAfter;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }
}
