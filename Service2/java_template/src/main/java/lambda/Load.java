package lambda;

import com.amazonaws.Response;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import saaf.Inspector;

;

/**
 * uwt.lambda_test::handleRequest
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class Load implements RequestHandler<Request, HashMap<String, Object>> {
  private String S3_BUCKET_NAME = "tcss462.sqlite"; // Change to your S3 bucket name

  @Override
  public HashMap<String, Object> handleRequest(Request request, Context context) {
    Inspector inspector = new Inspector();
    inspector.inspectAll();

    LambdaLogger logger = context.getLogger();
    HashMap<String, Object> response = new HashMap<>();

    // Generate a unique file name for each invocation
    String uniqueFileName = "mydatabase_" + UUID.randomUUID() + ".db";
    String sqliteFilePath = "/tmp/" + uniqueFileName;

    try {
      AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
      S3Object s3Object = s3Client.getObject(new GetObjectRequest(request.getBucketname(), request.getFilename()));

      try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqliteFilePath);
          InputStream objectData = s3Object.getObjectContent();
          Scanner scanner = new Scanner(objectData)) {

        ensureTableExists(con, logger);
        insertDataIntoTable(scanner, con, logger);
        // Upload database to S3
        uploadDatabaseToS3(sqliteFilePath, uniqueFileName, s3Client, logger);
      }
    } catch (Exception e) {
      logger.log("Error: " + e.getMessage());
      response.put("error", e.getMessage());
    }

    // Collect final information such as total runtime and cpu deltas.
    inspector.inspectAllDeltas();
    return inspector.finish();
  }

  private void ensureTableExists(Connection con, LambdaLogger logger) throws Exception {
    try (PreparedStatement ps = con
        .prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='mytable'")) {
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        logger.log("Table 'mytable' does not exist. Creating it.");
        try (PreparedStatement createTable = con.prepareStatement(
            "CREATE TABLE mytable (Region TEXT, Country TEXT, [Item Type] TEXT, [Sales Channel] TEXT, [Order Priority] TEXT, [Order Date] TEXT, [Order ID] TEXT, [Ship Date] TEXT, [Units Sold] TEXT, [Unit Price] TEXT, [Unit Cost] TEXT, [Total Revenue] TEXT, [Total Cost] TEXT, [Total Profit] TEXT, [Order Processing Time] TEXT, [Gross Margin] TEXT)")) {
          createTable.execute();
        }
      }
    }
  }

  private void insertDataIntoTable(Scanner scanner, Connection con, LambdaLogger logger) throws Exception {
    String sql = "INSERT INTO mytable (Region, Country, [Item Type], [Sales Channel], [Order Priority], [Order Date], [Order ID], [Ship Date], [Units Sold], [Unit Price], [Unit Cost], [Total Revenue], [Total Cost], [Total Profit], [Order Processing Time], [Gross Margin]) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      while (scanner.hasNextLine()) {
        String[] words = scanner.nextLine().split(",");
        for (int i = 0; i < words.length; i++) {
          ps.setString(i + 1, words[i]);
        }
        ps.executeUpdate();
      }
    }
  }

  // private List<HashMap<String, Object>> queryDatabase(Connection con,
  // LambdaLogger logger) throws Exception {
  // List<HashMap<String, Object>> allData = new ArrayList<>();
  // String query = "SELECT * FROM mytable";
  // try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs =
  // ps.executeQuery()) {
  // while (rs.next()) {
  // HashMap<String, Object> row = new HashMap<>();
  // row.put("Region", rs.getString("Region"));
  // row.put("Country", rs.getString("Country"));
  // row.put("Item Type", rs.getString("Item Type"));
  // row.put("Sales Channel", rs.getString("Sales Channel"));
  // row.put("Order Priority", rs.getString("Order Priority"));
  // row.put("Order Date", rs.getString("Order Date"));
  // row.put("Order ID", rs.getString("Order ID"));
  // row.put("Ship Date", rs.getString("Ship Date"));
  // row.put("Units Sold", rs.getString("Units Sold"));
  // row.put("Unit Price", rs.getString("Unit Price"));
  // row.put("Unit Cost", rs.getString("Unit Cost"));
  // row.put("Total Revenue", rs.getString("Total Revenue"));
  // row.put("Total Cost", rs.getString("Total Cost"));
  // row.put("Total Profit", rs.getString("Total Profit"));
  // row.put("Order Processing Time", rs.getString("Order Processing Time"));
  // row.put("Gross Margin", rs.getString("Gross Margin"));
  // allData.add(row);
  // }
  // }
  // return allData;
  // }

  public static boolean setCurrentDirectory(String directory_name) {
    boolean result = false; // Boolean indicating whether directory was set
    File directory; // Desired current working directory

    directory = new File(directory_name).getAbsoluteFile();
    if (directory.exists() || directory.mkdirs()) {
      result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
    }

    return result;
  }

  private void uploadDatabaseToS3(String filePath, String fileName, AmazonS3 s3Client, LambdaLogger logger) {
    try {
      File dbFile = new File(filePath);
      s3Client.putObject(new PutObjectRequest(S3_BUCKET_NAME, fileName, dbFile));
      logger.log("Database uploaded to S3 successfully.");
    } catch (Exception e) {
      logger.log("Error uploading database to S3: " + e.getMessage());
    }
  }
}
