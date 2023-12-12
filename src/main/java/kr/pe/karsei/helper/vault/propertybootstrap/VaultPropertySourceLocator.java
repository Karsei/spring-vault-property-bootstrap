package kr.pe.karsei.helper.vault.propertybootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.bootstrap.support.OriginTrackedCompositePropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Order(0)
@Getter
public class VaultPropertySourceLocator implements PropertySourceLocator {
    private final Logger logger = LoggerFactory.getLogger(VaultPropertySourceLocator.class);

    private final VaultPropertyProperties vaultPropertyProperties;
    public VaultPropertySourceLocator(VaultPropertyProperties properties) {
        this.vaultPropertyProperties = properties;
    }

    @Override
    public PropertySource<?> locate(Environment environment) {
        BeanUtils.copyProperties(environment, vaultPropertyProperties);

        RestTemplate restTemplate = getRestTemplate(vaultPropertyProperties);
        OriginTrackedCompositePropertySource composite = new OriginTrackedCompositePropertySource("vaultPropertyBootstrap");
        composite.addFirstPropertySource(new PropertiesPropertySource("configuredBootstrapProperty", vaultPropertyProperties.getOverrideProperties()));
        Map<String, Map<String, String>> propertyMap = vaultPropertyProperties.getPropertyMap();
        VaultPropertyProperties.Credentials credentials = vaultPropertyProperties.toCredentials();

        // 인증 토큰 발급
        String token;
        try {
            logger.info("Vault 토큰 발급 요청");
            token = getVaultAuthToken(restTemplate, credentials);
            logger.info("Vault 토큰 발급 성공");
            logger.debug(String.format("Vault 토큰: %s", token));
        } catch (Exception e) {
            if (vaultPropertyProperties.getFailFast()) {
                throw new IllegalStateException(e);
            }
            else {
                logger.warn("Vault 토큰을 획득하는 과정에서 오류가 발생했습니다.");
                return composite;
            }
        }

        // Secret 데이터 조회 및 할당
        for (String secretPath: propertyMap.keySet()) {
            try {
                getSecretAndMapToProperty(token, restTemplate, composite, credentials, secretPath, propertyMap.get(secretPath));
            }
            catch (Exception e) {
                if (vaultPropertyProperties.getFailFast()) {
                    throw new IllegalStateException(e);
                }
                else {
                    logger.warn(String.format("Vault 의 %s 으로부터 데이터를 불러오는 과정에서 오류가 발생했습니다.", secretPath), e);
                }
            }
        }

        return composite;
    }

    private RestTemplate getRestTemplate(VaultPropertyProperties properties) {
        if (properties.getRequestConnectTimeout() < 0) {
            throw new IllegalStateException("requestConnectTimeout 는 음수일 수 없습니다.");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getRequestConnectTimeout());
        return new RestTemplate(factory);
    }

    /**
     * Vault 인증 토큰을 획득합니다.
     * @param restTemplate RestTemplate 요청 객체
     * @param credentials 요청 파라미터 객체
     * @return 인증 토큰
     */
    public String getVaultAuthToken(RestTemplate restTemplate, VaultPropertyProperties.Credentials credentials) throws URISyntaxException {
        VaultAuthTokenRequest request = new VaultAuthTokenRequest(credentials.getRoleId(), credentials.getSecretId());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        RequestEntity<VaultAuthTokenRequest> requestEntity = new RequestEntity<>(request, headers, HttpMethod.POST, new URI(credentials.getUri() + VaultPropertyProperties.REQUEST_TOKEN_PATH));
        ResponseEntity<VaultAuthTokenResponse> body = restTemplate.exchange(requestEntity, VaultAuthTokenResponse.class);
        return Objects.requireNonNull(body.getBody()).auth.client_token;
    }

    /**
     * Secret 데이터를 Property 에 할당합니다.
     * @param token Vault 인증 토큰
     * @param restTemplate RestTemplate 요청 객체
     * @param composite propertySource Composite 객체
     * @param credentials 요청 필요 파라미터
     * @param secretPath Secret 이름
     * @param map Mapping 목록
     */
    public void getSecretAndMapToProperty(String token,
                                          RestTemplate restTemplate,
                                          OriginTrackedCompositePropertySource composite,
                                          VaultPropertyProperties.Credentials credentials,
                                          String secretPath,
                                          Map<String, String> map) throws URISyntaxException {
        String uri = credentials.getUri();
        String engineName = credentials.getEngineName();

        // Secret 데이터 요청 시에 Header 에 Vault 인증 토큰 필요
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add(VaultPropertyProperties.VAULT_TOKEN_HEADER, token);

        // 요청 주소 준비
        RequestEntity<?> requestEntity = new RequestEntity<>(httpHeaders, HttpMethod.GET, new URI(String.format("%s/v1/%s/%s", uri, engineName, secretPath)));

        // Secret Data 요청
        logger.info(String.format("%s 정보 요청", secretPath));
        logger.debug(requestEntity.getUrl().toString());
        ResponseEntity<VaultSecretResponse> exchange = restTemplate.exchange(requestEntity, VaultSecretResponse.class);
        Map<String, String> secretData = Objects.requireNonNull(exchange.getBody()).data;
        logger.info(String.format("%s 정보 성공", secretPath));

        // Property 할당
        Map<String, String> propertyData = new HashMap<>();
        for (String key: map.keySet()) {
            if (secretData.get(key) != null) {
                propertyData.put(map.get(key), secretData.get(key));
                logger.debug(String.format("%s -> %s 으로 할당", secretData.get(key), map.get(key)));
            }
        }

        composite.addPropertySource(new OriginTrackedMapPropertySource(String.format("VAULT_%s", secretPath), propertyData));
    }

    @Getter @Setter
    @AllArgsConstructor
    static class VaultAuthTokenRequest {
        @JsonProperty("role_id")
        private String roldId;
        @JsonProperty("secret_id")
        private String secretId;
    }

    @Setter
    static class VaultAuthTokenResponse {
        private AuthInfo auth;

        @Setter
        static class AuthInfo {
            private String client_token;
        }
    }

    @Setter
    static class VaultSecretResponse {
        private Map<String, String> data;
    }
}
