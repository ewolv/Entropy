package entropy.core.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import entropy.core.exception.TropBlacklistException;
import entropy.core.exception.TropException;
import entropy.core.exception.TropRelationException;
import entropy.core.exception.TropValueException;
import entropy.core.interfaces.Rater;
import entropy.core.interfaces.Trop;
import entropy.core.utils.TropUtils;

public class Validator {
  
  String address;
  /**
   * the trops to be validated
   */
  Map<String,Trop> trops;
  
  /**
   * trops in store that have the same id as incoming
   */
  Map<String,Trop> localCopies;
  
  /**
   * Validated trops will be returned to the client
   */
  Set<Trop> results = new TreeSet<Trop>();
  
  Set<String> blacklistRecipients;
  
  Rater rater;
  
  public Validator(String address, Rater rater, Map<String,Trop> trops, Map<String,Trop> localCopies, Set<String> blacklistRecipients) {
    
    if(address == null || address.equals("") || rater == null || trops == null || trops.isEmpty()) {
System.out.println("VALIDATOR NOT CONFIGURED");
System.exit(0);
    }
    this.address = address;
    this.rater = rater;
    this.trops = trops;
    this.localCopies = localCopies;
    this.blacklistRecipients = blacklistRecipients;
  }
  
  public Set<Trop> validate() throws TropException {
    if(trops == null || trops.isEmpty()) {
//TODO: LOGGING
System.out.println("VALIDATOR NOT CONFIGURED");
System.exit(0);
    }
    Trop t = getOrphan();
    v(t);
    return results;
  }
  
  private Status v(Trop t) throws TropException {
    if(blacklistRecipients.contains(t.getRecipient())) {
      throw new TropBlacklistException(t);
    }
    TropUtils.checkCreatorSignature(t);
    Set<Trop> children = new HashSet<Trop>();
    boolean ourChild = false;
    for(String cId : t.getChildIds()) {
      Trop c = trops.get(cId);
      switch(v(c)) {
      case FEE:
        break;
      default:
        if(t.getStatus()!=Status.REC) throw new TropRelationException(t);
      }
      if(address.equals(c.getCreator())) {
        ourChild = true;
      }
    }
    TropUtils.checkValue(t, children.toArray(new Trop[]{}));
    Trop localCopy = localCopies.get(t.getId());
    switch(t.getStatus()) {
    case REC:
      TropUtils.checkRecipientSignature(t);
      if(null!=localCopy) {
        switch(localCopy.getStatus()) {
        case REC:
          if(!localCopy.getCourierBlock().equals(t.getCourierBlock())) {
            rater.blacklist(t,localCopy);
          }
          break;
        default:
          if(ourChild) {
            rater.delivered(t,(Trop[]) children.toArray());
          } else {
            rater.notDelivered(t);
          }
        }
      }
      results.add(t);
      break;
    case PAY:
    case FEE:
      if(null!=localCopy) {
        byte feesLocal = 0;
        for(String id : localCopy.getChildIds()) {
          Trop localChild = localCopies.get(id);
          if(!address.equals(localChild.getCreator())) {
            feesLocal = (byte) ((byte)feesLocal & (byte)localChild.getValue());
          }
        }
        byte feesIncoming = 0;
        for(Trop c : children) {
          if(!address.equals(c.getCreator())) {
            feesLocal = (byte) ((byte)feesLocal & (byte)c.getValue());
          }
        }
        if (((int)feesIncoming & 0xff) <= ((int)feesLocal & 0xff)) {
          results.add(t);
        }
      } else {
        results.add(t);
      }
      break;
    }
    return t.getStatus();
  }
  
  private Trop getOrphan() {
	//this can be changed so we can add arbitrary trops, 
	//as long as they are well-formed and the parent is present
    Trop t = trops.values().toArray(new Trop[]{})[0];
    do {
       t = trops.get(t.getParentId());
    } while(!t.getParentId().equals(t.getId()));
    return t;
  }
}
