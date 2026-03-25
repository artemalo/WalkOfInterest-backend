# Этап сборки: используем официальный образ Maven для компиляции приложения
FROM maven:3.9-eclipse-temurin-25 AS builder

# Создаём рабочую директорию внутри контейнера
WORKDIR /app

# Копируем POM‑файл и загружаем зависимости (это позволит кэшировать зависимости при последующих сборках)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код приложения
COPY src ./src

# Собираем JAR‑файл с помощью Maven
RUN mvn package -DskipTests

# Основной этап: используем минимальный образ Java для запуска приложения
FROM eclipse-temurin:25-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR‑файл из этапа сборки
COPY --from=builder /app/target/woi-*.jar app.jar

# Открываем порт, на котором работает Spring Boot (по умолчанию — 8080)
EXPOSE 8080

# Задаём команду для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
