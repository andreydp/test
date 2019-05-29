import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.net.URISyntaxException;

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
            }
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Could not commit changes to local Git repository", e);
        }
    }

    private static void pushToRemoteRepo(Repository localRepo, String httpUrl, String user, String password) throws GitAPIException, URISyntaxException {
        final Git git = new Git(localRepo);
        // add remote repo:
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin");
        remoteAddCommand.setUri(new URIish(httpUrl));
        // you can add more settings here if needed

        remoteAddCommand.call();

        // push to remote:
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password));
        // you can add more settings here if needed
        pushCommand.call();
        Iterable<PushResult> results = pushCommand.call();
        for (PushResult result : results) {
            System.out.println("Pushed " + result.getMessages() + " " + result.getURI() + " updates: " + result.getRemoteUpdates().toString());
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

            commitAllChanges(localRepo, "test commit from jgit");

            pushToRemoteRepo(localRepo, httpUrl, user, password);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}

