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

/**
 * Created by jconfetti on 04/02/15.
 */
class MessageBrokerServerUrlMappings{
    static mappings = {
        "/api/message_broker_server.$format"(controller:"restMessageBrokerServer"){
            action = [GET: "list",POST:"add"]
        }
        "/api/message_broker_server/$id.$format"(controller:"restMessageBrokerServer"){
            action = [GET:"show",PUT:"update", DELETE:"delete"]
        }
        "/api/message_broker_server.$format?name=$name"(controller:"restMessageBrokerServer"){
            action = [GET:"listByNameILike"]
        }
    }
}
