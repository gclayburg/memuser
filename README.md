[![](https://images.microbadger.com/badges/version/gclayburg/memuser.svg)](https://microbadger.com/images/gclayburg/memuser "Get your own version badge on microbadger.com") [![](https://images.microbadger.com/badges/image/gclayburg/memuser.svg)](https://microbadger.com/images/gclayburg/memuser "Get your own image badge on microbadger.com")

# memuser
In-memory storage of User data

## quickstart

To build and run memuser, use the embedded gradle:

```bash
$ ./gradlew bootJarRun
```

This project is also published as a docker image on docker hub.  To run it:

```bash
$ docker run -d -p8080:8080 gclayburg/memuser:latest
```

Memuser should be running and listening on port 8080.

### Add a user

Add a minimal user
```bash
$ curl 'http://localhost:10001/api/v1/Users' -i -X POST \
    -H 'Content-Type: application/scim+json' \
    -H 'Accept: application/scim+json' \
    -d '
{
  "userName": "alicesmith",
  "displayName": "Alice P Smith"
}
'
```

More examples can be found in the [guide](https://gclayburg.github.io/memuser/).


