#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
for mailaddr in `ls sampleusers/email/*json`; do
  echo
  date_echo "loading $mailaddr"
  curl "http://$1/api/v2/Users" -X POST \
      -H 'Content-Type: application/json' \
      -H 'Accept: application/json' \
      -d @${mailaddr}
done

