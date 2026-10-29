FROM maven:3.9.10-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -ntp -q dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -q package -DskipTests

FROM eclipse-temurin:21.0.8_9-jre-alpine
WORKDIR /app
RUN addgroup -S spilab && adduser -S spilab -G spilab
USER spilab
COPY --from=build /src/target/spi-lab-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
