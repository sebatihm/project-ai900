package info.rio.project_ai901.aiTools;

import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.jackson.databind.JsonNode;


public interface AgentTool {
    String name();
    String description();
    Map<String, Object> properties();
    List<String> required();
    String execute(JsonNode args, UserContext ctx);

    default boolean isPublic() { return false; }
    default Set<String> allowedRoles() { return Set.of(); } 
}
