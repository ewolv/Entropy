package entropy.core.impl;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import entropy.core.exception.TropException;
import entropy.core.interfaces.Rater;
import entropy.core.interfaces.Trop;
import entropy.core.interfaces.TropStore;
import entropy.core.utils.SecurityUtils;

public class BaseTropStore implements TropStore {
  private String address;
  private Connection con;
  
  public BaseTropStore(String address, Connection con) {
    if(address == null || address.equals("") || con == null ){
 //TODO:LOGGING 
System.out.println("TropStore not configured");
System.exit(0);
    }
    this.address = address;
    this.con = con;
  }
  
  @Override
  public void add(Collection<Trop> trops) throws TropException {
    
    //prepare data for validator
    Map<String,Trop> localCopiesMap = new HashMap<String,Trop>(),
                     tropMap = new HashMap<String,Trop>();
    Set<String> ids = new HashSet<String>();
    for(Trop t : trops) {
      ids.add(t.getId());
      tropMap.put(t.getId(), t);
    }
    //get local copies of incoming trops
    Collection<Trop> localCopies = getTropById(ids.toArray(new String[] {}));
    System.out.println("fetching local copies...");
    for(Trop t : localCopies) {
      System.out.println(t);
      localCopiesMap.put(t.getId(),t);
      ids.addAll(t.getChildIds());
    }
    localCopies = getTropById(ids.toArray(new String[]{}));
    
    
    //rating callback
    Rater rater = new Rater() {
      @Override
      public void delivered(Trop t, Trop ... children) {
      }
      @Override
      public void notDelivered(Trop t, Trop ... children) {
      }
      @Override
      public void blacklist(Trop t1, Trop t2) {
        BaseTropStore.this.blacklist(t1, t2);
      }
    };
    
    Set<String> blacklistRecipients = new HashSet<String>();
    for(Trop t : getBlacklist()) {
      blacklistRecipients.add(t.getRecipient());
    }
    
    Validator v = new Validator(address, rater, tropMap, localCopiesMap, blacklistRecipients);
    System.out.println("Validating...");
    Set<Trop> results = v.validate();
    
    Set<String> resultIds = new TreeSet<String>();
    for(Trop t : results) {
      resultIds.add(t.getId());
    }
    System.out.println("Done validating. Insert.");
    System.out.println(results);
    insertOrUpdate(results.toArray(new Trop[]{}));
  }
  
  private void blacklist(Trop t1, Trop t2) {
    //insert the blacklist nodes into the database
    delete(t1.getId());
    t1.setBlackList(true);
    t2.setBlackList(true);
    insertOrUpdate(t1,t2);
    
    deleteRecipient(t1.getRecipient());
  }
  
  //if true remove branch (it's child includes this recipient
  //we'll need to construct our entire tree, we can do this the same way we get our children
  private boolean deleteRecipient(String recipientId) {
    
    return false;
    
  }

  private void delete(String... ids) {
    if(ids == null || ids.length == 0) return;
    //REMOVE FROM TROP TABLE
    StringBuilder sb = new StringBuilder("DELETE FROM TROP WHERE \"ID\" IN (");
    for(String id : ids) {
      sb.append("'" + id + "',");
    }
    sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
    sb.append(")");
    try {
      Statement st = con.createStatement();
      st.executeUpdate(sb.toString());
      st.close();
    } catch (SQLException e) {
//TODO: LOGGING
      e.printStackTrace();
System.out.println("FATAL - Error deleting trops from store.");
System.exit(0);
    }
    
    //REMOVE FROM COURIER KEY
    ////////////////////////////////////////////////////////////////////////////
    sb = new StringBuilder("DELETE FROM COURIER_KEYS WHERE \"ID\" IN (");
    for(String id : ids) {
      sb.append("'" + id + "',");
    }
    sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
    sb.append(")");
    try {
      Statement st = con.createStatement();
      st.executeUpdate(sb.toString());
      st.close();
    } catch (SQLException e) {
//TODO: LOGGING
      e.printStackTrace();
System.out.println("FATAL - Error deleting trops from store.");
System.exit(0);
    }
    ////////////////////////////////////////////////////////////////////////////
  }
  
  private void insertOrUpdate(Trop ... trops) {
    
    String insertTrop = "INSERT INTO TROP (\"PARENT_ID\",\"ID\",\"STATUS\",\"VALIDATED\"," +
    		                                  "\"VALUE\",\"R_VALUE\",\"APPRAISED_VALUE\"," +
    		                                  "\"FILE_DIGEST\",\"FILE_SIZE\",\"RECIPIENT\"," +
    		                                  "\"CREATOR\",\"CREATOR_SIG\",\"COURIER_BLOCK\",\"BLACKLIST\") " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    String insertCourierKey = "INSERT INTO COURIER_KEYS (\"ID\",\"COURIER_KEY\") VALUES (?,?)";
    //String updateTrop = "UPDATE TROP SET \"STATUS\" = ?, \"VALIDATED\" = ?, \"APPRAISED_VALUE\" = ?, \"COURIER_BLOCK\" = ?, \"BLACK_LIST\" = ? WHERE \"ID\" = ?";
    Set<String> ids = new HashSet<String>();
    for(Trop t : trops) {
      ids.add(t.getId());
    }
    //delete old trops
    Collection<Trop> exists = getTropById(ids.toArray(new String[]{}));
    ids.clear();
    for(Trop t : exists) {
      ids.add(t.getId());
    }
    delete(ids.toArray(new String[]{}));
    
    try {
      PreparedStatement psInsert = con.prepareStatement(insertTrop);
      for(Trop t : trops) {
        psInsert.setString(1,t.getParentId());
        psInsert.setString(2, t.getId());
        psInsert.setString(3,t.getStatus().toString());
        psInsert.setLong(4, t.getValidatedTime());
        psInsert.setInt(5, t.getValue());
        psInsert.setInt(6, t.getRecipientValue());
        psInsert.setDouble(7, t.getAppraisedValue());
        psInsert.setString(8, t.getFileDigest());
        psInsert.setLong(9, t.getFileSizeBytes());
        psInsert.setString(10,t.getRecipient());
        psInsert.setString(11,t.getCreator());
        psInsert.setString(12,t.getCreatorSignature());
        psInsert.setString(13,t.getCourierBlock());
        psInsert.setString(14, (t.isBlackList()?"T":"F"));
        psInsert.execute();
      }
      psInsert.close();
      psInsert = con.prepareStatement(insertCourierKey);
      for(Trop t : trops) {
        for(String courierKey : t.getCourierKeys()) {
          psInsert.setString(1, t.getId());
          psInsert.setString(2, courierKey);
          psInsert.execute();
        }
      }
      psInsert.close();
    } catch(SQLException se) {
//TODO: LOGGING
      se.printStackTrace();
System.out.println("FATAL : could not insert validated trops");
System.exit(0);
    }
  }
  
  @Override
  public Collection<Trop> getPayments() {
    Set<Trop> results = new TreeSet<Trop>();
//    try {
//      Statement st = con.createStatement();
//      ResultSet rs = st.executeQuery("SELECT * FROM TROP WHERE \"PARENT_ID\" = \"ID\"");
//      while(rs.next()) {
//        results.add(rowToTrop(rs));
//      }
//      rs.close();
//    } catch(SQLException se) {
////TODO: LOGGING
//System.out.println("FATAL : Could not get orphan node.");
//se.printStackTrace();
//System.exit(0);
//    }
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT * FROM TROP WHERE \"STATUS\" = 'PAY'");
      while(rs.next()) {
        results.add(rowToTrop(rs));
      }
    } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : could not retrieve payments.");
System.exit(0);
    }
    if (results.isEmpty()) return results;
    
    //RECOVER COURIER KEYS
    ////////////////////////////////////////////////////////////////////////////
    StringBuilder sb = new StringBuilder("SELECT * FROM COURIER_KEYS WHERE \"ID\" IN (");
    for(Trop t : results) {
      sb.append("'" + t.getId() + "',");
    }
    sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
    sb.append(")");
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery(sb.toString());
      st.close();
      while(rs.next()) {
        
      }
    } catch (SQLException e) {
//TODO: LOGGING
      e.printStackTrace();
System.out.println("FATAL - Error deleting trops from store.");
System.exit(0);
    }
    ////////////////////////////////////////////////////////////////////////////
    
    return results;
  }

  /**
   * Gets the heritage of complete payments
   */
  @Override
  public Collection<Trop> getDeliveries() {
    
    Map<String,Set<String>> pIdMap = new HashMap<String,Set<String>>();
    Map<String,String> idPidMap = new HashMap<String,String>();
    
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT \"ID\", \"PARENT_ID\" FROM TROP");
      while(rs.next()) {
        idPidMap.put(rs.getString("ID"),rs.getString("PARENT_ID"));
      }
    } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not retrieve trop ids from db.");
System.exit(0);
    }
    String pId = null;
    for(String id : idPidMap.keySet()) {
      pId = idPidMap.get(id);
      if (pIdMap.get(pId) == null) {
        pIdMap.put(pId,new HashSet<String>());
      }
      pIdMap.get(pId).add(id);
    }
    do {
       pId = idPidMap.get(pId);
    } while(!pId.equals(idPidMap.get(pId)));
    
    Set<String> temp = new HashSet<String>(),
                results = new HashSet<String>(),
                feePayIds = new HashSet<String>(),
                withFiles = new HashSet<String>();
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT \"ID\" FROM TROP WHERE STATUS IN ('PAY','FEE')");
      while(rs.next()) {
        feePayIds.add(rs.getString("ID"));
      }
      rs.close();
    } catch(SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not retrieve fees or payments from db.");
System.exit(0);
    }
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT \"ID\" FROM FILE_MAP");
      while(rs.next()) {
        withFiles.add(rs.getString("ID"));
      }
      rs.close();
    } catch(SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not retrieve trop ids with files from db.");
System.exit(0);
    }
    checkFiles(pId, pIdMap, feePayIds, withFiles, temp, results);
    
    return getTropById(results.toArray(new String[]{}));
  }
  
  //two different implementations of the recursive check - test both
  private boolean checkFiles(String id, Map<String,Set<String>> pIdMap, Set<String> feePayIds, Set<String> withFiles, Set<String> temp, Set<String> results) {
    boolean anyTrue = false,
            anyFalse = false;
    for(String cId : pIdMap.get(id)) {
      if(checkFiles(cId, pIdMap, feePayIds, withFiles, temp, results)) {
        anyTrue = true;
      } else {
        anyFalse = true;
      }
    }
    if(feePayIds.contains(id) && !anyFalse) { //fee payment cannot miss any
      temp.add(id);
      return true;
    } else if (!feePayIds.contains(id) && anyTrue) { //if receipt and any true add all in temp
      results.add(id);
      results.addAll(temp);
      temp.clear();
      return true;
    }
    if(!temp.isEmpty()) temp.clear();
    return false;
  }
  private void checkR(String id, Map<String,Set<String>> pIdMap, Set<String> feePayIds, Set<String> withFiles, Set<String> temp, Set<String> results) {
    if(!feePayIds.contains(id) && feePayIds.contains(pIdMap.get(id).toArray()[0])) { //has payment
      for(String cId : pIdMap.get(id)) {
        checkP(cId, pIdMap, feePayIds, withFiles, temp, results);
      }
    }
    if(!feePayIds.contains(id) && feePayIds.contains(pIdMap.get(id).toArray()[0])) {
      for(String cId : pIdMap.get(id)) {
        checkR(cId, pIdMap, feePayIds, withFiles, temp, results);
      }
    }
  }
  private boolean checkP(String id, Map<String,Set<String>> pIdMap, Set<String> feePayIds, Set<String> withFiles, Set<String> temp, Set<String> results) {
    boolean include = true;
    for(String cId : pIdMap.get(id)) {
      include = include && checkF(cId, pIdMap, feePayIds, withFiles, temp, results);
    }
    if(include) {
      results.addAll(temp);
    }
    temp = new HashSet<String>();
    return include;
  }
  private boolean checkF(String id, Map<String,Set<String>> pIdMap, Set<String> feePayIds, Set<String> withFiles, Set<String> temp, Set<String> results) {
    if(withFiles.contains(id)) {
      boolean childrenAreTrue = true;
      for(String cId : pIdMap.get(id)) {
        childrenAreTrue = childrenAreTrue && checkF(cId, pIdMap, feePayIds, withFiles, temp, results);
      }
      if(childrenAreTrue) {
        temp.add(id);
        return true;
      } else {
        return false;
      }
    }
    return false;
  }
  @Override
  public Collection<Trop> getBlacklist() {
    List<Trop> results = new ArrayList<Trop>();
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT * FROM TROP WHERE \"BLACKLIST\" = 'T'");
      while(rs.next()) {
        results.add(rowToTrop(rs));
      }
      rs.close();
    } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not get blacklist from db.");
System.exit(0);
    }
    return results;
  }

  @Override
  public Collection<Trop> getTropById(String ... ids) {
    List<Trop> results = new ArrayList<Trop>();
    if(ids == null || ids.length == 0) return results;
    
    StringBuilder sb = new StringBuilder("SELECT * FROM TROP WHERE \"ID\" IN (");
    for(String id : ids) {
      sb.append("'" + id + "',");
    }
    sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
    sb.append(")");
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery(sb.toString());
      while(rs.next()) {
        results.add(rowToTrop(rs));
      }
      rs.close();
    } catch (SQLException se) {
//TODO : LOGGING
System.out.println("FATAL : Could not retrieve trops by id from db.");
se.printStackTrace();
System.exit(0);
    }
    return results;
  }

  @Override
  public void updateFiles(String... pathsToBeAdded) {
    
    //Get existing paths
    Set<String> pathsToRemove = new HashSet<String>();
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT * FROM FILE_MAP");
      while(rs.next()) {
        if (new File(rs.getString("PATH")).length() != rs.getLong("SIZE")) {
          pathsToRemove.add(rs.getString("PATH"));
        }
      }
      rs.close();
    } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not retrieve file mappings from db.");
System.exit(0);
    }
    
    //remove from db if not empty
    if(!pathsToRemove.isEmpty()) {
      StringBuilder sb = new StringBuilder("DELETE FROM FILE_MAP WHERE \"PATH\" IN (");
      for(String path : pathsToRemove) {
        sb.append("'" + path + "',");
      }
      sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
      sb.append(")");
      try {
        Statement st = con.createStatement();
        st.executeUpdate(sb.toString());
        st.close();
      } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not remove file mappings from db.");
System.exit(0);
      }
    }
    Map<String,NewFileMapping> newMappings = new HashMap<String,NewFileMapping>();
    for (String path : pathsToBeAdded) {
      File f = new File(path);
      long s = f.length();
      if(s!=0) {
        String d = SecurityUtils.checksum(f);
        newMappings.put(d,new BaseTropStore.NewFileMapping(path, s, d));
      }
    }
    if (!newMappings.isEmpty()) {
      StringBuilder sb = new StringBuilder("SELECT \"ID\",\"DIGEST\" FROM TROP WHERE \"DIGEST\" IN (");
      for(NewFileMapping mapping : newMappings.values()) {
          sb.append("'" + mapping.digest + "',");
      }
      sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
      sb.append(")");
      try {
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sb.toString());
        while(rs.next()) {
          NewFileMapping nfm = newMappings.get(rs.getString("DIGEST"));
          nfm.id = rs.getString("ID");
        }
        rs.close();
      } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Could not retrieve trop info for file mappings from db.");
System.exit(0);
      }
    }
    try {
      PreparedStatement ps = con.prepareStatement("INSERT INTO FILE_MAP (\"ID\",\"DIGEST\",\"PATH\",\"SIZE\")" +
          " VALUES (?,?,?,?)");
      for (NewFileMapping nfm : newMappings.values()) {
        ps.setString(1,nfm.id);
        ps.setString(2,nfm.digest);
        ps.setString(3, nfm.path);
        ps.setLong(4,nfm.size);
        ps.executeUpdate();
      }
      ps.close();
    } catch (SQLException se) {
//TODO: LOGGING
System.out.println("FATAL : Unable to insert file mappings in db.");
System.exit(0);
    }
  }

  private static class NewFileMapping {
    String path;
    long size;
    String digest;
    String id;
    public NewFileMapping(String path, long size, String digest) {
      super();
      this.path = path;
      this.size = size;
      this.digest = digest;
    }
    
  }
  
  @Override
  public Collection<String> getFilePathsByDigest(Collection<String> digests) {
    Set<String> results = new HashSet<String>();

    StringBuilder sb = new StringBuilder("SELECT \"PATH\" FROM FILE_MAP WHERE \"DIGEST\" IN (");
    for(String digest : digests) {
      sb.append("'" + digest + "',");
    }
    sb = new StringBuilder(sb.substring(0,sb.length()-1));//remove extra ','
    sb.append(")");
    
    
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery(sb.toString());
      while(rs.next()) {
        results.add(rs.getString("PATH"));
      }
      rs.close();
    } catch (SQLException se){
//TODO: LOGGING
System.out.println("FATAL : Could not retrieve file paths by digest from db.");
System.exit(0);
    }
    return results;
  }
  
  private Trop rowToTrop(ResultSet rs) throws SQLException {
    TropBuilder tb = TropBuilder.getBuilder();
    
    //set id information
    tb.setParentId(rs.getString("PARENT_ID")).setId(rs.getString("ID")).setStatus(Status.enumFromString(rs.getString("STATUS")));
    
    //set value information
    tb.setValue((byte)rs.getInt("VALUE")).setRecipientValue((byte)rs.getInt("R_VALUE"));
    tb.setAppraisedValue(rs.getDouble("APPRAISED_VALUE"));
    
    //set message information
    tb.setFileDigest(rs.getString("FILE_DIGEST")).setFileSizeBytes(rs.getLong("FILE_SIZE"));
    
    //set creator and recipient information
    tb.setRecipient(rs.getString("RECIPIENT")).setCreator(rs.getString("CREATOR"));
    tb.setCreatorSignature(rs.getString("CREATOR_SIG")).setCourierBlock(rs.getString("COURIER_BLOCK"));
    
    tb.setBlacklist("T".equals(rs.getString("BLACKLIST")));
    return tb.build();
    
  }

  @Override
  public Collection<String> getTropIdsWithFiles() {
    List<String> results = new ArrayList<String>();
    try {
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery("SELECT \"ID\" FROM FILE_MAP");
      while(rs.next()) {
        results.add(rs.getString("ID"));
      }
      rs.close();
    } catch(SQLException se) {
//TODO : LOGGING
System.out.println("FATAL : could not get trop ids from file mapping in db");
se.printStackTrace();
System.exit(0);
    }
    return results;
  }
}
