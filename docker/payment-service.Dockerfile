# syntax=docker/dockerfile:1
#
# Build context is the REPO ROOT, not this folder — the wrapper, the aggregator pom and
# every module pom must be visible for `-pl payment-service -am` to resolve the reactor.
# See docker-compose.yml, which sets `context: ..`.

# ---- build stage ----------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# Poms first, sources second: editing a .java file then leaves the dependency
# layer untouched, so a rebuild does not re-resolve the whole tree.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY discovery-service/pom.xml discovery-service/
COPY gateway-service/pom.xml   gateway-service/
COPY booking-service/pom.xml   booking-service/
COPY payment-service/pom.xml   payment-service/

COPY payment-service/src/ payment-service/src/

# The cache mount persists ~/.m2 across builds AND shares it between all four
# service images, so only the first build pays the download cost.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -pl payment-service -am package -DskipTests

# ---- runtime stage --------------------------------------------------------
# JRE, not JDK: nothing here compiles, and it drops ~200MB off the image.
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /src/payment-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
