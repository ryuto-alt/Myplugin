@echo off
setlocal

REM JBR (JetBrains Runtime) を使用してMinecraftサーバーを起動
REM 完全なホットリロード対応

set JBR_HOME=%~dp0jbr\jbr-21.0.9-windows-x64-b1163.91
set AGENT_JAR=%~dp0agent\hotswap-agent.jar

REM まずビルドを実行
echo Building plugin...
call gradlew.bat build

REM runディレクトリに移動
cd run

REM JBRでサーバーを起動
echo Starting server with JBR + HotSwapAgent...
"%JBR_HOME%\bin\java.exe" ^
    -XX:+AllowEnhancedClassRedefinition ^
    -XX:HotswapAgent=fatjar ^
    -javaagent:"%AGENT_JAR%" ^
    -Xms2G ^
    -Xmx2G ^
    -jar paper.jar ^
    --nogui

endlocal
