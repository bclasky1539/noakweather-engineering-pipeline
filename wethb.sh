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
