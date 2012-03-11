package entropy.core.exception;

import entropy.core.interfaces.Trop;

public class TropRelationException extends TropException {

  public TropRelationException() {
    super();
  }

  public TropRelationException(Trop t) {
    super(t);
  }

  public TropRelationException(String s, Trop t) {
    super(s, t);
  }

  public TropRelationException(String s) {
    super(s);
  }
  
}
