#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
for president in `ls sampleusers/*json`; do
  echo
  date_echo "loading $president"
  curl "http://$1/api/v2/Users" -X POST \
      -H 'Content-Type: application/json' \
      -H 'Accept: application/json' \
      -d @${president}
done

