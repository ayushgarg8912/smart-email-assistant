# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# The app reads its port from the PORT env var (default 8080),
# which platforms like Render inject automatically.
ENTRYPOINT ["java", "-jar", "app.jar"]
