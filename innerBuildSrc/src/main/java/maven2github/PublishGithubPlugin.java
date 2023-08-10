package maven2github;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.internal.tasks.DefaultTaskContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.ArrayList;
import java.util.List;

public class PublishGithubPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("Hello world: " + project);

        project.afterEvaluate(_o -> {
            registerProject(_o);
            /*project.allprojects(it -> {
                TaskContainer tasks = it.getTasks();
                PublishToGithubTask folder = tasks.register("publishFolder", PublishToGithubTask.class).get();
                folder.dependsOn(
                        it.getTasksByName("publishToMavenLocal", true),


                        );
            });*/
        });
    }

    private TaskProvider<PublishToGithubTask> registerProject(Project project) {
        TaskContainer tasks = project.getTasks();
        List<Object> dependencies = new ArrayList<>();
        project.subprojects(sub -> {
            TaskProvider<PublishToGithubTask> publish = registerProject(sub);
           if(publish!=null) dependencies.add(publish);
        });

        TaskProvider<PublishToGithubTask> register = null;
        try {
            register = tasks.register("publishFolder", PublishToGithubTask.class, task -> {
                task.dependsOn(project.getTasksByName("publishToMavenLocal", true), dependencies);
            });
            System.out.println(project);
        } catch (Exception e) {
            return null;
//            return (TaskProvider<PublishToGithubTask>) tasks.getByName("publishFolder");
        }


        return register;
    }

}
