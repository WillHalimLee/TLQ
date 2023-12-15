package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import saaf.Inspector;

import java.sql.*;
import java.util.*;
import java.io.*;

public class Query implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String SQLITE_FILE_PATH = "/tmp/mydatabase.db";
    private static final String S3_BUCKET_NAME = "tcss462.sqlite"; // Replace with your S3 bucket name
    private static final String S3_OBJECT_KEY = "mydatabase.db"; // Replace with your object key in S3

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
        //Collect initial data.
        Inspector inspector = new Inspector();
        inspector.inspectAll();

        LambdaLogger logger = context.getLogger();
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        List<Map<String, Object>> queryResults = new ArrayList<>();

        try {
            // Check for local DB file or download from S3
            File dbFile = new File(SQLITE_FILE_PATH);
            if (!dbFile.exists()) {
                S3Object s3Object = s3Client.getObject(new GetObjectRequest(S3_BUCKET_NAME, S3_OBJECT_KEY));
                try (InputStream objectData = s3Object.getObjectContent();
                     OutputStream outputStream = new FileOutputStream(SQLITE_FILE_PATH)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = objectData.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }
            }

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Construct and execute the SQL query
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_FILE_PATH)) {
                String sqlQuery = constructSQLQuery(request);
                try (PreparedStatement pstmt = conn.prepareStatement(sqlQuery); ResultSet rs = pstmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object columnValue = rs.getObject(i);
                            row.put(columnName, columnValue);
                        }
                        queryResults.add(row);
                    }
                    
                }
            }

        } catch (ClassNotFoundException | SQLException | IOException e) {
            logger.log("Error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("queryResults", queryResults);
        
        //Collect final information such as total runtime and cpu deltas.
        inspector.inspectAllDeltas();
        return inspector.finish();
    }

    private String constructSQLQuery(Map<String, Object> request) {
        // Example construction of SQL query based on request
        // This should be modified to suit your specific requirements and ensure SQL injection prevention
        StringBuilder queryBuilder = new StringBuilder("SELECT *, ");
  
        // Add aggregation functions
        List<String> aggregations = (List<String>) request.get("aggregations");
        queryBuilder.append(String.join(", ", aggregations));
        queryBuilder.append(" FROM mytable "); // Replace 'mytable' with your actual table name

        // Add filters
        Map<String, String> filters = (Map<String, String>) request.get("filters");
        if (filters != null && !filters.isEmpty()) {
            queryBuilder.append(" WHERE ");
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                queryBuilder.append(entry.getKey()).append(" = '").append(entry.getValue()).append("' AND ");
            }
            queryBuilder.delete(queryBuilder.length() - 5, queryBuilder.length()); // Remove the last ' AND '
        }

        // Add GROUP BY clause if needed
        List<String> groupBy = (List<String>) request.get("groupBy");
        if (groupBy != null && !groupBy.isEmpty()) {
            queryBuilder.append(" GROUP BY ").append(String.join(", ", groupBy));
        }

        return queryBuilder.toString();
    }
}

