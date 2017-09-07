#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@0.0.3') _

pipeline {
    agent {
        label 'windows'
    }

    environment {
        GITHUB                            = credentials('github_integration')
        ATLAS_GITHUB_INTEGRATION_USER     = "${GITHUB_USR}"
        ATLAS_GITHUB_INTEGRATION_PASSWORD = "${GITHUB_PSW}"
        TRAVIS_JOB_NUMBER                 = "${BUILD_NUMBER}.WIN"
    }

    stages {
        stage('Preparation') {
            steps {
                sendSlackNotification "STARTED", true
            }
        }

        stage('Test') {
            steps {
                gradleWrapper "check"
            }
        }
    }

    post {
        always {
            gradleWrapper "jacocoTestReport"
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'build/reports/jacoco/test/html',
                reportFiles: 'index.html',
                reportName: 'Coverage',
                reportTitles: ''
            ])

            sendSlackNotification currentBuild.result, true
            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
            gradleWrapper "clean"
        }
    }
}