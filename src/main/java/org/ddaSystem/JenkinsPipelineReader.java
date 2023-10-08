package org.ddaSystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.api.Git;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenkinsPipelineReader {

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
    public static Map<String, String> extractStringsFromJenkinsFiles(String repositoryUrl, String username, String password) throws GitAPIException {
        Map<String, String> branchStrings = new HashMap<>();

        // Delete the temporary directory if it already exists
        deleteDirectory(new File("temp-repo2"));

        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);

        try (Git git = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setCredentialsProvider(cp)
                .setDirectory(new File("temp-repo2"))
                .call()) {

            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setHeads(true)
                    .setRemote(repositoryUrl)
                    .setCredentialsProvider(cp)
                    .call();

            for (Ref ref : refs) {
                String branchName = ref.getName().substring(ref.getName().lastIndexOf("/") + 1, ref.getName().length());
                System.out.println("Branch name is:" + ref.getName()+"<<::>>"+branchName);
//                git.checkout().setName(branchName).call();
                git.fetch().setCredentialsProvider(cp).call();
                git.pull().setCredentialsProvider(cp).call();
                git.checkout().setName(ref.getName()).call();
                git.close();
//                File jenkinsFile = new File("temp-repo2/Jenkinsfile");
//                if (jenkinsFile.exists()) {
//                    String content = new String(java.nio.file.Files.readAllBytes(jenkinsFile.toPath()));
//                    String extractedStrings = extractSpecificStrings(content);
//                    branchStrings.put(branchName, extractedStrings);
//                }
            }

//            git.fetch(new CredentialsProvider(cp)).call();
//            git.checkout().setName("main").call();

            //get list of branches in the repository

//            for (org.eclipse.jgit.lib.Ref ref : git.branchList().call()) {
//                String branchName = ref.getName();
//                System.out.println("Branch name is: " + branchName);
//                branchName = branchName.substring(branchName.lastIndexOf('/') + 1);
//
//                // Checkout the branch
//                System.out.println("Checking out branch: " + branchName);
//                git.checkout().setName(branchName).call();
//
//                // Read the content of the "Jenkinsfile"
//                File jenkinsFile = new File("temp-repo2/Jenkinsfile");
//                if (jenkinsFile.exists()) {
//                    String content = new String(java.nio.file.Files.readAllBytes(jenkinsFile.toPath()));
//                    String extractedStrings = extractSpecificStrings(content);
//                    branchStrings.put(branchName, extractedStrings);
//                }
//            }

            // Close the Git instance
            git.close();

        } catch (GitAPIException e) {
            e.printStackTrace();
        } finally {
            // Clean up the temporary directory
            deleteDirectory(new File("temp-repo"));
        }

        return branchStrings;
    }

    private static String extractSpecificStrings(String content) {
        // Assuming the pattern is "-DdeviceName=\"value\" -DplatformVersion=value -Dudid=value"
        String[] parts = content.split(" ");

        StringBuilder extractedStrings = new StringBuilder();

        for (String part : parts) {
            if (part.startsWith("-DdeviceName=")
//                    || part.startsWith("-DplatformVersion=")
//                    || part.startsWith("-Dudid=")
            ) {
                extractedStrings.append(part).append(" ");
            }
        }
        return extractedStrings.toString().trim();
    }


//    public static void main(String[] args) throws GitAPIException {
//        final String REMOTE_URL = "https://github.com/shahbazgulldaraz/daraz-android-jenkins.git";
//        final String USERNAME = "shahbaz.gull@daraz.com";
//        final String PASSWORD = "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";
////        final String PASSWORD = "Sarkar@10085";
//
//        Map<String, String> branchStrings = extractStringsFromJenkinsFiles(REMOTE_URL, USERNAME, PASSWORD);
//
//        // Print the branch names and extracted strings
//        for (Map.Entry<String, String> entry : branchStrings.entrySet()) {
//            System.out.println("Branch Name: " + entry.getKey());
//            System.out.println("Extracted Strings: " + entry.getValue());
//            System.out.println("-----------------------------");
//        }
//    }
}
