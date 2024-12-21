pipeline {
    agent any
    stages {
        stage('Test Docker CLI') {
            steps {
                sh 'docker --version'
                sh 'docker ps'
            }
        }
    }
}
