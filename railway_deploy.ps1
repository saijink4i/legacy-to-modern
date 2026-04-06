# Railway Deploy Helper Script
Write-Host "Railway 클라우드 배포를 시작합니다."

Write-Host "1. Railway CLI 도구 설치 확인..."
npm install -g @railway/cli

Write-Host "2. Railway 서비스에 로그인합니다. 브라우저가 열리면 인증해주세요."
railway login

Write-Host "3. 현재 폴더 코드를 프로젝트에 연결하고 배포 환경을 초기화합니다."
railway init

Write-Host "4. 프로젝트를 백엔드 컨테이너 서버로 최종 릴리스(업로드)합니다."
railway up
