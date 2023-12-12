package kr.pe.karsei.helper.vault.propertybootstrap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class VaultPropertyBootstrapConfiguration {
    @Bean
    @ConditionalOnMissingBean(VaultPropertyProperties.class)
    public VaultPropertyProperties vaultPropertyProperties() {
        return new VaultPropertyProperties();
    }

    @Bean
    @ConditionalOnMissingBean(VaultPropertySourceLocator.class)
    @ConditionalOnProperty(value = VaultPropertyProperties.PREFIX + ".enabled", matchIfMissing = true)
    public VaultPropertySourceLocator vaultPropertySourceLocator(VaultPropertyProperties properties) {
        return new VaultPropertySourceLocator(properties);
    }
}
