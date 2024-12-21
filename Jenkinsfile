pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                git(
                    url: 'https://github.com/felipe-mlopes/invest-wallet.git',
                    branch: 'main',
                    changelog: true,
                    poll: true
                )
            }
        }
        
        stage('Test Docker CLI') {
            steps {
                sh 'docker --version'
                sh 'docker ps'
            }
        }
    }
}