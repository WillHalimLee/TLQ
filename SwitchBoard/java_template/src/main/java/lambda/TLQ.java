package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.Map;

public class TLQ implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final Load loadProcess = new Load();
    private final Transform transformProcess = new Transform();
    private final Query queryProcess = new Query();
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
        String action = (String) request.get("action");

        switch (action) {
            case "transform":
                return transformProcess.handleRequest(request, context);
            case "load":
                return loadProcess.handleRequest(request, context); 
            case "query":
                return queryProcess.handleRequest(request, context);
            default:
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Invalid action");
                return response;
        }
    }

}
