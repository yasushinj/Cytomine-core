
class ProcessingServerUrlMappings {

    static mappings = {
        "/api/processing_server.$format"(controller: "restProcessingServer") {
            action = [GET:"list", POST:"add"]
        }

        "/api/processing_server/$id.$format"(controller: "restProcessingServer") {
            action = [GET:"show", DELETE:"delete"]
        }
    }

}