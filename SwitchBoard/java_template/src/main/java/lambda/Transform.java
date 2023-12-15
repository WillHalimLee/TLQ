package lambda;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import saaf.Inspector;

import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Transform implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {

        LambdaLogger logger = context.getLogger();
        String bucketName = "test.bucket.462.termproject"; // Replace with your actual bucket name
        String inputFileKey = "data.csv"; // Replace with the actual S3 key for the input file
        String outputFileKey = "output.csv"; // Replace with the desired S3 key for the output file
        String outputFilePath = "/tmp/output.csv";

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion("us-east-2").build();
        Map<String, Object> response = new HashMap<>();

        try {
            // Download file from S3 and setup BufferedReader
            S3Object s3object = s3Client.getObject(bucketName, inputFileKey);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(s3object.getObjectContent()));
                 PrintWriter pw = new PrintWriter(new FileWriter(outputFilePath))) {

                processCsvFile(br, pw);
            }

            // Upload processed file to S3
            uploadToS3(outputFilePath, bucketName, outputFileKey, s3Client); // Using the same s3Client instance
            response.put("message", "File processed and uploaded successfully");
        } catch (IOException | ParseException | AmazonServiceException e) {
            logger.log("Error: " + e.getMessage());
            response.put("error", e.getMessage());
        }
        return response;
    }

    private void processCsvFile(BufferedReader br, PrintWriter pw) throws IOException, ParseException {
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
            if (processedOrderIds.add(values[6])) {
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
                    newValues.add(String.format(Locale.US, "%.2f", grossMargin)); 
                    newValues.set(4, orderPriority); // Set transformed priority
                    
                    // Write the new line
                    pw.println(String.join(",", newValues));
                }
            }
        }

		private void uploadToS3(String filePath, String bucketName, String keyName, AmazonS3 s3Client) throws AmazonServiceException {
			s3Client.putObject(new PutObjectRequest(bucketName, keyName, new File(filePath)));
		}
    }

    
