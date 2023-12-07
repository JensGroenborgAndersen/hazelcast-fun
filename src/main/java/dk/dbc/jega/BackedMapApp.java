package dk.dbc.jega;

import dk.dbc.jega.rest.TestEndpoint;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Set;

@ApplicationPath("/fun")
public class BackedMapApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackedMapApp.class);
    @Resource(lookup = "jdbc/test")
    DataSource dataSource;

    @PostConstruct
    public void init() {
        Flyway flyway = Flyway.configure()
                .table("schema_version")
                .locations("classpath:db.migration")
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .load();
        Arrays.stream(flyway.info().pending()).forEach(i -> LOGGER.info("Executing: " + i.getScript()));
        LOGGER.info("Migrating the database");
        flyway.migrate();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(TestEndpoint.class);
    }
}
