package lambda;

/**
 *
 * @author Wes Lloyd
 */
public class Request {

    String name;
    String bucketname;
    String filename;

    public String getBucketname() {
        return bucketname;
    }
    
    public void setBucketname(String theBucketname){
        bucketname = theBucketname;
    }

    public String getFilename () {
        return filename;
    }

    public void setFilename(String theFilename) {
        filename = theFilename;
    }

    public String getName() {
        return name;
    }
    
    public String getNameALLCAPS() {
        return name.toUpperCase();
    }

    public void setName(String name) {
        this.name = name;
    }

    public Request(String name) {
        this.name = name;
    }

    public Request() {

    }
}
