package org.ddaSystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class GitFunctions {

    private static final String REMOTE_URL = "https://github.com/shahbazgulldaraz/dda_system.git";
    private static final String USERNAME = "shahbaz.gull@daraz.com";
    private static final String PASSWORD = "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";


    public static void performGitOperationsTwo(String branchName, String fileToUploadPath) throws GitAPIException {

        deleteDirectory(new File("temp-repo"));
        // Connect to the remote repository
        Git git = Git.cloneRepository()
                .setURI(REMOTE_URL)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
                .setDirectory(new File("temp-repo"))
                .call();

        // Fetch changes from the remote repository
        git.fetch().call();

        // Check if the branch exists
        boolean branchExists = git.branchList().call().stream()
                .anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName));

        // Do checkout else create a branch
        if (branchExists) {
            // Checkout the existing branch
            git.checkout().setName(branchName).call();
        } else {
            // Create a new branch
            git.checkout().setCreateBranch(true).setName(branchName).call();
        }

        // Add the file to the staging area
        git.add().addFilepattern(fileToUploadPath).call();

        // Commit the changes
        git.commit().setMessage("Added file " + fileToUploadPath).call();

        // Push the changes to the remote repository
        git.push().setRemote("origin").setRefSpecs(new RefSpec(branchName + ":" + branchName)).call();
    }





    public static void performGitOperationsThree(String branchName, String fileToUploadPath) {
//        try {
//            // Open the local Git repository
//            Repository repository = openLocalRepository();
//
//            // Create a new branch if it doesn't exist
//            createBranchIfNotExists(repository, branchName);
//
//            // Stage and commit the new file
//            stageAndCommitFile(String.valueOf(repository), fileToUploadPath);
//
//            // Push the changes to the remote repository
//            pushChangesToRemote(repository, branchName);
//
//            // Clean up resources
//            repository.close();
//        } catch (IOException | GitAPIException e) {
//            e.printStackTrace();
//        }
        deleteDirectory(new File("temp-repo"));
            try {
                // Open the existing online Git repository
                Git git = Git.cloneRepository()
                        .setURI(REMOTE_URL)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
                        .setDirectory(new File("temp-repo"))
                        .call();

                // Checkout the specified branch
                git.checkout()
                        .setName(branchName)
                        .call();

                // Add the new file to the repository
                git.add()
                        .addFilepattern(fileToUploadPath)
                        .call();

                // Commit the changes
                git.commit()
                        .setMessage("Added " + fileToUploadPath)
                        .call();

                // Push the changes to the remote repository
                git.push()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
                        .call();

                // Clean up the temporary local repository
                git.getRepository().close();
                deleteDirectory(new File("temp-repo"));
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }

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
