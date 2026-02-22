pipeline {
    agent any

    tools {
        // Jenkins 관리 > Global Tool Configuration에 등록된 이름과 일치해야 합니다.
        gradle 'gradle'
        jdk 'jdk-21'
    }

    environment {
        // 실제 등록된 Credential ID로 수정
        DOCKERHUB_CRED_ID = 'DOCKERHUB_PASSWORD'
        GITHUB_CRED_ID = 'ssh-jeckins-github--key'
        
        DOCKERHUB_USER = 'gusgh07'
        SOURCE_REPO_URL = 'https://github.com/gusgh075/be22-4st-team1-project.git'
        MANIFEST_REPO_URL = 'https://github.com/gusgh075/k8s-manifests.git'
        
        // 이미지 이름 및 태그 설정
        BACKEND_IMAGE = "${DOCKERHUB_USER}/map-log-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/map-log-frontend"
        TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                // 'github' 자격 증명과 소스 레포지토리 주소를 명시적으로 사용하여 체크아웃합니다.
                git credentialsId: 'github',
                    url: "${env.SOURCE_REPO_URL}",
                    branch: 'main'
            }
        }

        stage('Preparation') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker --version'
                        sh 'node --version'
                    } else {
                        bat 'docker --version'
                        bat 'node --version'
                    }
                }
            }
        }

        stage('Backend Source Build') {
            steps {
                script {
                    dir('map-log-backend') {
                        if (isUnix()) {
                            sh "chmod +x ./gradlew"
                            sh "./gradlew clean bootJar"
                        } else {
                            bat "gradlew.bat clean bootJar"
                        }
                    }
                }
            }
        }

        stage('Frontend Source Build') {
            steps {
                script {
                    // Jenkins Credentials에서 KAKAO_MAP_KEY를 가져와 빌드 시 주입
                    withCredentials([string(credentialsId: 'KAKAO_MAP_KEY', variable: 'KAKAO_KEY')]) {
                        dir('map-log-frontend') {
                            if (isUnix()) {
                                sh "npm install"
                                sh "VITE_API_BASE_URL=/api VITE_KAKAO_MAP_KEY=${KAKAO_KEY} npm run build"
                            } else {
                                bat "npm install"
                                bat "set VITE_API_BASE_URL=/api && set VITE_KAKAO_MAP_KEY=${KAKAO_KEY} && npm run build"
                            }
                        }
                    }
                }
            }
        }

        stage('Container Build and Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${env.DOCKERHUB_CRED_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        // Backend Image Build & Push
                        if (isUnix()) {
                            sh "docker build -t ${BACKEND_IMAGE}:${TAG} ./map-log-backend"
                            sh "docker tag ${BACKEND_IMAGE}:${TAG} ${BACKEND_IMAGE}:latest"
                            sh "docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}"
                            sh "docker push ${BACKEND_IMAGE}:${TAG}"
                            sh "docker push ${BACKEND_IMAGE}:latest"
                        } else {
                            bat "docker build -t ${BACKEND_IMAGE}:${TAG} ./map-log-backend"
                            bat "docker tag ${BACKEND_IMAGE}:${TAG} ${BACKEND_IMAGE}:latest"
                            bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                            bat "docker push ${BACKEND_IMAGE}:${TAG}"
                            bat "docker push ${BACKEND_IMAGE}:latest"
                        }

                        // Frontend Image Build & Push
                        if (isUnix()) {
                            sh "docker build -t ${FRONTEND_IMAGE}:${TAG} ./map-log-frontend"
                            sh "docker tag ${FRONTEND_IMAGE}:${TAG} ${FRONTEND_IMAGE}:latest"
                            sh "docker push ${FRONTEND_IMAGE}:${TAG}"
                            sh "docker push ${FRONTEND_IMAGE}:latest"
                        } else {
                            bat "docker build -t ${FRONTEND_IMAGE}:${TAG} ./map-log-frontend"
                            bat "docker tag ${FRONTEND_IMAGE}:${TAG} ${FRONTEND_IMAGE}:latest"
                            bat "docker push ${FRONTEND_IMAGE}:${TAG}"
                            bat "docker push ${FRONTEND_IMAGE}:latest"
                        }
                    }
                }
            }
        }

        stage('Update K8s Manifest') {
            steps {
                // 신규 발급받은 'github' 자격 증명(PAT)을 사용하여 HTTPS 주소로 클론합니다.
                git credentialsId: 'github',
                    url: "${env.MANIFEST_REPO_URL}",
                    branch: 'main'
                
                script { 
                    if (isUnix()) {
                        // Unix 시스템에서 deployment.yaml 파일 수정 후 commit 후 push
                        sh "sed -i 's|${BACKEND_IMAGE}:.*|${BACKEND_IMAGE}:${TAG}|g' deployment.yaml"
                        sh "sed -i 's|${FRONTEND_IMAGE}:.*|${FRONTEND_IMAGE}:${TAG}|g' deployment.yaml"
                        sh "git config user.name 'Jenkins CI'"
                        sh "git config user.email 'jenkins@example.com'"
                        sh "git add deployment.yaml"
                        sh "git commit -m '[UPDATE] ${TAG} image versioning'"
                        sh "git push -u origin main"
                    } else {
                        // Windows 시스템에서 deployment.yaml 파일 수정 후 commit 후 push
                        bat "powershell -Command \"(Get-Content deployment.yaml) -replace '${BACKEND_IMAGE}:.*', '${BACKEND_IMAGE}:${TAG}' | Set-Content deployment.yaml\""
                        bat "powershell -Command \"(Get-Content deployment.yaml) -replace '${FRONTEND_IMAGE}:.*', '${FRONTEND_IMAGE}:${TAG}' | Set-Content deployment.yaml\""
                        bat "git config user.name \"Jenkins CI\""
                        bat "git config user.email \"jenkins@example.com\""
                        bat "git add deployment.yaml"
                        bat "git commit -m \"[UPDATE] ${TAG} image versioning\""
                        bat "git push -u origin main"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (isUnix()) {
                    sh 'docker logout'
                } else {
                    bat 'docker logout'
                }
            }
        }
        success {
            echo 'Map-Log Pipeline succeeded!'
        }
        failure {
            echo 'Map-Log Pipeline failed!'
        }
    }
}
