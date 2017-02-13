package practicum;

import java.util.Vector;

/**
 * This class models the location of the cabbage, goat, wolf and farmer.
 * Moreover, it contains the logic that states when a move is valid and
 * when it is safe.
 */
public class Model {
    public enum Move {
        WOLF("Wolf"),
        CABBAGE("Cabbage"),
        GOAT("Goat"),
        FARMER("Farmer"),
        NOTHING("");
        private final String name;

        private Move(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum Result {
        FINISHED,
        EATEN,
        RETRY,
        NORMALMOVE,
        INIT,
    }

    public static final boolean LEFT = false;
    public static final boolean RIGHT = true;
    private Move[] allItems = {
            Move.WOLF,
            Move.CABBAGE,
            Move.GOAT,
            Move.FARMER,
    };
    /**
     * This array models the location of all items including the farmer.
     * The enumeration constants Move.WOLF, Move.CABBAGE, Move.GOAT and Move.FARMER
     * can be used as indices of the corresponding items using the ordinal() methods,
     * as in Move.WOLF.ordinal().
     * Moreover, the constants LEFT and RIGHT are used to specify whether an item is
     * on the left or the right side of the river.
     * For instance, if items[Move.GOAT.ordinal()] == RIGHT
     * this means that the goat is on the right side of the river.
     */
    private boolean[] items;
    private boolean[] initialState;
    /**
     * Variable sleeptime indicates how long methods showUnsafeMove, showInvalidMove
     * and * showInitialState must sleep, to give nicer visualization.
     */
    private int sleeptime;

    /**
     * Constructor, initialising the model.
     * Also, the initial state is saved, such that we can later on reset.
     */
    public Model(boolean[] initial, int sleeptime) {
        this.items = initial;
        this.initialState = initial.clone();
        this.sleeptime = sleeptime;
        showInitialState(false);
    }

    public boolean inInitialState() {
        for (int i = 0; i <= 3; i++) {
            if (items[i] != initialState[i])
                return false;
        }
        return true;
    }

    /**
     * Resets the model to the initial state.
     */
    public void reset() {
        this.items = initialState.clone();
        showInitialState(true);
    }

    /**
     * Computes whether the current state is the final state.
     * This is the case when wolf, goat and cabbage are on the right side on the river.
     */
    public boolean finished() {

        // implement the body of this method
        for (int i = 0; i <= 3; i++) {
            if (items[i] != RIGHT)
                return false;
        }
        return true;

    }

    /**
     * Computes whether the given move is valid.
     * The move can be Move.CABBAGE, Move.GOAT, Move.WOLF or Move.NOTHING,
     * denoting what the farmer should move.
     * Moving only the farmer is always valid, while an item can only be moved
     * when it is on the same side of the river as the farmer.
     */
    public boolean validMove(Move move) {

        // implement the body of this method

        if (move == Move.FARMER) {
            return true;
        } else {
            boolean farmer_side = items[Move.FARMER.ordinal()];
            boolean item_side = items[move.ordinal()];
            return (farmer_side == item_side);
        }
    }

    /**
     * Computes whether the given move is safe.
     * A move is unsafe when afterwards either the goat and the cabbage are unsupervised
     * on the same side, or when the goat and the wolf are unsupervised on the same side.
     */
    public boolean safeMove(Move move) {

        // implement the body of this method

        boolean farmer_next = !items[Move.FARMER.ordinal()];
        boolean wolf_next = (move == Move.WOLF) ? !items[Move.WOLF.ordinal()] : items[Move.WOLF.ordinal()];
        boolean goat_next = (move == Move.GOAT) ? !items[Move.GOAT.ordinal()] : items[Move.GOAT.ordinal()];
        boolean cabbage_next = (move == Move.CABBAGE) ? !items[Move.CABBAGE.ordinal()] : items[Move.CABBAGE.ordinal()];

        if (wolf_next == goat_next && wolf_next != farmer_next) {
            return false;
        }
        if (goat_next == cabbage_next && goat_next != farmer_next) {
            return false;
        }
        return true;

    }

    /**
     * Execute the given move.
     * This updates the information about who is at what side of the river.
     */
    public void doMove(Move move) {

        // implement the body of this method


        items[Move.FARMER.ordinal()] = !items[Move.FARMER.ordinal()];
        items[Move.WOLF.ordinal()] = (move == Move.WOLF) ? !items[Move.WOLF.ordinal()] : items[Move.WOLF.ordinal()];
        items[Move.GOAT.ordinal()] = (move == Move.GOAT) ? !items[Move.GOAT.ordinal()] : items[Move.GOAT.ordinal()];
        items[Move.CABBAGE.ordinal()] = (move == Move.CABBAGE) ? !items[Move.CABBAGE.ordinal()] : items[Move.CABBAGE.ordinal()];


    }

    /**
     * Sleep sleeptime
     */
    private void sleep() {
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
        }
    }


    /**
     * Methods showUnsafeMove, showInvalidMove and showInitialState
     * provide output that can be used for visualization.
     */
    private void showUnsafeMove(Move move) {
        doMove(move);
        System.err.println("[" + getState() + "]");
        sleep();
        doMove(move);
    }

    private void showInvalidMove(Move move) {
        Vector<Move> hl = new Vector<Move>();
        hl.add(move);
        hl.add(Move.FARMER);
        System.err.println("[" + getState(hl) + "]");
        sleep();
    }

    private void showInitialState(boolean doDelay) {
        if (doDelay)
            sleep();
        System.err.println("[" + getState() + "]");
    }

    /**
     * This method performs the move provided by the parameter
     * in case it is both valid and safe.
     * Its result is one of the following four options:
     * Result.RETRY      - the move was not valid (and thus was not performed)
     * Result.EATEN      - the move was not safe (and thus was not performed)
     * Result.FINISHED   - the move was valid and safe, and led to the final state
     * Result.NORMALMOVE - the move was valid and safe, and did not lead to
     * the final state
     */
    public Result doTransition(Move move) {

        // implement the body of this method (using validMove(), safeMove(),
        // doMove(), showInvalidMove(), and showUnsafeMove() )

        if (!validMove(move)) {
            showInvalidMove(move);
            return Result.RETRY;
        }
        if (!safeMove(move)) {
            showUnsafeMove(move);
            return Result.EATEN;
        }
        doMove(move);
        if (finished()) {
            return Result.FINISHED;
        }
        return Result.NORMALMOVE;
    }

    /**
     * Provides a String representation of the current state.
     */
    public String getState() {
        return getState(new Vector<Move>());
    }

    private String getState(Vector<Move> highLight) {
        String left = "";
        String right = "";
        for (Move m : allItems) {
            boolean hl = highLight.contains(m);
            String nm = nameOf(m, hl);
            if (items[m.ordinal()] == LEFT)
                left += nm + " ";
            else if (items[m.ordinal()] == RIGHT)
                right += nm + " ";
        }
        return left + "|RIVER| " + right;
    }

    private String nameOf(Move m, boolean hl) {
        if (hl)
            return m.toString().toUpperCase();
        else
            return m.toString();
    }
}
