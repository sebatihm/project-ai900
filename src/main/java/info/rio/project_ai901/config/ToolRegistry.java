package info.rio.project_ai901.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.azure.ai.agents.models.FunctionTool;
import com.azure.core.util.BinaryData;

import info.rio.project_ai901.aiTools.AgentTool;
import info.rio.project_ai901.aiTools.UserContext;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ToolRegistry {

  private final Map<String, AgentTool> tools;
  private final JsonMapper mapper;

  public ToolRegistry(List<AgentTool> list, JsonMapper mapper) {
      this.tools = list.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));
      this.mapper = mapper;
  }

    private FunctionTool toFunctionTool(AgentTool t) {
        Map<String, BinaryData> params = new HashMap<>();
        params.put("type", BinaryData.fromObject("object"));
        params.put("properties", BinaryData.fromObject(t.properties()));
        params.put("required", BinaryData.fromObject(t.required()));

        return new FunctionTool(t.name(), params, false)
                .setDescription(t.description());  
    }
    public List<FunctionTool> definitions(boolean onlyPublic) {
        return tools.values().stream()
                .filter(t -> !onlyPublic || t.isPublic())
                .map(this::toFunctionTool)
                .toList();
    }

    // Ejecución cuando el modelo pide una herramienta
    public String execute(String name, String argsJson, UserContext ctx) {
        AgentTool tool = tools.get(name);
        if (tool == null) return "Herramienta desconocida";
        try {
            return tool.execute(mapper.readTree(argsJson), ctx);
        } catch (Exception e) {
            return "Error al ejecutar la herramienta";   // texto, no excepción: el modelo lo explica al usuario
        }
    }
}