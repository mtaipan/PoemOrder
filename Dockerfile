# ====== build stage ======
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Кэшируем зависимости отдельно
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Копируем исходники и собираем
COPY src ./src
RUN mvn -q -e -DskipTests package

# ====== runtime stage ======
FROM eclipse-temurin:21-jre
WORKDIR /app

# Безопаснее запускать не под root
RUN useradd -m appuser
USER appuser

# Копируем собранный jar
COPY --from=build /app/target/*.jar /app/app.jar

# Пара дефолтных JVM опций (можешь править)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
