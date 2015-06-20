package ch.bakito.jenkins.plugin.kffb;


import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Builder;
import org.junit.Assert;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;

/**
 * KeepFirstFailingBuildPublisherTest
 */
public class KeepFirstFailingBuildPublisherTest extends HudsonTestCase {

  /**
   * @throws Exception
   */
  public void testOneSuccessful() throws Exception {
    FreeStyleBuild build = runBuilds(Result.SUCCESS);
    assertNotNull(build);
    assertNull(build.getDescription());
    Assert.assertFalse(build.isKeepLog());
    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  public void testOneFailing() throws Exception {
    FreeStyleBuild build = runBuilds(Result.FAILURE);
    assertNotNull(build);
    assertNotNull(build.getDescription());
    Assert.assertTrue(build.isKeepLog());
    assertNull(build.getPreviousBuild());
  }


  /**
   * @throws Exception
   */
  public void testOneUnstable() throws Exception {
    FreeStyleBuild build = runBuilds(Result.UNSTABLE);
    assertNotNull(build);
    assertNotNull(build.getDescription());
    Assert.assertTrue(build.isKeepLog());
    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  public void testFailure() throws Exception {
    FreeStyleBuild build = runBuilds(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);

    assertNotNull(build);
    assertNull(build.getDescription());
    Assert.assertFalse(build.isKeepLog());
    Assert.assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertNull(build.getDescription());
    Assert.assertFalse(build.isKeepLog());
    Assert.assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertNotNull(build.getDescription());
    Assert.assertTrue(build.isKeepLog());
    Assert.assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    Assert.assertEquals(Result.SUCCESS, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  public void testTwoFailures() throws Exception {
    FreeStyleBuild build = runBuilds(Result.SUCCESS, Result.FAILURE, Result.SUCCESS, Result.FAILURE);

    assertNotNull(build);
    assertNotNull(build.getDescription());
    Assert.assertTrue(build.isKeepLog());
    Assert.assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertNull(build.getDescription());
    Assert.assertFalse(build.isKeepLog());
    Assert.assertEquals(Result.SUCCESS, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    assertNull(build.getDescription());
    Assert.assertFalse(build.isKeepLog());
    Assert.assertEquals(Result.FAILURE, build.getResult());

    build = build.getPreviousBuild();
    assertNotNull(build);
    Assert.assertEquals(Result.SUCCESS, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  public void testAbort() throws Exception {
    FreeStyleBuild build = runBuilds(Result.ABORTED);

    assertNotNull(build);
    assertNotNull(build.getDescription());
    Assert.assertTrue(build.isKeepLog());
    Assert.assertEquals(Result.ABORTED, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  /**
   * @throws Exception
   */
  public void testUnstable() throws Exception {
    FreeStyleBuild build = runBuilds(Result.UNSTABLE);

    assertNotNull(build);
    assertNotNull(build.getDescription());
    Assert.assertTrue(build.isKeepLog());
    Assert.assertEquals(Result.UNSTABLE, build.getResult());

    assertNull(build.getPreviousBuild());
  }

  private FreeStyleBuild runBuilds(Result... results) throws Exception {
    FreeStyleProject project = createFreeStyleProject();
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
