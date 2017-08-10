#!/bin/sh
date_echo(){
    datestamp=$(date +%F_%T)
    echo "${datestamp} $*"
}

iterate_secrets(){
  date_echo "checking secrets in directory: $1"
  for file in $(ls $1); do
     myvars="${myvars} ${file}=$(cat $1/${file})"
     echo "adding secret: $file"
  done
  #i.e.,  env MONGOUSER=hi MONGOPASSWORD=supersecret runprog.sh

}
if [ $# -gt 0 ]; then
  myvars=""
  if [ -d $1 ]; then # first argument is a directory, lets assume kubernetes secrets are stored there
    iterate_secrets $1
    shift
  else
    if [ -d /tmp/secrets ]; then  #/tmp/secrets is the standard secret directory
      iterate_secrets /tmp/secrets
    fi
  fi
else
  if [ -d /tmp/secrets ]; then  #/tmp/secrets is the standard secret directory
    iterate_secrets /tmp/secrets
  fi
fi
#  env ${myvars} /approot/runprog.sh "$@"
export SOMEJUNK=hello
export ${myvars}
date_echo "env is: "
env
date_echo "myvars is: ${myvars}"
date_echo "which java"
which java
date_echo "java -version"
java -version
date_echo "running jar... with args: $@"
date_echo "java -Djava.security.egd=file:/dev/./urandom -jar /app.jar $@"

#exec the JVM so that it will get a SIGTERM signal on shutdown
exec java -Djava.security.egd=file:/dev/./urandom -jar /app.jar "$@"
