package com.hurios.huriosbackend.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuracion de coexistencia entre PostgreSQL/H2 (JPA) y MongoDB (NoSQL).
 * Separa los paquetes de repositorios para evitar conflictos de autodeteccion en Spring Data.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.hurios.huriosbackend.repository",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.hurios\\.huriosbackend\\.repository\\.mongo\\..*"
        )
)
@EnableMongoRepositories(
        basePackages = "com.hurios.huriosbackend.repository.mongo"
)
public class PersistenceConfig {
}
