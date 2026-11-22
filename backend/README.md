# 클래스팟: 현재 화면의 Firebase 백엔드

## 구현 범위

현재 프로젝트의 XML/Activity 화면을 유지한다. 기획서에 적힌 Compose 전환은 하지 않았다.

| 기존 화면 | 연결한 기능 |
| --- | --- |
| 로그인/회원가입 | 이메일 로그인, 회원가입 글씨 → 전용 가입 화면(이메일/비밀번호/비밀번호 확인) → Firebase Auth 계정 생성 → 홈에서 프로필 저장, 비밀번호 재설정, 로그인 유지 |
| 홈 | Firestore 학원 조회, 키워드/카테고리 검색, 가격/평점/반경 필터, 이름/가격/평점/후기 수/거리 정렬, 현재 위치 1회 조회, 실제 학원 카드, 목록 이동 |
| 학원 목록 | 실시간 조회, 카테고리 필터, 결과 개수, 로딩/빈 목록/오류/캐시 구분, 제목을 눌러 재시도 |

홈 레이아웃에는 `android:id`만 추가했다. 로그인/목록/카드 레이아웃과 AcademyAdapter의 표시 코드는 유지했다.
검색/필터/상세 버튼은 기본 다이얼로그로 연결했다. 프론트 담당자가 전용 화면을 구현할 때 교체하면 된다.
임시 `connection_test/ping` 요청은 로그인 기능으로 대체했다.

## 실제 Firebase 연결 상태 (2026-11-22 확인)

- 앱의 `app/google-services.json`은 실제 프로젝트 `mp-project-d71c8`를 가리킨다.
- `app/build.gradle.kts`에는 Google Services 플러그인과 Firebase Auth/Firestore SDK가 연결되어 있다. 실제 서버 사용을 위해 Gradle 의존성을 추가할 필요는 없다.
- Android Studio의 Build Variants에서 `debug`를 선택하면 실제 Firebase에 연결한다. `local`은 `demo-classpot` 에뮬레이터 전용이므로 실제 서버 확인에 사용하지 않는다.
- 실제 Android 기기에서도 `debug` 앱을 설치해 확인할 수 있다. Firebase 서버 이용에는 Android 에뮬레이터나 ADB 포트 전달이 필요하지 않다.
- Firebase CLI 로그인과 대상 프로젝트 접근 권한을 확인했다.
- 실제 Firestore `(default)`는 서울 `asia-northeast3` 리전에 있으며 `firestore.rules` 배포를 완료했다. 배포 후 서버 규칙을 다시 읽어 반영을 확인했다.
- 실제 `academies`에 샘플 130개를 등록했다. 재실행 결과는 추가 0개/기존 130개 유지이며, 데이터 검사 4개도 통과했다.
- 기존 공개 `connection_test/ping` 규칙은 제거되었다. 현재 학원 조회에는 로그인이 필요하다.
- Authentication 설정 조회/수정은 `404 CONFIGURATION_NOT_FOUND`를 반환했다. 콘솔에서 최초 설정과 이메일/비밀번호 활성화가 남아 있다. 브라우저 제어 승인 시간 만료로 콘솔 설정은 완료하지 못했다.
- 실제 사용자 회원가입/로그인, 본인 프로필 저장, 인증된 학원 조회는 Authentication 활성화 이후 검증해야 한다. Android 실행 검사는 이번에 수행하지 않았다.

### 남은 작업: Authentication 최초 설정

[Firebase Authentication 콘솔](https://console.firebase.google.com/project/mp-project-d71c8/authentication)에서 시작하기를 누르고 Sign-in method의 이메일/비밀번호를 활성화한다. 이메일 링크 로그인은 필요하지 않다.

프로젝트 루트 터미널에서 다음 명령으로 프로젝트 접근 권한이 있는 Google 계정에 로그인한다. 인증 코드와 서비스 계정 키는 채팅이나 저장소에 붙여 넣지 않는다.

```powershell
& .\backend\node_modules\.bin\firebase.cmd login
& .\backend\node_modules\.bin\firebase.cmd projects:list
```

현재 PC는 CLI 로그인이 완료되어 있다. `backend` 폴더에서 실행한다면 위 경로 대신 `.\node_modules\.bin\firebase.cmd`를 사용한다.

## 새 환경 설정 및 재배포

1. Firebase 프로젝트 `mp-project-d71c8`에서 Authentication → Sign-in method → 이메일/비밀번호를 활성화한다.
2. Firestore 기본 데이터베이스 `(default)`를 생성하고 Cloud Firestore API를 활성화한다.
3. 아래 규칙 배포와 샘플 데이터 등록을 수행한다. 앱은 데이터를 자동 업로드하지 않는다.

```powershell
cd backend
npm ci
npx firebase login
npx firebase deploy --config ../firebase.json --project mp-project-d71c8 --only "firestore:rules,firestore:indexes"
```

배포 대상 규칙은 저장소 루트의 `firestore.rules`다. 기존 콘솔의 다른 기능 규칙이 있다면 함께 검토하여 병합한다.
이 규칙은 학원 목록을 로그인 사용자에게만 공개하며, 학원 원본 변경은 Admin SDK에 한정한다.
사용자 프로필은 본인만 조회/생성/갱신할 수 있고, 다른 사용자의 프로필 조회나 `role` 추가는 거부한다.
테스트용 공개 ping 규칙은 포함하지 않는다. 아직 구현하지 않은 컬렉션은 닫혀 있다.

### 샘플 학원 등록

기존 `app/src/main/assets/academies.json`의 130개 데이터를 사용한다. 문법 오류 한 줄을 수정했고,
샘플 평점/이미지/학원 정보는 검증된 실운영 데이터가 아니다. `isSample: true`로 표시한다.
평점은 기존 샘플 값이며 실제 후기 집계가 아니다. `reviewCount`는 근거 없는 수치를 만들지 않고 0으로 저장한다.

먼저 쓰기 없이 데이터를 검증한다.

```powershell
npm run seed:preview
```

이번 등록에는 로그인한 Firebase CLI 계정의 권한을 사용하는 REST 경로를 사용했다. `backend` 폴더에서 실행한다.

```powershell
node scripts/seed-academies.mjs --project mp-project-d71c8 --write --firebase-cli
```

이 옵션은 설치된 `firebase-tools`의 내부 인증 모듈을 사용하므로 CLI 버전 변경 시 확인이 필요하다. 토큰은 파일이나 출력에 저장하지 않는다. 기존 문서는 덮어쓰지 않고 생성 시각은 서버에서 기록한다.

`--firebase-cli`를 생략하는 기본 경로에는 Admin SDK용 Application Default Credentials가 필요하다. Firebase CLI 로그인과 별개다.
예: 관리자가 제공한 서비스 계정 JSON을 저장소 밖에 보관하고 경로만 환경 변수로 지정한다.
키를 앱/Firestore/깃에 넣지 않는다.

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = 'C:\secure\classpot-service-account.json'
node scripts/seed-academies.mjs --project mp-project-d71c8 --write
```

문서 ID는 이름+주소의 SHA-256 일부로 고정된다. 재실행해도 중복 생성하거나 기존 데이터를 덮어쓰지 않는다.
중간에 실패해도 재실행할 수 있다. 등록은 관리자 도구로만 수행하며 앱 쓰기 권한을 열 필요가 없다.

## 데이터 계약

`academies/{academyId}`:

| 필드 | 타입 / 의미 |
| --- | --- |
| name, category, address, curriculum | 필수 문자열 |
| latitude, longitude | 유효한 위경도 숫자 |
| tuition | 0 이상의 정수, 월 수강료 원 단위 |
| rating | 0~5 숫자 |
| price, image | 가격 표시 문자열, 이미지 URL |
| reviewCount | 0 이상의 정수 (미입력 시 0) |
| isSample | 샘플 여부, 등록 도구가 true로 저장 |
| createdAt | 서버 타임스탬프 |

카테고리: `입시교육`, `어학`, `예체능`, `자격증`, `원데이클래스`. 화면의 `교육`은 `입시교육`으로 정규화한다.
AcademyMapper는 기존 JSON의 `가격`/`이미지` 키도 호환한다. 잘못된 필수 데이터는 조용히 누락하지 않고 오류로 표시한다.

`users/{Firebase UID}`: `email`, `displayName`, `createdAt`, `updatedAt`.
비밀번호는 Firebase Authentication만 관리한다. Firestore에 저장하지 않는다.
계정 생성과 프로필 저장은 서로 다른 작업이다. 프로필 저장 실패를 회원가입 실패로 오인하지 않도록 로그인은 유지하고 홈에서 오류를 알린다.
프로필 동기화는 이후 홈 화면을 새로 열 때 다시 실행한다. 보안 역할/학원 승인 정보는 사용자 프로필에 넣지 않는다.

## 프론트 담당자에게 넘길 인터페이스

- `data/AuthRepository`: `signIn`, `signUp`, `resetPassword`, `signOut`, `currentUser`, `syncProfile`.
- `data/AuthViewModel`: 회전 중 요청 상태 유지, 중복 요청 방지, 인증 결과.
- `data/AcademyRepository.observe`: 학원 목록과 캐시 여부. 반환한 ListenerRegistration을 종료 시 제거한다.
- `data/AcademyViewModel`: `state` 관찰, `updateSearch`, `start`/`stop`/`retry`.
- `data/AcademySearch`: 화면 의존성이 없는 키워드/필터/정렬/거리 계산.
- `data/CurrentLocation`: 포그라운드에서 위치 1회 조회, 15초 제한 및 화면 종료 시 취소.
- `ui/AuthenticatedActivity`: 인증된 화면의 공통 세션 검사.

위치 권한은 홈의 위치 검색 버튼을 누를 때만 요청한다. 거리는 마지막으로 설정한 기준 위치에서 계산한다.
위치를 얻지 못하면 가짜 좌표를 넣지 않는다. 권한 없이도 다른 검색 기능은 사용할 수 있다.
검색 조건과 기준 위치는 `academy_search` SharedPreferences에 저장된다. 필터 초기화는 위치 외 조건을 초기화한다.
`HomeActivity`와 `AcademyListActivity`가 같은 조건을 읽으며 뒤로 돌아오면 최신 조건이 반영된다.

현재 지역 샘플 130개를 Firestore에서 읽고 클라이언트 데이터 계층에서 필터링한다.
전국 규모 데이터에는 페이지네이션/지역 질의/geohash 인덱스를 추가해야 한다.
캐시 데이터를 서버 성공으로 표시하지 않으며, 권한 오류를 로컬 JSON 성공으로 숨기지 않는다.

## 아직 구현하지 않은 화면/외부 서비스

- Google/카카오 로그인: 기획서의 이메일 인증 범위 밖이며 OAuth 클라이언트/카카오 앱 설정이 없다. 버튼은 준비 중 안내를 표시한다.
- 이메일 찾기: 신원 확인 수단이 없으므로 다른 사용자의 이메일을 조회하는 기능을 만들지 않았다.
- 지도: 기존 자리표시자 유지. 지도 SDK/마커/지도 이동 범위 검색은 프론트 후속 작업이다. 현재 위치 버튼은 기기 위치로 검색 기준만 갱신한다.
- 이미지 로딩: URL은 제공하지만 기존 어댑터/이미지 레이아웃은 변경하지 않았다.
- 상세 전용 화면, 로그아웃 버튼/마이페이지, 즐겨찾기, 수강 승인, 인증 후기, 강좌, AI 비교는 아직 화면이 없다.
- Spring Boot/OpenAI 서버는 현재 화면에서 호출하지 않으므로 이번 범위에 추가하지 않았다. 이후 Firebase ID 토큰 검증, 학원 소유권, 승인 이력 검증을 서버에서 구현한다. OpenAI 키는 서버 환경 변수로만 사용한다.

## 검증 명령

### Running Devices에서 직접 조작하기

Pixel 8을 켠 후 프로젝트 루트의 PowerShell에서 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File backend/scripts/run-local.ps1
```

이 명령은 필요한 로컬 Firebase 서버를 시작하고, 샘플을 등록한 뒤 `local` 빌드를 설치하여 로그인 화면을 연다.
서버는 수동 테스트 동안 계속 실행된다. 새 PC에서는 먼저 `npm.cmd --prefix backend ci`를 실행한다.
서버를 새로 시작했을 때 로그는 `tmp/local-firebase`에 저장되며, 이미 실행 중인 서버가 있으면 재사용한다.

Android Studio의 View → Tool Windows → Running Devices → Pixel 8에서 직접 조작한다.
로그인 화면 하단 회원가입을 누르면 별도 가입 화면이 열린다. 그 화면에서 테스트 이메일(예: `visual-test@example.com`), 6자 이상의 비밀번호와 비밀번호 확인을 입력한 뒤 가입하기를 누른다.
이미 같은 로컬 이메일로 가입했다면 로그인 버튼을 누른다. 가입 성공 후 홈에서 목록 보기, 카테고리, 검색/필터를 확인한다.

앱 이름은 `클래스팟 로컬`, 패키지는 `com.example.mp2026.local`이다. 실제 Firebase용 앱과 함께 설치할 수 있다.
`app/src/local/google-services.json`은 실서비스 키가 없는 `demo-classpot` 전용 설정이다.
`LocalFirebaseApplication`은 로컬 빌드에만 포함되며 Auth/Firestore 모두 `127.0.0.1`의 ADB 전달 포트에 연결한다.
로컬 앱은 새 프로세스를 시작할 때 로그인 화면부터 표시한다. 로컬 서버 재시작 시 계정/데이터는 초기화될 수 있다.

Android Studio의 일반 Run으로 이 앱을 다시 실행하려면 Gradle Sync 후 Build Variants에서 `app`을 `local`로 선택한다.
PC/에뮬레이터를 재시작했다면 위 스크립트로 서버와 ADB 포트 전달을 다시 준비한다.
`debug`/`release`는 기존 실제 Firebase 설정을 사용한다. 지도는 아직 자리표시자이며 이번 변경에서 지도 SDK를 추가하지 않았다.

### 자동 검사

프로젝트 루트에서:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

`backend` 폴더에서:

```powershell
npm test
npm run test:rules
```

규칙 테스트는 실제 프로젝트 대신 `demo-classpot` 로컬 에뮬레이터에서 실행한다. Node 22 이상 및 Java 21 이상이 필요하다.

Android 통합 테스트는 Android 에뮬레이터와 로컬 Auth/Firestore 에뮬레이터를 켠 후 실행한다.

```powershell
# backend 폴더, 별도 터미널
npx firebase emulators:start --config ../firebase.json --project demo-classpot --only "auth,firestore"

# backend 폴더, 다른 터미널: 로컬 데이터 등록
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
node scripts/seed-academies.mjs --project demo-classpot --write

# 프로젝트 루트: Android APK 빌드/설치, ADB 포트 전달, 테스트 1개 실행 확인
powershell -File backend/scripts/test-android.ps1
```

통합 테스트는 명시적으로 활성화해야 실행되고, 테스트 프로세스에서만 Firebase 기본 앱을 가짜 프로젝트로 교체한다.
스크립트는 기본 `emulator-5554`를 사용한다. 다른 기기는 `-Serial` 인자로 지정한다.
SDK 경로가 다르면 `-AdbPath` 인자로 adb.exe 경로를 지정한다.
Windows에서 호스트 연결 지연을 피하도록 ADB reverse와 `emulatorHost=127.0.0.1` 인자를 사용한다.
테스트 코드의 기본값 `10.0.2.2`는 Android 에뮬레이터 전용 호스트 주소다.
현재 Gradle/기기 조합에서 connectedDebugAndroidTest가 성공하면서도 테스트 0개를 보고해,
실제로 테스트 1개가 실행됐는지 검사하는 위 스크립트를 제공한다.
debug 빌드만 로컬 HTTP를 허용하며 release 설정에는 적용하지 않는다.

### 기존 로컬 검증 기록 (빌드·규칙·Android 통합 검사는 이번에 재실행하지 않음)

- 디버그 APK와 Android 테스트 APK 빌드 성공, `lintDebug` 오류 없음.
- 신규 Kotlin 테스트 13개 통과 (검색/거리/정렬/데이터 변환).
- 샘플 데이터 검사 4개, Firestore 보안 규칙 테스트 5개 통과.
- 로컬 샘플 등록: 첫 실행 130개 추가, 재실행 0개 추가/130개 유지.
- Pixel 8 Android 17에서 Android 통합 테스트 1개 통과: 회원가입 → 프로필 생성/재동기화 → 서버 학원 130개 조회 → 홈 화면 생성 → 로그아웃/재로그인.
- 위 항목은 기존 로컬 검사 기록이다. 실제 Firebase 배포 및 등록 결과는 문서 상단의 최신 연결 상태를 참고한다.
