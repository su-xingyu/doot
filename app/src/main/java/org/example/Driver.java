package org.example;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.example.doop.DoopConventions;
import org.example.doop.DoopRenamer;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.options.Options;
import soot.shimple.Shimple;
import soot.shimple.ShimpleBody;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class Driver {
    private static final Logger logger = Logger.getLogger(Driver.class);

    private final Path doopDir;
    private final Optimizer optimizer;
    private final Inliner inliner;

    private final String example;
    private final String mainClass;

    private final Path baseDir;
    private final Path exampleDir;
    private final Path logDir;
    private final Path outputDir;
    private final Path tmpDir;
    private final Path inputDir;
    private final Path inlineResultDir;

    private final boolean inlineEnabled;
    private final boolean optimizeEnabled;

    public Driver(String example, String mainClass, String doopDir, boolean inlineEnabled, boolean optimizeEnabled) {
        this.doopDir = Paths.get(doopDir);
        this.optimizer = new Optimizer(this.doopDir);
        this.inliner = new Inliner(this.doopDir);

        this.example = example;
        this.mainClass = mainClass;

        this.baseDir = Paths.get(System.getProperty("user.dir")).getParent();
        this.exampleDir = Paths.get(baseDir + File.separator + "example" + File.separator + example);
        this.logDir = Paths.get(baseDir + File.separator + "log");
        this.outputDir = Paths.get(baseDir + File.separator + "output");
        this.tmpDir = Paths.get(baseDir + File.separator + "tmp");
        this.inputDir = Paths.get(tmpDir + File.separator + "input");
        this.inlineResultDir = Paths.get(tmpDir + File.separator + "inlineResult");

        this.inlineEnabled = inlineEnabled;
        this.optimizeEnabled = optimizeEnabled;
    }

    public void setupInput() throws IOException, InterruptedException {
        // Clear the results from previous executions
        if (Files.exists(inputDir)) {
            FileUtils.deleteDirectory(inputDir.toFile());
        }

        logger.debug("Copying input files to " + inputDir);
        FileUtils.copyDirectory(exampleDir.toFile(), inputDir.toFile());

        String command1 = "javac *.java";
        executeCommand(command1, inputDir, null);

        String command2 = "jar -cfe " + mainClass + ".jar " + mainClass + " *.class";
        executeCommand(command2, inputDir, null);
    }

    public void removeTmpDir() throws IOException {
        if (Files.exists(tmpDir)) {
            FileUtils.deleteDirectory(tmpDir.toFile());
        }
    }

    private void executeCommand(String command, @Nullable Path directory, @Nullable Path outputFile)
            throws IOException, InterruptedException {
        String[] bashCommand = new String[] {"bash", "-c", command};
        ProcessBuilder builder = new ProcessBuilder(bashCommand);
        if (directory != null) {
            logger.debug("Temporarily changed to directory: " + directory);
            builder.directory(directory.toFile());
        }
        if (outputFile != null) {
            logger.debug("Redirecting console output to " + outputFile);
            builder.redirectOutput(outputFile.toFile());
        }

        logger.debug("Executing command: " + command);
        Process process = builder.start();
        process.waitFor();
    }

    public void generateJarFromInlineResult() throws IOException, InterruptedException {
        String command = "jar -cfe " + mainClass + ".jar " + mainClass + " *.class";
        executeCommand(command, inlineResultDir, null);
    }

    public void setupSootForInline() throws DootException {
        G.reset();

        Options.v().set_src_prec(Options.src_prec_only_class);
        if (optimizeEnabled) {
            Options.v().set_output_dir(inlineResultDir.toString());
        } else {
            Options.v().set_output_dir(outputDir.toString());
        }
        Options.v().set_process_dir(Collections.singletonList(inputDir.toString()));
        Options.v().set_main_class(this.mainClass);

        Options.v().set_allow_phantom_refs(true);

        Scene.v().loadNecessaryClasses();
    }

    public void setupSootForOptimization() {
        G.reset();

        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_output_dir(outputDir.toString());
        if (inlineEnabled) {
            Options.v().set_process_dir(Collections.singletonList(inlineResultDir.toString()));
        } else {
            Options.v().set_process_dir(Collections.singletonList(inputDir.toString()));
        }
        Options.v().set_main_class(this.mainClass);

        applyDoopSettings();

        Scene.v().loadNecessaryClasses();

        // We need to transform the bodies of application classes to match the naming convention of Doop
        transformApplicationClasses();
    }

    private void applyDoopSettings() {
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb", "model-lambdametafactory:false");

        Options.v().set_via_shimple(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_full_resolver(true);
        Options.v().set_allow_phantom_refs(true);

        DoopConventions.setSeparator();
    }

    private void transformApplicationClasses() {
        // We are only interested in application classes
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                // System.out.println(sootMethod.getSubSignature());
                transformToDoopBody(sootMethod);
            }
        }
    }

    /*
     * This method is adapted from Doop - FactGenerator: generate
     */
    private void transformToDoopBody(SootMethod sootMethod) {
        if (sootMethod.isPhantom()) {
            return;
        }

        if (!(sootMethod.isAbstract() || sootMethod.isNative())) {
            if (!sootMethod.hasActiveBody()) {
                sootMethod.retrieveActiveBody();
            }

            Body b0 = sootMethod.getActiveBody();
            try {
                if (b0 != null) {
                    Body b = Shimple.v().newBody(b0);
                    sootMethod.setActiveBody(b);
                    DoopRenamer.transform(b);
                }
            } catch (RuntimeException ex) {
                System.err.println("Doop Body transformation failed for method " + sootMethod.getSignature());
                ex.printStackTrace();
                throw ex;
            }
        }
    }

    public void transformAllToJimpleBodies() throws DootException {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                transformToJimpleBody(sootMethod);
            }
        }
    }

    private void transformToJimpleBody(SootMethod sootMethod) throws DootException {
        if (sootMethod.isPhantom()) {
            return;
        }

        if (!(sootMethod.isAbstract() || sootMethod.isNative())) {
            if (!sootMethod.hasActiveBody()) {
                sootMethod.retrieveActiveBody();
            }

            Body b0 = sootMethod.getActiveBody();
            try {
                if (b0 != null) {
                    if (b0 instanceof JimpleBody) {
                        return;
                    }
                    else if (b0 instanceof ShimpleBody) {
                        Body b = ((ShimpleBody) b0).toJimpleBody();
                        sootMethod.setActiveBody(b);
                    }
                    else {
                        throw new DootException("Unsupported body type for Jimple body transformation");
                    }
                }
            } catch (RuntimeException ex) {
                System.err.println("Jimple Body transformation failed for method " + sootMethod.getSignature());
                ex.printStackTrace();
                throw ex;
            } catch (DootException e) {
                throw e;
            }
        }
    }

    public void invokeDoopForOptimization() throws IOException, InterruptedException {
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        Path optimizeInputDir;
        if (inlineEnabled) {
            optimizeInputDir = inlineResultDir;
        }
        else {
            optimizeInputDir = inputDir;
        }

        // MustAlias analysis is intra-procedural. We don't need any context for Doop analysis
        String command = "./doop "
                + "-a context-insensitive "
                + "-i " + optimizeInputDir + File.separator + mainClass + ".jar "
                + "--extra-logic " + inputDir + File.separator + "optimization.dl "
                + "--stats none";
        logger.debug("Invoking doop");

        Path doopLog = Paths.get(logDir + File.separator + System.currentTimeMillis() + ".log");
        executeCommand(command, doopDir, doopLog);
    }

    public void optimize() throws DootException, IOException {
        optimizer.optimize();
    }

    public void invokeDoopForInline() throws IOException, InterruptedException {
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        // Only 1-call-site-sensitivity+heap analysis is implemented
        String command = "./doop "
                + "-a 1-call-site-sensitive+heap "
                + "-i " + inputDir + File.separator + mainClass + ".jar "
                + "--extra-logic " + inputDir + File.separator + "inline.dl "
                + "--stats none";
        logger.debug("Invoking doop");

        Path doopLog = Paths.get(logDir + File.separator + System.currentTimeMillis() + ".log");
        executeCommand(command, doopDir, doopLog);
    }

    public void inline() throws IOException, DootException {
        inliner.inline();
    }
}









