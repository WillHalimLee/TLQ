package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import saaf.Inspector;

import java.util.HashMap;
import java.util.Map;

public class TLQ implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final Load loadProcess = new Load();
    private final Transform transformProcess = new Transform();
    private final Query queryProcess = new Query();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
        Inspector inspector = new Inspector();
        inspector.inspectAll();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            System.out.println("Interruption occurred while sleeping...");
        }
        String action = (String) request.get("action");
        switch (action) {
            case "transform":
                transformProcess.handleRequest(request, context);
                inspector.inspectAllDeltas();
                return inspector.finish();
            case "load":
                loadProcess.handleRequest(request, context);
                inspector.inspectAllDeltas();
                return inspector.finish();
            case "query":
                queryProcess.handleRequest(request, context);
                inspector.inspectAllDeltas();
                return inspector.finish();
            default:
                inspector.inspectAllDeltas();
                return inspector.finish();
        }

    }

}
