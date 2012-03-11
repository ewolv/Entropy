package entropy.core.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Base64;

import entropy.core.impl.Status;
import entropy.core.interfaces.Trop;

/**
 * @author eric
 *
 */
public class TropBuilder {

  public Trop build() {
    
    Trop t = new Trop() {
      
      private String parentId = TropBuilder.this.parentId;
      @Override
      public String getParentId() {
        return this.parentId;
      }
      private String id = TropBuilder.this.id;
      @Override
      public String getId() {
        return this.id;
      }
      private Status status = TropBuilder.this.status;
      @Override
      public Status getStatus() {
        return this.status;
      }
      @Override
      public void setStatus(Status status) {
        this.status = status;
      }
      private long validatedTime = TropBuilder.this.validatedTime;
      @Override
      public long getValidatedTime() {
        return validatedTime;
      }
      @Override
      public void setValidatedTime(long validatedTime) {
        this.validatedTime = validatedTime;
      }
      private byte value = TropBuilder.this.value;
      @Override
      public byte getValue() {
        return this.value;
      }
      private byte recipientValue = TropBuilder.this.recipientValue;
      @Override
      public byte getRecipientValue() {
        return this.recipientValue;
      }
      private double appraisedValue = TropBuilder.this.appraisedValue;
      @Override
      public double getAppraisedValue() {
        return this.appraisedValue;
      }
      @Override
      public void setAppraisedValue(double appraisedValue) {
        this.appraisedValue = appraisedValue;
      }
      private String fileDigest = TropBuilder.this.fileDigest;
      @Override
      public String getFileDigest() {
        return this.fileDigest;
      }
      private long fileSizeBytes = TropBuilder.this.fileSizeBytes;
      @Override
      public long getFileSizeBytes() {
        return this.fileSizeBytes;
      }
      private String recipient = TropBuilder.this.recipient;
      @Override
      public String getRecipient() {
        return this.recipient;
      }
      private String creator = TropBuilder.this.creator;
      @Override
      public String getCreator() {
        return this.creator;
      }
      private String creatorSignature = TropBuilder.this.creatorSignature;
      @Override
      public String getCreatorSignature() {
        return this.creatorSignature;
      }
      @Override
      public void setCreatorSignature(String creatorSignature) {
        this.creatorSignature = creatorSignature;
      }
      private String courierBlock = TropBuilder.this.courierBlock;
      @Override
      public String getCourierBlock() {
        return this.courierBlock;
      }
      @Override
      public void setCourierBlock(String courierBlock) {
        this.courierBlock = courierBlock;
      }
      private Set<String> courierKeys = (TropBuilder.this.courierKeys == null ? new TreeSet<String>() : TropBuilder.this.courierKeys);
      @Override
      public Set<String> getCourierKeys() {
        return this.courierKeys;
      }
      @Override
      public void addCourierKey(String courierKey) {
        if(courierKeys == null) {
          courierKeys = new TreeSet<String>();
        }
        courierKeys.add(courierKey);
      }
      
      
      private Set<String> childIds = (TropBuilder.this.childIds == null ? new TreeSet<String>() : TropBuilder.this.childIds);
      @Override
      public Set<String> getChildIds() {
        return this.childIds;
      }
      /**
       * Add a child to this trop.  Adds id to list and subtracts amount 
       * from value less fees.
       */
      @Override
      public void addChildId(String childId) {
        if(childIds == null) {
          childIds = new TreeSet<String>();
        }
        childIds.add(childId);
      }
      private boolean blacklist = TropBuilder.this.blacklist;
      @Override
      public boolean isBlackList() {
        return this.blacklist;
      }
      @Override
      public void setBlackList(boolean b) {
        this.blacklist = b;
      }

      private int zeroCount = TropBuilder.this.zeroCount;
      @Override
      public int getZeroCount() {
        return this.zeroCount;
      }
      
      public void setZeroCount(int zeroCount) {
        this.zeroCount = zeroCount;
      }
      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(appraisedValue);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (blacklist ? 1231 : 1237);
        result = prime * result + ((childIds == null) ? 0 : childIds.hashCode());
        result = prime * result
            + ((courierBlock == null) ? 0 : courierBlock.hashCode());
        result = prime * result
            + ((courierKeys == null) ? 0 : courierKeys.hashCode());
        result = prime * result + ((creator == null) ? 0 : creator.hashCode());
        result = prime * result
            + ((creatorSignature == null) ? 0 : creatorSignature.hashCode());
        result = prime * result
            + ((fileDigest == null) ? 0 : fileDigest.hashCode());
        result = prime * result + (int) (fileSizeBytes ^ (fileSizeBytes >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((parentId == null) ? 0 : parentId.hashCode());
        result = prime * result
            + ((recipient == null) ? 0 : recipient.hashCode());
        result = prime * result + recipientValue;
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + (int) (validatedTime ^ (validatedTime >>> 32));
        result = prime * result + value;
        result = prime * result + zeroCount;
        return result;
      }
      @Override
      public boolean equals(Object obj) {
        if (this == obj)
          return true;
        if (obj == null)
          return false;
        if (getClass() != obj.getClass())
          return false;
        Trop other = (Trop) obj;
        if (Double.doubleToLongBits(appraisedValue) != Double
            .doubleToLongBits(other.getAppraisedValue()))
          return false;
        if (zeroCount != other.getZeroCount())
          return false;
        if (blacklist != other.isBlackList())
          return false;
        if (childIds == null) {
          if (other.getChildIds() != null)
            return false;
        } else if (!childIds.equals(other.getChildIds()))
          return false;
        if (courierBlock == null) {
          if (other.getCourierBlock() != null)
            return false;
        } else if (!courierBlock.equals(other.getCourierBlock()))
          return false;
        if (courierKeys == null) {
          if (other.getCourierKeys() != null)
            return false;
        } else if (!courierKeys.equals(other.getCourierKeys()))
          return false;
        if (creator == null) {
          if (other.getCreator() != null)
            return false;
        } else if (!creator.equals(other.getCreator()))
          return false;
        if (creatorSignature == null) {
          if (other.getCreatorSignature() != null)
            return false;
        } else if (!creatorSignature.equals(other.getCreatorSignature()))
          return false;
        if (fileDigest == null) {
          if (other.getFileDigest() != null)
            return false;
        } else if (!fileDigest.equals(other.getFileDigest()))
          return false;
        if (fileSizeBytes != other.getFileSizeBytes())
          return false;
        if (id == null) {
          if (other.getId() != null)
            return false;
        } else if (!id.equals(other.getId()))
          return false;
        if (parentId == null) {
          if (other.getParentId() != null)
            return false;
        } else if (!parentId.equals(other.getParentId()))
          return false;
        if (recipient == null) {
          if (other.getRecipient() != null)
            return false;
        } else if (!recipient.equals(other.getRecipient()))
          return false;
        if (recipientValue != other.getRecipientValue())
          return false;
        if (status != other.getStatus())
          return false;
        if (validatedTime != other.getValidatedTime())
          return false;
        if (value != other.getValue())
          return false;
        return true;
      }

      @Override
      public String toString() {
        return "Trop [parentId=" + parentId + ", id=" + id + ", status="
            + status + ", validatedTime=" + validatedTime + ", value=" + value
            + ", zeroCount=" + zeroCount
            + ", recipientValue=" + recipientValue + ", appraisedValue="
            + appraisedValue + ", fileDigest=" + fileDigest + ", fileSizeBytes="
            + fileSizeBytes + ", recipient=" + recipient + ", creator=" + creator
            + ", creatorSignature=" + creatorSignature + ", courierBlock="
            + courierBlock + ", blacklist=" + blacklist + ", childIds="
            + childIds + ", courierKeys=" + courierKeys + "]";
      }
      @Override
      public int compareTo(Trop o) {
        return id.compareTo(o.getId());
      }
      
    };
    if(t.getId() == null) {
      
    }
    return t;
  }
  

  private TropBuilder() {
  }
  /**
   * Reset all values to null or 0
   * 
   * @return
   */
  public TropBuilder clear() {
    this.appraisedValue = 0;
    this.blacklist = false;
    this.courierBlock = null;
    this.courierKeys = null;
    this.creator = null;
    this.creatorSignature = null;
    this.childIds = null;
    this.fileDigest = null;
    this.fileSizeBytes = 0;
    this.id = null;
    this.parentId = null;
    this.recipient = null;
    this.recipientValue = (byte) 0;
    this.status = null;
    this.validatedTime = 0;
    this.value = (byte)0;
    return this;
  }
  /**
   * Clears the trop builder and returns the reference
   * @return
   */
  public static TropBuilder getBuilder() {
    return tropBuilder.clear();
  }
  private static TropBuilder tropBuilder = new TropBuilder();
  
  private Status status = Status.FEE;
  private long validatedTime;
  
  private String parentId;
  private String id;
  
  private byte value;
  private byte recipientValue;
  private double appraisedValue;
  private int zeroCount;
  
  private String fileDigest;
  private long fileSizeBytes;
  
  private String recipient;
  private String creator;
  
  private String creatorSignature;
  private String courierBlock;
  private Set<String> courierKeys;
  
  private Set<String> childIds;
  
  private boolean blacklist;

  public TropBuilder setStatus(Status status) {
    this.status = status;
    return this;
  }

  public TropBuilder setValidatedTime(long validatedTime) {
    this.validatedTime = validatedTime;
    return this;
  }

  public TropBuilder setParentId(String parentId) {
    this.parentId = parentId;
    return this;
  }

  public TropBuilder setId(String id) {
    this.id = id;
    return this;
  }
  
  /**
   * hash the ID
   * 
   * (parentId + status + value + receipient value + filedigest + filesize +
   *  recipient + creator) >>=(SHA-256)=> ID
   * @return
   */
  public TropBuilder calculateId() {
    MessageDigest digest = null;
    StringBuilder sb = new StringBuilder();
    try {
    digest = MessageDigest.getInstance("SHA-256");
    if(status == null || parentId == null || fileDigest == null || recipient == null || creator == null) {
      throw new RuntimeException("TROP IS NOT PROPERLY FORMATTED.");
    }
    //create the string to hash
    sb.append(parentId);
    sb.append(value);
    sb.append(recipientValue);
    sb.append(fileDigest);
    sb.append(fileSizeBytes);
    sb.append(recipient);
    sb.append(creator);
    
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    try {
      id = Base64.encodeBase64String(digest==null?null:digest.digest(Base64.decodeBase64(sb.toString().getBytes("UTF8"))));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return this;
  }

  public TropBuilder setValue(byte value) {
    this.value = value;
    return this;
  }

  public TropBuilder setRecipientValue(byte recipientValue) {
    this.recipientValue = recipientValue;
    return this;
  }

  public TropBuilder setAppraisedValue(double appraisedValue) {
    this.appraisedValue = appraisedValue;
    return this;
  }

  public TropBuilder setFileDigest(String fileDigest) {
    this.fileDigest = fileDigest;
    return this;
  }

  public TropBuilder setFileSizeBytes(long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
    return this;
  }

  public TropBuilder setRecipient(String recipient) {
    this.recipient = recipient;
    return this;
  }

  public TropBuilder setCreator(String creator) {
    this.creator = creator;
    return this;
  }

  public TropBuilder setCreatorSignature(String creatorSignature) {
    this.creatorSignature = creatorSignature;
    return this;
  }

  public TropBuilder setCourierBlock(String courierBlock) {
    this.courierBlock = courierBlock;
    return this;
  }

  public TropBuilder setCourierKeys(Set<String> courierKeys) {
    this.courierKeys = courierKeys;
    return this;
  }
  public TropBuilder addCourierKey(String key) {
    if(this.courierKeys==null) {
      this.courierKeys = new TreeSet<String>();
    }
    this.courierKeys.add(key);
    return this;
  }
  public TropBuilder setChildIds(Set<String> childIds) {
    this.childIds = childIds;
    return this;
  }

  public void addChildId(String childId) {
    if(this.childIds == null) {
      this.childIds = new TreeSet<String>();
    }
    this.childIds.add(childId);
  }
  
  public TropBuilder setBlacklist(boolean blacklist) {
    this.blacklist = blacklist;
    return this;
  }
  public TropBuilder setZeroCount(int zeroCount) {
    this.zeroCount = zeroCount;
    return this;
  }
  public int getZeroCount() {
    return zeroCount;
  }
}