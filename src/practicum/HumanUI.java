package practicum;

import java.util.*;
/**
 * This class implements the user interface of the program for a human user,
 * by extending the general UI class.
 */
public class HumanUI extends UI {
  Scanner scanner = new Scanner(System.in);
  /**
   * Constructs a user interface based on a model.
   */
  public HumanUI(Model model){
    super(model);
  }
  /**
   * Ask the user which item to transport.
   */
  public Model.Move getMove(InitialReminder ir, String prompt) {
      String choice = "";
      while (!(choice.equals("c?") || choice.equals("g?")
             ||choice.equals("w?") || choice.equals("n?"))) {
        errln("\n"+prompt );
        errln("Type c?(abbage), g?(oat), w?(olf)," + "or n?(othing): ");
        if (! scanner.hasNext()) // end-of-input
	  System.exit(0);
        choice = scanner.next();
        if (ir!=null)
	  ir.reset();
	input(choice);
      }
      report("have choice: "+choice);
      Model.Move move = Model.Move.NOTHING;
      if (choice.equals("g?")) move = Model.Move.GOAT;
      if (choice.equals("c?")) move = Model.Move.CABBAGE;
      if (choice.equals("w?")) move = Model.Move.WOLF;
      return move;
  }
  /**
   * Report the result of the move to the user.
   */
  public void reportResult(Model.Result result) {
      if(result == Model.Result.FINISHED)
        output("done!");
      else if (result == Model.Result.EATEN)
        output("eaten!");
      else if (result == Model.Result.RETRY)
        output("retry!");
      else if (result == Model.Result.INIT)
        output("init!");
      else
	nooutput("delta"); 
  }
  private void output(String s) {
    long now = System.currentTimeMillis();
    System.out.println(s);
    System.out.flush();
    report(now, "State: ["+ model.getState() + "] yesoutput: "+s);
  }
  private void input(String s) {
    report("State: ["+ model.getState() + "] read: "+s);
  }
}
