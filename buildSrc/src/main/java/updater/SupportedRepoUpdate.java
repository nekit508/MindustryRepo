package updater;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import org.apache.commons.io.*;
import org.gradle.api.*;
import org.gradle.api.tasks.*;
import org.xml.sax.*;
import updater.SupportedRepos.*;
import updater.process2.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class SupportedRepoUpdate extends DefaultTask {
    public SupportedRepoUpdate() {
        getOutputs().upToDateWhen(task -> false);
    }

    static String readLast(Reader reader) throws IOException {
        // read everything into a buffer
        int n;
        char[] part = new char[(1/*1 << 13*/)];
        StringBuilder sb = new StringBuilder();
        char openChar = '{';
        char closeChar = '}';
        int deep = 0;
        while ((n = reader.read(part, 0, part.length)) != -1) {
            if (deep == 0 && part[0] == '[') continue;
            if (deep == 0 && part[0] == ']') break;
            if (closeChar == part[0]) {
                deep--;
            }
            if (openChar == part[0]) {
                deep++;
            }
            sb.append(part, 0, n);
            if (deep == 0) break;
        }
        return sb.toString().trim();
    }

    @TaskAction
    public void run() throws IOException, NoSuchAlgorithmException, InterruptedException, SAXException {
        Vars.root = Fi.get(getProject().getBuildDir().getAbsolutePath()).parent();
        ArrayList<SupportedRepo> supportedRepos = getProject().getExtensions().getByType(SupportedRepos.class).repos;
        Vars.innerBuildSrc = Vars.root.child("innerBuildSrc2");
        Vars.repository = Vars.root.child("repository");
        Vars.sources = Vars.root.child("sources");

        Seq<Fi> filesToHash = new Seq<>();
        for (SupportedRepo repo : supportedRepos) {
            String repoAuthor = repo.author();
            String repoName = repo.name();
            Log.info("Processing @/@", repoAuthor, repoName);
            Fi supportedVersionsFile = Vars.root.child("supportedVersions").child(repoAuthor).child(repoName).child("supported-versions.txt");
            ObjectSet<String> supportedVersions = new ObjectSet<>();
            if (supportedVersionsFile.exists()) {
                for (String str : supportedVersionsFile.readString().split("\n")) {
                    if (str.matches("((#|//|\\\\).*|/\\*\\*?[^*]*\\*/)")) continue;
                    supportedVersions.add(str.trim());
                }
            }

            Jval lastJson;
            RepoType repoType = repoName.equals("rhino") ? RepoType.Rhino : RepoType.Other;
            {
                if (repoType.isRhino()) {
                    URL url = new URL("https://api.github.com/repos/" + repoAuthor + "/" + repoName + "/commits");
                    Log.info(url);
                    try (InputStream stream = url.openStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        String text = readLast(reader);
                        lastJson = Jval.read(text);
                    }
                } else {
                    URL url = new URL("https://api.github.com/repos/" + repoAuthor + "/" + repoName + "/releases");
                    Log.info(url);
                    try (InputStream stream = url.openStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        String text = readLast(reader);
                        lastJson = Jval.read(text);
                    }
                }
            }
            String lastTag = lastJson.getString("tag_name", null);
            if (lastTag == null) {
                if (repoType.isRhino()) {
                    lastTag=lastJson.getString("sha");
                } else {
                    Log.warn("Has no version for @/@", repoAuthor, repoName);
                    continue;
                }
            }
            if (!supportedVersions.add(lastTag)) {
                System.out.println("No updates found");
                continue;
            }
            System.out.println("Found new tag " + lastTag + "!!!");
            processRepo(repoAuthor, repoType, repoName, lastTag, filesToHash);

            System.out.println("Saving tag");
            supportedVersionsFile.writeString("\n" + lastTag, true);
        }


        System.out.println("Creating .md5 and .sha1");
        if (filesToHash.isEmpty()) {
            System.out.println("Files to make .md5 and .sha1 not found");
        }
        for (Fi toHash : filesToHash) {
            GenerateHashes.process(toHash);
        }


        System.out.println("Done.");
//        Vars.sources.child("tmp.lastJson").writeString(lastJson.toString(Jval.Jformat.formatted));


    }

    static enum RepoType {
        Rhino,
        Other;

        public boolean isRhino() {
            return this == Rhino;
        }
    }

    private void processRepo(String author, RepoType type, String repoName, String version, Seq<Fi> filesToHash) {
        try {
            Fi sourcesFi = Vars.sources.child(author).child(repoName).child("sources.zip");
            Log.info("Downloading " + author + "/" + repoName);
            Time.mark();
            if (type == RepoType.Rhino) {
                FileUtils.copyURLToFile(new URL("https://codeload.github.com/" + author + "/" + repoName + "/zip/" + version), sourcesFi.file(), 10_000, 10_000);
            } else {
                FileUtils.copyURLToFile(new URL("https://codeload.github.com/" + author + "/" + repoName + "/zip/refs/tags/" + version), sourcesFi.file(), 10_000, 10_000);
            }
            Log.info("Time to download: @ms", Time.elapsed());
            ProjectProcessor.process(author, repoName, new ZipFi(sourcesFi), version, filesToHash);
        } catch (IOException | NoSuchAlgorithmException | InterruptedException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
