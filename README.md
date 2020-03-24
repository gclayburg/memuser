[![](https://images.microbadger.com/badges/version/gclayburg/memuser.svg)](https://microbadger.com/images/gclayburg/memuser "Get your own version badge on microbadger.com") [![](https://images.microbadger.com/badges/image/gclayburg/memuser.svg)](https://microbadger.com/images/gclayburg/memuser "Get your own image badge on microbadger.com")
[![Build Status](https://travis-ci.org/gclayburg/memuser.svg?branch=master)](https://travis-ci.org/gclayburg/memuser)
![Docker Pulls](https://img.shields.io/docker/pulls/gclayburg/memuser)

# memuser
In-memory storage of User data using the [SCIM](http://www.simplecloud.info/) protocol

## build and run

To build and run memuser, use the embedded gradle:

```bash
$ ./gradlew bootJarRun
```

## run from docker image

This project is also published as a docker image on docker hub.  To run a pre-built docker image:

```bash
$ docker run -d -p8080:8080 gclayburg/memuser:latest
```

Memuser should be running and listening on port 8080.

### Add a user

Add a minimal user
```bash
$ curl 'http://localhost:8080/api/v2/Users' -i -X POST \
    -H 'Content-Type: application/scim+json' \
    -H 'Accept: application/scim+json' \
    -d '
{
  "userName": "alicesmith",
  "displayName": "Alice P Smith"
}
'
```

### Get user list

```bash
$ curl 'http://localhost:8080/api/v2/Users/' -i \
    -H 'Accept: application/scim+json'
```    

# Multi-domains
New in version 0.8.0 is the ability to add users to an arbitrary, independent domain.  For example, add a user to 'fakehr':

### Add a user to fakehr

Add a minimal user
```bash
$ curl 'http://localhost:8080/api/multiv2/fakehr/Users' -i -X POST \
    -H 'Content-Type: application/scim+json' \
    -H 'Accept: application/scim+json' \
    -d '
{
  "userName": "alicesmith",
  "displayName": "Alice P Smith"
}
'
```

As you can see, there is no setup of fakehr required - just add users to it.
### Get user list from fakehr

```bash
$ curl 'http://localhost:8080/api/multiv2/fakehr/Users/' -i \
    -H 'Accept: application/scim+json'
```    


More examples can be found in the [guide](https://gclayburg.github.io/memuser/).

# Version history

version 0.6.5

version 0.7.0
- GET /api/v2/User is now pageable using 2 different clients
  - responses are now pageable per SCIM RFC 7644.  pingidentity SCIM 2.0 client uses this page format
  - responses are now pageable per RFC 5988. jhipster generated clients use this page format
- added CORS support

version 0.7.1
- requests now honor X-Forwarded-Proto and X-Forwarded-Host headers for use behind a proxy

version 0.7.2
- fixed bug related to X-Forwarded* headers

version 0.8.0
- new option to add unlimited number of test users on startup
- new ability for multiple domains of Users where each domain is stored completely independent from each other
