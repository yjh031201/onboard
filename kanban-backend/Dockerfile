## 1단계: mvnw로 빌드 (레이어 캐싱을 위해 의존성 다운로드 후 소스 복사)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# mvnw는 윈도우에서 체크아웃되어 CRLF일 수 있어 Linux sh가 못 읽는 문제를 방지
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q -B dependency:go-offline

COPY src ./src
RUN ./mvnw -q -B package -DskipTests

## 2단계: 빌드 산출물만 담은 실행 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
