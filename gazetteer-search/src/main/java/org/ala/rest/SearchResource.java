package org.ala.rest;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.xml.XML;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;


public class SearchResource extends AbstractResource {//ReflectiveResource {

//    @Override
//    protected Object handleObjectGet() throws Exception {
//
//        String query = getRequest().getAttributes().get("q").toString();
//        String nameTerm = query.split("=")[1].replace(",type","");
//
//        if (query.contains("type")) {
//            String typeTerm =  query.split("=")[2].replace(",","");
//            return new Search(nameTerm.replace("+", "* AND ") + "*", typeTerm);
//        } else {
//            return new Search(nameTerm.replace("+", "* AND ") + "*");
//        }
//    }

//    @Override
//    public Map getMap() throws Exception {
//         String query = getRequest().getAttributes().get("q").toString();
//        String nameTerm = query.split("=")[1].replace(",type","");
//
//        if (query.contains("type")) {
//            String typeTerm =  query.split("=")[2].replace(",","");
//            return new Search(nameTerm.replace("+", "* AND ") + "*", typeTerm).getMap();
//        } else {
//            return new Search(nameTerm.replace("+", "* AND ") + "*").getMap();
//        }
//     }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {

        List<DataFormat> formats = new ArrayList();
        formats.add(new StringFormat(MediaType.APPLICATION_XML));

        return formats;
    }

    @Override
    public void handleGet() {
        XStream xstream = new XStream();
        DataFormat format = getFormatGet();

        String query = getRequest().getAttributes().get("q").toString();
        String nameTerm = query.split("=")[1].replace(",type","");
        Search searchObj;
        if (query.contains("type")) {
            String typeTerm =  query.split("=")[2].replace(",","");
            searchObj = new Search(nameTerm.replace("+", "* AND ") + "*", typeTerm);
            xstream.processAnnotations(Search.class);
            //System.out.println(xstream.toXML(searchObj));
            String xmlString = xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        } else {
            searchObj = new Search(nameTerm.replace("+", "* AND ") + "*");
            xstream.processAnnotations(Search.class);
            //System.out.println(xstream.toXML(searchObj));
            String xmlString =  xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        }
        
    }
}
