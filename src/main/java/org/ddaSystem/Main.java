package org.ddaSystem;

import java.io.IOException;
import java.sql.Connection;
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



    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
            Main app = new Main();
            Base base = new Base();
            base.cleanUpDataBase();
            CurlRequest curlRequest = new CurlRequest();
             curlRequest.curlRequest();
            app.scheduleJobs(base.getJobDetailsSortedByOSVersion(), base.getVenturesWithPriorities());
    }



    // Define a method to schedule jobs
    public void scheduleJobs(List<String> jobs_names, List<Venture> ventures) throws SQLException, InterruptedException, IOException {
        List<String> jobs_names_list = new ArrayList<>(jobs_names);
        List<Venture> ventureList = new ArrayList<>(ventures);
        int maxLoopCount = jobs_names_list.size() >= 8 ? 3 : 2;
        int totalAllocations = ventureList.size() * maxLoopCount;
        System.out.println("Total allocations are: "+totalAllocations+"\n\n\n");
        // Sort jobs by OS version in descending order
        jobs_names_list.sort(Comparator.comparingInt(job -> {
            try {
                return Integer.parseInt(base.getJobOSVersion(job));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
        Collections.reverse(jobs_names_list);
        for (int i = 0; i < maxLoopCount; i++) {
            for (int j = 0; j < ventureList.size(); j++) {
                Buyer buyerInfo = base.findAvailableBuyer(ventureList.get(j));
                allocateJobs(jobs_names_list.get(j), ventureList.get(j), buyerInfo);
            }
            Collections.shuffle(jobs_names_list);
        }
    }



    private void allocateJobs(String jobName, Venture ventureName, Buyer buyerInfo) throws SQLException, InterruptedException, IOException {
        java.sql.Timestamp executionStartDate = new java.sql.Timestamp(System.currentTimeMillis());
        int jobOSVersion=Integer.parseInt(base.getJobOSVersion(jobName));
        base.insertExecutionRecord(ventureName, jobName,jobOSVersion , buyerInfo, executionStartDate);
        String ventureNameString = ventureName.getName();
        String deviceUdidString = jobName;
        String buyerEmailString = buyerInfo.getEmail();
        String buyerPasswordString = buyerInfo.getPassword();
        //send curl request calling curl here

        CurlRequest curlRequest = new CurlRequest();
        System.out.println("Sending curl request to the device: " + deviceUdidString +" <::> "+ jobOSVersion +" <::> "+ buyerEmailString +" <::> "+ventureNameString);
        curlRequest.sendCurlPostRequest(deviceUdidString, buyerEmailString, buyerPasswordString,ventureNameString);
//        base.updateJobIsFreeOrOccupied(jobName, curlRequest.isJobInQueue(jobName,curlRequest.sendCurlGetRequest(curlRequest.jenkins_queue_url, curlRequest.username, curlRequest.password)));
        int sleepTime = random.nextInt((10000 - 5000) + 1) + 5000;
//        System.out.println("\n\nSleeping for " + (sleepTime / 1000) + " seconds.");
        Thread.sleep(sleepTime);
//        base.DeviceStatusScheduler(jobName,"1");
    }
}
