pipeline {
    agent {
        docker {
            image 'eclipse-temurin:17.0.9_9-jdk-jammy'
            args '--network host -u root -v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    stages {
        stage ('Unit Tests') {
            steps {
                echo "Running Unit Tests..."
                sh 'mvn test'
            }
        }

        stage('Build') {
            steps {
                echo "Building Application..."
                sh 'mvn clean package -DskipTests'
            }
        }
    }
}
