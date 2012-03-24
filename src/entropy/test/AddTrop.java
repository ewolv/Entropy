package entropy.test;

import org.junit.Test;

import junit.framework.TestCase;

public class AddTrop extends TestCase {

  /**
   * Insert a random heritage into an empty store.
   */
  @Test
  public void insertValidEmptyStore() {
    
  }
  
  /**
   * Insert a random heritage into a store containing a few of the
   * incoming trops.
   */
  @Test
  public void insertValidRandomStore() {
    
  }
  
  /**
   * Insert heritage, have distinct receipt with same id in store (blacklist.)
   */
  @Test
  public void insertBlacklist() {
    
  }
  
  /**
   * Insert invalid heritage with a missing fee.
   * Should throw an exception notifying the user of the trop missing the data.
   */
  @Test
  public void insertInvalid() {
    
  }
  
  /**
   * Insert a heritage into the store. The store should include a delivery
   * which is in the incoming heritage signed by the recipient with a child created
   * by this node.
   * this will call the rater and overwrite the delivery with the receipt and children
   */
  @Test
  public void insertDeliveredNotEmpty() {
    
  }
  
  /**
   * Same as above, with an empty store
   */
  @Test
  public void insertDeliveredEmpty() {
    
  }
  
  /**
   * Insert heeritage into the store, the store will include a delivery which
   * is in the incoming heritage signed by the recipient without any child 
   * created by this node..
   * Call the rater and overwrite the delivery with the incoming receipt and children
   */
  @Test
  public void insertNotDelivered() {
    
  }
  
}
