# SCASP - Smart Campus Academic Services Platform

Smart school management system.

## Build and test (JDK 21+, no Maven needed)
    ./build.sh

Compiles the JDK-only modules and runs 50 automated checks for Stages 1, 2, 3, 5 and 8.

## Run
    java -cp out:lib/sqlite-jdbc.jar scasp.net.GatewayMain     # TCP gateway on port 9090
    java -cp out scasp.rmi.AdminServer                         # RMI server on port 1099
    java -cp out scasp.rmi.AdminClient                         # RMI client
    nc localhost 9090   then type: PING, COURSES, REGISTER S1 CS101, WITHDRAW S1 CS101, REPORT, QUIT

### Windows PowerShell
From the project directory, run `.\run.cmd`. It compiles the JDK-only modules and starts the TCP gateway on port 9090. Press Ctrl+C to stop it. In a second PowerShell window, run `.\client.cmd` to send commands such as PING, COURSES, REGISTER S1 CS101, REPORT, and QUIT.

## Modules
| Folder | Stage | Status |
|---|---|---|
| core-domain | 1 Collections | built and tested |
| concurrent-processing | 2 Multithreading | built and tested (includes race demo) |
| network-gateway | 3 Networking | built and tested (TCP, UDP, URL fetch) |
| enterprise-services | 4 EJB | source only: deploy to a Jakarta EE server |
| persistence-jdbc, database | 5 JDBC | built and tested (SQLite) |
| web-application | 6-7 Servlets, JSP | source only: `mvn package`, deploy WAR to Tomcat 10+ |
| rmi-administration | 8 RMI | built and tested |
| docs/corba-migration | 9 CORBA | written dossier |
| android-client | 10 Android | source only: open in Android Studio |

Not compiled or run in the build environment: enterprise-services, web-application, android-client (they need a Jakarta EE runtime or the Android SDK).
Stage 0 items (charter, requirements, risk register, Git protection, CI) and manual evidence (thread dump, deployment logs, screenshots, usability test, contribution log) are group tasks that cannot be generated as code.

## Sample logins (web demo): student1 / demo, staff1 / demo
