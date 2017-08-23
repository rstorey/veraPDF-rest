package org.verapdf.rest.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;

public class StartupJob extends Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupJob.class);

    @Override
    public void doRun() throws JobInterruptException {
        // TODO: Establish connection to CTS REST server
        LOGGER.debug("Executing fake startup job");
        // TODO: Cleanup any leftover delegated processes from a previous run
    }
}
