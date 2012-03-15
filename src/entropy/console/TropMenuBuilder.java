package entropy.console;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;

import asg.cliche.Command;
import entropy.core.exception.TropException;
import entropy.core.impl.Status;
import entropy.core.interfaces.Trop;
import entropy.core.interfaces.TropStore;
import entropy.core.utils.FileUtils;
import entropy.core.utils.SecurityUtils;
import entropy.core.utils.TropUtils;

/**
 * Object used to build a new trop in the command prompt.
 * 
 * This object is modified using commands and arguments.
 * When the object is set-up, the user can execute the review command
 * to encrypt the file and build new trops.
 * 
 * The user can then call the complete command to finalize the file (if necessary)
 * and add the new updated trops to the store.
 *
 */
public class TropMenuBuilder {
  public TropMenuBuilder(TropStore tropStore, PublicKey publicKey, PrivateKey privateKey) {
    this.tropStore = tropStore;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }
  TropStore tropStore;
  PublicKey publicKey;
  PrivateKey privateKey;
  
  //the following values need too be set to build the new trop
  Trop parent;
  String recipient;
  byte value;
  byte rValue;
  File fileToEncrypt;
  File temporaryReady;
  String fileDigest = ""; //Set as empty string intentionally - consequence null in trop builder
  long fileSizeBytes;
  Collection<Trop> siblings;
  Collection<Trop> newTropFamily;
  
  @Command(name="Parent Id", abbrev="p", description="Base64-encoded id of parent trop")
  public String parentId(String parentId) {
    Trop temp = tropStore.getTropById(parentId).toArray(new Trop[]{})[0];
    if(temp == null || Status.REC == temp.getStatus()) {
      return "invalid trop - no change.";
    }
    parent = temp;
    siblings = tropStore.getTropById(parent.getChildIds().toArray(new String[]{}));
    return "done.";
  }
  
  @Command(name="Value", abbrev="v", description="8-bit binary string")
  public String value(String svalue) {
    if(svalue.matches("(0|1)(0|1)(0|1)(0|1)(0|1)(0|1)(0|1)(0|1)")) {
      for(byte b: svalue.getBytes()) value = (byte) ((value << 1) | (b & 1));
      return "done.";
    }
    return "invalid - no change.";
  }
  @Command(name="Recipient Value", abbrev="rv", description="8-bit binary string")
  public String recipientValue(String value) {
    if(value.matches("(0|1)(0|1)(0|1)(0|1)(0|1)(0|1)(0|1)(0|1)")) {
      for(byte b: value.getBytes()) rValue = (byte) ((rValue << 1) | (b & 1));
      return "done.";
    }
    return "invalid - no change.";
  }
  @Command(name="Input File Path", abbrev="f", description="Full path - 'n' for no file")
  public String filePath(String value) {
    if ("n".equals(value)) {
      return "done.";
    }
    File temp = new File(value);
    if (temp.exists()) {
      fileToEncrypt = temp;
      return "done.";
    }
    return "invalid - no change.";
  }
  @Command(name="Recipient", abbrev="r", description="Base64-encoded public key of recipient")
  public void recipient(String recipient) {
    this.recipient = recipient;
    System.out.println("done.");
  }
  @Command(name="Review", abbrev="rev", description="Construct and display.")
  public Collection<Trop> review() {
    //encrypt file if present
    if(fileToEncrypt != null) {
      //encrypt the child and calculate digest
      temporaryReady = new File(FileUtils.encryptFile(publicKey, fileToEncrypt.getAbsolutePath(), System.getProperty("user.home")+"/Entropy/Files"));
      fileDigest = SecurityUtils.checksum(temporaryReady);
      fileSizeBytes = temporaryReady.length();
    }
    
    try {
      newTropFamily = TropUtils.createFee(parent, value, rValue, fileDigest, fileSizeBytes, recipient, publicKey, privateKey, siblings);
    } catch (TropException e) {
      e.printStackTrace();
      return null;
    }
    return newTropFamily;
  }
  @Command(name="Complete", abbrev="c", description="Add to store and insert file.")
  public String complete() throws TropException {
    //make file permanent
    if(newTropFamily!=null&&!newTropFamily.isEmpty()) {
      if(temporaryReady!=null&&temporaryReady.exists()) {
        temporaryReady.renameTo(new File(System.getProperty("user.home") + "/Entropy/Files/" + fileDigest + ".zip"));
      }
      System.out.println("Adding to tropStore...");
      tropStore.add(newTropFamily);
      temporaryReady.delete();
      return "done. 'exit' to return.";
    } else {
      return "not ready.";
    }
  }
}
