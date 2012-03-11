package entropy.client;

import static net.mindview.util.SwingConsole.run;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.codec.binary.Base64;

import sun.org.mozilla.javascript.SecurityUtilities;

import entropy.core.exception.TropException;
import entropy.core.impl.BaseTropStore;
import entropy.core.interfaces.Trop;
import entropy.core.interfaces.TropStore;
import entropy.core.utils.FileUtils;
import entropy.core.utils.SecurityUtils;
import entropy.core.utils.TropUtils;


public class Entropy  extends JFrame {
  Connection con;
  PrivateKey privateKey;
  PublicKey publicKey;
  String address;
  TropStore tropStore;
  
  public Entropy() {
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    try {
      loadDependencies();
    } catch (Exception e) {
//TODO: LOGGING
System.out.println("FATAL : Could not initialize Entropy");
e.printStackTrace();
System.exit(0);
    }
    
    masterTable = new JTable();
    masterTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {
            {null,null,null,null, null, null, null, null, null}
        },
        new String [] {
            "Status", "Parent ID", "ID","Creator", "Recipient", "Value", "File", "Select"
        }
        ) {
        Class[] types = new Class [] {
            java.lang.String.class,java.lang.String.class,java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false,false, false, false, false, true
        };
        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    sign.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String tropId = getSelected().toArray(new String[]{})[0];
        
      }
    });
    createFee.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String tropId = getSelected().toArray(new String[]{})[0];
        Trop parent = tropStore.getTropById(tropId).toArray(new Trop[]{})[0];
        Collection<Trop> children = tropStore.getTropById(parent.getChildIds().toArray(new String[]{}));
        
        byte totalFees = 0;
        for(Trop c : children) {
          totalFees = (byte) ((byte)totalFees & (byte)c.getValue());
        }
        byte shiftValue = parent.getValue();
        while(shiftValue>0) { 
          shiftValue = (byte) (shiftValue << 1);
        }
        CreateFeeWindow cfw = new CreateFeeWindow(parent, children, Entropy.this,(byte)((int)totalFees & 0xff - (int)shiftValue & 0xff));
        run(cfw, 300, 200);
        
      }
    });
    
    dropTrop.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
      }
    });
    loadTrop.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<Trop> trops = FileUtils.xmlToTrop(System.getProperty("user.dir")+"/trade/trop.xml");
        try {
          tropStore.add(trops);
        } catch (TropException e1) {
          e1.printStackTrace();
        }
        updateMasterTable();
      }
    });
    dropReq.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
      }
    });
    procReq.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
      }
    });
    loadFile.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
      }
    });

    addressField = new JTextField(address);
    addressField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addressField.setText(address);
      }
    });
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(sign);
    buttonPanel.add(createFee);
    buttonPanel.add(dropTrop);
    buttonPanel.add(loadTrop);
    buttonPanel.add(dropReq);
    buttonPanel.add(procReq);
    buttonPanel.add(loadFile);
    add(BorderLayout.NORTH, addressField);
    add(BorderLayout.SOUTH, buttonPanel);
    add(BorderLayout.CENTER, new JScrollPane(masterTable));
    updateMasterTable();
  }
  
  public static void main(String[] args) {
    run(new Entropy(), 800, 250);
  }
  private List<String> getSelected() {
    List<String> results = new ArrayList<String>();
    //table model to update
    DefaultTableModel model = (DefaultTableModel)masterTable.getModel();
    
    for(int x = 0; x < model.getRowCount(); x++) {
      if(model.getValueAt(x,7) == null){
        continue;
      }
      if((Boolean)model.getValueAt(x, 7) == true)results.add((String)model.getValueAt(x,2));
    }
    return results;
  }
  private JTable masterTable;
  JTextField addressField;
  private JButton createFee = new JButton("Create Fee"),
                  sign = new JButton("Sign"),
                  dropTrop = new JButton("Drop Trop"),
                  loadTrop = new JButton("Load Trop"),
                  dropReq = new JButton("Drop Req"),
                  procReq = new JButton("Process Req"),
                  loadFile = new JButton("Load File");
  private void updateMasterTable() {
      //get all from storage to update the main window
      Collection<Trop> payments = tropStore.getPayments();
      //table model to update
      DefaultTableModel model = (DefaultTableModel)masterTable.getModel();
      //clear table
      model.setRowCount(0);
      //update table
      model.fireTableDataChanged();
      
      //add data to the table, from others first, to other second, rest third
      
      //pull the IDs off of all trops
      Set<String> ids = new TreeSet<String>();
      Set<Trop> toUsFromOther = new TreeSet<Trop>();
      Set<Trop> toOther = new TreeSet<Trop>();
      Set<Trop> toUsFromUs = new TreeSet<Trop>();
      
      for(Trop t : payments) {
          ids.add(t.getId());
          if(!address.equals(t.getCreator())) {
              //from other
              if(!address.equals(t.getRecipient())) {
                  //from other to us
                  toUsFromOther.add(t);
              } else {
                  //from other to other
                  toOther.add(t);
              }
          } else {
              //from us
              if(!address.equals(t.getRecipient())) {
                  //from us to other
                  toOther.add(t);
              } else {
                  //from us to us
                  toUsFromUs.add(t);
              }
          }
      }
      Collection<String> tropsWithFiles = tropStore.getTropIdsWithFiles();
      
      for(Trop t : toUsFromOther) {
        model.addRow(new Object[] {t.getStatus().toString(),t.getParentId(),t.getId(),t.getCreator(),t.getRecipient(),TropUtils.byteToBinaryString(t.getValue()),t.getFileSizeBytes() == 0 ? true : tropsWithFiles.contains(t.getId())});
      }
      for(Trop t : toOther) {
        model.addRow(new Object[] {t.getStatus().toString(),t.getParentId(),t.getId(),t.getCreator(),t.getRecipient(),TropUtils.byteToBinaryString(t.getValue()),t.getFileSizeBytes() == 0 ? true : tropsWithFiles.contains(t.getId())});
      }
      for(Trop t : toUsFromUs) {
        model.addRow(new Object[] {t.getStatus().toString(),t.getParentId(),t.getId(),t.getCreator(),t.getRecipient(),TropUtils.byteToBinaryString(t.getValue()),t.getFileSizeBytes() == 0 ? true : tropsWithFiles.contains(t.getId())});
      }
      model.fireTableDataChanged();
  }


  
  
  private void loadDependencies() throws ClassNotFoundException, FileNotFoundException, IOException, SQLException, NoSuchAlgorithmException {
    Class.forName("org.sqlite.JDBC");
    
    con = DriverManager.getConnection("jdbc:sqlite:tropStore.sqlite");

    DatabaseMetaData meta = con.getMetaData();
    
    ResultSet res = meta.getTables(null, null, null, 
       new String[] {"TABLE"});
    if(!res.next()) {
          String
              tropTable =
                  "CREATE TABLE TROP" + 
                  "(\"PARENT_ID\" VARCHAR(44), \"ID\" VARCHAR(44), \"STATUS\" VARCHAR(3), \"VALIDATED\" BIGINT," +
                  "\"VALUE\" INTEGER, \"R_VALUE\" INTEGER, \"APPRAISED_VALUE\" DOUBLE PRECISION," +
                  "\"FILE_DIGEST\" VARCHAR(44), \"FILE_SIZE\" BIGINT," + 
                  "\"RECIPIENT\" TEXT, \"CREATOR\" TEXT, \"CREATOR_SIG\" TEXT, \"COURIER_BLOCK\" TEXT," +
                  "\"BLACKLIST\" VARCHAR(1))",
              fileMapTable =
                  "CREATE TABLE FILE_MAP " +
                  "(\"ID\" VARCHAR(44), "  +
                  "\"PATH\" TEXT, " +
                  "\"DIGEST\" BIGINT, " +
                  "\"SIZE\" BIGINT)";
          try {
            Statement sql = con.createStatement();
            sql.executeUpdate(tropTable);
            sql.executeUpdate(fileMapTable);
            sql.close();
          } catch (SQLException e) {
            System.out.println("DATABASE OUT OF SYNC - DELETE TROPSTORE.SQLITE");
            System.exit(0);
          }
    }
    
    //recover private key
    File privKeyFile = new File("PRIVATE_KEY.dat");
    byte[] encodedPrivKey = new byte[(int)privKeyFile.length()];
    File pubKeyFile = new File("PUBLIC_KEY.dat");
    byte[] encodedPubKey = new byte[(int)pubKeyFile.length()];
    try {
      FileInputStream keyInputStream = new FileInputStream(privKeyFile);
      keyInputStream.read(encodedPrivKey);
      keyInputStream.close();
      KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
      privateKey = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivKey));

      keyInputStream = new FileInputStream(pubKeyFile);
      keyInputStream.read(encodedPubKey);
      keyInputStream.close();
      rSAKeyFactory = KeyFactory.getInstance("RSA");
      publicKey = rSAKeyFactory.generatePublic(new X509EncodedKeySpec(encodedPubKey));
      
    } catch (FileNotFoundException e) {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
      kpg.initialize(2048);
      KeyPair keyPair = kpg.genKeyPair();
      privateKey = keyPair.getPrivate();
      publicKey = keyPair.getPublic();
      FileOutputStream fout = new FileOutputStream("PRIVATE_KEY.dat");
      for(Byte b : privateKey.getEncoded())
        fout.write(b);
      fout.close();
      fout = new FileOutputStream("PUBLIC_KEY.dat");
      for(Byte b : publicKey.getEncoded()) 
        fout.write(b);
      fout.close();
    } catch (InvalidKeySpecException e) {
      e.printStackTrace();
    }
    
    address = Base64.encodeBase64String(publicKey.getEncoded());
    tropStore = new BaseTropStore(address, con);
  }

  public void createFee(Trop parent, Collection<Trop> children, String recipient, String selectedFile, String stringBinaryValue, String stringBinaryRValue) {
    byte rValue = 0;
    for(byte b: stringBinaryRValue.getBytes()) rValue = (byte) ((rValue << 1) | (b & 1)); 
    byte value = 0;
    for(byte b: stringBinaryValue.getBytes()) value = (byte) ((value << 1) | (b & 1));
    long fileSizeBytes = 0;
    String fileDigest = "0";
    File outFile = null;
    if(selectedFile!=null &&!selectedFile.equals("Select file to encrypt. Don't touch for no file.")) {
    //encrypt the child and calculate digest
    outFile = new File(FileUtils.encryptFile(publicKey, selectedFile));
    fileDigest = SecurityUtils.checksum(outFile);
    fileSizeBytes = outFile.length();
    }
    
    try {
      Collection<Trop> newFamily = TropUtils.createFee(parent, value, rValue, fileDigest, fileSizeBytes, recipient, publicKey, privateKey, children);
      System.out.println(newFamily);
      
      tropStore.add(newFamily);
      updateMasterTable();
    } catch (TropException e) {
//TODO: LOGGING
System.out.println("FATAL : Could not create fee");
e.printStackTrace();
System.exit(0);
    }
  }
}
