@echo off
title Dining Philosophers Simulator Launcher
color 0b
echo ==========================================================
echo   Dining Philosophers Simulator (Spring Boot + Java 17)
echo ==========================================================
if not exist "data" mkdir data
echo Starting Spring Boot on http://localhost:8080 ...
mvn spring-boot:run
pause
