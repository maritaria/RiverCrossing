package practicum;

import java.util.*;
/**
 * This class creates a model and instantiates the user interface.
 * Initially, all items and the farmer are on the left side of the river.
 */
public class Practicum {
  /**
   * Parse command line arguments and create model.
   * One command line argument is expected: a timeout value (in milliseconds).
   * The timeout value can be positive and negative.
   * If positive, an instance of class InitialReminder will be created --
   * it prints a message once every  timout  number of milliseconds,
   * when the Model is in the initial state.
   * If negative, no such instance is created.
   * The default timeout is negative.
   * The absolute value of timeout (ok, 2/3 of it) is used by the Model class to delay
   * outputs for animation/visualization, 
   */
  public static void main (String[] args) {
    boolean[] initial = { Model.LEFT, Model.LEFT, Model.LEFT, Model.LEFT };
    int miliseconds = -1*750; // negative: by default suppress the 'init!' messages
    boolean showInit = true;
    boolean human = true;
    for(int i=0; i < args.length; i++) {
      if (args[i].equals("-h"))
	    human = true;
      else if (args[i].equals("-t"))
	    human = false;
      else if (args.length == i+1) {
	    try {
	      miliseconds = Integer.parseInt(args[i]);
	    } catch(NumberFormatException nfe) {
            System.err.println("ignoring invalid timeout argument: \""+args[i]+"\""+
                "; using default timeout ("+miliseconds+")");
	    }
      } else
        System.err.println("ignoring argument: \""+args[i]+"\"");
    }
    if (miliseconds < 0) {
	miliseconds =  -miliseconds;
	showInit = false;
    }
    Model model = new Model(initial, (2*miliseconds)/3);
    UI ui;
    if(human)
        ui = new HumanUI(model);
    else
        ui = new TorXUI(model);
    new Engine(model, ui).play(miliseconds, showInit);
  }
}
/**
 * This class is the game engine.
 * It uses a UI to interact with the user or the tester.
 */
class Engine {
  Model model;
  UI ui;
  /**
   * Constructs an engine that uses a UI.
   */
  public Engine(Model model, UI ui){
    this.model = model;
    this.ui = ui;
  }
  /**
   * Plays the (infinite) game.
   *
   * First, the user is welcomed to the game. Then, indefinitely
   * the following happens:
   *  (1) the current state is presented
   *  (2) the user is asked which item to transport
   *  (3) the choice is parsed to the representation that is used by the model
   *  (4) the move is performed on the model
   *  (5) the result of the move is given to the user
   */
  public void play(int miliseconds, boolean showInit) {
    ui.welcome("Welcome to the wolf-goat-cabbage puzzle!");
    InitialReminder ir = null;
    if (showInit) {
    	ir = new InitialReminder(model, ui, miliseconds);
    }
    boolean stop = false;
    while(!stop) {
      ui.presentState("The current state: " + model.getState());
      Model.Move move = ui.getMove(ir, "Which item do you want to transport?");
      ui.diag("have choice: "+move.toString());
      ui.diag("about to do transition");
      Model.Result result = model.doTransition(move);   
      ui.diag("done transition");
      ui.reportResult(result);
      if(result == Model.Result.FINISHED) {
        model.reset();
} } } }
/**
 * This class takes care of outputting the action init! when the model
 * is in the initial state and no action was provided for some number of miliseconds.
 */
class InitialReminder {
  Timer timer;
  Model model;
  UI ui;
  int miliseconds;

  public InitialReminder(Model model, UI ui, int miliseconds) {
    this.miliseconds = miliseconds;
    this.model = model;
    this.ui = ui;
    set();
  }
  public void set() {
    timer = new Timer();
    timer.scheduleAtFixedRate(new Reminder(), miliseconds, miliseconds);
  }
  public void reset() {
    ui.diag("cancelling timer...");
    timer.cancel();
    set();
    ui.diag("done cancelling timer");
  }

  class Reminder extends TimerTask{
    public void run () {
      ui.diag("timertask");
      if (model.inInitialState()) {
	      ui.reportResult(Model.Result.INIT);
} } } }
