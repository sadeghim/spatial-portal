package au.org.ala.spatial.composer.gazetteer;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Angus
 */
public final class GazetteerPointSearch {

    private static final Logger LOGGER = Logger.getLogger(GazetteerPointSearch.class);

    private GazetteerPointSearch() {
        //to hide public constructor
    }

    /**
     * *
     * Given a lon,lat and layer - queries the gaz for a polygon
     *
     * @param lon   longitude
     * @param lat   latitude
     * @param layer geoserver layer to search
     * @return returns a link to a geojson feature in the gaz
     */
    public static Map<String, String> pointSearch(String lon, String lat, String layer, String geoserver) {
        try {
            String uri = CommonData.getLayersServer() + "/intersect/" + layer + "/" + lat + "/" + lon;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(uri);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            LOGGER.debug("URI: " + uri);
            LOGGER.debug("result: " + result);
            LOGGER.debug("slist: " + slist);

            JSONArray ja = JSONArray.fromObject(slist);

            if (ja != null && !ja.isEmpty()) {
                JSONObject jo = ja.getJSONObject(0);

                Map<String, String> map = new HashMap<String, String>();
                for (Object k : jo.keySet()) {
                    map.put((String) k, jo.getString((String) k));
                }

                return map;
            }
        } catch (Exception e1) {
            LOGGER.error("error with gaz point search", e1);
        }
        return null;
    }
}
