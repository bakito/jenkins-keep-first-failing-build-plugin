package ch.bakito.jenkins.plugin.kffb;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.io.Files;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;

/**
 * KeepFirstFailingBuildPublisher
 */
public class KeepFirstFailingBuildPublisher extends Recorder {

  private static final String FIRST_FAILED_BUILD = "FIRST_FAILED_BUILD";

  /**
   * Constructor.
   */
  @DataBoundConstructor
  public KeepFirstFailingBuildPublisher() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  /**
   * @param build
   * @param launcher
   * @param listener
   * @return
   * @throws InterruptedException
   */
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
    Run<?, ?> prev = build.getPreviousBuild();
    try {
      if (build.getResult().isWorseThan(Result.SUCCESS) && (prev == null || prev.getResult().isBetterOrEqualTo(Result.SUCCESS))) {
        build.keepLog(true);
        Files.touch(getMarkerFile(build));

        checkHierarchy(build);
      }
      if (build.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
        checkHierarchy(build);
      }
    } catch (IOException e) {
      e.printStackTrace(listener.error("error while processing build logs"));
    }
    return true;
  }

  private void checkHierarchy(Run<?, ?> run) throws IOException {
    Run<?, ?> prev = run;
    while ((prev = prev.getPreviousBuild()) != null) {
      File marker = getMarkerFile(prev);
      if (prev.isKeepLog() && marker.exists()) {
        prev.keepLog(false);
        marker.delete();
      }
    }
  }

  static File getMarkerFile(Run<?, ?> run) {
    return new File(run.getRootDir(), KeepFirstFailingBuildPublisher.class.getSimpleName() + ".firstFailed");
  }

  private Object readResolve() throws ObjectStreamException {
    return this;
  }

  /**
   * DescriptorImpl
   */
  @Extension(ordinal = -900)
  // set ordinal to achieve the extension is executed after the other extensions that may change
  // the build state.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    /**
     * Constructor.
     */
    public DescriptorImpl() {
      super(KeepFirstFailingBuildPublisher.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.DescriptionSetter_DisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
      return req.bindJSON(KeepFirstFailingBuildPublisher.class, formData);
    }
  }
}
