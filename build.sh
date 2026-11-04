#!/bin/sh
# Builds all JDK-only modules and runs the tests. Requires JDK 21+. No Maven needed.
set -e
rm -rf out && mkdir out
javac -d out -cp lib/sqlite-jdbc.jar \
  $(find core-domain concurrent-processing network-gateway persistence-jdbc rmi-administration tests -name '*.java')
java -cp out:lib/sqlite-jdbc.jar scasp.AllTests
