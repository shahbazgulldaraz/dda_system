package org.ddaSystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class GitFunctions {

//    public void performGitOperations(String deviceUdid, String deviceOS, String executionVenture) {
//        try {
//            // Execute Git commands
//            executeCommand("git fetch");
//            executeCommand("git pull");
//            String branchName = deviceUdid + "_" + deviceOS + "_" + executionVenture;
//            boolean branchExists = executeCommand("git branch | grep -w " + branchName);
//
//            if (!branchExists) {
//                // Create a new branch
//                executeCommand("git checkout -b " + branchName);
//            } else {
//                // Checkout existing branch
//                executeCommand("git checkout " + branchName);
//            }
//
//            // Perform other Git operations
//            executeCommand("git add .");
//            executeCommand("git commit -m \"" + branchName + "\"");
//            executeCommand("git push");
//
//            System.out.println("Git operations completed successfully.");
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//            System.out.println("Error executing Git operations.");
//        }
//    }


    public static void performGitOperationsTwo(String branchName, String fileToUploadPath) {
        try {
            // Connect to the remote repository
            Git git = Git.cloneRepository()
                    .setURI("https://github.com/shahbazgulldaraz/dda_system.git")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("shahbaz.gull@daraz.com", "Sarkar@10085"))
                    .call();
            System.out.println("Git operations started.");
            // 1) git fetch
            git.fetch().call();

            // 2) git pull
            git.pull().call();

            // 3) git branch
            List<Ref> branches = git.branchList().call();

            // 4) find if any branch name is equal to DeviceUdid_DeviceOS_ExecutionVenture
            boolean branchExists = branches.stream()
                    .anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName));


            // 5) Do checkout else create a branch
            if (branchExists) {
                // Push the file to the existing branch
                git.add().addFilepattern(fileToUploadPath).call();
                git.commit().setMessage("Added file " + fileToUploadPath).call();
                git.push().call();
            } else {
                // Create a new branch and push the file to it
                git.checkout().setCreateBranch(true).setName(branchName).call();
                git.add().addFilepattern(fileToUploadPath).call();
                git.commit().setMessage("Added file " + fileToUploadPath).call();
                git.push().setRemote("origin").setRefSpecs(new RefSpec(branchName + ":" + branchName)).call();
            }

            System.out.println("Git operations completed successfully.");

        } catch (RefNotAdvertisedException e) {
            System.out.println("Error: Remote origin did not advertise Ref for branch master.");
            e.printStackTrace();
        } catch (GitAPIException e) {
            System.out.println("Error performing Git operations.");
            e.printStackTrace();
        }
    }

    public void performGitOperations(String deviceUdid, String deviceOS, String executionVenture) {

        String repositoryPath = "git@github.com:shahbazgulldaraz/daraz-android-jenkins.git";
        try {
            // Step 1: Fetch latest changes
            executeCommand("git fetch", repositoryPath);

            // Step 2: Pull latest changes
            executeCommand("git pull", repositoryPath);

            // Step 3: Create or checkout branch
            String branchName = deviceUdid + "_" + deviceOS + "_" + executionVenture;
            boolean branchExists = executeCommand("git branch | grep -w " + branchName, repositoryPath);

            if (!branchExists) {
                // Create a new branch
                executeCommand("git checkout -b " + branchName, repositoryPath);
            } else {
                // Checkout existing branch
                executeCommand("git checkout " + branchName, repositoryPath);
            }

            // Step 4: Stage changes
            executeCommand("git add .", repositoryPath);

            // Step 5: Commit changes
            executeCommand("git commit -m \"" + branchName + "\"", repositoryPath);

            // Step 6: Push changes
            executeCommand("git push", repositoryPath);

            System.out.println("Git operations completed successfully.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error executing Git operations.");
        }
    }

    private boolean executeCommand(String command, String repositoryPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        if (repositoryPath != null && !repositoryPath.isEmpty()) {
            processBuilder.directory(new File(repositoryPath));
        }
        processBuilder.command(command);

        Process process = processBuilder.start();

        process.waitFor();  // Wait for the process to complete

        // Read the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Print the output
        System.out.println(output.toString());

        // Return true if the process exited successfully (exit code 0)
        return process.exitValue() == 0;
    }


}
