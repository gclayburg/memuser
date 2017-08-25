# memuser
In-memory storage of User data

## quickstart

To build and run memuser, use the embedded gradle:

```bash
$ ./gradlew bootJarRun
```

Or you can run a pre-built docker image:

```bash
$ docker run -d -p10001:10001 gclayburg/memuser:latest
```

Memuser should be running and listening on port 10001.

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

More examples can be found in the guide.

