[![](https://images.microbadger.com/badges/version/gclayburg/memuser.svg)](https://microbadger.com/images/gclayburg/memuser "Get your own version badge on microbadger.com") [![](https://images.microbadger.com/badges/image/gclayburg/memuser.svg)](https://microbadger.com/images/gclayburg/memuser "Get your own image badge on microbadger.com")
[![Build Status](https://travis-ci.org/gclayburg/memuser.svg?branch=master)](https://travis-ci.org/gclayburg/memuser)


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

More examples can be found in the [guide](https://gclayburg.github.io/memuser/).


