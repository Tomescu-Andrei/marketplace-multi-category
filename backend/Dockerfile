FROM maven:3.9.11-eclipse-temurin-21 AS toolchain

# PostgreSQL is used only inside the build stage, for reproducible Flyway/jOOQ generation.
FROM postgres:17-bookworm AS build
COPY --from=toolchain /opt/java/openjdk /opt/java/openjdk
COPY --from=toolchain /usr/share/maven /usr/share/maven
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:/usr/share/maven/bin:${PATH}"
WORKDIR /workspace
COPY pom.xml ./
COPY build-tools ./build-tools
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 sh build-tools/build-image.sh

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system marketplace \
    && useradd --system --gid marketplace marketplace
WORKDIR /app
COPY --from=build --chown=marketplace:marketplace /workspace/target/marketplace-backend-0.0.1-SNAPSHOT.jar app.jar
USER marketplace
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
