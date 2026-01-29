# Base image - Java 21
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# JAR 파일 복사
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
