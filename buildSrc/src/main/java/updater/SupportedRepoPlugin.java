package updater;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

public class SupportedRepoPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("supportedRepos", SupportedRepos.class);
        TaskContainer tasks = project.getTasks();
        var myTask = tasks.create("checkOtherUpdates", SupportedRepoUpdate.class);

        tasks.create("checkUpdates", it -> {
            it.dependsOn(myTask);
            it.dependsOn(tasks.getByName("checkMindustryUpdates"));
        });

    }
}
