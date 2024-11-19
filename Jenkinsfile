pipeline {
    triggers {
        pollSCM '* * * * *'
    }

    stages {
        stage('Build') {
            steps {
                sh './mvnw clean'
            }
        }

        stage('Unit Test') {
            steps {
                sh './mvnw test'
            }
        }
    }
}