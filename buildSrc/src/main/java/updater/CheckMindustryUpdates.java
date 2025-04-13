package updater;

import arc.files.Fi;
import arc.struct.ObjectSet;
import arc.util.serialization.Jval;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.xml.sax.SAXException;
import updater.process.GenerateHashes;
import updater.process.LibrariesDownloader;
import updater.process.ProjectProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class CheckMindustryUpdates extends DefaultTask {
    public CheckMindustryUpdates() {
        getOutputs().upToDateWhen(task -> false);
    }

    static String readLast(Reader reader) throws IOException {
        // read everything into a buffer
        int n;
        char[] part = new char[(1/*1 << 13*/)];
        StringBuilder sb = new StringBuilder();
        char openChar = '{';
        char closeChar = '}';
        int indent = 0;
        while ((n = reader.read(part, 0, part.length)) != -1) {
            if (indent == 0 && part[0] == '[') continue;
            if (closeChar == part[0]) {
                indent--;
            }
            if (openChar == part[0]) {
                indent++;
            }
            sb.append(part, 0, n);
            if (indent == 0) break;
        }
        return sb.toString().trim();
    }

    @TaskAction
    public void run() throws IOException, NoSuchAlgorithmException, InterruptedException, SAXException {
        Vars.root = Fi.get(getProject().getBuildDir().getAbsolutePath()).parent();
        Vars.innerBuildSrc = Vars.root.child("innerBuildSrc");
        Vars.repository = Vars.root.child("repository");
        Vars.sources = Vars.root.child("sources");
        Fi supportedVersionsFile = Vars.root.child("supported-versions.txt");
        ObjectSet<String> supportedVersions = new ObjectSet<>();
        if (supportedVersionsFile.exists()) {
            for (String str : supportedVersionsFile.readString().split("\n")) {
                if (str.matches("((#|//|\\\\).*|/\\*\\*?[^*]*\\*/)")) continue;
                supportedVersions.add(str.trim());
            }
        }

        Jval lastJson;
        {
            URL url = new URL("https://api.github.com/repos/Anuken/Mindustry/releases");

            try (InputStream stream = url.openStream()) {
                InputStreamReader reader = new InputStreamReader(stream);
                String text = readLast(reader);
                System.out.println("Mindustry releases");
                System.out.println(text);
                lastJson = Jval.read(text);
            }
        }
        String lastTag = lastJson.getString("tag_name");
        if (!supportedVersions.add(lastTag)) {
            System.out.println("No updates found");
            return;
        }
        System.out.println("Found new tag "+lastTag+"!!!");
        LibrariesDownloader.download(lastTag, lastTag);

        System.out.println("Processing Arc");
        ProjectProcessor.process("Arc",LibrariesDownloader.arcZip(), lastTag);

        System.out.println("Processing Mindustry");
        ProjectProcessor.process("Mindustry",LibrariesDownloader.mindustryZip(), lastTag);


        System.out.println("Creating .md5 and .sha1");
        GenerateHashes.process(Vars.repository);

        System.out.println("Saving tag");
        supportedVersionsFile.writeString("\n" + lastTag, true);


        System.out.println("Done.");
//        Vars.sources.child("tmp.lastJson").writeString(lastJson.toString(Jval.Jformat.formatted));


    }
}
