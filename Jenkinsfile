pipeline {
    agent any

//     environment {
//         IMAGE_TAG = ''
//     }
    stage('Checkout') {
        steps {
            checkout scm
            script {
                def commitMsg = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                if (commitMsg.contains('[skip ci]')) {
                    currentBuild.result = 'NOT_BUILT'
                    error('Manifest update commit detected. Skipping pipeline.')
                }

                def shortHash = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

                // BUILD_NUMBER 확인
                echo "BUILD_NUMBER from env: ${env.BUILD_NUMBER}"
                echo "BUILD_NUMBER from currentBuild: ${currentBuild.number}"

                def buildNum = currentBuild.number.toString()  // ← env 대신 currentBuild 사용
                env.IMAGE_TAG = "${buildNum}-${shortHash}"
                echo "Image tag: ${env.IMAGE_TAG}"
            }
        }
    }

        stage('Test') {
            parallel {
                stage('Backend Test') {
                    steps {
                        dir('map-log-backend') {
                            sh 'chmod +x gradlew'
                            sh './gradlew test --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit testResults: 'map-log-backend/build/test-results/**/*.xml',
                                  allowEmptyResults: true
                        }
                    }
                }
                stage('Frontend Test') {
                    steps {
                        dir('map-log-frontend') {
                            sh 'npm ci'
                            sh 'unset NODE_OPTIONS && npm test'
                        }
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USERNAME',
                        passwordVariable: 'DOCKERHUB_PASSWORD'
                    )
                ]) {
                    script {
                        def tag = env.IMAGE_TAG
                        def username = env.DOCKERHUB_USERNAME
                        def password = env.DOCKERHUB_PASSWORD
                        sh "echo '${password}' | docker login -u '${username}' --password-stdin"
                        sh "docker build -t ${username}/maplog-backend:${tag} ./map-log-backend"
                        sh "docker push ${username}/maplog-backend:${tag}"
                        sh "docker rmi ${username}/maplog-backend:${tag}"
                        sh "docker build -t ${username}/maplog-frontend:${tag} ./map-log-frontend"
                        sh "docker push ${username}/maplog-frontend:${tag}"
                        sh "docker rmi ${username}/maplog-frontend:${tag}"
                    }
                }
            }
        }

        stage('Update K8s Manifest') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USERNAME',
                        passwordVariable: 'DOCKERHUB_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'github-credentials',
                        usernameVariable: 'GIT_USERNAME',
                        passwordVariable: 'GIT_PASSWORD'
                    )
                ]) {
                    script {
                        env.REPO_URL_CLEAN = env.GIT_URL.replace('https://', '')
                        def tag = env.IMAGE_TAG
                        def username = env.DOCKERHUB_USERNAME

                        if (!tag) {
                            error("IMAGE_TAG is not set. Cannot update K8s manifest.")
                        }

                        sh """
                            sed -i '' "s|name: docker.io/[^/]*/maplog-backend|name: docker.io/${username}/maplog-backend|g" k8s/kustomization.yaml
                            sed -i '' "s|name: docker.io/[^/]*/maplog-frontend|name: docker.io/${username}/maplog-frontend|g" k8s/kustomization.yaml
                            sed -i '' '/name: docker.io.*maplog-backend/,/newTag:/{s/newTag: .*/newTag: ${tag}/}' k8s/kustomization.yaml
                            sed -i '' '/name: docker.io.*maplog-frontend/,/newTag:/{s/newTag: .*/newTag: ${tag}/}' k8s/kustomization.yaml
                            git config user.email "jenkins@maplog.local"
                            git config user.name  "Jenkins CI"
                            git remote set-url origin "https://\${GIT_USERNAME}:\${GIT_PASSWORD}@\${REPO_URL_CLEAN}"
                            git add k8s/kustomization.yaml
                            git diff --staged --quiet || (git commit -m "ci: update image tag to ${tag} [skip ci]" && git push origin HEAD:main)
                        """
                    }
                }
            }
        }

    }

    post {
        always {
            sh 'docker logout || true'
            cleanWs()
        }
        success {
            echo "Pipeline succeeded. Deployed image tag: ${env.IMAGE_TAG}"
        }
        failure {
            echo "Pipeline failed."
        }
    }
}