package org.example;

import org.example.doop.DoopConventions;
import org.example.doop.DoopRenamer;
import soot.*;
import soot.options.Options;
import soot.shimple.Shimple;

import java.io.File;
import java.util.Collections;

public class Driver {
    private static String EXAMPLE_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "example";
    private static String TMP_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "tmp";
    private static String OUTPUT_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "output";

    private String example;
    private String mainClass;

    public Driver(String example, String mainClass) {
        this.example = example;
        this.mainClass = mainClass;
    }

    public void setupSoot() {
        G.reset();

        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_output_dir(OUTPUT_DIR);
        Options.v().set_process_dir(Collections.singletonList(EXAMPLE_DIR + File.separator + this.example));
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
        Options.v().set_output_format(Options.output_format_shimple);
        Options.v().set_keep_line_number(true);
        Options.v().set_full_resolver(true);
        Options.v().set_allow_phantom_refs(true);

        DoopConventions.setSeparator();
    }

    private void transformApplicationClasses() {
        // We are only interested in application classes
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
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
                System.err.println("Fact generation failed for method " + sootMethod.getSignature() + ".");
                ex.printStackTrace();
                throw ex;
            }
        }
    }
}
