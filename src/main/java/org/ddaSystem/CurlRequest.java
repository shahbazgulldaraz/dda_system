package org.ddaSystem;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

public class CurlRequest {

    Base base = new Base();

    // Initialize lists to store job names and their statuses
    public List<String> jobNames = new ArrayList<>();
    public List<String> jobStatus = new ArrayList<>();

    // Set Jenkins credentials
    String base_url = base.constructURL("/job/daraz-android-jenkins/api/json");

    String job_url =base.constructURL("/job/daraz-android-jenkins/job/");
    String jenkins_queue_url = base.constructURL("/queue/api/json");

    String jSon_path = "/api/json";
    String username ="shahbaz";
    String password = "11edaefcb2147f807810ea2217b1b231b1";



    // Define the path to save the JSON output
    String outputFilePath = "output.json";



    // Main method for making CURL request and processing response
    public void curlRequest() {
        try {
            // Send a CURL request to the Jenkins API and get JSON response
            String jsonResponse = sendCurlGetRequest(base_url, username, password);

            // Parse JSON response into a JSONObject
            JSONObject jobObject = new JSONObject(jsonResponse);

            // Print job status
            printJobStatus(jobObject);

            // Get detailed information about each job
            getJobInfo(jobNames);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send a CURL request and return the response as a String
    public String sendCurlGetRequest(String url, String username, String password) throws IOException {
        // Set up the connection and set request headers
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic " + base.getEncodedCredentials(username, password));

        // Read the response
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        return response.toString();
    }


    public String sendCurlPostRequest(String Job_name, String buyer_username, String buyer_password, String env) throws IOException {
        System.out.println("This is the job name: "+Job_name);
        System.out.println("This is the buyer username: "+buyer_username);
        System.out.println("This is the buyer password: "+buyer_password);
        System.out.println("This is the env: "+env);

        // Define your data parameters
        String data = "TAG_NAME=your_tag_name" +
                "&BRANCH=master" +
                "&REGRESSION_TYPE=Smoke" +
                "&RERUN_FAILED_ONLY=NO" +
                "&ENV="+env +
                "&RERUN_FILE_PATH=/mnt/storage1/rerunfiles/daraz-android-jenkins/REPLACEME" +
                "&APP_ENVIRONMENT=Live" +
                "&BUILD_TYPE=prod" +
                "&FILE_TITLE=PkBuyerOne.yml";

        // Set up the connection and set request headers
        URL apiUrl = new URL(job_url+Job_name+"/buildWithParameters");
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + base.getEncodedCredentials(username, password));
        connection.setDoOutput(true);

        // Write the data parameters to the request body
        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }

        // Read the response
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        return response.toString();
    }

    public void getJobInfo(List<String> jobNames) {
        for (String jobName : jobNames) {
            try {
                // Construct the API URL for the Jenkins job
                String apiUrl = job_url + jobName + jSon_path;

                // Send a CURL request to the Jenkins API and parse the response as a JSONObject
                JSONObject jsonObject = new JSONObject(sendCurlGetRequest(apiUrl, username, password));

                // Retrieve the last build number of the job
                String lastBuildNumber = jsonObject.getJSONObject("lastBuild").optString("number");
                //Retrieve Next Build Number
                String NextBuildNumber = jsonObject.optString("nextBuildNumber","Next Build Number Not Found");

                // Construct the API URL for the last build of the Jenkins job
                String apiLastBuildUrl = job_url + jobName + "/" + lastBuildNumber + jSon_path;

                // Print the URL of the last build
                System.out.println("This is the Last build URL: " + apiLastBuildUrl);

                // Send a CURL request to get information about the last build
                jsonObject = new JSONObject(sendCurlGetRequest(apiLastBuildUrl, username, password));

                // Write the JSON response of the last build to a file
//                writeJsonToFile(jsonObject, jobName + "_" + lastBuildNumber + ".json");

                // Retrieve build details
                String result = jsonObject.optString("result", "Result Not Found");
                boolean inProgress = jsonObject.optBoolean("inProgress");
                String TimeStamp = jsonObject.optString("timestamp", "Timestamp Not Found");
                String duration = jsonObject.optString("duration", "Duration Not Found");
                String estimatedDuration = jsonObject.optString("estimatedDuration", "Estimated Duration Not Found");
//                String NextBuildNumber = jsonObject.optString("nextBuild", "Next Build Number Not Found");

                // Print the information about the last build
                System.out.println("-------------------------------------");
                System.out.println("Job Name: " + jobName);
                Base base = new Base();
                System.out.println("Last Build Number: " + lastBuildNumber);
                System.out.println("Build inProgress: " + inProgress);
                System.out.println("Build Status: " + result);
                System.out.println("Build TimeStamp: " + Base.convertToReadableDateAndTimeFormat(Long.parseLong(TimeStamp)));
                System.out.println("Build Duration: " + Base.convertToReadableTimeFormat(Long.parseLong(duration)));
                System.out.println("Build Estimated Duration: " + Base.convertToReadableTimeFormat(Long.parseLong(estimatedDuration)));
                System.out.println("Next Build Number: " + NextBuildNumber);
                Boolean job_in_queue = isJobInQueue(jobName, sendCurlGetRequest(jenkins_queue_url, username, password));
                System.out.println("Job in queue >>>>>>: " + job_in_queue);
                base.storeJobDetailsInDB(jobName,inProgress,job_in_queue);
                System.out.println("Job details stored in DB");
                System.out.println(base.getJobDetailsSortedByOSVersion());

                System.out.println("-------------------------------------");

            } catch (Exception e) {
                // Handle any exceptions that may occur
                e.printStackTrace();
            }
        }
    }

    // write a method which will insert Job names in the database with OS version attached with it.


    // write a method which will check if the job is in queue or not
    public boolean isJobInQueue(String jobName, String queueJson) {
        JSONObject queueObj = new JSONObject(queueJson);
        JSONArray items = queueObj.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject task = item.getJSONObject("task");

            if (task.getString("name").equals(jobName)) {
                return true;
            }
        }

        return false;
    }







    // Method to print job statuses from a JSONObject and return a list of job names
    public List<String> printJobStatus(JSONObject jobObject) {
        JSONArray jobsArray = new JSONArray(jobObject.getJSONArray("jobs").toString());  // Get jobs array
        for (int i = 0; i < jobsArray.length(); i++) {  // Loop through jobs
            JSONObject job = jobsArray.getJSONObject(i);  // Get each job as a JSONObject
            String name = job.getString("name");  // Get job name
            jobNames.add(name);  // Add job name to the list
        }
        return jobNames;  // Return the list of job names
    }

}