import org.flywaydb.core.Flyway
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target
import org.testcontainers.containers.PostgreSQLContainer

// Generate against the actual PostgreSQL schema without touching the development database.
def postgres = new PostgreSQLContainer('postgres:17-alpine')
try {
    postgres.start()
    Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations('filesystem:' + new File(project.basedir, 'src/main/resources/db/migration').absolutePath)
            .load()
            .migrate()

    GenerationTool.generate(new Configuration()
            .withJdbc(new Jdbc()
                    .withDriver('org.postgresql.Driver')
                    .withUrl(postgres.jdbcUrl)
                    .withUser(postgres.username)
                    .withPassword(postgres.password))
            .withGenerator(new Generator()
                    .withDatabase(new Database()
                            .withName('org.jooq.meta.postgres.PostgresDatabase')
                            .withInputSchema('public')
                            .withIncludes('categories|category_attributes|users|listings'))
                    .withTarget(new Target()
                            .withPackageName('com.marketplace.jooq')
                            .withDirectory(new File(project.build.directory, 'generated-sources/jooq').absolutePath))))
} finally {
    postgres.stop()
}
