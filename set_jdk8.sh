#!/bin/bash


export JAVA_HOME=/usr/lib/jvm/jdk1.8.0_251
export PATH=${JAVA_HOME}/bin:${PATH}
#export MAVEN_OPTS=-Dhttps.protocols=TLSv1.2
#export JAVA_OPTS="-Dhttps.protocols=SSLv3,TLSv1,TLSv1.1,TLSv1.2"
#export GRAILS_OPTS="-Dhttps.protocols=SSLv3,TLSv1,TLSv1.1,TLSv1.2"

sdk use grails 2.4.4
sdk use groovy 3.0.4
sdk use gradle 3.2

