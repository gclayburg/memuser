FROM openjdk:8u131-jre-alpine

RUN adduser -D -s /bin/sh springboot
COPY ./bootrunner.sh /home/springboot/bootrunner.sh
RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
WORKDIR /home/springboot
USER springboot

ADD dependenciesLayer/ /home/springboot/app/
ADD classesLayer/ /home/springboot/app/

ARG VCS_REF
ARG VCS_URL
ARG BUILD_DATE
ARG VERSION
ARG DESCRIPTION
LABEL maintainer="https://github.com/gclayburg" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url=$VCS_URL \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0" \
      org.label-schema.description=$DESCRIPTION

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["./bootrunner.sh"]
