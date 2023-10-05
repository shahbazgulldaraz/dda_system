package org.ddaSystem;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JenkinsFileGen {

//    public String executeVenture(String venture, String availableDevice, Buyer availableBuyer) {
//        StringBuilder pipelineScript = new StringBuilder();
//
//        pipelineScript.append("pipeline {\n");
//        pipelineScript.append("    agent any\n");
//        pipelineScript.append("    options {\n");
//        pipelineScript.append("          disableConcurrentBuilds()\n");
//        pipelineScript.append("    }\n");
//        pipelineScript.append("    triggers {\n");
//        pipelineScript.append("        cron 'H 23 * * * '\n");
//        pipelineScript.append("    }\n");
//        pipelineScript.append("    stages {\n");
//        pipelineScript.append("        stage('Clone git repository') {\n");
//        pipelineScript.append("            steps {\n");
//        pipelineScript.append("                git(url: 'git@github.com', branch: 'master', credentialsId: 'ssh-server-machine')\n");
//        pipelineScript.append("            }\n");
//        pipelineScript.append("        }\n");
//        pipelineScript.append("        stage('Execute tests') {\n");
//        pipelineScript.append("            steps {\n");
//        pipelineScript.append("                script {\n");
//        pipelineScript.append("                    if (params.RERUN_FAILED_ONLY == \"NO\") {\n");
//        pipelineScript.append("                        timeout(time: 630, unit: 'MINUTES') {\n");
//        pipelineScript.append("                            echo \"Running tests with tag: $tag\"\n");
//        pipelineScript.append("                            def fileTitle = params.FILE_TITLE\n");
//        pipelineScript.append("                            echo \"Running with this buyers file: ${fileTitle}\"\n");
//        // Add more pipeline steps and logic here
//        pipelineScript.append("                        }\n");
//        pipelineScript.append("                    }\n");
//        pipelineScript.append("                }\n");
//        pipelineScript.append("            }\n");
//        pipelineScript.append("        }\n");
//        // Add more stages and pipeline steps here
//        pipelineScript.append("    }\n");
//        pipelineScript.append("}");
//
//        return pipelineScript.toString();

//    }
public void generatePipeline(String ventureNameString, String deviceUdidString, String deviceOSVersionString, String buyerEmailString, String buyerPasswordString) {
        String pipelineScript = String.format(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    options {\n" +
                        "        disableConcurrentBuilds()\n" +
                        "    }\n" +
                        "    stages {\n" +
                        "        stage('Initialize') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    versionCode = sh(\n" +
                        "                        script: \"adb -s 63dc6a69 shell dumpsys package com.daraz.android | grep -E 'versionCode|versionName' | awk -F= '{print \\$2}'\",\n" +
                        "                        returnStdout: true\n" +
                        "                    ).trim()\n" +
                        "                    currentBuild.description = \"Venture = "+ventureNameString+" \\nRegression Type = ‘Full’ \\nBuild Version = ${versionCode}\"\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Clone git repository') {\n" +
                        "            steps {\n" +
                        "                git(url: 'git@gitlab.alibaba-inc.com:shahbaz.gull/DarazMobileAutomation.git', branch: params.BRANCH, credentialsId: 'ssh-server-machine')\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Select Build Type') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    if (params.BUILD_TYPE == \".dev\") {\n" +
                        "                        sh 'sleep 3s'\n" +
                        "                        sh \"sed -i \\\"s/dev= .*;/dev= \\\\\\\".dev\\\\\\\";/\\\" \\$(pwd)/src/main/java/global/PathHelper.java\"\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Execute tests') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    if (params.RERUN_FAILED_ONLY == \"NO\") {\n" +
                        "                        timeout(time: 630, unit: 'MINUTES') {\n" +
                        "                            echo \"Running tests with tag: $tag\"\n" +
                        "                            sh \"mvn clean test -Dcucumber.options=\\\"src/test/java --tags $tag --glue global.APP.stepsDefinitions\\\" -DsuiteXmlFile=/var/config/automation/android/default.xml -Dport=40010 -DsystemPort=50010 -DdeviceName=\\\""+deviceUdidString+"\\\" -DplatformVersion="+deviceOSVersionString+" -Dudid=‘"+deviceUdidString+"’ -Denv="+ventureNameString+" -DuserName=‘"+buyerEmailString+"’,-DuserPassword=‘"+buyerPasswordString+"’ -DapplicationYml=‘src/main/java/application.yml’ -Denvironment=$params.APP_ENVIRONMENT\"\n" +
                        "                        }\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Execute tests (Re-attempts)') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    if (params.RERUN_FAILED_ONLY == \"NO\") {\n" +
                        "                        sh \"mvn surefire:test -Dcucumber.options=\\\"@JenkinsFailedScenarios/$params.ENV/Rerun.txt --glue global.APP.stepsDefinitions\\\" -DsuiteXmlFile=/var/config/automation/android/default.xml -Dport=40010 -DsystemPort=50010 -DdeviceName=\\\""+deviceUdidString+"\\\" -DplatformVersion="+deviceOSVersionString+" -Dudid=‘"+deviceUdidString+"’ -Denv="+ventureNameString+" -DapplicationYml=‘src/main/java/application.yml’ -Denvironment=$params.APP_ENVIRONMENT\"\n" +
                        "                    }else{\n" +
                        "                        sh \"mvn clean test -Dcucumber.options=\\\"@${params.RERUN_FILE_PATH} --glue global.APP.stepsDefinitions\\\" -DsuiteXmlFile=/var/config/automation/android/default.xml -Dport=40010 -DsystemPort=50010 -DdeviceName=\\\"Execution_Device\\\" -DplatformVersion="+deviceOSVersionString+" -Dudid="+deviceUdidString+" -Denv="+ventureNameString+" -DuserName=‘"+buyerEmailString+"’,-DuserPassword=‘"+buyerPasswordString+"’ -DapplicationYml=‘src/main/java/application.yml’ -Denvironment=$params.APP_ENVIRONMENT\"\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Report') {\n" +
                        "            steps {\n" +
                        "                echo \"Generating reports\"\n" +
                        "                allure(results: [[path: 'target/allure-results']], reportBuildPolicy: 'ALWAYS')\n" +
                        "            }\n" +
                        "            post {\n" +
                        "                always {\n" +
                        "                    echo \"Uploading reports to storage\"\n" +
                        "                    sh \"mkdir -p /mnt/storage1/rerunfiles/${env.JOB_NAME}/${env.BUILD_NUMBER}/\"\n" +
                        "                    sh \"cp target/cucumber-reports/rerun-reports/rerun.txt /mnt/storage1/rerunfiles/${env.JOB_NAME}-${env.BUILD_NUMBER}-$params.ENV-rerun.txt\"\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n"
        );

        // Write the pipelineScript to a file (e.g., Jenkinsfile.groovy)
        String fileName = deviceUdidString + "-" + deviceOSVersionString + "-" + ventureNameString.replaceAll("[^a-zA-Z0-9]","-");
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(pipelineScript);
            //get new created file path and store it in a variable
            String filePath = new File(fileName).getAbsolutePath();
            System.out.println("Jenkinsfile written successfully.");
            try {
                GitFunctions.performGitOperationsTwo(fileName,filePath);
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




