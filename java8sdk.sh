#!/bin/bash
LOG_CONTEXT="-"  #override to add extra stuff to log messages
date_echo(){
    datestamp=$(date +%F_%T)
    echo "${datestamp} ${LOG_CONTEXT} $*"
}

do_shell(){
# Execute command in shell, while logging complete command to stdout
    echo "$(date +%F_%T) --> $*"
    eval "$*"
    STATUS=$?
    return $STATUS
}

do_shell_fail(){
# Execute command in shell, while logging complete command to stdout
    echo "$(date +%F_%T) --> $*"
    eval "$*"
    STATUS=$?
    if [[ $STATUS -ne 0 ]]; then # exit entire script to fail the build
      exit $STATUS;
    fi
    return $STATUS
}
CHOSEN_JDK=$1
shift
do_shell "java -version"

if [ -f "$HOME/.sdkman" ]; then  #standard SDK install location
	export SDKMAN_DIR="$HOME/.sdkman"
elif [ -f /usr/local/sdk/.sdkman ]; then  #jenkins build agent uses this
	export SDKMAN_DIR=/usr/local/sdk/.sdkman
fi
source "$SDKMAN_DIR/bin/sdkman-init.sh"
do_shell "java -version"
date_echo "JAVA_HOME is $JAVA_HOME"

do_shell "sdk use java $CHOSEN_JDK"
sdkstatus=$?
if [ $sdkstatus -ne 0 ]; then
  echo "this sdk not installed. Installing..."
  yes n | sdk install java $CHOSEN_JDK
  sdkinstall=$?
  if [ $sdkinstall -ne 0 ]; then
    echo "ERROR Cannot install $CHOSEN_JDK"
    exit 2
  fi
fi
do_shell_fail "java -version"
do_shell_fail "$*"


#running
#$ ./java8sdk.sh 11.0.22-tem ./gradlew clean build --info
