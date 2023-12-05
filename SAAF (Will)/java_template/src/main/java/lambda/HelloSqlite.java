package lambda;

import com.amazonaws.Response;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import saaf.Inspector;

;

/**
 * uwt.lambda_test::handleRequest
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class HelloSqlite implements RequestHandler<Request, HashMap<String, Object>> {
  /**
   * Lambda Function Handler
   *
   * @param request Request POJO with defined variables from Request.java
   * @param context
   * @return HashMap that Lambda will automatically convert into JSON.
   */
  @Override
  public HashMap<String, Object> handleRequest(Request request, Context context) {
    LambdaLogger logger = context.getLogger();
    HashMap<String, Object> response = new HashMap<>();

    try {
      AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
      S3Object s3Object = s3Client.getObject(new GetObjectRequest(request.getBucketname(), request.getFilename()));
      try (Connection con = DriverManager.getConnection("jdbc:sqlite:");
          InputStream objectData = s3Object.getObjectContent();
          Scanner scanner = new Scanner(objectData)) {

        ensureTableExists(con, logger);
        insertDataIntoTable(scanner, con, logger);
        response.put("OrderIDs", queryTable(con, logger));
      } catch (Exception e) {
        logger.log("Database or S3 Error: " + e.getMessage());
        response.put("error", e.getMessage());
      }

      Thread.sleep(200); // Ensuring separate Lambda instances for concurrent calls
    } catch (Exception e) {
      logger.log("Error: " + e.getMessage());
      response.put("error", e.getMessage());
    }

    return response;
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

  private LinkedList<String> queryTable(Connection con, LambdaLogger logger) throws Exception {
    LinkedList<String> orderIDs = new LinkedList<>();
    try (PreparedStatement ps = con.prepareStatement("SELECT * FROM mytable");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        orderIDs.add(rs.getString("Order ID"));
      }
    }
    return orderIDs;
  }

  public static boolean setCurrentDirectory(String directory_name) {
    boolean result = false; // Boolean indicating whether directory was set
    File directory; // Desired current working directory

    directory = new File(directory_name).getAbsoluteFile();
    if (directory.exists() || directory.mkdirs()) {
      result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
    }

    return result;
  }
}
