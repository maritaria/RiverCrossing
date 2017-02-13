package practicum;

import java.io.*;

/**
 * This class implements the user interface of the program for testing with TorX,
 * by extending the general UI class.
 */
public class TorXUI extends UI {
  DataInputStream in = new DataInputStream(System.in);
  DataOutputStream out = new DataOutputStream(System.out);
  /**
   * Constructs a user interface based on a model.
   */
  public TorXUI(Model model) {
    super(model);
  }
  /**
   * Ask the user which item to transport.
   */
  public Model.Move getMove(InitialReminder ir, String prompt) {
    byte b = 0;
    Model.Move move = null;
    while(move == null) {
        try {
          b = in.readByte();
        } catch (EOFException e) { // end-of-input
	        System.exit(0);
        } catch (IOException e) {
            errln("ioexception when reading input: "+e.getMessage());
            System.exit(1);
        }
        System.err.printf("read %x\n", b);
        if      (isBitSet(b,0)) move = Model.Move.NOTHING;
        else if (isBitSet(b,1)) move = Model.Move.GOAT;
        else if (isBitSet(b,2)) move = Model.Move.CABBAGE;
        else if (isBitSet(b,3)) move = Model.Move.WOLF;
        else                    errln("unexpected input");
        if (ir!=null)
	      ir.reset();
        input(b);
    }
    return move;
  }
  /**
   * Report the result of the move to the user.
   */
  public void reportResult(Model.Result result) {
      byte b = 0;
      if(result == Model.Result.FINISHED)    output(setBit(b,0));
      else if (result == Model.Result.EATEN) output(setBit(b,1));
      else if (result == Model.Result.RETRY) output(setBit(b,2));
      else if (result == Model.Result.INIT)  output(setBit(b,3));
      else                                   nooutput("delta"); 
  }
 /*
  * bit operations
  */
  private static Boolean isBitSet(byte b, int bit) {
    return (b & (1 << bit)) != 0;
  }
  private byte setBit(byte b, int i) {
    b |= (1 << i);
    return b;
  }
 /*
  * output routines
  */
  private void output(byte b) {
    long now = System.currentTimeMillis();
    try { 
      out.writeByte(b);
      out.flush();
    } catch(IOException e) {
      errln("ioexception when writing output: "+e.getMessage());
      System.exit(1);
    }
    report(now, "State: ["+ model.getState() + "] yesoutput: "+String.format("%x", b));
  }
  private void input(Byte b) {
    report("State: ["+ model.getState() + "] read: "+String.format("%x", b));
  }
}
