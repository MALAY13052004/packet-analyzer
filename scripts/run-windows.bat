@echo off
cd /d "%~dp0.."
where java >nul 2>&1 || (echo Java 21+ is required.& pause & exit /b 1)
where mvn >nul 2>&1 || (echo Maven is required to build PacketLab.& pause & exit /b 1)
echo Starting PacketLab...
mvn spring-boot:run
pause
