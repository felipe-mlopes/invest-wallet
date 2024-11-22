
# Invest Wallet

## 📌 Descrição do Projeto

É uma aplicação web para gerenciamento de investimentos pessoais, permitindo que usuários acompanhem, calculem e visualizem seus investimentos de forma simples e intuitiva.    

## 🚀 Principais funcionalidades:

- Criação e autenticação de usuário usando Spring Security com JWT;
- Validação de cadastro com envio de e-mail utilizando Java Mail Sender;
- Criação de uma carteira de investimento;
- Adição, edição e remoção de um ativo na carteira de investimento;
- Adição, edição e remoção de compra/venda de um ativo na carteira de investimento;
- Inserção de dados históricos via arquivo csv (ex: compra/venda de ativos, ganhos de rendimentos) usando Opencsv;
- Inclusão de dados mensais automatizado usando web scraping de um portal de investimento com Jsoup e cron job com Scheduled Task.

## 🛠️ Tecnologias e Dependências

### Backend
- Spring Boot 3.3.2
- Java 17
- Spring Cloud OpenFeign
- Spring Security
- MongoDB
- Spring Actuator

### Autenticação
- JWT (Java-JWT)

### Utilitários
- Lombok
- Validation
- OpenCSV (Geração de CSV)
- Apache POI (Geração de Excel)
- Jsoup (Web Scraping)

### Testes
- Spring Boot Test
- TestContainers
- JUnit Jupiter

### Infraestrutura
- Docker

### Comunicação
- Apache HttpClient
- Spring Mail Sender

## 📋 Pré-requisitos

- JDK 17
- Maven 3.8+
- Docker
- Git
- IDE de sua preferência (IntelliJ IDEA, Eclipse, VS Code)

## 🔧 Instalação

### Clonar o repositório

```bash
# Clonar o projeto
git clone https://github.com/seu-usuario/invest-wallet.git

# Entrar no diretório do projeto
cd invest-wallet
```

### Configurar Variáveis de Ambiente

```bash
# .env
MONGO_DATABASE=investwallet
MONGO_USER=investuser
MONGO_PASSWORD=sua-senha-segura
JWT_SECRET=sua-chave-secreta-jwt
```

### Iniciar Bancos de Dados

```bash
# Iniciar o banco de dados MongoDB via Docker
docker-compose up -d
```

### Instalar Dependências e Rodar Aplicação

```bash
# Instalar dependências
mvn clean install

# Rodar aplicação
mvn spring-boot:run
```

## 🤝 Contribuição

1. Faça um fork do projeto
2. Crie uma branch para sua feature
3. Commit suas alterações
4. Faça um push para a branch
5. Abra um Pull Request

## 📝 Licença

## 📞 Contato

Felipe Lopes
- Linkedin: https://www.linkedin.com/in/felipe-mlopes
- E-mail: felipe.mlopes03@gmail.com
