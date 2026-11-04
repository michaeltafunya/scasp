# Android client (Stage 10)
Create an Android Studio project (Empty Views Activity, package `zm.scasp`, min SDK 24) and copy `MainActivity.java`,
`activity_main.xml` and the manifest entries over the generated ones. Start the TCP gateway on port 9090
(see `scasp.net.GatewayServer`) and run the emulator; `10.0.2.2` reaches the host machine.
Test: normal, offline (stop the server: cached list shows), denied permission (toast), rotation (state kept).
