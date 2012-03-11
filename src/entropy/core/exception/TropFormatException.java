package entropy.core.exception;

import entropy.core.interfaces.Trop;

public class TropFormatException extends TropException {

  public TropFormatException() {
    super();
  }

  public TropFormatException(Trop t) {
    super(t);
  }

  public TropFormatException(String s, Trop t) {
    super(s, t);
  }

  public TropFormatException(String s) {
    super(s);
  }

}
