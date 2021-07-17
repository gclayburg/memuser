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
echo "TEST RESOURCES are ${TEST_RESOURCES}"
echo "REAL_HOSTNAME is ${REAL_HOSTNAME}"
env
do_shell_fail newman run --env-var Server=${REAL_HOSTNAME:-localhost} --env-var Port=:443 --env-var Api=api/multiv2/sometestdomain --insecure src/test/resources/SCIM_Tests.postman_collection.json --reporters cli,junit --reporter-junit-export "build/test-results/TEST-postman-newman.xml"
#/etc/newman/wait-for.sh -t 600 memuser:8080 -- /usr/local/bin/newman run --env-var Server=proxy --env-var Port=:443 --env-var Api=api/multiv2/sometestdomain --ssl-extra-ca-certs /etc/newman/certstargaryclayburgcom.pem --insecure ${TEST_RESOURCES}/SCIM_Tests.postman_collection.json --suppress-exit-code 1