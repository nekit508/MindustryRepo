package updater;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CheckMindustryUpdatesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().create("checkMindustryUpdates", CheckMindustryUpdates.class);
    }
}
