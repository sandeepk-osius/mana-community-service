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

                // Drop and recreate tournament_match status constraint to include DRAFT and BYE
                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.tournament_match') IS NOT NULL THEN
                            ALTER TABLE manacommunity.tournament_match
                                DROP CONSTRAINT IF EXISTS tournament_match_status_check;
                                
                            ALTER TABLE manacommunity.tournament_match
                                ADD CONSTRAINT tournament_match_status_check CHECK (status IN (
                                    'DRAFT','PUBLISHED','SCHEDULED','LIVE','COMPLETED','POSTPONED','CANCELLED','BYE'
                                ));
                          END IF;
                        END $$;
                        """);
                log.info("tournament_match status constraint patched to support DRAFT.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher failed: {}", e.getMessage(), e);
            }

            // Independent patch: ensure tournament_match.config_id has ON DELETE CASCADE
            // so deleting/clearing a config removes its matches in one DB operation.
            // Idempotent — does nothing once the cascade FK is already in place.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          existing_fk text;
                          col_attnum  smallint;
                        BEGIN
                          -- Skip on fresh databases where the table isn't created yet.
                          IF to_regclass('manacommunity.tournament_match') IS NULL THEN
                            RETURN;
                          END IF;

                          -- Already cascading? Nothing to do.
                          IF EXISTS (
                            SELECT 1 FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = 'tournament_match'
                              AND con.conname = 'fk_tournament_match_config'
                              AND con.contype = 'f'
                              AND con.confdeltype = 'c'
                          ) THEN
                            RETURN;
                          END IF;

                          SELECT a.attnum INTO col_attnum
                          FROM pg_attribute a
                          JOIN pg_class rel ON rel.oid = a.attrelid
                          JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                          WHERE nsp.nspname = 'manacommunity'
                            AND rel.relname = 'tournament_match'
                            AND a.attname = 'config_id';

                          -- Drop any existing FK defined on config_id (e.g. a non-cascading one).
                          FOR existing_fk IN
                            SELECT con.conname
                            FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = 'tournament_match'
                              AND con.contype = 'f'
                              AND con.conkey = ARRAY[col_attnum]
                          LOOP
                            EXECUTE format('ALTER TABLE manacommunity.tournament_match DROP CONSTRAINT %I', existing_fk);
                          END LOOP;

                          ALTER TABLE manacommunity.tournament_match
                            ADD CONSTRAINT fk_tournament_match_config
                            FOREIGN KEY (config_id)
                            REFERENCES manacommunity.tournament_config(id)
                            ON DELETE CASCADE;
                        END $$;
                        """);

                log.info("tournament_match.config_id FK patched with ON DELETE CASCADE.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher cascade-FK patch failed: {}", e.getMessage(), e);
            }
        }
    }
}
