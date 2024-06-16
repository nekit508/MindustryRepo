package updater;

import org.gradle.api.*;
import org.gradle.api.tasks.TaskContainer;

public class SupportedRepoPlugin implements Plugin<Project>{
    @Override
    public void apply(Project project){
        project.getExtensions().create("supportedRepos", SupportedRepos.class);
        TaskContainer tasks = project.getTasks();
        var myTask=tasks.create("checkOtherUpdates", SupportedRepoUpdate.class);

        tasks.create("checkUpdates")
                .doFirst(it->it.dependsOn(myTask))
                .doLast(it->it.dependsOn(tasks.getByName("checkMindustryUpdates")));
    }
}
