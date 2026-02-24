pipeline {
    agent any

    environment {
        IMAGE_TAG = ''
    }
    stages {

        // ─────────────────────────────────────────────────────────────────
        // 1. Checkout
        // ─────────────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // 커밋 메시지에 [skip ci] 가 있으면 매니페스트 업데이트 커밋이므로 중단
                    def commitMsg = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                    if (commitMsg.contains('[skip ci]')) {
                        currentBuild.result = 'NOT_BUILT'
                        error('Manifest update commit detected. Skipping pipeline.')
                    }

                    def shortHash = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortHash}"
                    echo "Image tag: ${env.IMAGE_TAG}"
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 2. Test (Backend / Frontend 병렬)
        //    Jenkins 에이전트에 Java 21 + Node 20 이상 필요
        // ─────────────────────────────────────────────────────────────────
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

        // ─────────────────────────────────────────────────────────────────
        // 3. Docker Build & Push (Backend / Frontend 병렬)
        //    Jenkins Credentials ID: 'dockerhub-credentials'
        //    (Username / Password 형식으로 등록)
        // ─────────────────────────────────────────────────────────────────
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
                        sh "echo '${DOCKERHUB_PASSWORD}' | docker login -u '${DOCKERHUB_USERNAME}' --password-stdin"
                        sh "docker build -t ${DOCKERHUB_USERNAME}/maplog-backend:${tag} ./map-log-backend"
                        sh "docker push ${DOCKERHUB_USERNAME}/maplog-backend:${tag}"
                        sh "docker rmi ${DOCKERHUB_USERNAME}/maplog-backend:${tag}"
                        sh "docker build -t ${DOCKERHUB_USERNAME}/maplog-frontend:${tag} ./map-log-frontend"
                        sh "docker push ${DOCKERHUB_USERNAME}/maplog-frontend:${tag}"
                        sh "docker rmi ${DOCKERHUB_USERNAME}/maplog-frontend:${tag}"
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 4. Update K8s Manifest (GitOps)
        //    kustomization.yaml 의 이미지명/태그를 업데이트 후 커밋·푸시
        //    ArgoCD 가 변경을 감지해 자동 배포
        //
        //    Jenkins Credentials ID: 'github-credentials'
        //    (GitHub Personal Access Token 을 Password 에 등록)
        // ─────────────────────────────────────────────────────────────────
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
                        // GIT_URL 에서 https:// 제거해 자격증명 삽입용 URL 구성
                        env.REPO_URL_CLEAN = env.GIT_URL.replace('https://', '')
                    }

                    sh """
                        # DockerHub ID 교체 (초기 your-dockerhub-id 플레이스홀더 포함 대응)
                        sed -i '' "s|name: docker.io/[^/]*/maplog-backend|name: docker.io/\${DOCKERHUB_USERNAME}/maplog-backend|g" k8s/kustomization.yaml
                        sed -i '' "s|name: docker.io/[^/]*/maplog-frontend|name: docker.io/\${DOCKERHUB_USERNAME}/maplog-frontend|g" k8s/kustomization.yaml

                        # 이미지 태그 업데이트 (줄바꿈으로 분리)
                        sed -i '' '/name: docker.io.*maplog-backend/{
                        n
                        s/newTag: .*/newTag: '"${IMAGE_TAG}"'/
                        }' k8s/kustomization.yaml
                        sed -i '' '/name: docker.io.*maplog-frontend/{
                        n
                        s/newTag: .*/newTag: '"${IMAGE_TAG}"'/
                        }' k8s/kustomization.yaml

                        git config user.email "jenkins@maplog.local"
                        git config user.name  "Jenkins CI"

                        git remote set-url origin "https://\${GIT_USERNAME}:\${GIT_PASSWORD}@\${REPO_URL_CLEAN}"

                        git add k8s/kustomization.yaml
                        git commit -m "ci: update image tag to \${IMAGE_TAG} [skip ci]"
                        git push origin HEAD:main
                    """
                }
            }
        }

    }

    // ─────────────────────────────────────────────────────────────────────
    // Post
    // ─────────────────────────────────────────────────────────────────────
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
