import org.flywaydb.core.Flyway
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target
import org.testcontainers.containers.PostgreSQLContainer

// Native Maven builds use Testcontainers. Docker image builds supply their own temporary database.
def postgres = null
try {
    def jdbcUrl = System.getenv('CODEGEN_DB_URL')
    def username = System.getenv('CODEGEN_DB_USERNAME')
    def password = System.getenv('CODEGEN_DB_PASSWORD')
    if (!jdbcUrl) {
        postgres = new PostgreSQLContainer('postgres:17-alpine')
        postgres.start()
        jdbcUrl = postgres.jdbcUrl
        username = postgres.username
        password = postgres.password
    } else if (!username || password == null) {
        throw new IllegalArgumentException('CODEGEN_DB_USERNAME and CODEGEN_DB_PASSWORD are required with CODEGEN_DB_URL')
    }
    Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations('filesystem:' + new File(project.basedir, 'src/main/resources/db/migration').absolutePath)
            .load()
            .migrate()

    GenerationTool.generate(new Configuration()
            .withJdbc(new Jdbc()
                    .withDriver('org.postgresql.Driver')
                    .withUrl(jdbcUrl)
                    .withUser(username)
                    .withPassword(password))
            .withGenerator(new Generator()
                    .withDatabase(new Database()
                            .withName('org.jooq.meta.postgres.PostgresDatabase')
                            .withInputSchema('public')
                            .withIncludes('categories|category_attributes|users|listings'))
                    .withTarget(new Target()
                            .withPackageName('com.marketplace.jooq')
                            .withDirectory(new File(project.build.directory, 'generated-sources/jooq').absolutePath))))
} finally {
    postgres?.stop()
}
