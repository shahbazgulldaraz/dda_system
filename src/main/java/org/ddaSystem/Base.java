package org.ddaSystem;

import org.sqlite.SQLiteConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.SystemUtils;

public class Base {
    Random random = new Random();
    private final String DATABASE_URL = databaseURL();

    // 95%
    //2Days Regresssion.
    //success stories in squads.

    public List<String> getConnectedDevices() throws IOException, InterruptedException {
        List<String> devices = new ArrayList<>();
        Process process = null;

        try {
            String command;

            if (SystemUtils.IS_OS_MAC) {
                System.out.println("\n\n\nGetting connected devices via SSH...\n\n\n");
                // SSH into the Ubuntu machine and run adb devices
                command = "ssh qaautomation@30.216.6.58 adb devices";
            } else {
                // Run ADB command directly on Mac
                System.out.println("\n\n\nGetting connected devices...\n\n\n");
                command = "adb devices";
            }

            // Execute the command
            process = Runtime.getRuntime().exec(command);

            // Capture the output of the command
            String output = captureOutput(process);

            // Process the output of adb devices and get the list of devices
            devices = processDevicesOutput(output);

        } finally {
            if (process != null) {
                process.waitFor(); // Wait for the process to finish
            }
        }

        handleDeviceResults(devices);
        return devices;
    }

    private String captureOutput(Process process) throws IOException {
        StringBuilder outputBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
//                System.out.println("And this is LINE::>" + line);
                outputBuilder.append(line).append("\n");
//                System.out.println("this is the OutPutLine:::>" + outputBuilder);
            }
        }

        // Output for debugging purposes
//        System.out.println("Command output:\n" + outputBuilder.toString());

        return outputBuilder.toString();
    }

    private List<String> processDevicesOutput(String output) throws IOException {
        List<String> devices = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
            String line;
            Pattern pattern = Pattern.compile("^(\\S+)\\s+device$");

            while ((line = reader.readLine()) != null) {
                if (line.endsWith("device")) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        devices.add(matcher.group(1));
                    }
                }
            }
        }

        return devices;
    }

    private void handleDeviceResults(List<String> devices) {
        if (devices.isEmpty()) {
            System.out.println("\n\nNo devices connected. Please connect a device and try again.\n\n");
            System.exit(0);
        } else {
            System.out.println("\n\nConnected devices are >> " + devices);
        }
    }
    public String executeAdbCommand(String device, String command) throws IOException, InterruptedException {
        String status = "";
        System.out.println("\n\n\nExecuting command >> " + command + "\n\n\n");
        System.out.println("\n\n\nDevice >> " + device + "\n\n\n");
        Process process = Runtime.getRuntime().exec("adb -s " + device + command  );
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            status = line;
        }

        return status;
    }

    public void setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            Connection connection = DriverManager.getConnection(DATABASE_URL, config.toProperties());

            // Create Ventures table
            String createVenturesTable = "CREATE TABLE IF NOT EXISTS Ventures (\n"
                    + "    Venture_ID INTEGER PRIMARY KEY,\n"
                    + "    Venture_Name TEXT NOT NULL,\n"
                    + "    Venture_Priority INTEGER\n"
                    + ");";
            PreparedStatement createVenturesStatement = connection.prepareStatement(createVenturesTable);
            createVenturesStatement.execute();

            // Create Buyers table
            String createBuyersTable = "CREATE TABLE IF NOT EXISTS Buyers (\n"
                    + "    Buyer_ID INTEGER PRIMARY KEY,\n"
                    + "    Buyer_Email TEXT NOT NULL,\n"
                    + "    Buyer_Password TEXT NOT NULL,\n"
                    + "    Buyer_Venture TEXT NOT NULL,\n"
                    + "    Buyer_Free BOOLEAN DEFAULT 1,\n"
                    + "    Buyer_Created_Date TEXT NOT NULL,\n"
                    + "    FOREIGN KEY (Buyer_Venture) REFERENCES Ventures (Venture_Name)\n"
                    + ");";
            PreparedStatement createBuyersStatement = connection.prepareStatement(createBuyersTable);
            createBuyersStatement.execute();

            // Create Devices table
            String createDevicesTable = "CREATE TABLE IF NOT EXISTS Devices (\n"
                    + "    Device_ID INTEGER PRIMARY KEY,\n"
                    + "    Device_UDID TEXT NOT NULL,\n"
                    + "    Device_OS_Version INTEGER,\n"
                    + "    Device_Name TEXT,\n"
                    + "    Device_Status BOOLEAN DEFAULT 0,\n"
                    + "    Device_Made_By TEXT,\n"
                    + "    Device_Model TEXT,\n"
                    + "    Device_Free BOOLEAN DEFAULT 1,\n"
                    + "    Device_Created_Date TEXT NOT NULL\n"
                    + ");";
            PreparedStatement createDevicesStatement = connection.prepareStatement(createDevicesTable);
            createDevicesStatement.execute();

            // Create Execution table
            String createExecutionTable = "CREATE TABLE IF NOT EXISTS Execution (\n"
                    + "    Execution_ID INTEGER PRIMARY KEY,\n"
                    + "    Execution_venture TEXT NOT NULL,\n"
                    + "    Execution_Device TEXT NOT NULL,\n"
                    + "    Execution_Device_OS_Version INTEGER,\n"
                    + "    Execution_Buyer TEXT NOT NULL,\n"
                    + "    Execution_Date TEXT NOT NULL,\n"
                    + "    FOREIGN KEY (Execution_venture) REFERENCES Ventures (Venture_Name),\n"
                    + "    FOREIGN KEY (Execution_Device) REFERENCES Devices (Device_UDID),\n"
                    + "    FOREIGN KEY (Execution_Device_OS_Version) REFERENCES Devices (Device_OS_Version),\n"
                    + "    FOREIGN KEY (Execution_Buyer) REFERENCES Buyers (Buyer_Email)\n"
                    + ");";
            PreparedStatement createExecutionStatement = connection.prepareStatement(createExecutionTable);
            createExecutionStatement.execute();

            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }


    public void insertExecutionRecord_DB(Venture venture, String device, int deviceOSVersion, String buyer, java.sql.Timestamp startTime) throws SQLException {
        String insertQuery = "INSERT INTO Execution (Execution_venture, Execution_Device, Execution_Device_OS_Version, Execution_Buyer,Execution_Buyer_Password, Execution_Date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            // Set the values for the PreparedStatement
            preparedStatement.setString(1, venture.getName());
            preparedStatement.setString(2, device);
            preparedStatement.setInt(3, deviceOSVersion);
            preparedStatement.setString(4, buyer);
            preparedStatement.setString(5, "123456");

            // Format the timestamp to the desired format "yyyy-MM-dd HH:mm:ss"
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedStartTime = dateFormat.format(new Date(startTime.getTime()));

            preparedStatement.setString(6, formattedStartTime);

            // Execute the INSERT query
            preparedStatement.executeUpdate();

//            System.out.println("Execution record inserted successfully.");
        }
//        updateJobIsFreeOrOccupied_DB(device,false);
//        updateBuyerIsFreeOrOccopied_DB(buyer,false);
    }

    private void updateBuyerIsFreeOrOccopied_DB(String email, boolean b) {
        String updateQuery = "UPDATE Buyers SET Buyer_Free = ? WHERE Buyer_Email = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            // Set the values for the PreparedStatement
            preparedStatement.setBoolean(1, b);
            preparedStatement.setString(2, email);

            // Execute the UPDATE query
            preparedStatement.executeUpdate();

//            System.out.println("Buyer free or occupied updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error updating buyer free or occupied: " + e.getMessage());
        }
    }

    public void cleanUpDataBase_DB() {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {

                    String[] queries = {
                    "PRAGMA foreign_keys=off;",
                    "DELETE FROM Execution;",
                    "DELETE FROM Jenkins_jobs;",
//                    No need of these two queries, because we are already deteting the execution and Jenkins_jobs table, when there are no records in the db what will it update.
//                    "UPDATE Jenkins_jobs SET Device_Is_Free = 1;",
//                    "UPDATE Jenkins_jobs SET Job_In_Queue = 0;",
                    "UPDATE Buyers SET Buyer_Free =1;",
                    "PRAGMA foreign_keys=on;"
            };

            for (String query : queries) {
                statement.execute(query);
            }

            System.out.println("Database Cleaned!!!! \nQueries executed successfully.!!!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateJobIsFreeOrOccupied_DB(String JobName, Boolean FreeOrNot) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
            String updateQuery = "UPDATE Jenkins_jobs SET Device_Is_Free = ? WHERE Job_Name = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setBoolean(1, FreeOrNot);
                updateStatement.setString(2, JobName);
                updateStatement.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String constructJenkinsURL(String Path) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")|| osName.contains("Ubuntu")) {
            // If the operating system is Linux
            return "http://localhost:8080"+Path;
        } else {
            // If the operating system is macOS
            return "http://30.216.6.58:8080"+Path;
        }
    }

    public String databaseURL() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")|| osName.contains("Ubuntu")) {
            // If the operating system is Linux
            return "jdbc:sqlite:/mnt/storage1/configs/DDASystem.db";
        } else {
            // If the operating system is macOS
            return "jdbc:sqlite:DDASystem.db";
        }
    }


    public void updateDeviceInformation(List<String> devices) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
            for (String device : devices) {
                //random between 0 and 1 for device status
                String currentState = String.valueOf(random.nextInt(1 - 0 + 1) + 0);
                String osVersion = String.valueOf(random.nextInt(13 - 7 + 1) + 7);
                String deviceModel = "TestingModel";
                String deviceManufacturer = "TestingManufacturer";
//                String currentState = executeAdbCommand(device, " shell dumpsys window | grep \"mCurrentFocus\"").contains("com.daraz.android") ? "0" : "1";
//                String osVersion = executeAdbCommand(device, " shell getprop ro.build.version.release");
//                String deviceModel = executeAdbCommand(device, " shell getprop ro.product.model");
//                String deviceManufacturer = executeAdbCommand(device, " shell getprop ro.product.manufacturer");
                String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                String selectQuery = "SELECT * FROM Devices WHERE Device_UDID = ?";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                    selectStatement.setString(1, device);
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        if (resultSet.next()) {
                            // Update the existing record
                            String updateQuery = "UPDATE Devices SET Device_Status = ?, Device_Made_By = ?, Device_Model = ?, Device_OS_Version = ?, Device_Created_Date = ? WHERE Device_UDID = ?";
                            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                                updateStatement.setString(1, currentState);
                                updateStatement.setString(2, deviceManufacturer);
                                updateStatement.setString(3, deviceModel);
                                updateStatement.setString(4, osVersion);
                                updateStatement.setString(5, currentDate);
                                updateStatement.setString(6, device);
                                updateStatement.executeUpdate();
                            }
                        } else {
                            // Insert a new record
                            String insertQuery = "INSERT INTO Devices (Device_UDID, Device_Status, Device_Made_By, Device_Model, Device_OS_Version, Device_Created_Date) VALUES (?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                                insertStatement.setString(1, device);
                                insertStatement.setString(2, currentState);
                                insertStatement.setString(3, deviceManufacturer);
                                insertStatement.setString(4, deviceModel);
                                insertStatement.setString(5, osVersion);
                                insertStatement.setString(6, currentDate);
                                insertStatement.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public Set<String> getBuyerSet() throws SQLException {
        Set<String> emailVentureSet = new HashSet<>();

        String query = "SELECT Buyer_Email, Buyer_Venture FROM Buyers";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String email = resultSet.getString("Buyer_Email");
                String venture = resultSet.getString("Buyer_Venture");
                emailVentureSet.add(email + " - " + venture);
            }
        }

        return emailVentureSet;
    }


    public Buyer findAvailableBuyer_DB(Venture venture) {
        Buyer buyer = null;
        String query = "SELECT * FROM Buyers WHERE Buyer_Free = ? AND Buyer_Venture = ? ORDER BY RANDOM() LIMIT 1";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setBoolean(1, true); // Assuming "Active" corresponds to true
            statement.setString(2, venture.getName());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String email = resultSet.getString("Buyer_Email");
                    String password = resultSet.getString("Buyer_Password");
                    String buyerVenture = resultSet.getString("Buyer_Venture");
                    String status = resultSet.getString("Buyer_Free");
                    buyer = new Buyer(email, password, status, buyerVenture);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return buyer;
    }

    //write a method which takes string "JobsName",and store it in the "Device_Name" column then it will break the string into two strings,
    // the string before "_os_" will be the Device_Name and the string after "_os_" is the Os_version and will be stored as OS_Version
    // if the "jobName" dont have "_os_" then store the whole string in the Device_Name column, in Jenkins_Jobs table.
    // This method will return the list of devices from  Jenkins_Jobs table in descending order.

    public void     storeJobDetailsIn_DB(String jobName, boolean inProgress, Boolean job_in_queue, List<String> devices) throws IOException, InterruptedException {
        String deviceName;
        String osVersion;
        String deviceUdid;

        System.out.println("I am here in storeJobDetailsIn_DB : >"+jobName+" "+inProgress+" "+job_in_queue);
            // Use a more robust regular expression to match the jobName pattern
            if (jobName.matches("^(\\w+)_os_(\\d+)_udid_(\\w+)$")) {
                Matcher matcher = Pattern.compile("^(\\w+)_os_(\\d+)_udid_(\\w+)$").matcher(jobName);

                // Call matches() to ensure the pattern matches the input string
                if (matcher.matches()) {
                    deviceName = matcher.group(1);
                    osVersion = matcher.group(2);
                    deviceUdid = matcher.group(3);
                    if(devices.contains(deviceUdid)) {
                        System.out.println("\n\n\nThis Is the Data to insert " + jobName + " " + deviceName + " " + osVersion + " " + deviceUdid + " " + inProgress + " " + job_in_queue);
                        insertJobNameAndOSIn_DB(jobName, deviceName,osVersion, deviceUdid, inProgress, job_in_queue);
                    }
                } else {
                    System.out.println("These are the invalid Job Names:>" + jobName);
                    return;
                }
            } else {
                System.out.println("These are the invalid Job Names:>" + jobName);
                return;
            }
    }


    private void insertJobNameAndOSIn_DB(String jobName, String deviceName, String osVersion, String deviceUdid, boolean inProgress, boolean job_in_queue) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
            String selectQuery = "SELECT * FROM Jenkins_jobs WHERE Job_Name = ?";
            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                selectStatement.setString(1, jobName);
                ResultSet resultSet = selectStatement.executeQuery();

                if (resultSet.next()) {
                    // If the job already exists, update its columns
                    String updateQuery = "UPDATE Jenkins_jobs SET Device_Name_In_Job = ?, Device_Udid = ?, Device_Os_Version = ?, Device_Is_Free = ?, Job_In_Queue = ? WHERE Job_Name = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, deviceName);
                        updateStatement.setString(2, deviceUdid);
                        updateStatement.setInt(3, Integer.parseInt(osVersion));
                        updateStatement.setBoolean(4, !inProgress);
                        updateStatement.setBoolean(5, job_in_queue);
                        updateStatement.setString(6, jobName);
                        updateStatement.executeUpdate();
                    }
                } else {
                    // If the job doesn't exist, insert a new record
                    String insertQuery = "INSERT INTO Jenkins_jobs (Job_Name, Device_Name_In_Job, Device_Udid, Device_Os_Version, Device_Is_Free, Job_In_Queue) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                        insertStatement.setString(1, jobName);
                        insertStatement.setString(2, deviceName);
                        insertStatement.setString(3, deviceUdid);
                        insertStatement.setInt(4, Integer.parseInt(osVersion));
                        insertStatement.setBoolean(5, !inProgress);
                        insertStatement.setBoolean(6, job_in_queue);
                        insertStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }
    }



//    private void    insertJobNameAndOSIn_DB(String jobName, String deviceName, String osVersion, String deviceUdid, boolean inProgress, boolean job_in_queue) {
//        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
//            String selectQuery = "SELECT * FROM Jenkins_jobs WHERE Job_Name = ?";
//            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
//                selectStatement.setString(1, jobName);
//                ResultSet resultSet = selectStatement.executeQuery();
//
//                if (resultSet.next()) {
//                    // If the job already exists, update its columns
//                    String updateQuery = "UPDATE Jenkins_jobs SET Device_Name_In_Job = ?, Device_Udid = ?, Device_Os_Version = ?, Device_Is_Free = ?, Job_In_Queue = ? WHERE Job_Name = ?";
//                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
//                        updateStatement.setString(1, deviceName);
//                        updateStatement.setString(2, deviceUdid);
//                        updateStatement.setInt(3, Integer.parseInt(osVersion));
//                        updateStatement.setBoolean(4, !inProgress);
//                        updateStatement.setBoolean(5, job_in_queue);
//                        updateStatement.setString(6, jobName);
//                        updateStatement.executeUpdate();
//                    }
//                } else {
//                    // If the job doesn't exist, insert a new record
//                    String insertQuery = "INSERT INTO Jenkins_jobs (Job_Name, Device_Name_In_Job, Device_Udid, Device_Os_Version, Device_Is_Free, Job_In_Queue) VALUES (?, ?, ?, ?, ?)";
//                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
//                        insertStatement.setString(1, jobName);
//                        insertStatement.setString(2, deviceName);
//                        insertStatement.setString(3, deviceUdid);
//                        insertStatement.setInt(4, Integer.parseInt(osVersion));
//                        insertStatement.setBoolean(5, !inProgress);
//                        insertStatement.setBoolean(6, job_in_queue);
//                        insertStatement.executeUpdate();
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace(); // Handle the exception appropriately
//        }
//    }


    //get Job details from Jenkins_Jobs table in descending order of Device_Os_Version
    public List<String> getJobDetailsSortedByOSVersion_DB() throws SQLException {
        List<String> jobDetails = new ArrayList<>();
        String selectQuery = "SELECT Job_Name FROM Jenkins_jobs WHERE Job_In_Queue = 0 ORDER BY Device_Os_Version DESC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = selectStatement.executeQuery())
        {
            while (resultSet.next()) {
                String jobName = resultSet.getString("Job_Name");
                jobDetails.add(jobName);
            }
        }
        return jobDetails;
    }


    // Method to convert a duration in milliseconds to a readable format
    public static String convertToReadableTimeFormat(long milliseconds) {
        long seconds = (milliseconds / 1000) % 60;    // Extract seconds
        long minutes = (milliseconds / (1000 * 60)) % 60;    // Extract minutes
        long hours = (milliseconds / (1000 * 60 * 60)) % 24;  // Extract hours
        long days = milliseconds / (1000 * 60 * 60 * 24);     // Extract days

        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
    }

    // Method to convert a timestamp in milliseconds to a readable date and time format
    public static String convertToReadableDateAndTimeFormat(long milliseconds) {
        Date date = new Date(milliseconds);  // Create a Date object from the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm a"); // Define date format
        return sdf.format(date);  // Format the date and return as a string
    }

    // Method to encode username and password for HTTP Basic Authentication
    public String getEncodedCredentials(String username, String password) {
        String credentials = username + ":" + password;  // Combine username and password
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());  // Encode to Base64
    }


    public String getDeviceOSVersion(String deviceUDID) throws SQLException {
        String osVersion = null;
        String selectQuery = "SELECT Device_OS_Version FROM Devices " +
                "WHERE Device_UDID = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

            selectStatement.setString(1, deviceUDID);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    osVersion = resultSet.getString("Device_OS_Version");
                }
            }
        }

        return osVersion;
    }

    public String getJobOSVersion_DB(String JobName) throws SQLException {
        String osVersion = null;
        String selectQuery = "SELECT Device_Os_Version FROM Jenkins_jobs " +
                "WHERE Job_Name = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

            selectStatement.setString(1, JobName);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    osVersion = resultSet.getString("Device_OS_Version");
                }
            }
        }

        return osVersion;
    }

    public List<Venture> getVenturesWithPriorities_DB() throws SQLException {
        List<Venture> ventures = new ArrayList<>();
        String selectQuery = "SELECT Venture_Name, Venture_Priority FROM Ventures " +
                "ORDER BY Venture_Priority ASC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                String ventureName = resultSet.getString("Venture_Name");
                int venturePriority = resultSet.getInt("Venture_Priority");
                Venture venture = new Venture(ventureName, venturePriority);
                ventures.add(venture);
            }
        }

        // Sort the ventures based on Venture_Priority
        ventures.sort(Comparator.comparingInt(Venture::getPriority));
//        System.out.println("\n\n\nVentures with priorities are: " + ventures + "\n\n\n");
        return ventures;
    }
}
