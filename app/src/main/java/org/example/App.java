/*
 * This source file was generated by the Gradle 'init' task
 */
package org.example;

import soot.PackManager;
import soot.options.Options;

import java.io.IOException;

public class App {

    public static void main(String[] args) throws DootException, IOException, InterruptedException {
        Parameters parameters = new Parameters();
        parameters.initFromArgs(args);

        Driver driver = new Driver(parameters._example, parameters._mainClass, parameters._doopDir, parameters._inline,
                parameters._optimize);

        driver.setupInput();

        if (parameters._inline) {
            // For test purpose, sometimes we want to skip Doop invocation and use cached results
            if (!parameters._skipDoop) {
                driver.invokeDoopForInline();
            }

            driver.setupSootForInline();
            driver.inline();

            // Write inline results to tmp directory for later use of optimization
            if (parameters._optimize) {
                Options.v().set_output_format(Options.output_format_class);
                PackManager.v().runPacks();
                PackManager.v().writeOutput();
                driver.generateJarFromInlineResult();
            }
        }

        if (parameters._optimize) {
            if (!parameters._skipDoop) {
                driver.invokeDoopForOptimization();
            }

            driver.setupSootForOptimization();
            driver.optimize();
        }

        switch (parameters._outputFormat) {
            case "shimple":
                Options.v().set_output_format(Options.output_format_shimple);
                break;
            case "class":
                // If we enable optimization, methods will have Shimple bodies at this point. Soot doesn't compile over
                // Shimple bodies.
                if (parameters._optimize) {
                    driver.transformAllToJimpleBodies();
                }
                Options.v().set_output_format(Options.output_format_class);
                break;
            default:
                throw new DootException("Unsupported output format: " + parameters._outputFormat);
        }
        PackManager.v().runPacks();
        PackManager.v().writeOutput();

        if (!parameters._keepTmpDir) {
            driver.removeTmpDir();
        }
    }
}
