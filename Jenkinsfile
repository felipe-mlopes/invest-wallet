pipeline {
    agent { 
        docker {
            image 'maven:3.9-eclipse-temurin-17'
        }
     }

     environment {
        CI = "true"
     }

    stages {
        stage('Build and Compile') {
            steps {
                sh 'mvn --version'
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }
    }
}
