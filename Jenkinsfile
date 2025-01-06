pipeline {
    agent any
    
    tools {
        maven 'M3'
    }

    environment {
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
        DOCKER_IMAGE = 'felipemlp/invest-wallet'
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

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    def dockerImageTag = "${env.DOCKER_IMAGE}:${env.BUILD_NUMBER}"
                    def dockerImageLatest = "${env.DOCKER_IMAGE}:latest"
                    
                    withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_TOKEN')]) {
                        sh "echo ${DOCKER_TOKEN} | docker login -u ${DOCKER_USER} --password-stdin"
                    }
                    sh "docker build -t ${dockerImageTag} ."
                    sh "docker push ${dockerImageTag}"
                    sh "docker tag ${dockerImageTag} ${dockerImageLatest}"
                    sh "docker push ${dockerImageLatest}"
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