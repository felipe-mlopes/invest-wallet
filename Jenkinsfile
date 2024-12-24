pipeline {
    agent any
    
    tools {
        maven 'M3'
    }

    environment {
        TESTCONTAINERS_RYUK_DISABLED = "true"
        DOCKER_HOST = "unix:///var/run/docker.sock"
        DOCKER_IP = "172.17.0.1"
        MAVEN_OPTS = '-Xmx2048m'
        TESTCONTAINERS_CHECKS_DISABLE = "true"
        TESTCONTAINERS_REUSE_ENABLE = "true"
        DOCKER_CLIENT_TIMEOUT = "120"
        COMPOSE_HTTP_TIMEOUT = "120"
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

        stage('Prepare Test Environment') {
            steps {
                sh '''
                    echo "testcontainers.reuse.enable=true" > /root/.testcontainers.properties
                    chmod 644 /root/.testcontainers.properties
                    
                    # Garante que o Docker tem as permissões corretas
                    chmod 666 /var/run/docker.sock
                    
                    # Limpa containers antigos se necessário
                    docker container prune -f
                '''
            }
        }

        stage('Integration Tests') {
            steps {
                sh '''
                    mvn verify -Pfailsafe
                    echo "Checking for integration test reports:"
                    ls -la target/failsafe-reports/ || echo "No failsafe-reports directory found"
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml'
                }
                failure {
                    sh '''
                        echo "Maven logs:"
                        cat target/failsafe-reports/*.txt || echo "No test logs found"
                    '''
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