package adapter;

/**
 * Interface offered by the TorX-specific adapter part that interacts with JTorX.
 * Note that an adapter consists of two parts:
 * <ol>
 * <li>a TorX-specific part: interacts with JTorX, implements this interface
 * <li>a SUT-specific part: interacts with the SUT, implements {@link IUTConnector}
 * </ol>
 * This interface allows the SUT-specific adapter part
 * to enqueue observations with the TorX-specific adapter part,
 * and to report to it that interaction with the system under test ended.
 */
public interface TorXConnector {

	/**
	 * Enqueues an observation to be handed over to JTorX.
	 * @param label		representation of the observation as model label
	 */
	void enqueueObservation(String label);

	/**
	 * Reports to the TorX-specific part
	 * that interaction with the system under test has ended.
	 * Typically this will be invoked by the SUT-specific adapter part
	 * when the system under test has taken initiative to end this interaction.
	 * Typically, the TorX-specific part will then decide to end the test run.
	 */
	void eot();
}
