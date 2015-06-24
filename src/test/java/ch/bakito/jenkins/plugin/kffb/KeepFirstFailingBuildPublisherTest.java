package ch.bakito.jenkins.plugin.kffb;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static ch.bakito.jenkins.plugin.kffb.KeepFirstFailingBuildPublisher.getMarkerFile;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;

/**
 * KeepFirstFailingBuildPublisherTest
 */
public class KeepFirstFailingBuildPublisherTest  {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  /**
   * @throws Exception
   */
  @Test
  public void testOneSuccessful() throws Exception {
    FreeStyleBuild build = runBuilds(Result.SUCCESS);
    assertNotNull(build);
    assertNull(build.getDescription());
    assertFalse(build.isKeepLog());
    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testOneFailing() throws Exception {
    FreeStyleBuild build = runBuilds(Result.FAILURE);
    assertNotNull(build);
    assertTrue(getMarkerFile(build).exists());
    Assert.assertTrue(build.isKeepLog());
    assertNull(build.getPreviousBuild());
  }


  /**
   * @throws Exception
   */
  @Test
  public void testOneUnstable() throws Exception {
    FreeStyleBuild build = runBuilds(Result.UNSTABLE);
    assertNotNull(build);
   assertTrue(getMarkerFile(build).exists());
    Assert.assertTrue(build.isKeepLog());
    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFailure() throws Exception {
    FreeStyleBuild build = runBuilds(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);

    assertNotNull(build);
    assertFalse(getMarkerFile(build).exists());
    assertFalse(build.isKeepLog());
    assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertFalse(getMarkerFile(build).exists());
    assertFalse(build.isKeepLog());
    assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertTrue(getMarkerFile(build).exists());
    Assert.assertTrue(build.isKeepLog());
    assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertEquals(Result.SUCCESS, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testTwoFailures() throws Exception {
    FreeStyleBuild build = runBuilds(Result.SUCCESS, Result.FAILURE, Result.SUCCESS, Result.FAILURE);

    assertNotNull(build);
    assertTrue(getMarkerFile(build).exists());
    Assert.assertTrue(build.isKeepLog());
    assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertFalse(getMarkerFile(build).exists());
    assertFalse(build.isKeepLog());
    assertEquals(Result.SUCCESS, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertFalse(getMarkerFile(build).exists());
    assertFalse(build.isKeepLog());
    assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertEquals(Result.SUCCESS, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testAbort() throws Exception {
    FreeStyleBuild build = runBuilds(Result.ABORTED);

    assertNotNull(build);
    assertTrue(getMarkerFile(build).exists());
    Assert.assertTrue(build.isKeepLog());
    assertEquals(Result.ABORTED, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testUnstable() throws Exception {
    FreeStyleBuild build = runBuilds(Result.UNSTABLE);

    assertNotNull(build);
    assertTrue(getMarkerFile(build).exists());
    Assert.assertTrue(build.isKeepLog());
    assertEquals(Result.UNSTABLE, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  private FreeStyleBuild runBuilds(Result... results) throws Exception {
    FreeStyleProject project = j.createFreeStyleProject();
    MyBuilder myBuilder = new MyBuilder();
    project.getBuildersList().add(myBuilder);
    project.getPublishersList().add(new KeepFirstFailingBuildPublisher());
    FreeStyleBuild build = null;
    for (Result result : results) {
      myBuilder.setResult(result);
      build = project.scheduleBuild2(0).get();
    }
    return build;
  }

  private static final class MyBuilder extends Builder {

    private Result result;

    public MyBuilder() {
      super();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws InterruptedException, IOException {
      build.setResult(result);
      return true;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Result result) {
      this.result = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Descriptor<Builder> getDescriptor() {
      return new Descriptor<Builder>() {

        @Override
        public String getDisplayName() {
          return "";
        }
      };
    }
  }
}
