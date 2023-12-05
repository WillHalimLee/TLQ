package lambda;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HelloMain implements RequestHandler<HashMap<String, Object>, HashMap<String, Object>> {

    @Override
    public HashMap<String, Object> handleRequest(HashMap<String, Object> request, Context context) {
        LambdaLogger logger = context.getLogger();
        String inputFilePath = "/tmp/data.csv"; // Adjusted for Lambda's writable tmp directory
        String outputFilePath = "/tmp/output.csv"; // Adjusted for Lambda's writable tmp directory

        HashMap<String, Object> response = new HashMap<>();

        try {
            processCsvFile(inputFilePath, outputFilePath);
            uploadToS3(outputFilePath, "462projectbucket", "output.csv"); // Replace with your bucket name and object key
            response.put("message", "File processed and uploaded successfully");
        } catch (IOException | ParseException | AmazonServiceException e) {
            logger.log("Error: " + e.getMessage());
            response.put("error", e.getMessage());
        }

        return response;
    }

    private void processCsvFile(String inputFilePath, String outputFilePath) throws IOException, ParseException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             PrintWriter pw = new PrintWriter(new FileWriter(outputFilePath))) {
            
            String line = br.readLine(); // Read the header
            List<String> headers = new ArrayList<>(Arrays.asList(line.split(",")));

            // Add new columns
            headers.add("Order Processing Time");
            headers.add("Gross Margin");
            pw.println(String.join(",", headers));

            Set<String> processedOrderIds = new HashSet<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (processedOrderIds.add(values[6])) { // Check for duplicate Order ID
                    // Calculate Order Processing Time
                    Date orderDate = dateFormat.parse(values[5]);
                    Date shipDate = dateFormat.parse(values[7]);
                    long processingTime = TimeUnit.DAYS.convert(shipDate.getTime() - orderDate.getTime(), TimeUnit.MILLISECONDS);

                    // Transform Order Priority
                    String orderPriority = values[4];
                    switch (orderPriority) {
                        case "L": orderPriority = "Low"; break;
                        case "M": orderPriority = "Medium"; break;
                        case "H": orderPriority = "High"; break;
                        case "C": orderPriority = "Critical"; break;
                    }

                    // Calculate Gross Margin
                    double totalProfit = Double.parseDouble(values[13]);
                    double totalRevenue = Double.parseDouble(values[11]);
                    double grossMargin = totalProfit / totalRevenue;

                    // Add new data to the line
                    List<String> newValues = new ArrayList<>(Arrays.asList(values));
                    newValues.add(String.valueOf(processingTime));
                    newValues.add(String.format(Locale.US, "%.2f", grossMargin)); // Assuming US locale for decimal point
                    newValues.set(4, orderPriority); // Set transformed priority
                    
                    // Write the new line
                    pw.println(String.join(",", newValues));
                }
            }
        }
    }

    private void uploadToS3(String filePath, String bucketName, String keyName) throws AmazonServiceException {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                                .withRegion("us-east-1") // Replace with your region
                                .build();
        s3Client.putObject(new PutObjectRequest(bucketName, keyName, new File(filePath)));
    }
}
