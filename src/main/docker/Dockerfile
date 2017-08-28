FROM openjdk:8-jre-alpine

LABEL maintainer="gclaybur@comcast.net" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/gclayburg/memuser"

RUN adduser -D -s /bin/sh springboot
COPY ./bootrunner.sh /home/springboot/bootrunner.sh
RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
WORKDIR /home/springboot
USER springboot

ADD dependenciesLayer/ /app/
ADD classesLayer/ /app/

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["./bootrunner.sh"]
