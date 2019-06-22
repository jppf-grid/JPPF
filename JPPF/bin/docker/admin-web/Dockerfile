FROM tomcat:9-jdk8-openjdk-slim

# Note: port 8080 is already exposed in the base image

WORKDIR /usr/local/tomcat

RUN rm -rf webapps/ROOT && rm -rf webapps/docs && rm -rf webapps/examples && mkdir ROOT \
  && rm -f conf/tomcat-users.xml

# ROOT folder on the host contains the exploded JPPF-web-admin-x.y.z.war
COPY ROOT webapps/ROOT
