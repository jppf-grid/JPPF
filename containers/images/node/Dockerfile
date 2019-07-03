FROM openjdk:8-slim

COPY JPPF-@version@-node /jppf
WORKDIR /jppf

RUN /bin/sh -c "chmod +x ./start-node.sh" \
    && mkdir /jppf-config

COPY tls/*.* /jppf-config/

CMD ["sh",  "start-node.sh"]
