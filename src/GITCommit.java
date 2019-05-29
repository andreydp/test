import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;

public class GITCommit {
    public static void commitAllChanges(Repository repository, final String message) {
        try {
            final Git git = new Git(repository);
            git.add().addFilepattern(".").call();
            final Status status = git.status().call();
            if (status.getChanged().size() > 0 || status.getAdded().size() > 0
                    || status.getModified().size() > 0
                    || status.getRemoved().size() > 0) {
                System.out.println("Modified:");
                for (String s : status.getModified()) {
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
                System.out.println("Unversioned:");

                final RevCommit rev = git.commit().setAll(true).setMessage(message).call();
                System.out.println(("Git commit " + rev.getName() + " [" + message + "]"));
            }
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Could not commit changes to local Git repository", e);
        }
    }

    public static void main(String[] args) {
        String localPath = "D:\\InfoReach\\Test\\test\\.git";
        String httpUrl = "https://gitlab.inforeachinc.com/svn/TMSall.git";
        String user = "andrey@poletaev@inforeachinc.com";
        String password = "cydyadHuv8";
        try {
            Repository localRepo = null;
            try {
                localRepo = new FileRepository(localPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            commitAllChanges(localRepo, "test commit from jgit");

//            git.commit().setMessage( "Test jgit commit" ).call();

            // add remote repo:
//            RemoteAddCommand remoteAddCommand = git.remoteAdd();
//            remoteAddCommand.setName("origin");
//            remoteAddCommand.setUri(new URIish(httpUrl));
            // you can add more settings here if needed

//            remoteAddCommand.call();

            // push to remote:
//            PushCommand pushCommand = git.push();
//            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("andrey", "password"));
            // you can add more settings here if needed
//            pushCommand.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

