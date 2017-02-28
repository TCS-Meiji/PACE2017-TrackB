#!/bin/sh

rm -r bin
mkdir bin
javac -d bin ./src/tw/common/*.java ./src/fillin/main/*.java

