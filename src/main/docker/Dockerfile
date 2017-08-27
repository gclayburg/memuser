FROM openjdk:8-jre-alpine
LABEL maintainer="gclaybur@comcast.net" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/gclayburg/memuser"


COPY ./injectenv.sh /injectenv.sh
RUN chmod 755 /injectenv.sh

# add directly the war
ADD *.jar /app.jar

RUN sh -c 'touch /app.jar'
VOLUME /tmp
EXPOSE 8001
ENTRYPOINT ["/injectenv.sh"]

