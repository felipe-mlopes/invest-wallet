pipeline {
    agent {
        docker { 
            image 'maven:3.8.6-openjdk-17-slim'
            args '-v $HOME/.m2:/root/.m2'
        }
    }

    stages {
        stage ('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
    }
}