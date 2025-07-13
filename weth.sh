clear
echo off
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "Start of Run"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "Example metar - . weth.sh m KEWR y d"
echo "Example taf - . weth.sh t KEWR y d"
mvn "-Dexec.args=-classpath %classpath noakweather.NoakWeatherMain $1 $2 $3 $4" -Dexec.executable=$JAVA_HOME/bin/java org.codehaus.mojo:exec-maven-plugin:1.5.0:exec
echo $?
