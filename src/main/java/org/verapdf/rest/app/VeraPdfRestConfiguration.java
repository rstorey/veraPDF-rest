package org.verapdf.rest.app;

import io.dropwizard.Configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;


/**
 * Configuration object for the Dropwizard app. Reads defaults from
 * configuration YAML file. This class has to be "mutable" due to Dropwizard
 * requirements.
 * 
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>.</p>
 */
public class VeraPdfRestConfiguration extends Configuration {
    @JsonProperty("swagger")
    public SwaggerBundleConfiguration swaggerBundleConfiguration;
}
