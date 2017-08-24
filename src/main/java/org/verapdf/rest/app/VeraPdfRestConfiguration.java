package org.verapdf.rest.app;

import io.dropwizard.Configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.knowm.dropwizard.sundial.SundialConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


/**
 * Configuration object for the Dropwizard app. Reads defaults from
 * configuration YAML file. This class has to be "mutable" due to Dropwizard
 * requirements.
 * 
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 * @author <a href="mailto:rstorey@loc.gov">Rosie Storey</a>
 */
public class VeraPdfRestConfiguration extends Configuration {

    // swagger provides auto-documentation for the API at localhost:8080/swagger
    @Valid
    @NotNull
    private final SwaggerBundleConfiguration swagger = new SwaggerBundleConfiguration();

    // sundial provides job scheduling for polling, startup, and shutdown
    @Valid
    @NotNull
    private final SundialConfiguration sundial = new SundialConfiguration();

    @JsonProperty("swagger")
    public SwaggerBundleConfiguration getSwagger() {
        return swagger;
    }

    @JsonProperty("sundial")
    public SundialConfiguration getSundial() {
        return sundial;
    }

}
