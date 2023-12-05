package lambda;
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

import saaf.Response;
;
/**
 * uwt.lambda_test::handleRequest
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class HelloSqlite
  implements RequestHandler<Request, HashMap<String, Object>> {

  /**
   * Lambda Function Handler
   *
   * @param request Request POJO with defined variables from Request.java
   * @param context
   * @return HashMap that Lambda will automatically convert into JSON.
   */
  public HashMap<String, Object> handleRequest(Request request, Context context) {
    // Create logger
    LambdaLogger logger = context.getLogger();

    //Collect inital data.
    Inspector inspector = new Inspector();
    inspector.inspectAll();

    //****************START FUNCTION IMPLEMENTATION*************************
    //Add custom key/value attribute to SAAF's output. (OPTIONAL)
    //Grabbing our CSV from the bucket.
    String bucketname = request.getBucketname();
    String filename = request.getFilename();
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

    //get object file using source bucket and srcKey name
    S3Object s3Object = s3Client.getObject(
      new GetObjectRequest(bucketname, filename)
    );
    //get content of the file
    try {
      // Connection string an in-memory SQLite DB
      Connection con = DriverManager.getConnection("jdbc:sqlite:");

      // Connection string for a file-based SQlite DB
      //Connection con = DriverManager.getConnection("jdbc:sqlite:/tmp/mytest.db");

      // Detect if the table 'mytable' exists in the database
      // Check if 'mytable' exists and create it if not
      PreparedStatement ps = con.prepareStatement(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='mytable'"
      );
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        logger.log("Table 'mytable' does not exist. Creating it.");
        ps =
          con.prepareStatement(
            "CREATE TABLE mytable (Region TEXT, Country TEXT, [Item Type] TEXT, [Sales Channel] TEXT, [Order Priority] TEXT, [Order Date] TEXT, [Order ID] TEXT, [Ship Date] TEXT, [Units Sold] TEXT, [Unit Price] TEXT, [Unit Cost] TEXT, [Total Revenue] TEXT, [Total Cost] TEXT, [Total Profit] TEXT, [Order Processing Time] TEXT, [Gross Margin] TEXT)"
          );
        ps.execute();
      }
      rs.close();

      InputStream objectData = s3Object.getObjectContent();
      Scanner scanner = new Scanner(objectData);
      String sql =
        "INSERT INTO mytable (Region, Country, [Item Type], [Sales Channel], [Order Priority], [Order Date], [Order ID], [Ship Date], [Units Sold], [Unit Price], [Unit Cost], [Total Revenue], [Total Cost], [Total Profit], [Order Processing Time], [Gross Margin]) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

      try {
        ps = con.prepareStatement(sql);
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          String[] words = line.split(",");
          int i = 1;
          for (String word : words) {
            ps.setString(i++, word);
          }
          ps.executeUpdate();
        }
      } catch (SQLException e) {
        e.printStackTrace(); // Handle the exception properly
      } finally {
        // Close resources
        scanner.close();
        ps.close();
      }

      //Create and populate a separate response object for function output. (OPTIONAL)
      Response r = new Response();

      String pwd = System.getProperty("user.dir");
      logger.log("pwd=" + pwd);
      logger.log("set pwd to tmp");
      setCurrentDirectory("/tmp");
      pwd = System.getProperty("user.dir");
      logger.log("pwd=" + pwd);

      // Query mytable to obtain full resultset
      ps = con.prepareStatement("select * from mytable;");
      rs = ps.executeQuery();

      // Load query results for [name] column into a Java Linked List
      // ignore [col2] and [col3]
      LinkedList<String> ll = new LinkedList<String>();
      while (rs.next()) {
        logger.log("Order ID=" + rs.getString("Order ID"));
        ll.add(rs.getString("Order ID"));
      }
      rs.close();
      con.close();

      // sleep to ensure that concurrent calls obtain separate Lambdas
      try {
        Thread.sleep(200);
      } catch (InterruptedException ie) {
        logger.log("interrupted while sleeping...");
      }
    } catch (SQLException sqle) {
      logger.log("DB ERROR:" + sqle.toString());
      sqle.printStackTrace();
    }

    // Set return result in Response class, class is marshalled into JSON
    //****************END FUNCTION IMPLEMENTATION***************************

    //Collect final information such as total runtime and cpu deltas.
    inspector.inspectAllDeltas();
    return inspector.finish();
  }

  public static boolean setCurrentDirectory(String directory_name) {
    boolean result = false; // Boolean indicating whether directory was set
    File directory; // Desired current working directory

    directory = new File(directory_name).getAbsoluteFile();
    if (directory.exists() || directory.mkdirs()) {
      result =
        (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
    }

    return result;
  }
}

 