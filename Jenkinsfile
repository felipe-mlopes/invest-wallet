pipeline {
    agent any

    stages {
        stage ('Unit Tests') {
            agent {
                docker {
                    image 'maven:3.8.6-openjdk-17-slim'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn test'
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'maven:3.8.6-openjdk-17-slim'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
    }
}
