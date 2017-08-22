package org.verapdf.rest.environment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author <a href="mailto:rstorey@loc.gov">Rosie Storey</a>.</p>
 */
@JacksonXmlRootElement
public class VeraPDFDetails {


    private final static VeraPDFDetails INSTANCE = new VeraPDFDetails();
    private final String version;
    private final String modelVersions;

    private VeraPDFDetails() {
        this.version = "1.4.1";
        this.modelVersions = "1.6.0,1.7.0";
    }

    /**
     * @return the VeraPDF details instance
     */
    public static VeraPDFDetails getInstance() {
        return INSTANCE;
    }

    /**
     * @return the veraPDF version
     */
    @JsonProperty
    public String getVersion() {
        return this.version;
    }

    /**
     * @return the veraPDF model validation versions
     */
    @JsonProperty
    public String getModelVersions() {
        return this.modelVersions;
    }
}
