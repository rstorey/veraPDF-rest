package org.verapdf.rest.jobs;


import org.knowm.sundial.annotations.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;

import java.util.concurrent.TimeUnit;

// TODO: Make these time intervals configuration values in the YAML file
@SimpleTrigger(repeatInterval = 30, timeUnit = TimeUnit.SECONDS)
public class PollingJob extends Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingJob.class);

    @Override
    public void doRun() throws JobInterruptException {
        // TODO: check the service request queue for asynchronous operation requests to fulfill
        LOGGER.debug("Checking fake service request queue for async operation requests");

    }
}
