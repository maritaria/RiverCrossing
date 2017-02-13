package com.company;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.lang.Thread;

/**
 * This class implements the TorX-specific part of an adapter.
 * Note that an adapter consists of two parts:
 * <ol>
 * <li>a TorX-specific part: interacts with JTorX, implements {@link TorXConnector}
 * <li>a SUT-specific part: interacts with the SUT, implements {@link IUTConnector}
 * </ol>
 * The TorX-specific part is not specific for any system under test, wheras
 * the SUT-specific part is specific for a system under test
 * and thus to be implemented by the end user.
 * <p>
 * The adapter component that interacts with the system under test
 * will create and use an instance of this class to handle the
 * interaction with the system under test.
 * <p>
 * This adapter component sets up a separate thread to handle
 * the requests that the adapter will receive from JTorX.
 * <p>
 * When JTorX starts a test run it starts the adapter program as a separate process,
 * and once the adapter program has started JTorX will try to interact with it
 * by writing to its standard input and reading from its standard output.
 * The adapter will use an instance of this class to handle the interaction with JTorX.
 * JTorX reads also standard error of the adapter, but only expects diagnostics there.
 * <p>
 * <p>
 * Before starting this adapter, JTorX sets two environment variables:
 * <ul>
 * <li>TORXTIMEOUT  to the integer value given as timeout in the JTorX Config pane
 * <li>TORXTIMEUNIT to the unit given in the JTorX Config pane (seconds or miliseconds)
 * </ul>
 * This adapter component looks for these two environment variables and
 * uses their values to set the timeout value that is used to detect quiescence.
 * <p>
 * This adapter component communicates with JTorX using the TorX-Adapter interface,
 * which is a textual interface, where JTorX issues commands and the adapter responds.
 * Each command from JTorX must be followed by a response,
 * or else JTorX will hang (JTorX will wait forever for a response).
 * <ul>
 * <li>Each command and each response consist of a line of text.
 * <li>Each command and each response start with a keyword.
 * <li>All commands  start with a keyword that starts with C_
 * <li>All responses start with a keyword that starts with A_
 * </ul>
 * <p>
 * We now describe the interaction sequence, where we use the following notation:
 * <ul>
 * <li>we use " " to surround the commands and responses, to be able to show spaces
 * <li>we use \n to denote new-line characters
 * <li>we use <code>label</code> or <code>label2</code> to write a label from the model
 * <li>Note that labels can contains spaces but no TAB characters or other whitespace.
 * </ul>
 * <p>
 * For all commands, a valid response is "A_ERROR \n"
 * to indicate failure to perform the command.
 * JTorX may decide to end the test run when it receives A_ERROR.
 * <p>
 * JTorX starts by writing: "C_IOKIND\n"<br>
 * This Adapter responds:   "A_IOKIND \n"<br>
 * Note1: the space after A_IOKIND matters, without it JTorX hangs!<br>
 * Note2: This command is given by JTorX while starting the test run;
 * the adapter is allowed to wait with responding until it has set
 * up connection with the system-under-test.
 * JTorX does not start the actual testing until it has received the
 * A_IOKIND response.
 * <p>
 * Then, JTorX sends requests to apply stimuli or to return observations from the IUT.
 * The request to apply stimuli has the following form: "C_INPUT event=label\n"
 * where  label  is the label obtained from the model.<br>
 * To this the adapter can respond in multiple ways:
 * <ul>
 * <li>to indicate the unability to apply stimulus, but not a fatal error:
 * "A_INPUT_ERROR \n"
 * <li>to indicate that there was already a pending observation:
 * "A_OUTPUT event=outputLabel\n"
 * <li>to indicate that the stimulus was applied:
 * "A_INPUT event=label\n"
 * <li>to indicate that a slightly different stimulus was applied,
 * with label2 instead of label:
 * "A_INPUT event=label2\n"
 * <li>to indicate that something went really wrong:
 * "A_ERROR \n"
 * </ul>
 * <p>
 * The request for an observation has the following form:
 * "C_OUTPUT\n"<br>
 * To this the adapter can respond in the following ways:
 * <ul>
 * <li>to return an observation:
 * "A_OUTPUT event=label\n"
 * <li>to return quiescence (when even after waiting nothing was observed)
 * "A_OUTPUT event=delta\n"
 * <li>to indicate that something went really wrong:
 * "A_ERROR \n"
 * </ul>
 * <p>
 * Finally, JTorX will send a command to indicate end-of-testrun:
 * "C_QUIT\n"<br>
 * To this the adapter responds with "A_QUIT\n"
 * after which it closes the connection to the system-under-test and exits.
 */

public class TorXAdapter extends Thread implements TorXConnector {

    private InputStream in;
    @SuppressWarnings("unused")
    private OutputStream out;
    private BufferedReader torXIn;
    private BufferedWriter torXOut;
    private long timeOut;
    private BlockingQueue<Observation> obsQ;
    private IUTConnector iutConnector;
    private boolean allowedToStop = false;

    /**
     * Constructor for creation of an adapter component that interacts via
     * the TorX-Adapter protocol on its standard input and output.
     *
     * @param iutConnector reference to the {@link IUTConnector}
     *                     that interacts with the system under test;
     *                     it will be asked to try to apply stimuli.
     * @param defTimeOut   default timeout value in miliseconds;
     *                     this will be overruled by the value obtained from
     *                     environment variables <code>TORXTIMEOUT</code> and
     *                     <code>TORXTIMEUNIT</code>, but only when
     *                     <ul>
     *                     <li>both these variables are set, and
     *                     <li>the value of <code>TORXTIMEOUT</code> can be parsed
     *                     as a <code>long</code> value, and
     *                     <li>the value of <code>TORXTIMEUNIT</code> is either
     *                     <ul>
     *                     <li>the string <code>seconds</code> or
     *                     <li>the string <code>milliseconds</code>.
     *                     </ul>
     *                     </ul>
     *                     The resulting timeout value is printed to standard error.
     */
    public TorXAdapter(IUTConnector iutConnector, long defTimeOut) {
        this(iutConnector, defTimeOut, System.in, System.out);
    }

    /**
     * Constructor for creation of an adapter component that interacts via
     * the TorX-Adapter protocol on the given <code>InputStream</code> and
     * <code>OutputStream</code>.
     *
     * @param conn reference to the {@link IUTConnector}
     *             that interacts with the system under test;
     *             it will be asked to try to apply stimuli.
     * @param to   default timeout value in miliseconds;
     *             this will be overruled by the value obtained from
     *             environment variables <code>TORXTIMEOUT</code> and
     *             <code>TORXTIMEUNIT</code>, but only when
     *             <ul>
     *             <li>both these variables are set, and
     *             <li>the value of <code>TORXTIMEOUT</code> can be parsed
     *             as a <code>long</code> value, and
     *             <li>the value of <code>TORXTIMEUNIT</code> is either
     *             <ul>
     *             <li>the string <code>seconds</code> or
     *             <li>the string <code>milliseconds</code>.
     *             </ul>
     *             </ul>
     *             The resulting timeout value is printed to standard error.
     * @param in   stream from which TorX-Adapter protol requests are read
     * @param out  stream to which TorX-Adapter protol responses are written
     */
    public TorXAdapter(IUTConnector conn, long to, InputStream in, OutputStream out) {
        this.iutConnector = conn;
        this.in = in;
        this.out = out;
        timeOut = to;
        String timeOutVal = System.getenv("TORXTIMEOUT");
        String timeOutUnit = System.getenv("TORXTIMEUNIT");

        if (timeOutVal != null && timeOutUnit != null) {
            if (timeOutUnit.equalsIgnoreCase("seconds"))
                timeOut = Long.parseLong(timeOutVal) * 1000;
            else if (timeOutUnit.equalsIgnoreCase("milliseconds"))
                timeOut = Long.parseLong(timeOutVal);
            else
                timeOut = Long.parseLong(timeOutVal);
        }
        System.err.println("TorXAdapter Timeout value:  " + timeOut + " ms");

        obsQ = new LinkedBlockingQueue<Observation>();
        torXIn = new BufferedReader(new InputStreamReader(in));
        torXOut = new BufferedWriter(new OutputStreamWriter(out));
    }

    /**
     * Starts the TorX-Adapter request handler.
     */
    public void run() {
        Command c = new Command();
        long lastInteractionTime = System.currentTimeMillis();
        Observation obs;
        String label;
        String cs;

        while (c.readCommand(torXIn) && !allowedToStop) {
            cs = c.getCommand();
            System.err.println("got command: " + cs);
            try {
                if (cs.equals("C_QUIT")) {
                    output("A_QUIT" + "\n");
                    torXIn.close();
                    torXOut.close();
                    iutConnector.stop();
                    return;
                } else if (cs.equals("C_IOKIND")) {
                    output("A_IOKIND " + "\n"); // space matters!
                    continue;
                } else if (cs.equals("C_INPUT")) { // does obsQ contain pending observations?
                    obs = obsQ.poll();
                    if (obs != null) { // deliver pending observation
                        output("A_OUTPUT event=" + obs.getLabel() + "\n");
                    } else if ((label = c.getField("event")) != null) { // try to apply stimulus
                        if (iutConnector.applyStimulus(label)) { // acknowledge applying stimulus
                            output("A_INPUT event=" + label + "\n");
                            lastInteractionTime = System.currentTimeMillis();
                        } else { // something went wrong when trying to apply stimulus
                            output("A_INPUT_ERROR" + "\n");
                        }
                    } else { // cannot figure out what to use as stimulus
                        output("A_INPUT_ERROR" + "\n");
                    }
                    System.err.println("handled stimulus");
                } else if (cs.equals("C_OUTPUT")) {
                    // because we timestamp each interaction with IUT, we only wait
                    //    (timeOut - time_spent_since_last_interaction)
                    // if the value on previous line is non-negative
                    // otherwise, we do  not have to wait at all
                    long timeToWait = timeOut - (System.currentTimeMillis() - lastInteractionTime);
                    System.err.println("waiting for observation " + timeToWait + " ms");
                    if (timeToWait < 0)
                        timeToWait = 0;
                    obs = obsQ.poll(timeToWait, TimeUnit.MILLISECONDS);
                    System.err.println("got obs: " + (obs != null));
                    if (obs != null) {
                        output("A_OUTPUT event=" + obs.getLabel() + "\n");
                    } else {
                        System.err.println("A_OUTPUT event=delta");
                        output("A_OUTPUT event=delta" + "\n");
                        // lastInteractionTime = System.currentTimeMillis();
                    }
                    System.err.println("delivered observation");
                } else { // unexpected command
                    output("A_ERROR" + "\n");
                }
            } catch (Exception e) {
                System.err.println("torxReader got exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        iutConnector.stop();
    }

    private void output(String s) {
        try {
            torXOut.write(s);
            torXOut.flush();
        } catch (Exception e) {
            System.err.println("output got exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void enqueueObservation(String label) {
        Observation obs = new Observation(label, System.currentTimeMillis());
        try {
            obsQ.put(obs);
        } catch (InterruptedException e) {
            System.err.println("enqueueObservation exception: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public void eot() {
        System.err.println("eot received");
        allowedToStop = true;
        if (false) { // the following does not work: the reader remains blocked readin System.in
            System.err.println("eot: closing input of TorX reader");
            try {
                in.close();
                //torXIn.close();
            } catch (java.io.IOException e) {
                System.err.println("eot: caught exception while closing input of TorX reader: " + e.getMessage());
            }
            System.err.println("eot: closed input of TorX reader");
            System.err.flush();
        } else { // backup plan
            iutConnector.stop();
        }
    }

    /**
     * This class represents a TorX-adapter procol command received from JTorX.
     */
    class Command {
        private String command;
        private Map<String, String> fieldsMap;
        private String[] commands = {
                "C_IOKIND",
                "C_INPUT",
                "C_OUTPUT",
                "C_QUIT",
        };

        public Command() {
            fieldsMap = new HashMap<String, String>();
        }

        public String getCommand() {
            return command;
        }

        public String getField(String f) {
            return fieldsMap.get(f);
        }

        /**
         * returns false when failed to read command (due to eof etc.)
         */
        public boolean readCommand(BufferedReader torXIn) {
            String s;
            while (true) {
                try {
                    s = torXIn.readLine();
                } catch (Exception e) {
                    System.err.println("torxReader got exception: " + e.getMessage());
                    // e.printStackTrace();
                    return false;
                }
                System.err.println("got input from torx: (" + s + ")");
                if (s == null) { // end-of-input
                    return false;
                }
                if (s.trim().equals("")) {
                    continue;
                }
                parse(s);
                if (getCommand() == null) {
                    System.err.println("could not get command from: " + s);
                    continue;
                }
                return true;
            }
        }

        public void parse(String s) {
            command = null;
            fieldsMap.clear();
            for (String a : commands) {
                if (s.startsWith(a + " ") ||
                        s.startsWith(a + "\t") ||
                        s.startsWith(a + "\n") ||
                        s.equals(a)) {
                    s = s.replaceFirst(a + "[ \t]+", a + "\t");
                    String[] fields = s.split("[\t]");
                    command = fields[0];
                    for (int i = 1; i < fields.length; i++) {
                        String[] elem = fields[i].split("=", 2);
                        if (elem.length == 2)
                            fieldsMap.put(elem[0], elem[1]);
                    }
                }
            }
        }
    }

    /**
     * This class represents a single observation.
     * It contains the model label that represents the interaction with the SUT
     * and a timestamp (that will be set when it is enqueued).
     */
    class Observation {
        long stamp;
        String label;

        /**
         * Constructor
         *
         * @param label model label that represents the interaction with the SUT
         * @param stamp timestamp, indicating this adapter part received the observation
         */
        public Observation(String label, long stamp) {
            this.stamp = stamp;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public long getStamp() {
            return stamp;
        }
    }

}
