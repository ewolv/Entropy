package entropy.core.exception;

import entropy.core.interfaces.Trop;

public class TropValueException extends TropException {

  public TropValueException() {
    super();
  }

  public TropValueException(Trop t) {
    super(t);
    // TODO Auto-generated constructor stub
  }

  public TropValueException(String s, Trop t) {
    super(s, t);
  }

  public TropValueException(String s) {
    super(s);
  }

}
