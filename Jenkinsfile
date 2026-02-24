pipeline {
    agent any

    // 전역 도구 설정: Jenkins 관리 > Global Tool Configuration에 등록된 이름과 일치해야 합니다.
    tools {
        gradle 'gradle'
        jdk 'openJDK21'
    }

    // 파이프라인에서 사용할 환경 변수 정의
    environment {
        // Jenkins Credentials에 등록된 자격 증명 ID
        DOCKERHUB_CRED_ID = 'DOCKERHUB_PASSWORD'
        // fix(1): 실제 사용하는 credential ID와 일치하도록 수정 ('ssh-jeckins-github--key' → 'github')
        GITHUB_CRED_ID = 'github'

        // Docker Hub 사용자 정보 및 레포지토리 URL
        DOCKERHUB_USER = 'gusgh07'
        SOURCE_REPO_URL = 'https://github.com/20251029-hanhwa-swcamp-22th/be22-4st-team1-project.git'
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
                // fix(1): 하드코딩 제거 — GITHUB_CRED_ID 환경 변수 사용
                git credentialsId: "${env.GITHUB_CRED_ID}",
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
                    } else {
                        bat 'docker --version'
                    }
                }
            }
        }

        // fix(2)(3): 'Backend Source Build', 'Frontend Source Build' 스테이지 제거
        // 각 Dockerfile이 멀티스테이지 빌드로 소스 컴파일을 직접 수행하므로 Jenkins에서의 사전 빌드는 중복입니다.
        // 프론트엔드 카카오맵 API 키는 아래 docker build 의 --build-arg 로 안전하게 전달합니다.

        // 3단계: Docker 컨테이너 빌드 및 레지스트리 푸시
        stage('Container Build and Push') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "${env.DOCKERHUB_CRED_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS'),
                        // fix(3): 프론트엔드 docker build 에 카카오맵 키를 전달하기 위해 이 스테이지에서 credential 획득
                        string(credentialsId: 'KAKAO_MAP_KEY', variable: 'KAKAO_KEY')
                    ]) {
                        // fix(4): isUnix() 이중 체크를 하나의 블록으로 통합, docker login을 push 직전으로 이동
                        if (isUnix()) {
                            sh "docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}"

                            // 백엔드 이미지 빌드 및 푸시
                            sh "docker build -t ${BACKEND_IMAGE}:${TAG} ./map-log-backend"
                            sh "docker tag ${BACKEND_IMAGE}:${TAG} ${BACKEND_IMAGE}:latest"
                            sh "docker push ${BACKEND_IMAGE}:${TAG}"
                            sh "docker push ${BACKEND_IMAGE}:latest"

                            // fix(3): 프론트엔드 이미지 빌드 시 --build-arg 로 카카오맵 API 키 주입
                            sh "docker build --build-arg VITE_KAKAO_MAP_KEY=${KAKAO_KEY} -t ${FRONTEND_IMAGE}:${TAG} ./map-log-frontend"
                            sh "docker tag ${FRONTEND_IMAGE}:${TAG} ${FRONTEND_IMAGE}:latest"
                            sh "docker push ${FRONTEND_IMAGE}:${TAG}"
                            sh "docker push ${FRONTEND_IMAGE}:latest"
                        } else {
                            bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"

                            // 백엔드 이미지 빌드 및 푸시
                            bat "docker build -t ${BACKEND_IMAGE}:${TAG} ./map-log-backend"
                            bat "docker tag ${BACKEND_IMAGE}:${TAG} ${BACKEND_IMAGE}:latest"
                            bat "docker push ${BACKEND_IMAGE}:${TAG}"
                            bat "docker push ${BACKEND_IMAGE}:latest"

                            // fix(3): 프론트엔드 이미지 빌드 시 --build-arg 로 카카오맵 API 키 주입
                            bat "docker build --build-arg VITE_KAKAO_MAP_KEY=%KAKAO_KEY% -t ${FRONTEND_IMAGE}:${TAG} ./map-log-frontend"
                            bat "docker tag ${FRONTEND_IMAGE}:${TAG} ${FRONTEND_IMAGE}:latest"
                            bat "docker push ${FRONTEND_IMAGE}:${TAG}"
                            bat "docker push ${FRONTEND_IMAGE}:latest"
                        }
                    }
                }
            }
        }

        // 4단계: Kubernetes 배포 설정 파일(Manifest) 업데이트
        stage('Update K8s Manifest') {
            steps {
                // fix(5): dir() 블록으로 서브디렉토리에 클론하여 소스 워크스페이스를 덮어쓰지 않도록 수정
                dir('k8s-manifests') {
                    // fix(1): GITHUB_CRED_ID 환경 변수 사용
                    git credentialsId: "${env.GITHUB_CRED_ID}",
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
                            sh "git push origin main"
                        } else {
                            // Windows 환경을 위한 PowerShell 기반 태그 교체 및 Git 명령 실행
                            bat "powershell -Command \"(Get-Content deployment.yaml) -replace '${BACKEND_IMAGE}:.*', '${BACKEND_IMAGE}:${TAG}' | Set-Content deployment.yaml\""
                            bat "powershell -Command \"(Get-Content deployment.yaml) -replace '${FRONTEND_IMAGE}:.*', '${FRONTEND_IMAGE}:${TAG}' | Set-Content deployment.yaml\""
                            bat "git config user.name \"Jenkins CI\""
                            bat "git config user.email \"jenkins@example.com\""
                            bat "git add deployment.yaml"
                            bat "git commit -m \"[UPDATE] ${TAG} image versioning\""
                            bat "git push origin main"
                        }
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
