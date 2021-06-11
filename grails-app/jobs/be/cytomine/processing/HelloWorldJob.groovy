package be.cytomine.processing

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.security.UserJob
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils

/**
 * Created with IntelliJ IDEA.
 * User: stevben
 * Date: 24/11/13
 * Time: 23:20
 * To change this template use File | Settings | File Templates.
 */
class HelloWorldJob {
    //def sessionRequired = true
    def concurrent = true

    def jobParameterService

    static triggers = {
        //simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute(context) {
        log.info "Start HellWorld"

        Boolean preview = (Boolean) context.mergedJobDataMap.get('preview')
        Job job = (Job) context.mergedJobDataMap.get('job')
        UserJob userJob = (UserJob) context.mergedJobDataMap.get('userJob')
        def jobParameters = context.mergedJobDataMap.get('jobParameters')
        SpringSecurityUtils.doWithAuth(userJob.getUser().getUsername(), {
            def args = []
            jobParameters.each {
                args << [ name : "--"+it.getSoftwareParameter().getName(), value : it.getValue()]
            }

            if (preview) {
                args << [ name : "--preview", value : ""]
            }
            // execute job
            log.info "execute $job with $args"

            rabbitSend('helloWorldQueue', (args as JSON).toString())
        })

    }
}
