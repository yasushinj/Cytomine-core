class SoftwareUserRepositoryUrlMappings {

    static mappings = {
        "/api/software_user_repository.$format"(controller: "restSoftwareUserRepository") {
            action = [GET: "list", POST: "add"]
        }
        "/api/software_user_repository/$id.$format"(controller: "restSoftwareUserRepository") {
            action = [GET: "show", PUT: "update", DELETE: "delete"]
        }
        "/api/software_user_repository/refresh_user_repositories.$format"(controller: "restSoftwareUserRepository") {
            action = [GET: "refreshUserRepositories"]
        }
    }

}