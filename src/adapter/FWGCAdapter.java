package adapter;

import java.io.*;
import java.lang.ProcessBuilder;

/**
 * This class implements the SUT-specific component of an adapter for
 * the farmer-cabbgage-goat-wolf program.
 * It uses an instance of {@link TorXAdapter} to handle the interaction with JTorX.
 * <p>
 * This SUT-specific part
 * <ul>
 * <li>is started as a separate process (and thus it implements <code>main</code>)
 * <li>starts the given command+args as the fcgw (SUT) program, and
 * <li>starts a reader thread, to read from the standard output of the command,
 * to read responses from the SUT program
 * <li> starts a TorXAdapter, to handle interaction with JTorX;
 * this SUT-specific part implements the {@link IUTConnector} interface,
 * such that the TorXAdapter can ask this SUT-specific part to apply stimuli.
 * </ul>
 */
public class FWGCAdapter implements IUTConnector {

    private DataOutputStream iutIn;
    private IUTReader iutReader;
    private ErrReader errReader;
    private TorXAdapter torxAdapter;

    /**
     * Create an instance of TorXAdapter (with given <code>timeOut</code>)
     * Start program given in <code>args</code> as a separate process
     * (e.g. using ProcessBuilder, see import above)
     * and attach an IUTReader to its standard output (and start it)
     * and attach an ErrReader to its standard error (and start it)
     * and setup variable <code>iutIn</code> so we can use it to
     * write to the standard input of the program that we started.
     * When starting the program fails, catch the exception, write a message
     * to standard error and exit the adapter program with status 1.
     * Otherwise, start the TorXAdapter instance.
     */
    public FWGCAdapter(long timeOut, String[] args) {

        // implement this method

        torxAdapter = new TorXAdapter(this, timeOut);

        ProcessBuilder builder = new ProcessBuilder(args);
        Process proc = null;
        try {
            proc = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        OutputStream processInput = proc.getOutputStream();
        InputStream processOutput = proc.getInputStream();
        InputStream processError = proc.getErrorStream();

        iutReader = new IUTReader(torxAdapter, processOutput);
        errReader = new ErrReader(torxAdapter, processError);
        iutIn = new DataOutputStream(processInput);
        torxAdapter.start();
    }

    /**
     * Apply stimulus represented by given label string;
     * return true when stimulus was applied, or false otherwise.
     */
    public boolean applyStimulus(String label) {
        System.err.println("applyStim got label: " + label);
        try {
            byte b = encode(label);
            System.err.printf("applyStim writing %x: \n", b);
            return apply(b);
        } catch (EncodeError e) {
            System.err.println("applyStim encoding exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Encode the given label string to a byte; throw EncodeError for unknown label.
     */
    private byte encode(String label) throws EncodeError {

        // implement this method

        if (label.equals("n?")) {
            return 0x01;
        }
        if (label.equals("g?")) {
            return 0x02;
        }
        if (label.equals("c?")) {
            return 0x04;
        }
        if (label.equals("w?")) {
            return 0x08;
        }
        throw new EncodeError("Unknown label '" + label + "'");
    }

    /**
     * Decode the given byte to a label string; return null for unknown byte.
     */
    private String decode(byte b) {

        // implement this method

        switch (b) {
            case 0x01:
                return "done!";
            case 0x02:
                return "eaten!";
            case 0x04:
                return "retry!";
            default:
                return null;
        }

    }

    /**
     * Apply stimulus byte; catch possible exception;
     * return true when stimulus was applied, or false otherwise.
     */
    private boolean apply(byte b) {

        try {
            iutIn.writeByte(b);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This is invoked when we can stop testing, so we simply exit.
     */
    public void stop() {
        //  should we notify iutReader to finish (return)?
        System.err.println("got request to stop");
        System.exit(0);
    }


    /**
     * Reader for the standard error of the SUT.
     * Reads lines of text, and writes the to standard error.
     */
    public class ErrReader extends Thread {
        private BufferedReader r;
        private TorXConnector torxConnector;

        public ErrReader(TorXConnector torxConnector, InputStream is) {
            // just read lines of text, and we use a BufferedReader to help us.
            // for other IUTs you may have to do something else
            this.r = new BufferedReader(new InputStreamReader(is));
            this.torxConnector = torxConnector;
        }

        public void run() {
            String s = null;
            // we forever try to read a line and write it to standard error;
            // for other IUTs you may have to do something else
            while (true) {
                try {
                    s = r.readLine();
                    if (s == null) {
                        System.err.println("errReader read eof");
                        return;
                    }
                    System.err.println("err: " + s);
                } catch (Exception e) {
                    System.err.println("iutReader got exception: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reader for the standard output of the SUT.
     * Reads bytes, decodes them to labels, and enqueus
     * the labels at the TorXConnector.
     */
    public class IUTReader extends Thread {
        private DataInputStream r;
        private TorXConnector torxConnector;

        public IUTReader(TorXConnector torxConnector, InputStream is) {
            // for the example we just read bytes
            this.r = new DataInputStream(is);
            this.torxConnector = torxConnector;
        }

        public void run() {
            byte b = 0;
            // we forever try to read a byte and decode it into a model label
            // for other IUTs you may have to do something else
            while (true) {
                try {
                    b = r.readByte();
                    String label = decode(b);
                    if (label != null)
                        torxConnector.enqueueObservation(label);
                    else
                        System.err.println("iutReader got unexpected observation: ");
                } catch (EOFException e) {
                    System.err.println("iutReader got eof exception: " + e.getMessage());
                    torxConnector.eot();
                    return;
                } catch (Exception e) {
                    System.err.println("iutReader got exception: " + e.getMessage());
                }
            }
        }
    }

}

/**
 * Exception that we throw when we have to encode an unknown label
 */
class EncodeError extends Exception {
    public EncodeError(String error) {
        super(error);
    }
}

