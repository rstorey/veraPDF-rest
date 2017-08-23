package org.verapdf.rest.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;

public class StopJob extends Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopJob.class);
    @Override
    public void doRun() throws JobInterruptException {
        // TODO: on shutdown, kill any delegated processes
        LOGGER.debug("Executing fake shutdown job");
    }
}
