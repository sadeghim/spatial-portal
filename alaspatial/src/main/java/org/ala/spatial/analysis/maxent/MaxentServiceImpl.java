package org.ala.spatial.analysis.maxent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import org.ala.spatial.domain.Species;

/**
 * Gets the submitted parameters and runs a maxent model
 * 
 * @author ajayr
 */
public class MaxentServiceImpl implements MaxentService {

    MaxentSettings cmdMaxent = null;

    public MaxentServiceImpl() {
        cmdMaxent = new MaxentSettings();
    }

    public MaxentSettings getMaxentSettings() {
        return cmdMaxent;
    }

    public void setMaxentSettings(MaxentSettings cmdMaxent) {
        this.cmdMaxent = cmdMaxent;
    }

    public String setupSpecies(List speciesList, HttpSession session) {
        try {
            File spFile = File.createTempFile("points_", ".csv", generateSessionDirectory(this.cmdMaxent.getOutputPath()));
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            spWriter.write("spname, longitude, latitude \n");
            Iterator<Species> itr = speciesList.listIterator();
            while(itr.hasNext()) {
                Species sp = itr.next(); 
                spWriter.write("species, " + sp.getLongitude() + ", " + sp.getLatitude() + "\n");
            }
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;

    }

    private File generateSessionDirectory(String thePath) {
        File fDir = null;

        try {
            //fDir = new File(cmdPath + sessionId);
            fDir = new File(thePath);
            fDir.mkdir();
        } catch (Exception e) {
        }

        return fDir;
    }

    /*
     * sets up the parameters and runs a process
     */
    @Override
    public int process() {
        return runCommand(cmdMaxent.toString());
    }

    private int runCommand(String command) {
        Runtime runtime = Runtime.getRuntime();

        try {
            String[] acmd = new String[3];
            acmd[0] = "cmd.exe";
            acmd[1] = "/C";
            acmd[2] = command;

            //System.out.println("Execing " + acmd[0] + " " + acmd[1] + " " + acmd[2]);
            System.out.println("Exec'ing " + command);
            Process proc = runtime.exec(command);

            /*
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", false);

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", false);

            // kick them off
            errorGobbler.start();
            outputGobbler.start();
             */

            System.out.println("Setting up output stream readers");
            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;

            System.out.printf("Output of running %s is:", command);
            // Arrays.toString(acmd)

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitVal = proc.waitFor();

            /*
            // if good, add it to geoserver
            if (exitVal == 0) {
                String cmd2 =
                Process proc2 = runtime.exec()
            }
            */

            // any error???
            return exitVal;
        } catch (Exception e) {
            System.out.println("OOOOPPPSSS: " + e.toString());
            System.out.println("{success: false , responseText: 'Error occurred' + " + e.toString() + "}");
            e.printStackTrace(System.out);
        }

        return 1;
    }
}
