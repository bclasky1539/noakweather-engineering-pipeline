#!/bin/bash
REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)" || exit 1
cd "$REPO_ROOT" || exit 1

clear
echo off
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "Building Project"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
clear
#mvn clean compile test package
#mvn clean install
mvn validate
mvn clean compile
cat target/classes/application.properties
echo $?
