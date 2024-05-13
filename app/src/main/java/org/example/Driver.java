package org.example;

import fj.P;
import org.apache.log4j.Logger;
import org.example.doop.DoopConventions;
import org.example.doop.DoopRenamer;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.shimple.Shimple;
import soot.shimple.ShimpleBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class Driver {
    private static final Logger logger = Logger.getLogger(Driver.class);

    private final Path doopDir;
    private final Optimizer optimizer;

    private final String example;
    private final String mainClass;

    private final Path baseDir;
    private final Path exampleDir;
    private final Path logDir;
    private final Path outputDir;

    public Driver(String example, String mainClass, String doopDir) {
        this.doopDir = Paths.get(doopDir);
        this.optimizer = new Optimizer(this.doopDir);

        this.example = example;
        this.mainClass = mainClass;

        this.baseDir = Paths.get(System.getProperty("user.dir")).getParent();
        this.exampleDir = Paths.get(baseDir + File.separator + "example");
        this.logDir = Paths.get(baseDir + File.separator + "log");
        this.outputDir = Paths.get(baseDir + File.separator + "output");
    }

    public void setupSoot() {
        G.reset();

        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_output_dir(outputDir.toString());
        Options.v().set_process_dir(Collections.singletonList(exampleDir + File.separator + this.example));
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

    public void invokeDoop() throws IOException, InterruptedException {
        logger.debug("Doop directory: " + doopDir);

        String command = "./doop "
                + "-a context-insensitive "
                + "-i " + exampleDir + File.separator + example + File.separator + mainClass + ".jar "
                + "--extra-logic " + exampleDir + File.separator + example + File.separator + "extra.dl "
                + "--stats none";
        logger.debug("Invoking Doop: " + command);

        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }
        File doopLog = new File(logDir + File.separator + System.currentTimeMillis() + ".log");
        logger.debug("Redirecting Doop console output to " + doopLog);

        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        builder.directory(doopDir.toFile());
        builder.redirectOutput(doopLog);
        Process process = builder.start();
        process.waitFor();
    }

    public void optimize() throws DootException, IOException {
        optimizer.optimize();
    }
}









