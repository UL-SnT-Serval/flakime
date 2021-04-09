package lu.uni.serval.flakime.core.flakime.maven;

import edu.stanford.nlp.util.CollectionUtils;
import javassist.NotFoundException;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.flakime.maven.utils.MavenLogger;
import lu.uni.serval.flakime.core.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.flakime.core.instrumentation.strategies.Strategy;
import lu.uni.serval.flakime.core.instrumentation.strategies.StrategyFactory;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.*;

@Mojo(name = "flakime-injector", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FlakimeMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    MavenProject mavenProject;

    @Parameter(defaultValue = "uniformDistribution", property = "flakime.strategy")
    String strategy;

    @Parameter(defaultValue = "false",property = "flakime.disableReport")
    boolean disableReport;

    @Parameter(defaultValue = "0.1", property = "flakime.flakeRate")
    double flakeRate;

    @Parameter(defaultValue = "@org.junit.jupiter.api.Test,@org.junit.Test,@org.junit.jupiter.api.Test",property = "flakime.testAnnotations", required = false)
    Set<String> testAnnotations ;

    @Parameter(defaultValue = " ",property = "flakime.testPattern",required = false)
    String testPattern;

    @Parameter(defaultValue = "target/test-classes", property = "flakime.testClassDirectory")
    private String testClassDirectory;

    @Parameter(defaultValue = "src/test/java", property = "flakime.testSourceDirectory")
    private String testSourceDirectory;

    @Parameter
    private Properties strategyParameters;

    @Parameter(defaultValue = "${project.build.directory}/flakime")
    private File outputDirectory;

    @Parameter(defaultValue = "FLAKIME_DISABLE", property = "flakime.disableFlag")
    private String disableFlagName;

    @Parameter(defaultValue = "false",property = "flakime.skip")
    private boolean skip;

    /**
     * Plugin mojo entry point. The method iterates over all test-classes contained
     * in the project. For each of the test classes the method iterates over all the
     * test method (annotated by @test). Finally the method calculates the flakiness
     * probability of the given test method following the given strategy. If the
     * test flakiness probability is greater than the flakerate, the test method is
     * instrumented. Otherwise the test method is skipped.
     *
     *
     * @throws MojoExecutionException Thrown if any of the steps throws an exception
     *                                during its execution.
     */

    @Override
    public void execute() throws MojoExecutionException {
        Strategy strategyImpl = null;
        Log logger = getLog();

        if(!skip)
            try {
                final MavenLogger mavenLogger = new MavenLogger(logger);
                final Project project = initializeProject(mavenProject, mavenLogger);
                strategyImpl = StrategyFactory.fromName(strategy, strategyParameters, mavenLogger);
                logger.info("Test annotations :["+String.join(",",testAnnotations)+"]");
                logger.info(String.format("Strategy %s loaded", strategyImpl.getClass().getName()));
                logger.info(String.format("FlakeRate: %f", flakeRate));
                int ntests = project.getTestClasses().stream().reduce(0, (sub, elem) -> sub + elem.getnTestMethods(), Integer::sum);
                logger.info(String.format("Found %d classes with %d tests", project.getNumberClasses(),ntests));
                logger.debug(String.format("Running preProcess of %s", strategyImpl.getClass().getSimpleName()));

                strategyImpl.preProcess(project,flakeRate);

                for (TestClass testClass : project) {
                    logger.debug(String.format("Process class %s", testClass.getName()));
                    for (TestMethod testMethod : testClass) {
                        logger.debug(String.format("\tProcess method %s", testMethod.getName()));
                        instrument(testMethod, strategyImpl, outputDirectory, disableFlagName,
                                flakeRate,disableReport);
                    }
                    testClass.write();
                }
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
                throw new MojoExecutionException(e.getMessage(), e);
            } finally {
                if (strategyImpl != null) {
                    strategyImpl.postProcess();
                }
            }
    }
    private void instrument(TestMethod testMethod,Strategy strategyImpl,File outputDirectory,String disableFlagName, double flakeRate,boolean disableReport){
        try{
            FlakimeInstrumenter.instrument(testMethod, strategyImpl, outputDirectory, disableFlagName,
                    flakeRate,disableReport);
        }catch (Exception e){
            getLog().warn(String.format("Failed to instrument method %s: %s", testMethod.getName(),
                    e.getMessage()));
        }
    }
    /**
     * This method parse the {@code Maven project} into a {@code Project}
     *
     * @param mavenProject The target maven project containing the tests.
     * @param mavenLogger  Reference to logger
     * @return The instantiated project
     * @throws NotFoundException                     Thrown if the directories
     *                                               contains only jars or do not
     *                                               exist.
     * @throws DependencyResolutionRequiredException Thrown if an artifact is used
     *                                               but not resolved
     */
    public Project initializeProject(MavenProject mavenProject, MavenLogger mavenLogger)
            throws NotFoundException, DependencyResolutionRequiredException {

        return new Project(mavenLogger, testAnnotations,testPattern, getDirectory(testClassDirectory),
                getDirectory(testSourceDirectory), mavenProject.getTestClasspathElements());
    }

    private File getDirectory(String path) {
        final File directory = new File(path);

        if (directory.isAbsolute()) {
            return directory;
        } else {
            return new File(mavenProject.getBasedir(), path);
        }
    }
}
