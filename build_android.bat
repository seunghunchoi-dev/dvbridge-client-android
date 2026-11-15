@echo off
rem DVBridge Client (Android). Keep this file ASCII. If the folder path has non-ASCII characters, run it through an ASCII junction (mklink /J).
if not defined JAVA_HOME set "JAVA_HOME=C:\AndroidDev\jdk17"
if not defined ANDROID_HOME set "ANDROID_HOME=C:\AndroidDev\sdk"
cd /d "%~dp0"
rem Result: composeApp\build\outputs\apk\release\composeApp-release.apk

call "%~dp0gradlew.bat" :composeApp:assembleRelease :composeApp:bundleRelease %*
