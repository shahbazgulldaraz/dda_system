package org.ddaSystem;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.sql.SQLException;


public class Main {

    Base base = new Base();
    Random random = new Random();



    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
        Main app = new Main();
        Base base = new Base();
        base.cleanUpDataBase_DB();
        CurlRequest curlRequest = new CurlRequest();
        curlRequest.curlRequest();
        app.scheduleJobs(base.getJobDetailsSortedByOSVersion_DB(), base.getVenturesWithPriorities_DB());
    }



    //    This method orchestrates the allocation of jobs to ventures. Here's a breakdown of the comments:
//
//    Create copies of job names and ventures to avoid modifying the original lists.
//    Determine how many times the allocation loop will run based on the number of job names.
//    Calculate the total number of job allocations.
//    Sort job names by OS version in descending order.
//    Reverse the order of job names after sorting.
//    Create a map to store the count of allocations for each job name.
//    Create a set to track allocated jobs for each venture.
//    Calculate the maximum number of allocations allowed per venture.
//    Handle cases where the venture or job names list is empty.
//    Create a map to track the number of allocations for each venture.
//    Loop until the total number of allocations is reached.
//    Iterate through the list of ventures.
//    Check if the venture has reached the maximum allocations.
//    Get the job name for the current iteration.
//    Check if the job can be allocated to the venture.
//    Find an available buyer for the venture.
//    Allocate the job to the venture.
//    Update allocation counts for the job and venture.
//    Add the allocated job to the set for the venture.
//    Shuffle the job names list to change the allocation order.
    public void scheduleJobs(List<String> jobs_names, List<Venture> ventures) throws SQLException, InterruptedException, IOException {
        // Create a copy of the job names and venture lists to avoid modifying the original lists
        List<String> jobs_names_list = new ArrayList<>(jobs_names);
        List<Venture> ventureList = new ArrayList<>(ventures);

        // Determine how many times the allocation loop should run based on the number of job names
        int maxLoopCount = jobs_names_list.size() >= 8 ? 2 : 1;
//        int maxLoopCount = 5;

        // Calculate the total number of job allocations
        int totalAllocationsByJobs = jobs_names_list.size() * maxLoopCount;
        System.out.println("This is Max loop count: "+maxLoopCount);
        System.out.println("This is jobs names list: "+jobs_names_list.size());
        System.out.println("This is total job names list size: "+jobs_names_list.size());
        System.out.println("Total Allocation are:" + totalAllocationsByJobs);

        // Sort job names by OS version in descending order
        jobs_names_list.sort(Comparator.comparingInt(job -> {
            try {
                return Integer.parseInt(base.getJobOSVersion_DB(job));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));

        // Reverse the order of job names after sorting
        Collections.reverse(jobs_names_list);

        // Create a map to store the count of allocations for each job name
        Map<String, Integer> jobNameListCountMap = new HashMap<>();

        // Create a set to track allocated jobs for each venture
        Map<Venture, Set<String>> allocatedJobs = new HashMap<>();

        // Calculate the maximum number of allocations allowed per venture,
        // if the number is completely divisible by the number of ventures then proceed with the calculation
        // else get the lower value divisible by the number of ventures

        int maxEachVentureWillBeExecuted;
        if (totalAllocationsByJobs % ventureList.size() == 0) {
            maxEachVentureWillBeExecuted = totalAllocationsByJobs / ventureList.size();
            System.out.println("Max totalAllocationsByJobs % ventureList.size(): " + maxEachVentureWillBeExecuted);
        } else {
            // If not, get the lower value that is divisible by the number of ventures
            maxEachVentureWillBeExecuted = (totalAllocationsByJobs / ventureList.size()) * ventureList.size();
            System.out.println("Max each venture will be executed: " + maxEachVentureWillBeExecuted);
        }
//        int maxEachVentureWillBeExecuted = totalAllocationsByJobs / ventureList.size();

        // Handle the case where either the venture list or job names list is empty
        if (ventureList.isEmpty() || jobs_names_list.isEmpty()) {
            System.out.println("Venture list or job names list is empty. Cannot proceed.");
            return; // Or handle the situation appropriately.
        }

        // Create a map to track the number of allocations for each venture
        Map<Venture, Integer> ventureAllocationCount = new HashMap<>();

        try {
//            System.out.println("This is the total number of allocations: " + totalAllocationsByJobs);
//            System.out.println("This is the total allocations by jobs: " + jobNameListCountMap);
            // Shuffle the job names list before entering the outer loop
            Collections.shuffle(jobs_names_list);

            for (; jobNameListCountMap.values().stream().mapToInt(Integer::intValue).sum() < 10; Collections.shuffle(jobs_names_list)) {
                // Iterate through the list of ventures
                for (int j = 0; j < ventureList.size(); j++) {
                    Venture venture = ventureList.get(j);

                    // Check if the venture has reached the maximum allocations
                    if (ventureAllocationCount.getOrDefault(venture, 0) >= maxEachVentureWillBeExecuted) {
                        continue; // Skip this venture
                    }

                    String jobName = jobs_names_list.get(j);

                    System.out.println("This is the total number of jobNameListCountMap: " + jobNameListCountMap);
                    // Check if the job has been allocated less than 2 times and is not already allocated to this venture
                    if (jobNameListCountMap.getOrDefault(jobName, 0) < 2 &&
                            !allocatedJobs.getOrDefault(venture, Collections.emptySet()).contains(jobName)) {

                        // Find an available buyer for this venture
                        Buyer buyerInfo = base.findAvailableBuyer_DB(venture);

                        // Allocate the job to the venture
                        allocateJobs(jobName, venture, buyerInfo);

                        // Increment the count for the job and venture
                        jobNameListCountMap.put(jobName, jobNameListCountMap.getOrDefault(jobName, 0) + 1);
                        ventureAllocationCount.put(venture, ventureAllocationCount.getOrDefault(venture, 0) + 1);

                        // Add the allocated job to the set for the venture
                        allocatedJobs.computeIfAbsent(venture, k -> new HashSet<>()).add(jobName);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while allocating jobs to ventures.");
        }

    }




    private void allocateJobs(String jobName, Venture ventureName, Buyer buyerInfo) throws SQLException, InterruptedException, IOException {
        // Extract venture name, device UDID, buyer email, and buyer password from the provided objects
        String ventureNameString = ventureName.getName();
        String deviceUdidString = jobName;
        String buyerEmailString = buyerInfo.getEmail();
        String buyerPasswordString = buyerInfo.getPassword();

        // Get the current timestamp as the execution start date
        java.sql.Timestamp executionStartDate = new java.sql.Timestamp(System.currentTimeMillis());

        // Get the job OS version
        int jobOSVersion = Integer.parseInt(base.getJobOSVersion_DB(jobName));

        System.out.println("This is what i want to insert in the database, job name: " + jobName + " venture name: "
                + ventureName.getName() + " buyer email: " + buyerEmailString + " execution start date: "
                + executionStartDate + " job OS version: " + jobOSVersion);
        try {
            // Insert an execution record for the job and venture
            base.insertExecutionRecord_DB(ventureName, jobName, jobOSVersion, buyerEmailString, executionStartDate);
        } catch (Exception e) {
            // Handle any exceptions that may occur during the record insertion
            System.out.println("Exception occurred while inserting execution record for job: " + jobName + " to venture: " + ventureName.getName());
        }

//         Create a CurlRequest object to send a request to the device

        // Print a message indicating the curl request being sent
        System.out.println("Sending curl request to the device: " + jobName + " <::> " + jobOSVersion + " <::> " + buyerEmailString + " <::> " + ventureNameString+"\n\n\n");

        // Send a curl post request with device UDID, buyer email, buyer password, and venture name
        //if Mac then do not execute the curl request and if ubuntu then execute the curl request.
        if(SystemUtils.IS_OS_MAC) {
            System.out.println("This is mac os, so not executing the curl request");
//            CurlRequest curlRequest = new CurlRequest();
//            curlRequest.sendCurlPostRequest(deviceUdidString, buyerEmailString, buyerPasswordString, ventureNameString);
            // Generate a random sleep time between 5000 and 10000 milliseconds (5 to 10 seconds)
//            int sleepTime = random.nextInt((10000 - 5000) + 1) + 5000;

            // Print the sleep time in seconds
//            System.out.println("\n\nSleeping for " + (sleepTime / 1000) + " seconds.\n\n\n");

            // Sleep for the calculated time to simulate a job execution
//            Thread.sleep(sleepTime);
        }else {
        CurlRequest curlRequest = new CurlRequest();
            curlRequest.sendCurlPostRequest(deviceUdidString, buyerEmailString, buyerPasswordString, ventureNameString);
            // Generate a random sleep time between 5000 and 10000 milliseconds (5 to 10 seconds)
            int sleepTime = random.nextInt((10000 - 5000) + 1) + 5000;

            // Print the sleep time in seconds
            System.out.println("\n\nSleeping for " + (sleepTime / 1000) + " seconds.\n\n\n");

            // Sleep for the calculated time to simulate a job execution
            Thread.sleep(sleepTime);
        }

    }

    // Create a method which will fetch the current status from each Job, is the execution on going or not.
    // and update the status in the database, against each buyer and job.
    // if the execution is on going then update the status as busy
    // if the execution is not on going then update the status as free
    // if the execution is not on going and the job is in queue then update the status as false
    // if the execution is not on going and the job is not in queue then update the status as free




}
