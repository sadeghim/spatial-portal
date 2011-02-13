package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(FilteringResultsWCController.class);
    public Button mapspecies;
    public Label results_label2_occurrences;
    public Label results_label2_species;
    public Label sdLabel;
    public String[] results = null;
    public String pid;
    String shape;
    private String satServer;
    private SettingsSupplementary settingsSupplementary = null;
    //Row rowUpdating;
    //Row rowCounts;
    int results_count = 0;
    int results_count_occurrences = 0;
    boolean addedListener = false;
    Label lblArea;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    @Override
    public void redraw(Writer out) throws java.io.IOException {
        super.redraw(out);

        setUpdatingCount(true);

        System.out.println("redraw:filteringresultswccontroller");
        if (!addedListener) {
            addedListener = true;
            //register for viewport changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    // refresh count may be required if area is
                    // not an envelope.
                    String area = getMapComposer().getSelectionArea();
                    if (!area.startsWith("ENVELOPE(") && !area.startsWith("LAYER(")) {
                        refreshCount();
                    }
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener("filteringResults", el);

        }
    }

    void setUpdatingCount(boolean set) {
        //rowUpdating.setVisible(set);
        //rowCounts.setVisible(!set);
        results_label2_occurrences.setValue("updating...");
        results_label2_species.setValue("updating...");

    }

    public void populateList() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/list");

            String out = postInfo(sbProcessUrl.toString());
            //remove trailing ','
            if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
                out = out.substring(0, out.length() - 1);
            }
            results = out.split("\\|");
            java.util.Arrays.sort(results);

            if (results.length == 0 || results[0].trim().length() == 0) {
                results_label2_species.setValue("0");
                results_label2_occurrences.setValue("0");
                mapspecies.setDisabled(true);
                results = null;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isTabOpen() {
        return true; //getMapComposer().getPortalSession().getCurrentNavigationTab() == PortalSession.LINK_TAB;
    }

    public void refreshCount() {
        //check if tab is open
        if (!isTabOpen() || !updateParameters()) {
            return;
        }

        setUpdatingCount(true);
        Events.echoEvent("onRefreshCount", this, null);
    }

    public void onRefreshCount(Event e) throws Exception {
        //temporary:
        intersectWithSpeciesDistributions();

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/count");

            getMapComposer().updateUserLogAnalysis("species count", "area: " + shape, "", "species list in area");

            String[] out = postInfo(sbProcessUrl.toString()).split("\n");

            results_count = Integer.parseInt(out[0]);
            results_count_occurrences = Integer.parseInt(out[1]);

            setUpdatingCount(false);

            if (results_count == 0) {
                //results_label.setValue("no species in active area");
                results_label2_species.setValue("0");
                results_label2_occurrences.setValue("0");
                mapspecies.setDisabled(true);
                results = null;
                return;
            }

            results_label2_species.setValue(String.valueOf(results_count));
            results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));

            // toggle the map button
            if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
                mapspecies.setDisabled(false);
            } else {
                mapspecies.setDisabled(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onClick$download() {
        //preview species list
        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/AnalysisSpeciesListResults.zul", this, null);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$downloadsamples() {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list");

            String samplesfile = postInfo(sbProcessUrl.toString());

            URL u = new URL(satServer + "/alaspatial/" + samplesfile);
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            String sdate = date.format(new Date());

            String spid = pid;
            if (spid == null || spid.equals("none")) {
                spid = String.valueOf(System.currentTimeMillis());
            }

            Filedownload.save(u.openStream(), "application/zip", u.getFile());
            getMapComposer().updateUserLogAnalysis("Sampling", "", "", u.getFile(), pid, "Sampling download");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$downloadsamplesPreview() {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        //validate with 'occurrences count'
        if (results_count_occurrences > settingsSupplementary.getValueAsInt("max_record_count_download")) {
            getMapComposer().showMessage(results_count_occurrences + " occurrences in the active area.  Cannot produce sample for more than " + settingsSupplementary.getValueAsInt("max_record_count_download") + " occurrences.");
            return;
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list/preview");

            String slist = postInfo(sbProcessUrl.toString());

            String[] aslist = slist.split(";");
            System.out.println("Result count: " + aslist.length);
            int count = 0;
            for (int i = 0; i < aslist.length; i++) {
                String[] rec = aslist[i].split("~");
                if (rec.length > 0) {
                    count++;
                }
            }
            count--; //don't include header in count

            if (slist.trim().length() == 0 || count == 0) {
                getMapComposer().showMessage("No records available for selected criteria.");
                return;
            }

            //create window
            SamplingAreaResultsWCController window = (SamplingAreaResultsWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingAreaResults.zul", this, null);
            window.parent = this;
            window.doModal();

            if (count == 1) {
                window.samplingresultslabel.setValue("preview: 1 record");
            } else {
                window.samplingresultslabel.setValue("preview: " + count + " records");
            }

            // load into the results popup
            String[] top_row = null;
            for (int i = 0; i < aslist.length; i++) {
                if (i == 0) {
                    top_row = aslist[i].split("~");
                }
                String[] rec = aslist[i].split("~");

                System.out.println("Column Count: " + rec.length);

                Row r = new Row();
                r.setParent(window.results_rows);
                // set the value
                for (int k = 0; k < rec.length && k < top_row.length; k++) {
                    Label label = new Label(rec[k]);
                    label.setParent(r);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$mapspecies() {
        getMapComposer().addToSession("Species in Active area", "lsid=aa");
        onMapSpecies(null);
    }

    String registerPointsInArea(String area) {
        //register with alaspatial using data.getPid();
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("species/area/register");

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/" + sbProcessUrl.toString());
            get.addParameter("area", URLEncoder.encode(area, "UTF-8"));
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onMapSpecies(Event event) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            String area = getMapComposer().getSelectionArea();
            //String polygon = getMapComposer().getSelectionAreaPolygon();

            StringBuffer sbProcessUrl = new StringBuffer();

            //register points with a new id for mapping
            String lsid = registerPointsInArea(area);
            getMapComposer().mapSpeciesByLsid(lsid, "Species in Active area");

            getMapComposer().updateUserLogAnalysis("Sampling", sbProcessUrl.toString(), "", satServer + "/alaspatial/" + sbProcessUrl.toString(), pid, "map species in area");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    private String postInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", URLEncoder.encode(shape, "UTF-8"));

            System.out.println("satServer:" + satServer + " ** postInfo:" + urlPart + " ** " + shape);

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    boolean updateParameters() {
        //extract 'shape' and 'pid' from composer
        String area = getMapComposer().getSelectionArea();

        if (area.contains("ENVELOPE(")) {
            shape = "none";
            pid = area.substring(9, area.length() - 1);
            return true;
        } else {
            pid = "none";
            if (shape == null || !shape.equalsIgnoreCase(area)) {
                shape = area;
                return true;
            } else {
                return false;
            }
        }
    }

    static public void open() {
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, null);
        try {
            win.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void refreshCount(int newCount, int newOccurrencesCount) {
        results_count = newCount;
        results_count_occurrences = newOccurrencesCount;
        if (results_count == 0) {
            results_label2_species.setValue(String.valueOf(results_count));
            results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
            results = null;
        }

        results_label2_species.setValue(String.valueOf(results_count));
        results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
        setUpdatingCount(false);

        // toggle the map button
        if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
            mapspecies.setDisabled(false);
        } else {
            mapspecies.setDisabled(true);
        }
    }
    Textbox taLSIDs;

    public void onClick$btnAddLSIDs(Event event) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            String lsids = taLSIDs.getValue().trim();
            lsids = lsids.replace("\n", ",");
            lsids = lsids.replace("\t", ",");
            lsids = lsids.replace(" ", "");

            String[] split = lsids.split(",");

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/species/lsid/register");
            sbProcessUrl.append("?lsids=" + URLEncoder.encode(lsids.replace(".", "__"), "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();

            System.out.println("btnAddLSIDs:" + pid);

            getMapComposer().mapSpeciesByLsid(pid, "User entered LSIDs");

            //getMapComposer().updateUserLogAnalysis("Sampling", "", "", u.getFile(), pid, "Sampling download");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    Textbox taSpeciesDistribution;

    public void intersectWithSpeciesDistributions() {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("ws/intersect/shape");
            //sbProcessUrl.append("?area=" + URLEncoder.encode(shape, "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
            get.addParameter("area", URLEncoder.encode(shape, "UTF-8"));
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            if (result == 200) {
                String txt = get.getResponseBodyAsString();
                taSpeciesDistribution.setText(txt);
                String[] lines = txt.split("\n");
                if (lines[0].length() == 0) {
                    sdLabel.setValue("0");
                } else {
                    sdLabel.setValue(String.valueOf(lines.length));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$sdDownload(Event event) {
        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        Filedownload.save(taSpeciesDistribution.getText(), "text/plain", "Species_distributions_" + sdate + "_" + spid + ".csv");
    }
}
