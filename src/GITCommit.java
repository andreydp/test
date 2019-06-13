import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class GITCommit {
    final static String DIR_TO_COMMIT = "D:/InfoReach/Test/test/";
    final static String GIT_USER = "poletaiev@gmail.com";
    final static String GIT_PASSWORD = "mygithub99";

    public static boolean isCurrentBranchForward(Appendable log, Git git, String currentRevision) throws GitAPIException, IOException {
        boolean result = false;

        String remoteNewRevision = git.fetch().setDryRun(true).call().getAdvertisedRef(Constants.HEAD).getObjectId().getName();
        String currentNewRevision = git.getRepository().resolve("master").getName();

        // Check against remote repo, i.e. if there are new commits not fetched, dryRun = true
        if (!remoteNewRevision.equals(currentRevision)) {
            printAndLog(log, "Remote has newer revision: " + remoteNewRevision);
            result = true;
        }

        // Check against current repo, i.e. if changes fetched and and not applied. VERY VERY BAD CASE!!!
        else if (!currentNewRevision.equals(currentRevision)) {
            printAndLog(log,"Current repo has newer revision not merged " + remoteNewRevision);
            result = true;
        }
        return result;
    }

    public static boolean commitAllChanges(Git git, final String message, Appendable log) throws IOException {
        boolean okToCommit = true;
        try {
            String currentRevision = git.getRepository().resolve(Constants.HEAD).getName();
            if (isCurrentBranchForward(log, git, currentRevision)) {
                printAndLog(log, "There are undelivered changes. Won't commit. Exiting...");
                okToCommit = false;
            }
            git.add().addFilepattern(".").call();
            final Status status = git.status().call();
            if (status.getConflicting().size() > 0) {
                printAndLog(log, "**** error: One or more conflicts are found. Manual intervention is required!");
                okToCommit = false;
            }
            if (status.getChanged().size() > 0 || status.getAdded().size() > 0 || status.getRemoved().size() > 0) {
                if (status.getChanged().size() > 0)
                    for (String s : status.getChanged()) {
                        printAndLog(log, "modified " + s);
                    }
                if (status.getAdded().size() > 0)
                    for (String s : status.getAdded()) {
                        printAndLog(log, "added " + s);
                    }
                if (status.getMissing().size() > 0)
                    for (String s : status.getMissing()) {
                        printAndLog(log, "deleted " + s);
                    }
                if (okToCommit) {
                    final RevCommit rev = git.commit().setAll(true).setMessage(message).call();
                    printAndLog(log, "Git commit " + rev.getName() + " [" + message + "]");
                }
            } else {
                printAndLog(log, "No changes to commit! Exiting...");
                System.exit(0);
            }
        } catch (Exception e) {

            printAndLog(log, "**** error: Unexpected error happened. Manual intervention is required.");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            printAndLog(log, sw.toString());
            return false;
        }
        return okToCommit;
    }

    private static boolean pushToRemoteRepo(Appendable log, Git git, String user, String password) throws GitAPIException, IOException {
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password));
        Iterable<PushResult> results = pushCommand.call();
        String expectedRevision;
        boolean pushedOk = false;
        for (PushResult result : results) {
            System.out.println("Pushing...  " + result.getMessages() + " Remote repository: " + result.getURI());
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                RemoteRefUpdate.Status status = update.getStatus();
                if (!(status.equals(RemoteRefUpdate.Status.OK) || status.equals(RemoteRefUpdate.Status.UP_TO_DATE))) {
                    expectedRevision = update.getExpectedOldObjectId().getName();
                    printAndLog(log, "Push FAILED!...  Status: " + status.toString());
                    printAndLog(log, "Please update/fix your working copy first");
                    printAndLog(log, "Expected remote revision: " + expectedRevision);
                    pushedOk = false;
                } else {
                    printAndLog(log, "Push successful! " + update.getNewObjectId().getName());
                    pushedOk = true;
                }
            }
        }
        return pushedOk;
    }

    static void printAndLog(Appendable log, String s) throws IOException {
        System.out.println(s);
        log.append(s).append("\n");
    }

    public static void main(String[] args) {
        try {
            StringBuilder logAll = new StringBuilder();
            Git git = Git.init().setDirectory(new File(DIR_TO_COMMIT)).call();
            String currentRevision = git.getRepository().resolve(Constants.HEAD).getName();
            boolean isCommitted = commitAllChanges(git, "AutoCommit " + new java.util.Date(), logAll);
            boolean isPushed = false;
            if (isCommitted) {
                isPushed = pushToRemoteRepo(logAll, git, GIT_USER, GIT_PASSWORD);
            }
            if(!isCommitted || !isPushed)
            {
                printAndLog(logAll, "Commit/Push failed! Resetting to previous revision...");
                git.reset().setMode(ResetCommand.ResetType.SOFT).setRef(currentRevision).call();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}