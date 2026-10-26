FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY target/client-feature-configuration-service-1.0.0.jar app.jar
USER app
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
