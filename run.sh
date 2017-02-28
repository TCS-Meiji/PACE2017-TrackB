#!/bin/sh

java -Xss256M -XX:+UseSerialGC -cp ./bin fillin/main/Solver $1
