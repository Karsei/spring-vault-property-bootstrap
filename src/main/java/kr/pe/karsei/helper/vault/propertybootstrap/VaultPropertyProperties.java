package kr.pe.karsei.helper.vault.propertybootstrap;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ConfigurationProperties(VaultPropertyProperties.PREFIX)
@Builder
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class VaultPropertyProperties {
    public static final String PREFIX = "vault-property";
    public static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    public static final String REQUEST_TOKEN_PATH = "/v1/auth/approle/login";

    @Builder.Default
    private Boolean enabled = true;
    @Builder.Default
    private String uri = "";
    @Builder.Default
    private String roleId = StringUtils.hasText(System.getenv("VAULT_ROLE_ID")) ? System.getenv("VAULT_ROLE_ID") : "";
    @Builder.Default
    private String secretId = StringUtils.hasText(System.getenv("VAULT_SECRET_ID")) ? System.getenv("VAULT_SECRET_ID") : "";
    @Builder.Default
    private String engineName = StringUtils.hasText(System.getenv("VAULT_ENGINE_NAME")) ? System.getenv("VAULT_ENGINE_NAME") : "";
    @Builder.Default
    private Boolean failFast = true;
    @Builder.Default
    private Integer requestConnectTimeout = 1000 * 10;
    @Builder.Default
    private Boolean overrideSystemProperties = false;
    @Builder.Default
    private Boolean allowOverride = true;
    @Builder.Default
    private Boolean overrideNone = true;
    @Builder.Default
    private Map<String, Map<String, String>> propertyMap = new HashMap<>();

    public Credentials toCredentials() {
        return new Credentials(uri, roleId, secretId, engineName);
    }

    public Properties getOverrideProperties() {
        Properties properties = new Properties();
        properties.setProperty("spring.cloud.config.override-system-properties", overrideSystemProperties.toString());
        properties.setProperty("spring.cloud.config.allow-override", allowOverride.toString());
        properties.setProperty("spring.cloud.config.override-none", overrideNone.toString());
        return properties;
    }

    @Getter
    @AllArgsConstructor
    public static class Credentials {
        private String uri;
        private String roleId;
        private String secretId;
        private String engineName;
    }
}
