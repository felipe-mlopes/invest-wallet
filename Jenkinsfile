pipeline {
    agent any
    
    tools {
        maven 'M3'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/felipe-mlopes/invest-wallet.git'
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Test - Unit') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test - Integration') {
            agent {
                docker {
                    image 'eclipse-temurin:17.0.9_9-jdk-jammy'
                    args '--network host -u root -v /var/run/docker.sock:/var/run/docker.sock'
                }
            }
            steps {
                sh sh './mvnw verify'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}