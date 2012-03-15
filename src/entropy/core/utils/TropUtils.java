package entropy.core.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import entropy.core.exception.TropException;
import entropy.core.exception.TropFormatException;
import entropy.core.exception.TropValueException;
import entropy.core.impl.Status;
import entropy.core.impl.TropBuilder;
import entropy.core.interfaces.Trop;

public class TropUtils {

  public static Set<Trop> sign(PrivateKey privateKey, Trop payment, Trop... fees) throws TropException {
    
    //Validation:
    //Ensure not null, parents and children agree on relationship, value of children does not exceed parent.
    
    if (payment == null || payment == null || fees == null) {
//TODO: LOGGING
String message = "All values required for signature.";
System.out.println(message);
throw new IllegalArgumentException(message);
    }
    
    checkValue(payment, fees);
    
    List<SecretKeySpec> recoveredKeys = new ArrayList<SecretKeySpec>(),
                        listToModify;
    Set<Trop> feeTracker = new TreeSet<Trop>(Arrays.asList(fees));
    for(String wrappedKey : payment.getCourierKeys()) {
      try {
      //TODO: LOGGING
        recoveredKeys.add(SecurityUtils.unwrapAESkeyWithRSAPrivate(privateKey, Base64.decodeBase64(wrappedKey.getBytes("UTF8"))));
      } catch (InvalidKeyException e) {
String message = "InvalidKeyException";
System.out.println(message);
throw new RuntimeException(message);
      } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
      } catch (NoSuchPaddingException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
      } catch (UnsupportedEncodingException e) {
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
      }
    }
    
    listToModify = new ArrayList<SecretKeySpec>(recoveredKeys);
    
    outer:
    do {
        recoveredKeys = listToModify;
        for(SecretKeySpec aesKey : recoveredKeys) {
          
          String courierBlockBase64EncodedString = payment.getCourierBlock();
          byte[] base64EncodedByteArrayCourierBlock = null;
          try {
          //TODO: LOGGING
            base64EncodedByteArrayCourierBlock = courierBlockBase64EncodedString.getBytes("UTF8");
          } catch (UnsupportedEncodingException e1) {
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
          }
          byte[] rawCblock = Base64.decodeBase64(base64EncodedByteArrayCourierBlock);

          byte[] recovered = null;
          try {
          //TODO: LOGGING
          recovered = SecurityUtils.decryptAES(aesKey, rawCblock);
          } catch (BadPaddingException bpe) {
            //thrown on wrong key
            continue;
          } catch (InvalidKeyException e) {
String message = "InvalidKeyException";
System.out.println(message);
throw new RuntimeException(message);
          } catch (IllegalBlockSizeException e) {
String message = "IllegalBlockSizeException";
System.out.println(message);
throw new RuntimeException(message);
          } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
          } catch (NoSuchPaddingException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
          }
          byte[] buriedCblock = new byte[recovered.length - 256];
          byte[] signature = new byte[256];
          
          for(int i = 0 ; i < recovered.length ; i++) {
            if (i < buriedCblock.length) {
              buriedCblock[i] = recovered[i];
            } else {
              signature[i-buriedCblock.length] = recovered[i];
            }
          }
          String base64signature = Base64.encodeBase64String(signature);
          String base64courierBlock = Base64.encodeBase64String(buriedCblock);
          boolean signatureExistsOnChild = false;
          for(Trop child : fees) { //check the fees for the signature
            if (base64signature.equals(child.getCreatorSignature())) {
              signatureExistsOnChild = true;
              feeTracker.remove(child);
              break;
            }
          }
          
          //if the courier block is our ID, then we have hit the bottom of couriers and can now validate the creator's signatrue
          if(base64courierBlock.equals(payment.getId())) {
            payment.setCourierBlock(base64signature); //we set this to validate the sig(id) of the creator after this loop
            break outer;
          } else if(signatureExistsOnChild) {
            payment.setCourierBlock(base64courierBlock);
            listToModify.remove(aesKey);
            continue outer;
          }
        }
        throw new RuntimeException("Could not validate trop " + payment.getId());
    } while(true);
    
    //Recover the public key of the creator
    PublicKey recoveredPub = null;
    try {
    //TODO: LOGGING
      recoveredPub = SecurityUtils.recoverPublicKey(Base64.decodeBase64(payment.getCreator().getBytes("UTF8")));
    } catch (InvalidKeySpecException e) {
String message = "InvalidKeySpecException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (UnsupportedEncodingException e) {
String message = "InvalidKeyException";
System.out.println(message);
throw new RuntimeException(message);
    }
    //Instantiate a signature and validate
    //If this fails, then a later courier tried to forge 
    try {
    //TODO: LOGGING
      if(!SecurityUtils.validateSignature(recoveredPub, Base64.decodeBase64(payment.getId()), Base64.decodeBase64(payment.getCourierBlock()))) {
        throw new RuntimeException("creator: '" + payment.getCreator() + "' signature on Trop " + payment.getId() + " cannot be validated.");
      }
    } catch (InvalidKeyException e) {
String message = "InvalidKeyException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (SignatureException e) {
// TODO Auto-generated catch block
e.printStackTrace();
    }
    
    //fees on the trop that we did not validate (they were not in the courier block)
    if(!feeTracker.isEmpty()) {
//TODO: LOGGING
String message = "FEES ON TROP NOT IN COURIER BLOCK";
System.out.println(message);
throw new RuntimeException(message);
    }
    
    //signature verified.
    //sign payment
    
    try {
    //TODO: LOGGING
      payment.setCourierBlock(Base64.encodeBase64String(SecurityUtils.sign(privateKey, calculateRecipientPreSig(payment))));
    } catch (InvalidKeyException e) {
String message = "Key not properly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (SignatureException e) {
String message = "Signature improperly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    }
    
    payment.setStatus(Status.REC);
    Set<Trop> results = new TreeSet<Trop>();
    for(Trop child : fees) {
      child.setStatus(Status.PAY);
      results.add(child);
    }
    results.add(payment);
    return results;
  }
  
  
  
  public static Collection<Trop> createFee(Trop parent, byte value, byte rValue,
      String fileDigest, long fileSizeBytes, String recipient,
      PublicKey publicKey, PrivateKey privateKey, Collection<Trop> siblings) throws TropException {
      
//Construct the new Trop  
//////////////////////////////////////////////////////////////////////////////
    TropBuilder tb = TropBuilder.getBuilder();
    tb.setStatus(Status.FEE);
    tb.setParentId(parent.getId());
    tb.setValue(value);
    tb.setRecipientValue(rValue);
    tb.setFileDigest(fileDigest);
    tb.setFileSizeBytes(fileSizeBytes);
    tb.setRecipient(recipient);
    tb.setCreator(Base64.encodeBase64String(publicKey.getEncoded()));
    tb.calculateId();
    Trop t = tb.build();
//////////////////////////////////////////////////////////////////////////////

    parent.addChildId(t.getId());
    siblings.add(t);
    checkValue(parent, siblings.toArray(new Trop[]{}));
      
      
 //Recover the recipient's public key from the Base64 encoded string
 //////////////////////////////////////////////////////////////////////////////
    PublicKey recipientPublicKey = null;
    
    try {
      recipientPublicKey = SecurityUtils.recoverPublicKey(Base64.decodeBase64(recipient));
    } catch(NoSuchAlgorithmException nsae) {
      //No recovery - exit application
//TODO: LOGGING - fatal
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch(InvalidKeySpecException ikse) {
//TODO: LOGGING - 
String message = "Key could not be recovered.";
System.out.println(message);
ikse.printStackTrace();
throw new RuntimeException(message);
    }
//////////////////////////////////////////////////////////////////////////////
    
    
//Decode our ID
//////////////////////////////////////////////////////////////////////////////
    String base64EncodedStringRepresentationID = t.getId();
    byte[] base64EncodedByteRepresentationID = null;
    try {
    //TODO: LOGGING - 
      base64EncodedByteRepresentationID = base64EncodedStringRepresentationID.getBytes("UTF8");
    } catch (UnsupportedEncodingException e) {
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
    }
    byte[] rawID = Base64.decodeBase64(base64EncodedByteRepresentationID);
//////////////////////////////////////////////////////////////////////////////
    

//We sign the id of this trop.  then append our signature to the end of 
//our id.  This will be used by the recipient to know that they 
//are at the bottom of the courier fee list.
//////////////////////////////////////////////////////////////////////////////
    byte[] courierBlock = null;
    try {
    //TODO: LOGGING - 
      courierBlock = SecurityUtils.sign(privateKey,rawID);
    } catch (InvalidKeyException e) {
String message = "Key not properly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (SignatureException e) {
String message = "Signature improperly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    }
    
    byte[] cblock = new byte[256 + 32];
    for(int i = 0 ; i < cblock.length ; i++) {
      if (i < rawID.length) {
        cblock[i] = rawID[i];
      } else {
        cblock[i] = courierBlock[i-rawID.length];
      }
    }
//////////////////////////////////////////////////////////////////////////////
    
    
//AES ENCRYPTION
//Encrypt our signature + id with an AES key.  
//////////////////////////////////////////////////////////////////////////////
    SecretKeySpec aesKey = null;
    try {
    //TODO: LOGGING - 
      aesKey = SecurityUtils.generateAESKey();
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    }
    
    byte[] firstCourierBlock = null;
    try {
    //TODO: LOGGING - 
      firstCourierBlock = SecurityUtils.encryptAES(aesKey, cblock);
    } catch (InvalidKeyException e) {
String message = "Key not properly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    } catch (IllegalBlockSizeException e) {
String message = "IllegalBlockSizeException ";
System.out.println(message);
throw new RuntimeException(message);
    } catch (BadPaddingException e) {
String message = "BadPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (NoSuchPaddingException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    }
//////////////////////////////////////////////////////////////////////////////
    
//Update courier block
//////////////////////////////////////////////////////////////////////////////
    String encodedFirstCourierBlock = Base64.encodeBase64String(firstCourierBlock);
    t.setCourierBlock(encodedFirstCourierBlock);
//////////////////////////////////////////////////////////////////////////////
    
    
//RSA ENCRYPTION
//Wrap our AES key with the Recipient's public key and add it to courier keys
//////////////////////////////////////////////////////////////////////////////
    byte[] rsaEncrypted = null;
    try {
    //TODO: LOGGING
      rsaEncrypted = SecurityUtils.wrapAESkeyWithRSAPublic(recipientPublicKey, aesKey);
    } catch (InvalidKeyException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (IllegalBlockSizeException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (NoSuchPaddingException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    }
    String encodedFirstCourierKey = Base64.encodeBase64String(rsaEncrypted);
    t.addCourierKey(encodedFirstCourierKey);
//////////////////////////////////////////////////////////////////////////////
    
//creator signature
//////////////////////////////////////////////////////////////////////////////
    byte[] preSig = calculateCreatorPreSig(t);
    byte[] creatorSignature = null;
    try {
      //TODO: LOGGING
      creatorSignature = SecurityUtils.sign(privateKey,preSig);
    } catch (InvalidKeyException e) {
String message = "Key not properly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (SignatureException e) {
String message = "Signature improperly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    }
    t.setCreatorSignature(Base64.encodeBase64String(creatorSignature));
//////////////////////////////////////////////////////////////////////////////
    
    
//ADD TO PARENT
    
//Get the parent CBlock to add our signature to.
//////////////////////////////////////////////////////////////////////////////
    String parentCBlockB64 = parent.getCourierBlock();
    byte[] parentCBlockB64Byte = null;
    try {
      parentCBlockB64Byte = parentCBlockB64.getBytes("UTF8");
    } catch (UnsupportedEncodingException e) {
//FATAL
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
    }
    byte[] parentCBlockRaw = Base64.decodeBase64(parentCBlockB64Byte);
    
    byte[] newParentCblock = new byte[parentCBlockRaw.length + 256]; //add our signature on top of it
    for(int i = 0 ; i < newParentCblock.length ; i++) {
      if (i < parentCBlockRaw.length) {
        newParentCblock[i] = parentCBlockRaw[i];
      } else {
        //we add the child ID to the parent cblock.
        newParentCblock[i] = creatorSignature[i-parentCBlockRaw.length];
      }
    }
//////////////////////////////////////////////////////////////////////////////
    
//encrypt the new cblock
//////////////////////////////////////////////////////////////////////////////
    try {
      //TODO: LOGGING
      aesKey = SecurityUtils.generateAESKey();
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    }
    byte[] aesEncryptedNewParentCBlock = null;
    try {
      aesEncryptedNewParentCBlock = SecurityUtils.encryptAES(aesKey, newParentCblock);
    } catch (InvalidKeyException e) {
String message = "Key not properly formatted.";
System.out.println(message);
throw new RuntimeException(message);
    } catch (IllegalBlockSizeException e) {
String message = "IllegalBlockSizeException ";
System.out.println(message);
throw new RuntimeException(message);
    } catch (BadPaddingException e) {
String message = "BadPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (NoSuchPaddingException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    }
    String base64NewParentCblock = Base64.encodeBase64String(aesEncryptedNewParentCBlock);
    parent.setCourierBlock(base64NewParentCblock);
//////////////////////////////////////////////////////////////////////////////
    
//parent recipient public key is needed
//////////////////////////////////////////////////////////////////////////////
    String base64EncodedParentKey = parent.getRecipient();
    byte[] base64EncParentKeyByte = base64EncodedParentKey.getBytes();
    byte[] parentRecKeyRaw = Base64.decodeBase64(base64EncParentKeyByte);
    PublicKey parentRecipientPublicKey = null;
    try {
      parentRecipientPublicKey = SecurityUtils.recoverPublicKey(parentRecKeyRaw);
    } catch(NoSuchAlgorithmException nsae) {
      //No recovery - exit application
//TODO: LOGGING - fatal
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch(InvalidKeySpecException ikse) {
//TODO: LOGGING - 
String message = "Key could not be recovered.";
System.out.println(message);
throw new RuntimeException(message);
    }
//////////////////////////////////////////////////////////////////////////////

//Encrypt our courier key so only the recipient can use it to open the CBlock
//////////////////////////////////////////////////////////////////////////////
    byte[] wrappedCourierKey = null;
    try {
      wrappedCourierKey = SecurityUtils.wrapAESkeyWithRSAPublic(parentRecipientPublicKey, aesKey);
    } catch (InvalidKeyException e) {
String message = "InvalidKeyException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (IllegalBlockSizeException e) {
String message = "IllegalBlockSizeException";
System.out.println(message);
throw new RuntimeException(message);
    } catch (NoSuchAlgorithmException e) {
System.out.println("Fatal Exception - Algorithm not found.");
System.exit(0);
    } catch (NoSuchPaddingException e) {
String message = "NoSuchPaddingException";
System.out.println(message);
throw new RuntimeException(message);
    }
    String base64CourierKey = Base64.encodeBase64String(wrappedCourierKey);
    parent.addCourierKey(base64CourierKey);
    
//////////////////////////////////////////////////////////////////////////////
    Set<Trop> results = new TreeSet<Trop>();
    results.add(t);
    results.add(parent);
    
    return results;
  }
  
  private static byte[] calculateCreatorPreSig(Trop t) {    
    StringBuilder digest = new StringBuilder();
    digest.append(t.getParentId()).append(t.getId());
    digest.append(t.getValue()).append(t.getRecipientValue()).append(t.getFileDigest());
    digest.append(t.getFileSizeBytes()).append(t.getRecipient()).append(t.getCreator());
    try {
      //TODO: LOGGING
      return digest.toString().getBytes("UTF8");
    } catch (UnsupportedEncodingException e) {
//FATAL
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
    }
    return null;
  }
  
  private static byte[] calculateRecipientPreSig(Trop t) {
    StringBuilder digest = new StringBuilder();
    digest.append(t.getParentId()).append(t.getId());
    digest.append(t.getValue()).append(t.getRecipientValue()).append(t.getFileDigest());
    digest.append(t.getFileSizeBytes()).append(t.getRecipient()).append(t.getCreator());
    //sort the children
    TreeSet<String> specificallyOrdered = new TreeSet<String>(t.getChildIds());
    
    for(String childId : specificallyOrdered) {
      digest.append(childId);
    }
    
    try {
      return Base64.decodeBase64(digest.toString().getBytes("UTF8"));
    } catch (UnsupportedEncodingException e) {
//FATAL
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
    }
    return null;
  }
  
  public static void checkValue(Trop t, Trop ... children) throws TropValueException {
    byte totalFees = 0;
    for(Trop c : children) {
      totalFees = (byte) ((byte)totalFees & (byte)c.getValue());
    }
    byte shiftValue = t.getValue();
    while(shiftValue>0) { //take advantage of two's complement representation.  Shift until negative.
      shiftValue = (byte) (shiftValue << 1);
    }
    //http://stackoverflow.com/questions/11088/what-is-the-best-way-to-work-around-the-fact-that-all-java-bytes-are-signed/19186#19186
    if (((int)totalFees & 0xff) > ((int)shiftValue & 0xff)) {
//TODO: LOGGING
      throw new TropValueException(t);
    }
  }

  public static void checkRecipientSignature(Trop t) throws TropFormatException {
    if(t.getParentId().equals(t.getId()))return; //orphan node
    byte[] preSignature = calculateRecipientPreSig(t);
    PublicKey publicKey = null;
    try {
      //TODO: LOGGING
      publicKey = SecurityUtils.recoverPublicKey(Base64.decodeBase64(t.getRecipient().getBytes("UTF8")));
    } catch (InvalidKeySpecException e) {
      throw new TropFormatException("Invalid creator signature", t);
    } catch (NoSuchAlgorithmException e) {
System.out.println("FATAL : ALGORITHM NOT FOUND.");
System.exit(0);
    } catch (UnsupportedEncodingException e) {
System.out.println("FATAL : ENCODING NOT FOUND);");
System.exit(0);
    }
    
    try {
      if(!SecurityUtils.validateSignature(publicKey, preSignature, Base64.decodeBase64(t.getCourierBlock().getBytes("UTF8")))) {
        throw new TropFormatException("Invalid recipient signature",t);
      }
    } catch (InvalidKeyException e) {
      throw new TropFormatException("Invalid recipient key", t);
    } catch (NoSuchAlgorithmException e) {
System.out.println("FATAL : ALGORITHM NOT FOUND.");
System.exit(0);
    } catch (SignatureException e) {
      throw new TropFormatException("Invalid creator signature", t);
    } catch (UnsupportedEncodingException e) {
System.out.println("FATAL : ENCODING NOT FOUND);");
System.exit(0);
    }
  }
  public static void checkCreatorSignature(Trop t) throws TropFormatException {
    if (t.getId().equals(t.getParentId()))return; //orphan node
    byte[] preSignature = calculateCreatorPreSig(t);
    PublicKey publicKey = null;
    try {
      //TODO: LOGGING
      publicKey = SecurityUtils.recoverPublicKey(Base64.decodeBase64(t.getCreator().getBytes("UTF8")));
    } catch (InvalidKeySpecException e) {
      throw new TropFormatException("Invalid creator signature", t);
    } catch (NoSuchAlgorithmException e) {
System.out.println("FATAL : ALGORITHM NOT FOUND.");
System.exit(0);
    } catch (UnsupportedEncodingException e) {
System.out.println("FATAL : ENCODING NOT FOUND);");
System.exit(0);
    }
    try {
      //TODO: LOGGING
      if(!SecurityUtils.validateSignature(publicKey, preSignature, Base64.decodeBase64(t.getCreatorSignature().getBytes("UTF8")))) {
        throw new TropFormatException("Invalid recipient key", t);
      }
    } catch (InvalidKeyException e) {
      throw new TropFormatException("Invalid recipient key", t);
    } catch (NoSuchAlgorithmException e) {
System.out.println("FATAL : ALGORITHM NOT FOUND.");
System.exit(0);
    } catch (SignatureException e) {
      throw new TropFormatException("Invalid creator signature", t);
    } catch (UnsupportedEncodingException e) {
System.out.println("FATAL : ENCODING NOT FOUND);");
System.exit(0);
    }
  }
  public static String byteToBinaryString(byte... bytes) {
    StringBuilder binary = new StringBuilder();
    for (byte b : bytes)
    {
       int val = b;
       for (int i = 0; i < 8; i++)
       {
          binary.append((val & 128) == 0 ? 0 : 1);
          val <<= 1;
       }
       binary.append(' ');
    }
    return binary.toString();
  }
  
  public static Trop constructOrphan(PublicKey publicKey, PrivateKey privateKey) {
    //construct orphan trop where parent id = trop id

    //Construct the new Trop  
    //////////////////////////////////////////////////////////////////////////////
    TropBuilder tb = TropBuilder.getBuilder();
    tb.setStatus(Status.PAY);
    tb.setParentId("the_beginning______________________________");
    tb.setId("the_beginning______________________________");
    tb.setValue((byte)127);
    tb.setRecipientValue((byte)0);
    tb.setFileSizeBytes(0);
    tb.setRecipient(Base64.encodeBase64String(publicKey.getEncoded()));
    tb.setCreator(Base64.encodeBase64String(publicKey.getEncoded()));
    Trop t = tb.build();
    //////////////////////////////////////////////////////////////////////////////
      
    //the orphan must have its courier block constructed so children can be added and it signed
    
    //Decode our ID
    //////////////////////////////////////////////////////////////////////////////
    String base64EncodedStringRepresentationID = t.getId();
    byte[] base64EncodedByteRepresentationID = null;
    try {
    //TODO: LOGGING - 
      base64EncodedByteRepresentationID = base64EncodedStringRepresentationID.getBytes("UTF8");
    } catch (UnsupportedEncodingException e) {
System.out.println("Fatal Exception - Encoding not found.");
System.exit(0);
    }
    byte[] rawID = Base64.decodeBase64(base64EncodedByteRepresentationID);
    //////////////////////////////////////////////////////////////////////////////
    
    // Sig(Id) + ID is the base courier block, these operations are all performed on raw bytes - not encoded


  //We sign the id of this trop.  then append the id to the end of 
  //our signature.  This will be used by the recipient to know that they 
  //are at the bottom of the courier fee list.
  //////////////////////////////////////////////////////////////////////////////
      byte[] courierBlock = null;
      try {
      //TODO: LOGGING - 
        courierBlock = SecurityUtils.sign(privateKey,rawID);
System.out.println("courierBlock.length: " + courierBlock.length);
      } catch (InvalidKeyException e) {
  String message = "Key not properly formatted.";
  System.out.println(message);
  throw new RuntimeException(message);
      } catch (NoSuchAlgorithmException e) {
  System.out.println("Fatal Exception - Algorithm not found.");
  System.exit(0);
      } catch (SignatureException e) {
  String message = "Signature improperly formatted.";
  System.out.println(message);
  throw new RuntimeException(message);
      }

      System.out.println("rawID.length: " + rawID.length);
      byte[] cblock = new byte[32 + 256];
      for(int i = 0 ; i < cblock.length ; i++) {
        if (i < rawID.length) {
          cblock[i] = rawID[i];
        } else {
          cblock[i] = courierBlock[i-rawID.length];
        }
      }
  //////////////////////////////////////////////////////////////////////////////
    

    //AES ENCRYPTION
    //Encrypt our signature + id with an AES key.  
    //////////////////////////////////////////////////////////////////////////////
        SecretKeySpec aesKey = null;
        try {
        //TODO: LOGGING - 
          aesKey = SecurityUtils.generateAESKey();
        } catch (NoSuchAlgorithmException e) {
    System.out.println("Fatal Exception - Algorithm not found.");
    System.exit(0);
        }
        
        byte[] firstCourierBlock = null;
        try {
        //TODO: LOGGING - 
          firstCourierBlock = SecurityUtils.encryptAES(aesKey, cblock);
        } catch (InvalidKeyException e) {
    String message = "Key not properly formatted.";
    System.out.println(message);
    throw new RuntimeException(message);
        } catch (IllegalBlockSizeException e) {
    String message = "IllegalBlockSizeException ";
    System.out.println(message);
    throw new RuntimeException(message);
        } catch (BadPaddingException e) {
    String message = "BadPaddingException";
    System.out.println(message);
    throw new RuntimeException(message);
        } catch (NoSuchAlgorithmException e) {
    System.out.println("Fatal Exception - Algorithm not found.");
    System.exit(0);
        } catch (NoSuchPaddingException e) {
    String message = "NoSuchPaddingException";
    System.out.println(message);
    throw new RuntimeException(message);
        }
    //////////////////////////////////////////////////////////////////////////////
        
    //Update courier block
    //////////////////////////////////////////////////////////////////////////////
        String encodedFirstCourierBlock = Base64.encodeBase64String(firstCourierBlock);
        t.setCourierBlock(encodedFirstCourierBlock);
    //////////////////////////////////////////////////////////////////////////////
        
        
    //RSA ENCRYPTION
    //Wrap our AES key with the Recipient's public key and add it to courier keys
    //////////////////////////////////////////////////////////////////////////////
        byte[] rsaEncrypted = null;
        try {
        //TODO: LOGGING
          rsaEncrypted = SecurityUtils.wrapAESkeyWithRSAPublic(publicKey, aesKey);
        } catch (InvalidKeyException e) {
    String message = "NoSuchPaddingException";
    System.out.println(message);
    throw new RuntimeException(message);
        } catch (IllegalBlockSizeException e) {
    String message = "NoSuchPaddingException";
    System.out.println(message);
    throw new RuntimeException(message);
        } catch (NoSuchAlgorithmException e) {
    System.out.println("Fatal Exception - Algorithm not found.");
    System.exit(0);
        } catch (NoSuchPaddingException e) {
    String message = "NoSuchPaddingException";
    System.out.println(message);
    throw new RuntimeException(message);
        }
        String encodedFirstCourierKey = Base64.encodeBase64String(rsaEncrypted);
        t.addCourierKey(encodedFirstCourierKey);
    //////////////////////////////////////////////////////////////////////////////
        
    //creator signature
    //////////////////////////////////////////////////////////////////////////////
        byte[] preSig = calculateCreatorPreSig(t);
        byte[] creatorSignature = null;
        try {
          //TODO: LOGGING
          creatorSignature = SecurityUtils.sign(privateKey,preSig);
        } catch (InvalidKeyException e) {
    String message = "Key not properly formatted.";
    System.out.println(message);
    throw new RuntimeException(message);
        } catch (NoSuchAlgorithmException e) {
    System.out.println("Fatal Exception - Algorithm not found.");
    System.exit(0);
        } catch (SignatureException e) {
    String message = "Signature improperly formatted.";
    System.out.println(message);
    throw new RuntimeException(message);
        }
        t.setCreatorSignature(Base64.encodeBase64String(creatorSignature));
    //////////////////////////////////////////////////////////////////////////////
    return t;
  }
}