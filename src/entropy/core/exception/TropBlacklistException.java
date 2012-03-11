package entropy.core.exception;

import entropy.core.interfaces.Trop;

public class TropBlacklistException extends TropException {

  public TropBlacklistException() {
    super();
  }

  public TropBlacklistException(String s, Trop t) {
    super(s, t);
  }

  public TropBlacklistException(String s) {
    super(s);
  }

  public TropBlacklistException(Trop t) {
    super(t);
  }

}
