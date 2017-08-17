FROM openjdk:8-jre-alpine

# add directly the war
ADD *.jar /app.jar

RUN sh -c 'touch /app.jar'
VOLUME /tmp
EXPOSE 8001
COPY ./injectenv.sh /injectenv.sh
RUN chmod 755 /injectenv.sh
ENTRYPOINT ["/injectenv.sh"]

