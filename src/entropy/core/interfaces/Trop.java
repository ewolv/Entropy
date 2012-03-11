package entropy.core.interfaces;

import java.util.Set;

import entropy.core.impl.Status;

/**
 * Object is a claim of value on the network.
 * 
 * The status can be one of three states:
 *  - Receipt
 *  - Payment
 *  - Fee
 * 
 * Receipt has been signed by the recipient, and no longer holds value.
 * Payment 
 * 
 * @author eric
 *
 */
public interface Trop extends Comparable<Trop> {
  
  public String getParentId();
  
  public String getId();
  public Status getStatus();
  public void setStatus(Status status);
  public void setValidatedTime(long validatedTime);
  
  public byte getValue();
  public byte getRecipientValue();
  
  public String getFileDigest();
  public long getFileSizeBytes();
  
  public String getRecipient();
  public String getCreator();
  public String getCreatorSignature();
  public void setCreatorSignature(String creatorSignature);
  public String getCourierBlock();
  public void setCourierBlock(String courierBlock);
  public Set<String> getCourierKeys();
  public void addCourierKey(String courierKey);
  
  public Set<String> getChildIds();
  public void addChildId(String childId);
  
  public boolean isBlackList();
  public void setBlackList(boolean blacklist);
  
  
  public long getValidatedTime();
  
  public double getAppraisedValue();
  public void setAppraisedValue(double appraisedValue);
  
  public int getZeroCount();
}