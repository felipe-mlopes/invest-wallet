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
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            environment {
                TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE = "/var/run/docker.sock"
                DOCKER_HOST = "unix:///var/run/docker.sock"
            }
            steps {
                sh 'mvn verify -Pfailsafe'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
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