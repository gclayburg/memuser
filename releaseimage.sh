#!/bin/bash
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

version=$1
do_shell_fail docker pull registry:5000/memuser:${version}
do_shell_fail docker pull registry:5000/memuser:latest
do_shell_fail docker tag registry:5000/memuser:${version} gclayburg/memuser:${version}
do_shell_fail docker tag registry:5000/memuser:${version} gclayburg/memuser:latest
do_shell_fail docker push gclayburg/memuser:${version}
do_shell_fail docker push gclayburg/memuser:latest
do_shell_fail curl -d \'\' https://hooks.microbadger.com/images/gclayburg/memuser/OxYEnjAXt1Nt6ww8mtEwZlR8nwQ=
