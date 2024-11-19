pipeline {
    agent {
        docker {
            image 'eclipse-temurin:17.0.9_9-jdk-jammy'
            args '--network host -u root -v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    triggers {
        pollSCM 'H/2 * * * *'
    }

    stages {
        stage('Build and Test') {
            steps {
                sh './mvnw verify'
            }
        }
    }
}