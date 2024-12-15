pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'maven:4.0.0-eclipse-temurin-17'
    }

    stages {
        stage('Run in Docker') {
            steps {
                script {
                    docker.image(DOCKER_IMAGE).inside('-v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.m2:/root/.m2') {
                        stage('Run Unit Tests') {
                            sh 'mvn test'
                        }
                        stage('Build Application') {
                            sh 'mvn clean package'
                        }
                    }
                }
            }
        }
    }
}
