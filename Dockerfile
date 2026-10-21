# 로컬 시연용 보조 산출물. 테스트는 네이티브 환경/CI에서 먼저 실행한다.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 10001 erp && useradd --uid 10001 --gid erp --create-home erp
WORKDIR /app
COPY --from=build --chown=erp:erp /workspace/target/publishing-erp-mini-0.1.0-SNAPSHOT.war /app/erp.war
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx768m", "-jar", "/app/erp.war"]
