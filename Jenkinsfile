pipeline {
    agent any
    
    tools {
        maven 'M3'
    }

    environment {
        TESTCONTAINERS_RYUK_DISABLED = "true"
        DOCKER_HOST = "unix:///var/run/docker.sock"
        DOCKER_IP = "172.17.0.1"
        
        MAVEN_OPTS = '-Xmx1024m'
        TESTCONTAINERS_CHECKS_DISABLE = "true"
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
            steps {
                sh '''
                    mvn -X verify -Pfailsafe
                    echo "Test reports:"
                    ls -l target/failsafe-reports/ || true
                '''
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
                failure {
                    sh 'cat target/failsafe-reports/*.txt || true'
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