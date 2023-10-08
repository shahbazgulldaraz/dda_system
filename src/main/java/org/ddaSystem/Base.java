package org.ddaSystem;

import org.json.JSONObject;
import org.sqlite.SQLiteConfig;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Base {
    Random random = new Random();


    private final String DATABASE_URL = databaseURL();


    public List<String> getConnectedDevices() throws IOException, InterruptedException {
//        System.out.println("\n\n\nGetting connected devices...\n\n\n");
        List<String> devices = new ArrayList<>();
        Process process = Runtime.getRuntime().exec("adb devices");
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        Pattern pattern = Pattern.compile("^(\\S+)\\s+device$"); // match the first non-whitespace characters
        while ((line = reader.readLine()) != null) {
            if (line.endsWith("device")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    devices.add(matcher.group(1));
                }
            }
        }
        if (devices.isEmpty()) {
            System.out.println("\n\n\nNo device connected to the system. Terminating the program.!!!!\n\n\n");
//            System.exit(0); Temprary blocked for development.
        } else {
            System.out.println("\n\nConnected devices are >> " + devices);
        }
        return devices;
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

//    public void insertExecutionRecord(Venture venture, String device, int deviceOSVersion, Buyer buyer, String executionStartTime, String executionEndTime) {
//        String insertQuery = "INSERT INTO Execution (Execution_venture, Execution_Device, Execution_Device_OS_Version, Execution_Buyer, Execution_Date, Execution_Time_End) " +
//                "VALUES (?, ?, ?, ?, ?, ?)";
//
//        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
//             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
//
//            // Set the values for the PreparedStatement
//            preparedStatement.setString(1, venture.getName());
//            preparedStatement.setString(2, device);
//            preparedStatement.setInt(3, deviceOSVersion);
//            preparedStatement.setString(4, buyer.getEmail());
//            preparedStatement.setString(5, executionStartTime); // Start time
//            preparedStatement.setString(6, executionEndTime); // End time
//
//            // Execute the INSERT query
//            preparedStatement.executeUpdate();
//
//            System.out.println("Execution record inserted successfully.");
//        } catch (SQLException e) {
//            e.printStackTrace();
//            System.err.println("Error inserting execution record: " + e.getMessage());
//        }
//    }


    public void insertExecutionRecord(Venture venture, String device, int deviceOSVersion, Buyer buyer, java.sql.Timestamp startTime) throws SQLException {
        String insertQuery = "INSERT INTO Execution (Execution_venture, Execution_Device, Execution_Device_OS_Version, Execution_Buyer,Execution_Buyer_Password, Execution_Date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            // Set the values for the PreparedStatement
            preparedStatement.setString(1, venture.getName());
            preparedStatement.setString(2, device);
            preparedStatement.setInt(3, deviceOSVersion);
            preparedStatement.setString(4, buyer.getEmail());
            preparedStatement.setString(5, buyer.getPassword());

            // Format the timestamp to the desired format "yyyy-MM-dd HH:mm:ss"
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedStartTime = dateFormat.format(new Date(startTime.getTime()));

            preparedStatement.setString(6, formattedStartTime);

            // Execute the INSERT query
            preparedStatement.executeUpdate();

            System.out.println("Execution record inserted successfully.");
        }
    }

    public void updateExecutionEndTime(Venture venture, String device, java.sql.Timestamp startTime, java.sql.Timestamp endTime) throws SQLException {
        String updateQuery = "UPDATE Execution SET Execution_Time_End = ? WHERE Execution_venture = ? AND Execution_Device = ? AND Execution_Date = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            // Set the values for the PreparedStatement
            preparedStatement.setTimestamp(1, endTime);
            preparedStatement.setString(2, venture.getName());
            preparedStatement.setString(3, device);
            preparedStatement.setTimestamp(4, startTime);

            // Execute the UPDATE query
            preparedStatement.executeUpdate();

            System.out.println("Execution end time updated successfully.");
        }
    }

    public void updateExecutionEndTime(String deviceUDID, String ventureName) throws SQLException {
        String updateQuery = "UPDATE Execution SET Execution_Time_End = ? WHERE Execution_Device = ? AND Execution_venture = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            // Get the current timestamp
            java.sql.Timestamp currentTime = new java.sql.Timestamp(System.currentTimeMillis());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedCurrentTime = dateFormat.format(new Date(currentTime.getTime()));

            // Set the values for the PreparedStatement
            preparedStatement.setString(1, formattedCurrentTime);
            preparedStatement.setString(2, deviceUDID);
            preparedStatement.setString(3, ventureName);

            // Execute the UPDATE query
            preparedStatement.executeUpdate();

            System.out.println("Execution end time updated successfully.");
        }
    }





    //write a method to update dummy records in the database as per the DDL mentioned above.
    public void insertDummyRecords(){
        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {

        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


//    public void updateDeviceInfo(List<String> devices) {
//        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
//            for (String device : devices) {
//                String currentState = executeAdbCommand(device, " shell dumpsys window | grep \"mCurrentFocus\"").contains("com.daraz.android") ? "Occupied" : "Vacant";
//                String osVersion = executeAdbCommand(device, " shell getprop ro.build.version.release");
//                String deviceModel = executeAdbCommand(device, " shell getprop ro.product.model");
//                String deviceManufacturer = executeAdbCommand(device, " shell getprop ro.product.manufacturer");
//                String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
//
//                // Check if the record for the device already exists in the database
//                String checkQuery = "SELECT * FROM Devices WHERE DeviceName = ?";
//                try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
//                    checkStatement.setString(1, device);
//                    if (checkStatement.executeQuery().next()) {
//                        // Update the record
//                        String updateQuery = "UPDATE Devices SET CurrentState = ?, Made = ?, Model = ?, DeviceStatus = ?, CreatedDate = ? WHERE DeviceName = ?";
//                        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
//                            updateStatement.setString(1, currentState);
//                            updateStatement.setString(2, deviceManufacturer);
//                            updateStatement.setString(3, deviceModel);
//                            updateStatement.setString(4, osVersion);
//                            updateStatement.setString(5, currentDate);
//                            updateStatement.setString(6, device);
//                            updateStatement.executeUpdate();
//                        }
//                    } else {
//                        // Insert a new record
//                        String insertQuery = "INSERT INTO Devices (DeviceName, CurrentState, Made, Model, DeviceStatus, CreatedDate) VALUES (?, ?, ?, ?, ?, ?)";
//                        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
//                            insertStatement.setString(1, device);
//                            insertStatement.setString(2, currentState);
//                            insertStatement.setString(3, deviceManufacturer);
//                            insertStatement.setString(4, deviceModel);
//                            insertStatement.setString(5, osVersion);
//                            insertStatement.setString(6, currentDate);
//                            insertStatement.executeUpdate();
//                        }
//                    }
//                }
//            }
//        } catch (SQLException | IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    //this method will take the UDID as a string and run adb command to get the device information and update the Device_Free column in Device table.
    public boolean updateDeviceIsFree(String udid) throws InterruptedException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
//            boolean device_free = executeAdbCommand(udid, " shell dumpsys window | grep \"mCurrentFocus\"").contains("com.daraz.android") ? true : false;
            int device_free = random.nextInt(1 - 0 + 1) + 0;
//            if (device_free == 0){
//                //thread sleep for random between 1 to 3 minutes
//                Thread.sleep((random.nextInt(3 - 1 + 1) + 1)*60*1000);
//            }
            String updateQuery = "UPDATE Devices SET Device_Free = ? WHERE Device_UDID = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setString(1, String.valueOf(device_free));
                updateStatement.setString(2, udid);
                updateStatement.executeUpdate();
            }
            if(device_free == 0){
                System.out.println("\n\n\nDevice " + udid + " is occupied-updateDeviceIsFree\n\n\n");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
                System.out.println("\n\n\nDevice " + udid + " is free-updateDeviceIsFree\n\n\n");
                return true;

    }

    //this method will update the Device_Free column in Device table.
    public void updateDeviceIsFreeOrOccupied(String deviceUdid,String FreeOrNot) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
            String updateQuery = "UPDATE Devices SET Device_Free = ? WHERE Device_UDID = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setString(1, FreeOrNot);
                updateStatement.setString(2, deviceUdid);
                updateStatement.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


    public void updateJobIsFreeOrOccupied(String JobName,Boolean FreeOrNot) throws SQLException {
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

    public String constructURL(String Path) {
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
            return "/mnt/storage1/configs/DDASystem.db";
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


    public Buyer findAvailableBuyer(Venture venture) {
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
                    String status = resultSet.getString("Buyer_Free");
                    String buyerVenture = resultSet.getString("Buyer_Venture");
                    buyer = new Buyer(email, password, status, buyerVenture);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return buyer;
    }


//    public boolean isDeviceAvailable(String device) throws SQLException {
//        String checkQuery = "SELECT CurrentState FROM Devices WHERE DeviceName = ?";
//
//        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
//             PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
//
//            checkStatement.setString(1, device);
//
//            try (ResultSet resultSet = checkStatement.executeQuery()) {
//                if (resultSet.next()) {
//                    String currentState = resultSet.getString("CurrentState");
//                    if (currentState.equals("Vacant")) {
//                        return true;
//                    }
//                }
//            }
//        }
//
//        return false;
//    }

    //write a method which takes string "JobsName",and store it in the "Device_Name" column then it will break the string into two strings,
    // the string before "_os_" will be the Device_Name and the string after "_os_" is the Os_version and will be stored as OS_Version
    // if the "jobName" dont have "_os_" then store the whole string in the Device_Name column, in Jenkins_Jobs table.
    // This method will return the list of devices from  Jenkins_Jobs table in descending order.

    public void storeJobDetailsInDB(String jobName, boolean inProgress, Boolean job_in_queue) {
        // Define a pattern to match "deviceName_os_version" format
        Pattern pattern = Pattern.compile("^(\\w+)_os_(\\d+)$");
        Matcher matcher = pattern.matcher(jobName);

        if (matcher.matches()) {
            String deviceName = matcher.group(1);
            String osVersion = matcher.group(2);

            try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
                String selectQuery = "SELECT * FROM Jenkins_jobs WHERE Job_Name = ?";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                    selectStatement.setString(1, jobName);
                    ResultSet resultSet = selectStatement.executeQuery();

                    if (resultSet.next()) {
                        // If the job already exists, update its columns
                        String updateQuery = "UPDATE Jenkins_jobs SET Device_Name_In_Job = ?, Device_Os_Version = ?, Device_Is_Free = ?, Job_In_Queue = ? WHERE Job_Name = ?";
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                            updateStatement.setString(1, deviceName);
                            updateStatement.setString(2, osVersion);
                            updateStatement.setBoolean(3, !inProgress);
                            updateStatement.setBoolean(4, job_in_queue);
                            updateStatement.setString(5, jobName);
                            updateStatement.executeUpdate();
                        }
                    } else {
                        // If the job doesn't exist, insert a new record
                        String insertQuery = "INSERT INTO Jenkins_jobs (Job_Name, Device_Name_In_Job, Device_Os_Version, Device_Is_Free, Job_In_Queue) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                            insertStatement.setString(1, jobName);
                            insertStatement.setString(2, deviceName);
                            insertStatement.setString(3, osVersion);
                            insertStatement.setBoolean(4, !inProgress);
                            insertStatement.setBoolean(5, job_in_queue);
                            insertStatement.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Handle the exception appropriately
            }
        }
    }


    //get Job details from Jenkins_Jobs table in descending order of Device_Os_Version
    public List<String> getJobDetailsSortedByOSVersion() throws SQLException {
        List<String> jobDetails = new ArrayList<>();
        String selectQuery = "SELECT Job_Name FROM Jenkins_jobs WHERE Device_Is_Free = 0 ORDER BY Device_Os_Version DESC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                String jobName = resultSet.getString("Job_Name");
                jobDetails.add(jobName);
            }
        }
        return jobDetails;
    }



    public void DeviceStatusScheduler(String deviceUdids, String Status) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateDeviceIsFreeOrOccupied(deviceUdids,Status);
                if(Status.equals("1")){
                    System.out.println(deviceUdids+"<<:::Device is FreeNow by scheduler");
                }else {
                    System.out.println(deviceUdids+"<<:::Device is Occupied by scheduler");
                }

            } catch (SQLException e) {
                e.printStackTrace(); // Handle the exception as per your application's needs
            }
        }, 0, 10, TimeUnit.SECONDS);
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


    // Method to write a JSONObject to a file in a specified file path
    public void writeJsonToFile(JSONObject json, String filePath) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(json.toString(4));  // Write JSON with 4-space indentation
        }
    }



    public List<String> getAvailableDeviceUDIDsSortedByOSVersion() throws SQLException {
        List<String> availableDeviceUDIDs = new ArrayList<>();
        String selectQuery = "SELECT Device_UDID FROM Devices " +
                "WHERE Device_Status = 1 AND Device_Free = 1 " +
                "ORDER BY Device_OS_Version DESC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                String deviceUDID = resultSet.getString("Device_UDID");
                availableDeviceUDIDs.add(deviceUDID);
            }
        }

        return availableDeviceUDIDs;
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

    public String getJobOSVersion(String JobName) throws SQLException {
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

    public String findAvailableDeviceWithDifferentOS(List<String> availableDeviceUDIDs, Set<String> executedVenturesWithOS) throws SQLException {
        for (String device : availableDeviceUDIDs) {
            String osVersion = getDeviceOSVersion(device);
            if (!executedVenturesWithOS.contains(osVersion)) {
                return device;
            }
        }
        return null;
    }

    public boolean hasVentureBeenExecutedToday(String ventureName) {
        boolean executedToday = false;
        String selectQuery = "SELECT COUNT(*) FROM Execution " +
                "WHERE Execution_venture = ? AND DATE(Execution_Date) = DATE('now', 'localtime')";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

            selectStatement.setString(1, ventureName);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    executedToday = count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return executedToday;
    }

    public List<Venture> getVenturesWithPriorities() throws SQLException {
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

    public String checkCurrentState(List<String> devices) throws IOException, InterruptedException {
        String status = "";
        for (String device : devices) {
            Process process = Runtime.getRuntime().exec("adb -s " + device + " shell dumpsys window | grep \"mCurrentFocus\"");
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                status = line;
            }
        }
        if(status.contains("com.daraz.android")){
            status = "Occupied";
        }else {
            status = "Vacant";
        }
        return status;
    }

    public boolean checkDeviceIsFree(String device) throws IOException, InterruptedException {
        String status = "";
        Process process = Runtime.getRuntime().exec("adb -s " + device + " shell dumpsys window | grep \"mCurrentFocus\"");
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            status = line;
        }
        if(status.contains("com.daraz.android")){
            return false;
        }else {
            return true;
        }
    }

    public List<String> getAvailableDeviceUDIDs() throws SQLException {
        List<String> availableDeviceUDIDs = new ArrayList<>();
        String selectQuery = "SELECT Device_UDID FROM Devices WHERE Device_Status = 1 AND Device_Free = 1";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                String deviceUDID = resultSet.getString("Device_UDID");
                availableDeviceUDIDs.add(deviceUDID);
            }
        }

        return availableDeviceUDIDs;
    }

    public boolean isDeviceAvailable(String device) throws SQLException {
        String selectQuery = "SELECT Device_Status FROM Devices WHERE Device_UDID = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

            selectStatement.setString(1, device);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    String deviceStatus = resultSet.getString("Device_Status");
                    if (deviceStatus.equals("Vacant")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private List<String> fetchRecords(String columnName, String tableName, String whereStatement) throws SQLException {
        List<String> records = new ArrayList<>();

        String query = "SELECT " + columnName + " FROM " + tableName + " WHERE " + whereStatement;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                records.add(resultSet.getString(columnName));
            }
        }

        return records;
    }


}