package updater;

import org.gradle.api.*;

public class SupportedRepoPlugin implements Plugin<Project>{
    @Override
    public void apply(Project project){
        project.getExtensions().create("supportedRepos", SupportedRepos.class);
        project.getTasks().create("checkOtherUpdates", SupportedRepoUpdate.class);

        project.getTasks().create("checkUpdates")
                .dependsOn("checkOtherUpdates","checkMindustryUpdates"
                );
    }
}
