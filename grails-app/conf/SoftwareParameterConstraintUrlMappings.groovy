class SoftwareParameterConstraintUrlMappings{

    static mappings = {
        "/api/software_parameter_constraint.$json" (controller: "restSoftwareParameterConstraint") {
            action = [GET: "list", POST: "add"]
        }
        "/api/software_parameter_constraint/$id.$json" (controller: "restSoftwareParameterConstraint") {
            action = [GET: "show", DELETE: "delete"]
        }
        "/api/software_parameter_constraint/evaluate/$id/value/$value.$json" (controller: "restSoftwareParameterConstraint") {
            action = [GET: "evaluate", POST: "evaluate"]
        }
    }

}