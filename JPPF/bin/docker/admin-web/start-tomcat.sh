#! /bin/bash

set -ex

if [ ! -z $TOMCAT_USERS ]; then
  echo $TOMCAT_USERS > /tomcat-conf/tomcat-users.xml
  chmod 777 /tomcat-conf && chmod 777 /tomcat-conf/tomcat-users.xml
fi
echo "tomcat_users = [$TOMCAT_USERS]"
ls /tomcat-conf
cat /tomcat-conf/tomcat-users.xml

bin/catalina.sh run
