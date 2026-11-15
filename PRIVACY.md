# 개인정보 처리방침 — DVBridge 클라이언트

시행일: 2026년 11월 15일 · 제작자: 6K2KZR 최승훈

DVBridge 클라이언트(이하 "앱")는 아마추어 무선 DMR 중계 서버인 **DVBridge**에 접속해 음성을 주고받는 앱입니다.
앱 제작자는 어떤 서버도 운영하지 않으며, 앱이 제작자에게 정보를 보내는 기능은 없습니다.

## 1. 앱이 다루는 정보

| 정보 | 어디에 쓰나 | 어디로 가나 |
|---|---|---|
| 서버 주소·포트 | 사용자가 고른 DVBridge 서버에 접속 | 기기 안에만 저장 |
| 콜사인, DMR ID, 이름, 장비명(사용자가 직접 입력, 비워 둘 수 있음) | 접속한 서버와 상대 무전국에 "누가 쓰는지" 표시 | 사용자가 접속한 DVBridge 서버 |
| 마이크 음성 | **말하기 버튼을 누르고 있는 동안에만** 녹음해 실시간 송신 | 사용자가 접속한 DVBridge 서버 (앱은 음성을 저장하지 않음) |
| 기기 모델명(예: Galaxy A17) | 장비명을 비워 두었을 때 기본값 | 사용자가 접속한 DVBridge 서버 |

- 앱은 위 정보를 **사용자가 직접 지정한 DVBridge 서버 외에는 어디에도 보내지 않습니다.**
- 광고, 분석 도구, 위치 정보, 연락처, 사진, 계정 로그인 기능이 없습니다.
- 서버와의 통신은 아마추어 무선 표준 방식(USRP, UDP)이며 암호화되지 않습니다. 아마추어 무선 통신은 법적으로 공개 통신입니다.
- DVBridge 서버를 거쳐 BrandMeister 등 공개 DMR 네트워크로 나간 음성과 콜사인은 그 네트워크의 정책에 따라 다른 무전국이 듣거나 기록할 수 있습니다.

## 2. 권한

- **마이크(RECORD_AUDIO)**: 말하기 버튼을 누를 때 음성을 보내기 위해
- **알림(POST_NOTIFICATIONS)·포그라운드 서비스(마이크·미디어 재생)**: 화면이 꺼져도 수신을 계속하고, 그 사실을 알림으로 보여 주기 위해
- **인터넷·네트워크 상태·화면 켜짐 유지(WAKE_LOCK)**: 서버 접속 유지

## 3. 보관과 삭제

- 설정은 기기 안에만 저장되며 앱을 지우면 함께 지워집니다. 앱 백업 기능은 꺼져 있습니다.
- 서버에 보낸 정보의 보관은 그 **DVBridge 서버 운영자**의 방침을 따릅니다.

## 4. 아동

이 앱은 아마추어 무선 자격을 가진 사람을 위한 앱이며 만 13세 미만 아동을 대상으로 하지 않습니다.

## 5. 문의

GitHub 저장소의 Issues: https://github.com/seunghunchoi-dev/dvbridge-client-android/issues

---

# Privacy Policy — DVBridge Client

Effective: 15 November 2026 · Author: 6K2KZR Choi Seunghun

DVBridge Client ("the app") connects to a **DVBridge** amateur-radio DMR bridge server chosen by the user.
The author does not operate any server, and the app never sends data to the author.

- **Data handled**: server address/port (stored on the device only); callsign, DMR ID, name and device name entered by the user (optional) and the device model — sent only to the DVBridge server the user connects to, so that the server and other stations can see who is talking; microphone audio — captured **only while the push-to-talk button is held** and streamed live to that server, never stored by the app.
- No ads, analytics, location, contacts, photos or account sign-in.
- Traffic uses the amateur-radio USRP protocol over UDP without encryption; amateur radio is public by law. Voice and callsign forwarded by the server to public DMR networks (e.g. BrandMeister) are subject to those networks' policies.
- **Permissions**: microphone (push-to-talk), notifications and foreground service (keep receiving with the screen off), internet/network state/wake lock (keep the connection).
- **Retention**: settings stay on the device and are removed when the app is uninstalled; app backup is disabled. Data sent to a server follows that server operator's policy.
- Not directed to children under 13.
- Contact: https://github.com/seunghunchoi-dev/dvbridge-client-android/issues
