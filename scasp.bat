@echo off
rem Usage: scasp.bat web | gateway [host] [port] | udp [port] | admin host port command [args] | race-demo | benchmark [dumpfile] | thread-dump
cd /d "%~dp0"
set CP=lib\*;out\classes
set CMD=%1
shift
set ARGS=%1 %2 %3 %4 %5 %6
if "%CMD%"=="web" java -cp "%CP%" scasp.web.WebServer
if "%CMD%"=="gateway" java -cp "%CP%" scasp.network.GatewayClient %ARGS%
if "%CMD%"=="udp" java -cp "%CP%" scasp.network.UdpNotifier %ARGS%
if "%CMD%"=="admin" java -cp "%CP%" scasp.rmi.AdminClient %ARGS%
if "%CMD%"=="race-demo" java -cp "%CP%" scasp.processing.RaceDemo
if "%CMD%"=="benchmark" java -cp "%CP%" scasp.processing.ProcessorBenchmark %ARGS%
if "%CMD%"=="thread-dump" java -cp "%CP%" scasp.processing.ThreadDump
