package ch.bakito.jenkins.plugin.kffb;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.io.ObjectStreamException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * KeepFirstFailingBuildPublisher
 */
public class KeepFirstFailingBuildPublisher extends Recorder {

  private static final String DESCRIPTION_TAG = "<!-- first failing build --> ";

  /**
   * Constructor.
   *
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
   * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild,
   *      hudson.Launcher, hudson.model.BuildListener)
   */
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException {
    Run<?, ?> prev = build.getPreviousBuild();
    try {
      if (build.getResult().isWorseThan(Result.SUCCESS)
          && (prev == null || prev.getResult().isBetterOrEqualTo(Result.SUCCESS))) {
        build.keepLog(true);
        build.setDescription(DESCRIPTION_TAG + Messages.First_FailedBuild_Description());
        checkHierarchy(build);
      }
      if (build.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
        checkHierarchy(build);
      }
    } catch (IOException e) {
      e.printStackTrace(listener.error("error while procesing build logs"));
    }
    return true;
  }

  private void checkHierarchy(Run<?, ?> build) throws IOException {
    Run<?, ?> prev = build;
    while ((prev = prev.getPreviousBuild()) != null) {
      if (prev.isKeepLog() && prev.getDescription() != null && prev.getDescription().startsWith(DESCRIPTION_TAG)) {
        prev.keepLog(false);
        prev.setDescription(null);
      }
    }
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
     *
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
