package com.manacommunity.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs DDL patches before Hibernate initialises, fixing check constraints
 * that Hibernate's ddl-auto:update won't touch on existing tables.
 */
@Configuration
public class SchemaConstraintPatcher {

    @Component
    public static class ConstraintPatcherDependencyRegistrar implements BeanFactoryPostProcessor {
        private static final Logger log = LoggerFactory.getLogger(ConstraintPatcherDependencyRegistrar.class);

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            try {
                String[] beanNames = beanFactory.getBeanNamesForType(
                        jakarta.persistence.EntityManagerFactory.class, true, false);
                for (String beanName : beanNames) {
                    BeanDefinition def = beanFactory.getBeanDefinition(beanName);
                    String[] existing = def.getDependsOn();
                    List<String> deps = existing == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(existing));
                    if (!deps.contains("constraintPatcher")) {
                        deps.add("constraintPatcher");
                        def.setDependsOn(deps.toArray(new String[0]));
                    }
                }
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher dependency registration failed: {}", e.getMessage());
            }
        }
    }

    @Component("constraintPatcher")
    public static class ConstraintPatcher {
        private static final Logger log = LoggerFactory.getLogger(ConstraintPatcher.class);

        @Autowired
        private DataSource dataSource;

        @PostConstruct
        public void patch() {
            try (Connection conn = dataSource.getConnection()) {
                String jdbcUrl = conn.getMetaData().getURL().toLowerCase();
                if (jdbcUrl.contains("h2") || jdbcUrl.contains(":mem:")) {
                    log.info("H2 database detected; skipping PostgreSQL constraint patching.");
                    return;
                }
            } catch (Exception e) {
                log.warn("Could not determine DB type; skipping constraint patch: {}", e.getMessage());
                return;
            }
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Drop both possible names — original migration used chk_tournament_type,
                // Hibernate ddl-auto:update generates tournament_config_tournament_type_check.
                stmt.execute("""
                        ALTER TABLE manacommunity.tournament_config
                            DROP CONSTRAINT IF EXISTS chk_tournament_type,
                            DROP CONSTRAINT IF EXISTS tournament_config_tournament_type_check
                        """);

                stmt.execute("""
                        ALTER TABLE manacommunity.tournament_config
                            ADD CONSTRAINT chk_tournament_type CHECK (tournament_type IN (
                                'KNOCKOUT','GROUP_KNOCKOUT','ROUND_ROBIN',
                                'DOUBLE_ELIMINATION','SWISS','SUPER_LEAGUE',
                                'KNOCKOUT_SINGLE','KNOCKOUT_DOUBLE',
                                'GROUP_PLAYOFF','LEAGUE','CUSTOM'
                            ))
                        """);

                log.info("tournament_config tournament_type constraint patched with extended values.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher failed: {}", e.getMessage(), e);
            }
        }
    }
}
