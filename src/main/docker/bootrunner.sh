#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
#exec the JVM so that it will get a SIGTERM signal and the app can shutdown gracefully

if [ -d /app ]; then
  #execute springboot expanded jar, which may have been constructed from several image layers
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp org.springframework.boot.loader.JarLauncher $*"
  exec "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp /app org.springframework.boot.loader.JarLauncher $*"
elif [ -f /app.jar ]; then
  # execute springboot jar
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar $*"
  exec "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar $*"
else
  date_echo "springboot application not found in /app or /app.jar"
  exit 1
fi 
