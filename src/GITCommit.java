import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

public class GITCommit {
    final static String DIR_TO_COMMIT = "D:/InfoReach/Test/test/";

    static boolean isCurrentBranchForward(Appendable log, Git git, String currentRevision) throws GitAPIException, IOException {
        boolean result = false;

//        CloneCommand cloneCommand = Git.cloneRepository();
//        cloneCommand.setURI("git@github.com:andreydp/test.git");
//        cloneCommand.setTransportConfigCallback(new TransportConfigCallback() {
//            @Override
//            public void configure(Transport transport) {
//                SshTransport sshTransport = (SshTransport) transport;
//                sshTransport.setSshSessionFactory(getSshSessionFactory());
//            }
//        });
//        cloneCommand.setDirectory(new File("D:\\test")).call();

        RevCommit remoteLatestCommit, currentLatestCommit;
        FetchCommand fetchCommand = git.fetch();
        fetchCommand.setRefSpecs(new RefSpec("refs/heads/*:refs/heads/*"));
        fetchCommand.setTransportConfigCallback(new TransportConfigCallback() {
            @Override
            public void configure(Transport transport) {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(getSshSessionFactory());
            }
        });
        fetchCommand.setRemote(git.getRepository().getConfig().getString("remote", "origin", "url"));
        ObjectId remoteNewRevisionId = fetchCommand.setDryRun(true).call().getAdvertisedRef(Constants.HEAD).getObjectId();
        RevWalk revWalk = new RevWalk(git.getRepository());
        remoteLatestCommit = revWalk.parseCommit(remoteNewRevisionId);
        currentLatestCommit = revWalk.parseCommit(git.getRepository().resolve("master"));

        //Check against remote repo, i.e. if there are new commits not fetched, dryRun = true
        if (!currentLatestCommit.equals(remoteLatestCommit)) {
            printAndLog(log, "Remote has different latest revision: " + remoteLatestCommit.getName());
            printAndLog(log, "Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(remoteLatestCommit.getCommitterIdent().getWhen()));
            printAndLog(log, "Author: " + remoteLatestCommit.getCommitterIdent().getEmailAddress());
            printAndLog(log, "Message: " + remoteLatestCommit.getFullMessage());
            result = true;
        }

        // Check against current repo, i.e. if changes fetched and and not applied. VERY VERY BAD CASE!!!
        else if (!currentLatestCommit.getName().equals(currentRevision)) {
            printAndLog(log, "Current repo has newer revision not merged " + currentLatestCommit.getName());
            result = true;
        }
        return result;
    }


    public static SshSessionFactory getSshSessionFactory() {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
            }
        };
        return sshSessionFactory;
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

    private static boolean pushToRemoteRepo(Appendable log, Git git) throws GitAPIException, IOException {
        PushCommand pushCommand = git.push();
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
                isPushed = pushToRemoteRepo(logAll, git);
            }
            if (!isCommitted || !isPushed) {
                printAndLog(logAll, "Commit/Push failed! Resetting to previous revision...");
                git.reset().setMode(ResetCommand.ResetType.SOFT).setRef(currentRevision).call();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}