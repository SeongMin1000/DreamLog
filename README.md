<p align="center">
  <img src="dreamlog_logo.png" alt="DreamLog Logo" width="200"/>
</p>

<p align="center"><i>당신의 꿈을 기록하고, AI와 함께 분석하며 내면을 탐험하는 안드로이드 앱</i></p>

---

DreamLog는 사용자가 간밤의 꿈을 기록하고, AI 기술을 통해 꿈의 의미를 해석하며 관련된 이미지를 생성해주는 감성적인 일기 앱입니다.  
사용자는 자신의 감정을 **셀카로 촬영**하여 기록하고, 꿈의 내용을 입력하면 **GPT 기반 해석과 이미지 생성**을 통해 **심리학적 통찰**을 제공합니다.

---

## ✨ 주요 기능

- 🔐 **로그인 및 회원가입**  
  Firebase Authentication을 이용한 사용자 인증 시스템

- 📝 **꿈 기록 및 관리**  
  꿈의 내용을 텍스트로 입력하고 저장, 목록에서 관리 (생성 / 조회 / 삭제)

- 🧠 **AI 꿈 분석**  
  OpenAI의 GPT-3.5-turbo 모델로 꿈 내용을 심리학적으로 해석

- 😊 **감정 분석 (이미지 기반)**  
  TensorFlow Lite 모델로 셀카 사진에서 감정 상태 추출

- 🎨 **AI 이미지 생성**  
  DALL·E API를 통해 꿈과 감정에 맞춘 이미지 자동 생성

- 👤 **프로필 관리**  
  사용자 이름, 연락처, 한 줄 소개, 프로필 이미지 수정 가능

- 📅 **캘린더 연동**  
  해석된 꿈을 스마트폰 캘린더 일정으로 자동 추가 가능

---

## 🖼️ 화면 구성

| 화면 | 설명 |
|------|------|
| **로그인/회원가입** | 앱의 시작점. Firebase 기반 인증 처리 |
| **메인 화면** | 기록된 꿈 목록을 최신순으로 표시 |
| **꿈 작성 화면** | 꿈 내용 입력 + 셀카 감정 분석 후 AI 요청 |
| **결과 화면** | GPT 해석 + DALL·E 이미지 시각화 결과 |
| **프로필 화면** | 사용자 정보 및 프로필 이미지 확인/수정 |

---

## 🛠️ 기술 스택 및 라이브러리

### 💬 언어 & 아키텍처
- Kotlin
- MVVM (Model-View-ViewModel)

### ☁️ 백엔드 (Firebase)
- Firebase Authentication (사용자 인증)
- Firebase Firestore (꿈 및 사용자 정보 저장)
- Firebase Storage (프로필 이미지 저장)

### 🤖 AI / 머신러닝
- OpenAI GPT-3.5 Turbo (꿈 해석)
- OpenAI DALL·E (이미지 생성)
- TensorFlow Lite (얼굴 감정 분석)

### 🌐 네트워킹 & 비동기
- Retrofit2, OkHttp3 (REST API 통신)
- Coroutines, WorkManager (비동기 처리)

### 🎨 UI 구성
- Android XML 기반 View
- ViewBinding & DataBinding
- Glide (이미지 로딩 및 캐싱)

---

## 📂 프로젝트 구조 
```
dreamlog/                         
└── app/                                                 
    ├── google-services.json           # Firebase 연동을 위한 설정 파일
    └── src/
        └── main/                     
            ├── AndroidManifest.xml    # 앱 권한, 컴포넌트 정의
            ├── assets/                # TFLite 모델 저장 폴더
            ├── java/com/example/dreamlog/
            │   ├── adapter/           # RecyclerView, Toolbar 등의 어댑터 클래스
            │   ├── api/               # GPT, 이미지 생성 등 외부 API 통신 인터페이스
            │   ├── model/             # 데이터 모델 클래스 (예: Dream, User)
            │   ├── util/              # 공통 유틸리티 클래스
            │   │   └── camera/        # 카메라 기능 관련 헬퍼 클래스
            │   └── viewmodel/         # 각 Activity 정의 클래스 
            └── res/                  
                ├── layout/            # UI 레이아웃 XML 파일
                ├── menu/              # Drawer 메뉴 설정 파일
                ├── values/            # 문자열, 색상, 스타일 등의 일반 리소스 정의
                ├── values-night/      # 다크 모드 전용 리소스 정의
                └── mipmap/            # 앱 아이콘 설정
```

