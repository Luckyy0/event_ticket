FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/inventory-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
