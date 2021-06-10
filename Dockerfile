# Copyright (c) 2009-2020. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#/usr/lib/jvm/java-1.8.0-openjdk-amd64
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM cytomine/tomcat7:v1.2.1

RUN apt-get install --yes --no-install-recommends \
    less 

# Setup JAVA_HOME, this is useful for docker commandline

#RUN export JAVA_HOME

RUN cd /var/lib/tomcat7/  && wget https://github.com/cytomine/Cytomine-core/releases/download/v3.0.3/restapidoc.json -O restapidoc.json
RUN rm -r /var/lib/tomcat7/webapps/
ADD ROOT.war /var/lib/tomcat7/webapps/ROOT.war

RUN mkdir -p /usr/share/tomcat7/.grails
RUN chmod -R 777 /var/lib/tomcat7
#RUN chmod -R 777 /usr/share/tomcat7/logs
RUN chmod -R 777 /usr/share/tomcat7
ADD addHosts.sh /tmp/addHosts.sh
RUN chmod +x /tmp/addHosts.sh
ADD setenv.sh /tmp/setenv.sh
RUN chmod +x /tmp/setenv.sh
ADD deploy.sh /tmp/deploy.sh
RUN chmod +x /tmp/deploy.sh


EXPOSE 8080
ENTRYPOINT ["/tmp/deploy.sh"]
