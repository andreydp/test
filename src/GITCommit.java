import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;

import java.io.IOException;
import java.net.URISyntaxException;

public class GITCommit {

    public static boolean isCurrentBranchForward(Git git, String currentRevision) throws GitAPIException, IOException {
        boolean result = false;

        String remoteNewRevision = git.fetch().setDryRun(true).call().getAdvertisedRef("HEAD").getObjectId().getName();
        String currentNewRevision = git.getRepository().resolve("master").getName();

        // Check against remote repo, i.e. if there are new commits not fetched, dryRun = true
        if (!remoteNewRevision.equals(currentRevision)) {
            System.out.println("Remote has newer revision: " + remoteNewRevision);
            result = true;
        }

        // Check against current repo, i.e. if changes fetched and and not applied. VERY VERY BAD CASE!!!
        else if (!currentNewRevision.equals(currentRevision)) {
            System.out.println("Current repo has newer revision not merged " + remoteNewRevision);
            result = true;
        }
        return result;
    }

    public static void commitAllChanges(Repository repository, final String message) {
        try {
            final Git git = new Git(repository);
            String currentRevision = git.getRepository().resolve("HEAD").getName();
            if (isCurrentBranchForward(git, currentRevision)) {
                System.out.println("There are undelivered changes. Won't commit. Exiting...");
                System.exit(1);
            }
            git.add().addFilepattern(".").call();
            final Status status = git.status().call();
            if (status.getConflicting().size() > 0) {
                System.err.println("**** error: One or more conflicts are found. Manual intervention is required!");
            }
            if (status.getChanged().size() > 0 || status.getAdded().size() > 0 || status.getRemoved().size() > 0) {
                if (status.getChanged().size() > 0) System.out.println("Modified:");
                for (String s : status.getChanged()) {
                    System.out.println(s);
                }
                if (status.getAdded().size() > 0) System.out.println("Added:");
                for (String s : status.getAdded()) {
                    System.out.println(s);
                }
                if (status.getRemoved().size() > 0) System.out.println("Deleted:");
                for (String s : status.getMissing()) {
                    System.out.println(s);;
                }

                final RevCommit rev = git.commit().setAll(true).setMessage(message).call();
                System.out.println(("Git commit " + rev.getName() + " [" + message + "]"));
            } else {
                System.out.println("No changes to commit! Exiting...");
                System.exit(0);
            }
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Could not commit changes to local Git repository", e);
        }
    }

    private static void pushToRemoteRepo(Repository localRepo, String httpUrl, String user, String password) throws GitAPIException, URISyntaxException, IOException {
        final Git git = new Git(localRepo);
        String currentRevision = git.getRepository().resolve("HEAD").getName();
        RemoteAddCommand remoteAddCommand = git.remoteAdd().setName("origin").setUri(new URIish(httpUrl));
        remoteAddCommand.call();
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password));
        pushCommand.call();
        Iterable<PushResult> results = pushCommand.call();
        String expectedRevision;
        boolean failed = false;
        for (PushResult result : results) {
            System.out.println("Pushing...  " + result.getMessages() + " Remote repository: " + result.getURI());
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                RemoteRefUpdate.Status status = update.getStatus();
                if (!(status.equals(RemoteRefUpdate.Status.OK) || status.equals(RemoteRefUpdate.Status.UP_TO_DATE))) {
                    expectedRevision = update.getExpectedOldObjectId().getName();
                    System.out.println("Push FAILED!...  Status: " + status.toString());
                    System.out.println("Please update/fix your working copy first");
                    System.out.println("Expected remote revision: " + expectedRevision);
                    failed = true;
                } else {
                    System.out.println("Push successful! " + update.getNewObjectId().getName());
                }
            }
        }
        if (failed) {
            System.out.println("Resetting working tree softly to previous revision: " + currentRevision);
            git.reset().setMode(ResetCommand.ResetType.SOFT).setRef(currentRevision).call();
        }
    }

    public static void main(String[] args) {
        String localPath = "D:\\InfoReach\\Test\\test\\.git";
        String httpUrl = "https://github.com/andreydp/test.git";
        String user = "poletaiev@gmail.com";
        String password = "mygithub99";
        try {
            Repository localRepo = null;
            try {
                localRepo = new FileRepository(localPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            commitAllChanges(localRepo, "AutoCommit " + new java.util.Date());
            pushToRemoteRepo(localRepo, httpUrl, user, password);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
