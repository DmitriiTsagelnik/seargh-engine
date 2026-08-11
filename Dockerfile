FROM eclipse-temurin:23-jdk

WORKDIR /app

COPY target/search-engine-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]