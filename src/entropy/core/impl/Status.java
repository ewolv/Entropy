package entropy.core.impl;

import java.util.HashMap;
import java.util.Map;
/**
* Trop can be a Fee, a Payment, or a Receipt
*
* @author eric
*
*/
public enum Status {
  REC() {
    public String toString() {
      return "REC";
    }
  },
  PAY() {
    public String toString() {
      return "PAY";
    }
  },
  FEE() {
    public String toString() {
      return "FEE";
    }
  };
  
  private static Map<String,Status> stringMap = new HashMap<String,Status>();
  
  static {
    for (Status s : Status.values()) {
      stringMap.put(s.toString(), s);
    }
  }
  
  public static Status enumFromString(String s) {
    if (!stringMap.containsKey(s)) {
      throw new IllegalArgumentException(s + " is not a valid Status");
    }
    return stringMap.get(s);
  }
}