import java.io.*;

/**
 * This class is used by follower, client and master to read the config.properties file
 * It is a singleton, and each class accesses the same instance.
 * The config file is written by hand before each run.
 */
public class props {
    private java.util.Properties prop;
    private static final props instance = new props();
    OutputStream output;

    /**
     * Creates new properties object and reads the config file into it.
     */
    private props() {
        try {
            prop = new java.util.Properties();

            InputStream inputStream = new FileInputStream("./src/config.properties");
            prop.load(inputStream);

    /*
            output = new FileOutputStream("application.properties");
*/

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '"  + "' not found in the classpath");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * returns the instance
     * @return props instance
     */
    public static props getInstance(){
        return instance;
    }

    /**
     * returns the data port from the config.properties file
     * @return int dataPort
     */
    public int getDataPort() {
        return Integer.parseInt(prop.getProperty("dataPort"));
    }

    /**
     * returns the command port from the config.properties file
     * @return int commandPort
     */
    public int getCommandPort() {
        return Integer.parseInt(prop.getProperty("commandPort"));
    }

    /**
     * returns the masterIP from the config.properties file
     * @return String masterIP
     */
    public String getMasterIP() {
        return prop.getProperty("masterIP");
    }

    /*public void setMasterIP(String masterIP) throws IOException {
        prop.setProperty("masterIP", masterIP);
        prop.store(output, null);
    }*/

    /**
     * returns the mongoUser from the config.properties file
     * @return String mongoUser
     */
    public String getMongoUser() {
        return prop.getProperty("mongoUser");
    }

    /**
     * returns the mongoPass from the config.properties file
     * @return String mongoPass
     */
    public String getMongoPass() {
        return prop.getProperty("mongoPass");
    }

    /**
     *
     * @return String representation of the config file
     */
    @Override
    public String toString() {
        return "props{" +
                "prop=" + prop +
                '}';
    }
}
