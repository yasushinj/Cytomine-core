class SoftwareRepositoryUrlMappings {

    static mappings = {
        "/api/software_repository.$format"(controller: "restSoftwareRepository") {
            action = [GET: "list", POST: "add"]
        }
        "/api/software_repository/$id.$format"(controller: "restSoftwareRepository") {
            action = [GET: "show", PUT: "update", DELETE: "delete"]
        }
        "/api/software_repository/refresh.$format"(controller: "restSoftwareRepository") {
            action = [GET: "refreshRepositories"]
        }
    }

}
