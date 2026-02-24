pipeline {
    agent any

    // 전역 도구 설정: Jenkins 관리 > Global Tool Configuration에 등록된 이름과 일치해야 합니다.
    tools {
        gradle 'gradle'
        jdk 'jdk-21'
    }

    // 파이프라인에서 사용할 환경 변수 정의
    environment {
        // Jenkins Credentials에 등록된 자격 증명 ID
        DOCKERHUB_CRED_ID = 'DOCKERHUB_PASSWORD'
        GITHUB_CRED_ID = 'ssh-jeckins-github--key'
        
        // Docker Hub 사용자 정보 및 레포지토리 URL
        DOCKERHUB_USER = 'gusgh07'
        SOURCE_REPO_URL = 'https://github.com/gusgh075/be22-4st-team1-project.git'
        MANIFEST_REPO_URL = 'https://github.com/gusgh075/k8s-manifests.git'
        
        // 이미지 이름 설정 (사용자명/이미지명 형식)
        BACKEND_IMAGE = "${DOCKERHUB_USER}/map-log-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/map-log-frontend"
        
        // 빌드 번호를 태그로 사용 (예: 1, 2, 3...)
        TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        // 1단계: 소스 코드 체크아웃
        stage('Checkout') {
            steps {
                // 'github' ID의 자격 증명을 사용하여 메인 브랜치의 소스 코드를 가져옵니다.
                git credentialsId: 'github',
                    url: "${env.SOURCE_REPO_URL}",
                    branch: 'main'
            }
        }

        // 2단계: 빌드 환경 확인
        stage('Preparation') {
            steps {
                script {
                    // 실행 환경(Linux/Unix 또는 Windows)에 따라 도구 버전을 확인합니다.
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

        // 3단계: 백엔드 소스 빌드 (Java/Gradle)
        stage('Backend Source Build') {
            steps {
                script {
                    dir('map-log-backend') {
                        if (isUnix()) {
                            // 실행 권한 부여 및 빌드 수행 (기존 빌드 내역 삭제 후 실행 가능한 Jar 파일 생성)
                            sh "chmod +x ./gradlew"
                            sh "./gradlew clean bootJar"
                        } else {
                            bat "gradlew.bat clean bootJar"
                        }
                    }
                }
            }
        }

        // 4단계: 프론트엔드 소스 빌드 (Vue/Node.js)
        stage('Frontend Source Build') {
            steps {
                script {
                    // Jenkins Credentials에서 API 키를 안전하게 가져와 빌드 시 환경 변수로 주입합니다.
                    withCredentials([string(credentialsId: 'KAKAO_MAP_KEY', variable: 'KAKAO_KEY')]) {
                        dir('map-log-frontend') {
                            if (isUnix()) {
                                sh "npm install"
                                // 빌드 시 백엔드 API 주소와 카카오맵 키를 주입하여 정적 파일 생성
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

        // 5단계: Docker 컨테이너 빌드 및 레지스트리 푸시
        stage('Container Build and Push') {
            steps {
                script {
                    // Docker Hub 로그인 정보를 가져옵니다.
                    withCredentials([usernamePassword(credentialsId: "${env.DOCKERHUB_CRED_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        
                        // 백엔드 이미지 빌드 및 푸시
                        if (isUnix()) {
                            // 빌드 번호 태그와 latest 태그를 동시에 생성하여 푸시합니다.
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

                        // 프론트엔드 이미지 빌드 및 푸시
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

        // 6단계: Kubernetes 배포 설정 파일(Manifest) 업데이트
        stage('Update K8s Manifest') {
            steps {
                // ArgoCD 등이 감시하는 별도의 Manifest 레포지토리를 체크아웃합니다.
                git credentialsId: 'github',
                    url: "${env.MANIFEST_REPO_URL}",
                    branch: 'main'
                
                script { 
                    if (isUnix()) {
                        // deployment.yaml 파일 내의 이미지 태그를 새로 빌드된 버전으로 교체합니다.
                        sh "sed -i 's|${BACKEND_IMAGE}:.*|${BACKEND_IMAGE}:${TAG}|g' deployment.yaml"
                        sh "sed -i 's|${FRONTEND_IMAGE}:.*|${FRONTEND_IMAGE}:${TAG}|g' deployment.yaml"
                        
                        // 변경사항을 커밋하고 Manifest 레포지토리에 푸시하여 배포를 트리거합니다.
                        sh "git config user.name 'Jenkins CI'"
                        sh "git config user.email 'jenkins@example.com'"
                        sh "git add deployment.yaml"
                        sh "git commit -m '[UPDATE] ${TAG} image versioning'"
                        sh "git push -u origin main"
                    } else {
                        // Windows 환경을 위한 PowerShell 기반 태그 교체 및 Git 명령 실행
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

    // 후속 조치: 성공/실패 여부에 따른 알림 전송
    post {
        always {
            script {
                // 사용이 끝난 Docker 로그아웃
                if (isUnix()) {
                    sh 'docker logout'
                } else {
                    bat 'docker logout'
                }
            }
        }
        // 빌드 성공 시 Discord 알림 전송
        success {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                discordSend(
                    description: """
                    **빌드 성공!** :tada:

                    **제목**: ${currentBuild.displayName}
                    **결과**: :white_check_mark: ${currentBuild.currentResult}
                    **실행 시간**: ${currentBuild.duration / 1000}s
                    **링크**: [빌드 결과 보기](${env.BUILD_URL})
                    """,
                    title: "${env.JOB_NAME} 빌드 성공!",
                    webhookURL: "$DISCORD"
                )
            }
        }
        // 빌드 실패 시 Discord 알림 전송
        failure {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                discordSend(
                    description: """
                    **빌드 실패!** :x:

                    **제목**: ${currentBuild.displayName}
                    **결과**: :x: ${currentBuild.currentResult}
                    **실행 시간**: ${currentBuild.duration / 1000}s
                    **링크**: [빌드 결과 보기](${env.BUILD_URL})
                    """,
                    title: "${env.JOB_NAME} 빌드 실패!",
                    webhookURL: "$DISCORD"
                )
            }
        }
    }
}
