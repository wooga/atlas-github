#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@0.0.3') _

pipeline {
    agent none

    stages {
        stage('Preparation') {
            agent any

            steps {
                sendSlackNotification "STARTED", true
            }
        }

        stage('check') {
            parallel {
                stage('Windows') {
                    agent {
                        label 'windows&&atlas'
                    }

                    environment {
                        COVERALLS_REPO_TOKEN                = credentials('atlas_github_coveralls_token')
                        TRAVIS_JOB_NUMBER                   = "${BUILD_NUMBER}.WIN"
                        GITHUB                              = credentials('github_integration_2')
                        ATLAS_GITHUB_INTEGRATION_USER       = "${GITHUB_USR}"
                        ATLAS_GITHUB_INTEGRATION_PASSWORD   = "${GITHUB_PSW}"
                    }

                    steps {
                        gradleWrapper "check"
                    }

                    post {
                        success {
                            gradleWrapper "jacocoTestReport coveralls"
                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'build/reports/jacoco/test/html',
                                reportFiles: 'index.html',
                                reportName: 'Coverage',
                                reportTitles: ''
                            ])
                        }

                        always {
                            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'

                        }
                    }
                }

                stage('macOS') {
                    agent {
                        label 'osx&&atlas'
                    }

                    environment {
                        COVERALLS_REPO_TOKEN                = credentials('atlas_github_coveralls_token')
                        TRAVIS_JOB_NUMBER                   = "${BUILD_NUMBER}.MACOS"
                        GITHUB                              = credentials('github_integration')
                        ATLAS_GITHUB_INTEGRATION_USER       = "${GITHUB_USR}"
                        ATLAS_GITHUB_INTEGRATION_PASSWORD   = "${GITHUB_PSW}"
                    }

                    steps {
                        gradleWrapper "check"
                    }

                    post {
                        success {
                            gradleWrapper "jacocoTestReport coveralls"
                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'build/reports/jacoco/test/html',
                                reportFiles: 'index.html',
                                reportName: 'Coverage',
                                reportTitles: ''
                            ])
                        }

                        always {
                            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'

                        }
                    }
                }
            }

            post {
                always {
                    sendSlackNotification currentBuild.result, true
                }
            }
        }
    }
}
