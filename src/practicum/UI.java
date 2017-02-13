package practicum;

import java.text.SimpleDateFormat;

/**
 * This abstract class implements general functionality of the user interface.
 * It needs to receive a model to work with.
 * Methods getMove() and reportResult() are abstract.
 */
public abstract class UI {
    Model model;
    SimpleDateFormat fmt;

    /**
     * Constructs a user interface based on a model.
     */
    public UI(Model model) {
        this.model = model;
        fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    /**
     * Welcome the user to the game.
     */
    public void welcome(String s) {
        errln(s);
    }

    /**
     * Present the current state to the user.
     */
    public void presentState(String s) {
        errln("\n" + s);
    }

    /**
     * Ask the user which item to transport.
     * Needs UI-method specific implementation.
     */
    public abstract Model.Move getMove(InitialReminder ir, String prompt);

    /**
     * Report the result of the move to the user.
     * Needs UI-method specific implementation.
     */
    public abstract void reportResult(Model.Result result);

    /**
     * Report diagnostic message to the user.
     */
    public void diag(String s) {
        report(s);
    }

    protected void nooutput(String s) {
        report("State: [" + model.getState() + "] nooutput: " + s);
    }

    protected void errln(String s) {
        System.err.println(s);
        System.err.flush();
    }

    protected void report(long now, String s) {
        errln(fmt.format(now) + " " + s);
    }

    protected void report(String s) {
        long now = System.currentTimeMillis();
        errln(fmt.format(now) + " " + s);
    }
}
