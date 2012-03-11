package entropy.core.exception;

import entropy.core.interfaces.Trop;

public class TropException extends Exception {
  Trop t;
  public TropException() {
    super();
  }
  public TropException(String s) {
    super(s);
  }
  public TropException(Trop t) {
    super();
    this.t = t;
  }
  public TropException(String s, Trop t) {
    super(s);
    this.t = t;
  }
  public Trop getTrop() {
    return t;
  }
}
