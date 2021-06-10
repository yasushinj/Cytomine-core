#!/bin/bash

#
# Copyright (c) 2009-2020. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,


cd /usr/lib/jvm/java-8-oracle/jre/lib/security; keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias bulkload.revealbio.com -file /tmp/live/bulkload.revealbio.com/fullchain.pem
cd /usr/lib/jvm/java-8-oracle/jre/lib/security; keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias bulkload-ims.revealbio.com -file /tmp/live/bulkload-ims.revealbio.com/fullchain.pem
cd /usr/lib/jvm/java-8-oracle/jre/lib/security; keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias bulkload-ims2.revealbio.com -file /tmp/live/bulkload-ims2.revealbio.com/fullchain.pem
cd /usr/lib/jvm/java-8-oracle/jre/lib/security; keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias bulkload-upload.revealbio.com -file /tmp/live/bulkload-upload.revealbio.com/fullchain.pem
