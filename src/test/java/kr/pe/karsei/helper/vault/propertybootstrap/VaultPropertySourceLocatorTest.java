package kr.pe.karsei.helper.vault.propertybootstrap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.bootstrap.support.OriginTrackedCompositePropertySource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class VaultPropertySourceLocatorTest {
    private final VaultPropertySourceLocator sourceLocator = new VaultPropertySourceLocator(VaultPropertyProperties.builder()
            .roleId("ROLE_ID")
            .secretId("SECRET_ID")
            .engineName("ENGINE_NAME")
            .build());

    @Test
    @DisplayName("Vault 인증 토큰 획득")
    public void getVaultAuthTokenTest() throws URISyntaxException {
        VaultPropertyProperties.Credentials credentials = sourceLocator.getVaultPropertyProperties().toCredentials();
        String vaultAuthToken = sourceLocator.getVaultAuthToken(testRestTemplate(), credentials);
        assertThat(vaultAuthToken).isNotNull();
    }

    private RestTemplate testRestTemplate() {
        return new RestTemplate(List.of(new MappingJackson2HttpMessageConverter()));
    }

    @Test
    @DisplayName("Property 할당")
    public void getSecretAndMapToPropertyTest() throws URISyntaxException {
        RestTemplate restTemplate = testRestTemplate();
        String vaultAuthToken = sourceLocator.getVaultAuthToken(testRestTemplate(), sourceLocator.getVaultPropertyProperties().toCredentials());
        OriginTrackedCompositePropertySource composite = new OriginTrackedCompositePropertySource("vaultTestComposite");
        HashMap<String, String> map = new HashMap<>();
        map.put("hostname", "karsei.mysql.userdb.hostname");
        map.put("port", "karsei.mysql.userdb.port");
        map.put("user", "karsei.mysql.userdb.username");
        map.put("password", "karsei.mysql.userdb.password");
        sourceLocator.getSecretAndMapToProperty(vaultAuthToken, restTemplate, composite, sourceLocator.getVaultPropertyProperties().toCredentials(), "some/secret", map);
    }
}
