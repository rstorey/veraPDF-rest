package org.verapdf.rest.app;

import io.dropwizard.setup.Environment;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.verapdf.rest.resources.ApiResource;
import org.verapdf.rest.resources.HomePageResource;

import static org.mockito.Mockito.*;

public class VeraPdfRestApplicationTest {
    private final Environment environment = mock(Environment.class);
    private final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
    private final VeraPdfRestApplication application = new VeraPdfRestApplication();
    private final VeraPdfRestConfiguration config = new VeraPdfRestConfiguration();

    @Before
    public void setup() throws Exception {
        when(environment.jersey()).thenReturn(jersey);
    }

    @Test
    public void buildsResources() throws Exception {
        application.run(config, environment);

        verify(jersey).register(isA(ApiResource.class));
        verify(jersey).register(isA(HomePageResource.class));

    }

}