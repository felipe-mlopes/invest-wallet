FROM eclipse-temurin:17-jdk

# Configuração do diretório de trabalho
WORKDIR /app

# Copiar o arquivo JAR gerado pelo Maven para o container
COPY target/*.jar app.jar

# Comando para executar o JAR
CMD ["java", "-jar", "app.jar"]
