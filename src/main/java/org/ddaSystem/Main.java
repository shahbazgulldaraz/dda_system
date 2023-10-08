package org.ddaSystem;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Main {

    Base base = new Base();
    Random random = new Random();


//    public Main(DelayHelper delayHelper) {
//        this.delayHelper = delayHelper;
//    }


    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
            Main app = new Main();
            Base base = new Base();
            CurlRequest curlRequest = new CurlRequest();
             curlRequest.curlRequest();
            app.scheduleJobs(base.getJobDetailsSortedByOSVersion(), base.getVenturesWithPriorities());

//        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//
//        // Get the current time
//        LocalDateTime now = LocalDateTime.now();
//
//        // Calculate the initial delay until 10pm
//        LocalTime tenPM = LocalTime.of(22, 0); // 10:00 PM
//        LocalDateTime nextExecutionTime = LocalDateTime.of(now.toLocalDate(), tenPM);
//        if (now.isAfter(nextExecutionTime)) {
//            nextExecutionTime = nextExecutionTime.plusDays(1); // Schedule for tomorrow if it's already past 10pm
//        }
//        Duration initialDelay = Duration.between(now, nextExecutionTime);
//
//        // Calculate the shutdown time at 7am
//        LocalTime sevenAM = LocalTime.of(7, 0); // 7:00 AM
//        LocalDateTime shutdownTime = LocalDateTime.of(now.toLocalDate().plusDays(1), sevenAM); // Tomorrow at 7am
//
//        // Schedule the task
//        scheduler.scheduleAtFixedRate(() -> {
//            System.out.println("Running at " + LocalDateTime.now());
//            try {
//             curlRequest.curlRequest();
//            app.scheduleJobs(base.getJobDetailsSortedByOSVersion(), base.getVenturesWithPriorities());
//
//                // Check if it's time to shutdown (after 7am)
//                if (LocalDateTime.now().isAfter(shutdownTime)) {
//                    System.out.println("Shutting down...");
//                    System.exit(0);
//                }
//            } catch (SQLException | IOException | InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }, initialDelay.toSeconds(), 24 * 60 * 60, TimeUnit.SECONDS); // 24 hours interval (for daily execution)
    }



    // Define a method to schedule jobs
    public void scheduleJobs(List<String> jobs_names, List<Venture> ventures) throws SQLException, InterruptedException, IOException {
        List<String> jobs_names_list = new ArrayList<>(jobs_names);
        List<Venture> ventureList = new ArrayList<>(ventures);
//        Map<String, Integer> allocationSet = new HashMap<>();

//        for (Venture venture : ventureList) {
//            String ventureName = venture.getName();
//            int availableDevices = deviceList.size();
//            int ventureAllocations = allocationSet.getOrDefault(ventureName, 0);
//            int maxAllocations = 3; // Set the maximum allocations per venture
//
//            int maxLoopCount = deviceList.size() >= 8 ? 3 : 2;
//            int totalAllocations = ventureList.size() * maxLoopCount;
//            System.out.println("Total allocations are: "+totalAllocations+"\n\n\n");
//            System.out.println("Devices to allocate: " + devicesToAllocate);
//
//            for (int i = 0; i < devicesToAllocate; i++) {
//                //Now i want to write a for loop which will itrate on venture list and assign each venture a single device.
//                for(int j=0; j<ventureList.size(); j++){
//                    if(ventureList.get(j).getName().equals(ventureName)){
//                        ventureList.remove(j);
//                    }
//                }
//                String deviceUdid = deviceList.get(random.nextInt(availableDevices));
//                Buyer buyerInfo = basic.findAvailableBuyer(venture);
//
//                if (deviceUdid != null && buyerInfo != null) {
////                    allocateDevice(deviceUdid, venture, buyerInfo);
//                    System.out.println("THis is the combination I am about to insert in the DB >>"+deviceUdid+"<<::"+ventureName+"<<::"+buyerInfo);
//                    allocationSet.put(ventureName, 0 + 1);
//                }
//            }
//                    System.out.println("\n\nRunning Again \n\n\n\n");
//        }

        int maxLoopCount = jobs_names_list.size() >= 8 ? 3 : 2;
        int totalAllocations = ventureList.size() * maxLoopCount;
        System.out.println("Total allocations are: "+totalAllocations+"\n\n\n");
        for (int i = 0; i < maxLoopCount; i++) {
            for (Venture venture: ventureList) {
                Collections.shuffle(jobs_names_list);
                Buyer buyerInfo = base.findAvailableBuyer(venture);
                allocateJobs(jobs_names_list.get(random.nextInt(jobs_names_list.size())), venture, buyerInfo);
                }
        }
    }



    private void allocateJobs(String jobName, Venture ventureName, Buyer buyerInfo) throws SQLException, InterruptedException, IOException {
        System.out.println("Device UDID is: " + jobName);
        System.out.println("Venture name is: " + ventureName);
        System.out.println("Buyer hash set is: " + buyerInfo + "\n\n\n");
        java.sql.Timestamp executionStartDate = new java.sql.Timestamp(System.currentTimeMillis());
        System.out.println("I am about to insert this following."+ventureName+ " <::> " + jobName+ " <::> " +Integer.parseInt(base.getJobOSVersion(jobName))+ " <::> " +buyerInfo+ " <::> " +executionStartDate);
        base.insertExecutionRecord(ventureName, jobName, Integer.parseInt(base.getJobOSVersion(jobName)), buyerInfo, executionStartDate);
        String ventureNameString = ventureName.getName();
        String deviceUdidString = jobName;
        String buyerEmailString = buyerInfo.getEmail();
        String buyerPasswordString = buyerInfo.getPassword();
        //send curl request calling curl here
        CurlRequest curlRequest = new CurlRequest();
        System.out.println("Sending curl request to the device: " + deviceUdidString);
//        curlRequest.sendCurlPostRequest(deviceUdidString, buyerEmailString, buyerPasswordString,ventureNameString);
//        base.updateJobIsFreeOrOccupied(jobName, curlRequest.isJobInQueue(jobName,curlRequest.sendCurlGetRequest(curlRequest.jenkins_queue_url, curlRequest.username, curlRequest.password)));
        int sleepTime = random.nextInt((10000 - 5000) + 1) + 5000;
        System.out.println("\n\nSleeping for " + (sleepTime / 1000) + " seconds.");
        Thread.sleep(sleepTime);
//        basic.DeviceStatusScheduler(jobName,"1");
    }
}



//Fetch total number of devices from the database "Devices" table where Devices_Status =1 and Device_free =1
// store them into a list
//get size of the list.
// if the size is >8 then, total number of ventures * by 3, if the device count is less than 6 then, total number of ventures * by 2
// Now the basic rule will be that excution must be started after 10Pm in the night and should do maximum 15 or 10 itrations.
