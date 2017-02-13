package com.company;

/**
 * Interface offered by the SUT-specific adapter part (part that interacts with SUT).
 * Note that an adapter consists of two parts:
 * <ol>
 * <li>a TorX-specific part: interacts with JTorX, implements {@link TorXConnector}
 * <li>a SUT-specific part: interacts with the SUT, implements this interface
 * </ol>
 * The TorX-specific adapter part uses an implementation of this interface
 * to (try to) apply stimuli to the system under test, and an implementation of this
 * interface will typically use the enqueueObservation function of the TorXConnector
 * interface to report observations.
 */

public interface IUTConnector {

    /**
     * Try to apply given stimulus, given as model label, to the system under test.
     * This will be invoked during the test run when a stimulus must be applied.
     * It may not be possible to apply the given stimulus, for example because the
     * label can not be mapped onto a concrete interaction with the SUT,
     * or because the concrete interaction can not be performed.
     * An example of the latter is when an exception is triggered by the write
     * operation that should deliver the concrete simulus to the system under test.
     *
     * @param label the label to apply (taken from the model)
     * @return    <code>true</code> if stimulus was applied;
     * <code>false</code> otherwise
     */
    boolean applyStimulus(String label);

    /**
     * Dispose of the system resources used by this IUTConnector.
     * This will be invoked at the end of a test run.
     */
    void stop();
}
