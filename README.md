# DMRBridge 클라이언트 — 안드로이드

아마추어 무선 DMR 중계 서버 **DMRBridge**에 붙어서 DMR 무전(BrandMeister 등)을 휴대폰으로 듣고 말하는 앱입니다.
큰 글씨와 큰 버튼으로 어르신도 쉽게 쓸 수 있게 만들었습니다.

- 제작자: **6K2KZR 최승훈** · 라이선스: [MIT](LICENSE)
- PC(Windows)판: [dmrbridge-client-pc](https://github.com/seunghunchoi-dev/dmrbridge-client-pc) — 화면과 통신 코드가 같습니다
- 설명서: [Wiki](https://github.com/seunghunchoi-dev/dmrbridge-client-android/wiki)
- 내려받기: [Releases](https://github.com/seunghunchoi-dev/dmrbridge-client-android/releases) (APK)
- 개인정보 처리방침: [PRIVACY.md](PRIVACY.md)

## 할 수 있는 것
- 누르고 말하기 (한 번 눌러 켜고 다시 눌러 끄기, 볼륨 아래 버튼으로 말하기도 가능)
- 상대방 콜사인·이름·지역·무전기 이름표 보기
- 채널 슬롯 — 개수 제한 없음, 즐겨찾기 ★, 그룹, 검색
- 번호로 채널 바꾸기, 타임슬롯 1/2, 브릿지 내부 채널
- 화면이 꺼져도 수신 계속 (알림 표시)
- 내 정보(콜사인·DMR ID·이름·장비명)를 서버로 보내 상대 무전기 이름표에 "콜사인 DMRBridge 장비명"으로 표시

## 쓰려면
- DMRBridge 서버 주소와 내 포트 번호 (서버 운영자에게 받음)
- 송신하려면 아마추어 무선국 허가(콜사인)와 DMR ID
- 안드로이드 8.0 이상

음성 변환(보코더)은 서버가 하므로 이 앱에는 보코더가 들어 있지 않습니다.

## 빌드
- JDK 17, Android SDK (API 36)
- `build_android.bat` (또는 `gradlew :composeApp:assembleRelease :composeApp:bundleRelease`)
- 결과: `composeApp/build/outputs/apk/release/` (APK), `composeApp/build/outputs/bundle/release/` (Google Play용 AAB)
- 서명: 저장소 루트에 `keystore.properties`(storeFile, storePassword, keyAlias, keyPassword)를 두면 서명됩니다. 이 파일과 키는 저장소에 올리지 않습니다.

## 구조
| 폴더 | 내용 |
|---|---|
| `composeApp/src/jvmShared` | PC판과 같은 공통 코드: 서버 통신(USRP), 소리 변환, 상태, 화면(Compose) |
| `composeApp/src/androidMain` | 안드로이드 전용: 마이크·스피커, 백그라운드 서비스, 알림 |
| `docs/PROTOCOL.md` | 클라이언트 ⇄ DMRBridge 통신 규격 |
| `docs/play-store` | Google Play 등록 자료와 체크리스트 |

## 사용한 오픈소스
Kotlin, kotlinx.coroutines, kotlinx.serialization, Compose Multiplatform, AndroidX, Guava listenablefuture — 모두 Apache License 2.0.
아이콘은 제작자가 직접 그렸습니다.

DMRBridge 서버도 같은 제작자가 만들었습니다. BrandMeister는 해당 단체의 이름이며, 이 앱은 그 단체와 관계가 없습니다.
