FROM tomcat:9-jdk8-openjdk-slim

# Note: port 8080 is already exposed in the base image

WORKDIR /usr/local/tomcat

COPY start-tomcat.sh .

# remove unused root, docs and examples web apps, along with the config files that we replace
RUN rm -rf webapps/ROOT && rm -rf webapps/docs && rm -rf webapps/examples && mkdir ROOT \
  && rm -f conf/tomcat-users.xml && rm -f conf/server.xml \
  && mkdir /tomcat-conf && chmod +rwx ./start-tomcat.sh

# ROOT folder on the host contains the exploded JPPF-web-admin-x.y.z.war
COPY ROOT webapps/ROOT
COPY server.xml conf
COPY tomcat-users.xml /tomcat-conf
RUN chmod 777 /tomcat-conf && chmod 777 /tomcat-conf/tomcat-users.xml

CMD ["./start-tomcat.sh"]
