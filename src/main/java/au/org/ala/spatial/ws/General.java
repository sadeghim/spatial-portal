/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.ws;

import au.org.ala.spatial.util.FacetCache;
import au.org.emii.portal.config.ConfigurationLoaderStage1Impl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;

/**
 * @author Adam
 */
@Controller
public class General {

    @Inject
    private FacetCache facetCache;

    @RequestMapping(value = "/admin/reloadconfig", method = RequestMethod.GET)
    @ResponseBody
    public String reloadConfig() {
        //signal for reload
        ConfigurationLoaderStage1Impl.LOADERS.get(0).interrupt();

        //reload the facet cache
        facetCache.reloadCache();

        //return summary

        //was it successful?
        return "";
    }
}
