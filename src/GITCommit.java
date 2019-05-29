import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class GITCommit {
    public static void commitAllChanges(Repository repository, final String message) {
        try {
            final Git git = new Git(repository);
            git.add().addFilepattern(".").call();
            final Status status = git.status().call();
            if (status.getConflicting().size() > 0) {
                System.err.println("**** error: One or more conflicts are found. Manual intervention is required!");
            }
            if (status.getChanged().size() > 0 || status.getAdded().size() > 0 || status.getRemoved().size() > 0) {
                System.out.println("Modified:");
                for (String s : status.getChanged()) {
                    System.out.println(s);
                }
                System.out.println("Added:");
                for (String s : status.getAdded()) {
                    System.out.println(s);
                }
                System.out.println("Deleted:");
                for (String s : status.getMissing()) {
                    System.out.println(s);
                }

                final RevCommit rev = git.commit().setAll(true).setMessage(message).call();
                System.out.println(("Git commit " + rev.getName() + " [" + message + "]"));
            } else {
                System.out.println("No changes to commit");
            }
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Could not commit changes to local Git repository", e);
        }
    }

    private static void pushToRemoteRepo(Repository localRepo, String httpUrl, String user, String password) throws GitAPIException, URISyntaxException, IOException {
        final Git git = new Git(localRepo);
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin");
        remoteAddCommand.setUri(new URIish(httpUrl));
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
                }
            }
        }
        if (failed) {
            System.out.println("Resetting working tree softly to origin/master...");
            git.reset().setMode(ResetCommand.ResetType.SOFT).setRef("origin/master").call();
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
