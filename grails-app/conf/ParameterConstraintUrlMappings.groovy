class ParameterConstraintUrlMappings {

    static mappings = {
        "/api/parameter_constraint.$format" (controller: "restParameterConstraint") {
            action = [GET: "list", POST: "add"]
        }
        "/api/parameter_constraint/$id.$format" (controller: "restParameterConstraint") {
            action = [GET: "show", DELETE: "delete"]
        }
    }

}