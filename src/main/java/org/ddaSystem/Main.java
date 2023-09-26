package org.ddaSystem;

import com.sun.source.tree.WhileLoopTree;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.sql.SQLException;


public class Main {

    Basic basic = new Basic();
    Random random = new Random();


//    public Main(DelayHelper delayHelper) {
//        this.delayHelper = delayHelper;
//    }


    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
            Main app = new Main();
            Basic basic = new Basic();
//            basic.setupDatabase();

//            List<String> connectedDevices = basic.getConnectedDevices();
//            basic.updateDeviceInformation(connectedDevices);
//            System.out.println("Connected devices are:>>> " + connectedDevices);
            // Initialize job scheduler
//            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//            scheduler.scheduleAtFixedRate(() -> {
//                try {
//                    System.out.println("\n\nScheduling jobs...");
//                    //fetch Device_UDID from the Database "Devices" table where Devices_Status =1 and Device_free =1 sorted by OS_VERSION decending order then store them into a list.
////                    List<String> connectedDevicesInDB = basic.getAvailableDeviceUDIDsSortedByOSVersion();
////                    System.out.println("\n\n\nConnected devices in DB are:>>> " + connectedDevicesInDB);
//                    // Schedule jobs
////                    int loopCount = 0;
////                    while(basic.getAvailableDeviceUDIDsSortedByOSVersion().size()>0 && loopCount<16){
////                        System.out.println("Available devices are:>>> " + basic.getAvailableDeviceUDIDsSortedByOSVersion());
////                        System.out.println("Ventures with priorities are:>>> " + basic.getVenturesWithPriorities());
//                        app.scheduleJobs(basic.getAvailableDeviceUDIDsSortedByOSVersion(), basic.getVenturesWithPriorities());
////                        System.out.println("\n\nLoop count is: " + loopCount);
////                        loopCount++;
////                    }
//                } catch (SQLException | InterruptedException | IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }, 0, 1, TimeUnit.SECONDS);

            app.scheduleJobs(basic.getAvailableDeviceUDIDsSortedByOSVersion(), basic.getVenturesWithPriorities());
        }


    // Define a method to schedule jobs
    public void scheduleJobs(List<String> devices, List<Venture> ventures) throws SQLException, InterruptedException, IOException {
        List<String> deviceList = new ArrayList<>(devices);
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

        int maxLoopCount = deviceList.size() >= 8 ? 3 : 2;
        int totalAllocations = ventureList.size() * maxLoopCount;
        System.out.println("Total allocations are: "+totalAllocations+"\n\n\n");
        for (int i = 0; i < maxLoopCount; i++) {
            for (Venture venture: ventureList) {
                Collections.shuffle(deviceList);
                Buyer buyerInfo = basic.findAvailableBuyer(venture);
                System.out.println("\n THis is the combination I am about to insert in the DB >>" + deviceList.get(random.nextInt(deviceList.size())) + "<<::" + venture.getName() + "<<::" + buyerInfo.getEmail() + "<<::" + buyerInfo.getPassword());
                allocateDevice(deviceList.get(random.nextInt(deviceList.size())), venture, buyerInfo);
                }
        }
    }



    private void allocateDevice(String deviceUdid, Venture ventureName, Buyer buyerInfo) throws SQLException, InterruptedException{
        System.out.println("Device UDID is: " + deviceUdid);
        System.out.println("Venture name is: " + ventureName);
        System.out.println("Buyer hash set is: " + buyerInfo + "\n\n\n");
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp executionStartDate = new java.sql.Timestamp(System.currentTimeMillis());
        basic.insertExecutionRecord(ventureName, deviceUdid, Integer.parseInt(basic.getDeviceOSVersion(deviceUdid)), buyerInfo, executionStartDate);
        String ventureNameString = ventureName.getName();
        String deviceUdidString = deviceUdid;
        String deviceOSVersionString = basic.getDeviceOSVersion(deviceUdid);
        String buyerEmailString = buyerInfo.getEmail();
        String buyerPasswordString = buyerInfo.getPassword();
        JenkinsFileGen jenkinsFileGen = new JenkinsFileGen();
        jenkinsFileGen.generatePipeline(ventureNameString, deviceUdidString, deviceOSVersionString, buyerEmailString, buyerPasswordString);
        System.out.println("Jenkins file generated successfully.");
        basic.updateDeviceIsFreeOrOccupied(deviceUdid,"0");

        // Generate a random number between 5000 and 10000 (5 to 10 seconds)
        int sleepTime = random.nextInt((10000 - 5000) + 1) + 5000;
        System.out.println("\n\nSleeping for " + (sleepTime / 1000) + " seconds.");
        Thread.sleep(sleepTime);
        basic.updateExecutionEndTime(deviceUdid, ventureName.getName());
        DeviceStatusScheduler(deviceUdid,"1");
    }

    private void DeviceStatusScheduler(String deviceUdids, String Status) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                basic.updateDeviceIsFreeOrOccupied(deviceUdids,Status);
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
}

//Fetch total number of devices from the database "Devices" table where Devices_Status =1 and Device_free =1
// store them into a list
//get size of the list.
// if the size is >8 then, total number of ventures * by 3, if the device count is less than 6 then, total number of ventures * by 2
// Now the basic rule will be that excution must be started after 10Pm in the night and should do maximum 15 or 10 itrations.
