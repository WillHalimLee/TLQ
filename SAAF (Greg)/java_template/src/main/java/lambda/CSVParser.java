import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.text.ParseException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class CSVParser {
    public static void main(String[] args) {
        String inputFilePath = "./data.csv";
        String outputFilePath = "./output.csv";

        try (
            BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
            PrintWriter pw = new PrintWriter(new FileWriter(outputFilePath))
        ) {
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
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        // Upload to S3
        String bucketName = "462projectbucket";
        String fileObjKeyName = "output.csv";
        String fileName = "./output.csv";

        // Assuming AWS credentials are set in environment variables or AWS credentials file
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                                .withRegion("us-east-1") // Replace with your region
                                .build();

        try {
            s3Client.putObject(new PutObjectRequest(bucketName, fileObjKeyName, new File(fileName)));
            System.out.println("File uploaded successfully");
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
        }
    }
}
