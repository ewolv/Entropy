package entropy.console;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.Shell;
import asg.cliche.ShellDependent;
import asg.cliche.ShellFactory;
import asg.cliche.ShellManageable;

import java.beans.FeatureDescriptor;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.swing.border.EmptyBorder;

import org.apache.commons.codec.binary.Base64;

import entropy.core.exception.TropException;
import entropy.core.impl.BaseTropStore;
import entropy.core.impl.Status;
import entropy.core.impl.TropBuilder;
import entropy.core.interfaces.Trop;
import entropy.core.interfaces.TropStore;
import entropy.core.utils.FileUtils;
import entropy.core.utils.SecurityUtils;
import entropy.core.utils.TropUtils;

public class Entropy {

  Connection con;
  PrivateKey privateKey;
  PublicKey publicKey;
  String address;
  File homeDirectory;
  TropStore tropStore;
  
  @Command(name="Display Trops", abbrev="d", description="Display trops in store.")
  public Collection<Trop> displayTrops() {
      return tropStore.getPayments();
  }

  @Command(name="View Trop", abbrev="v", description="Display single trop's information.")
  public String viewTrops(
      @Param(name="Id", description="First several characters of the ID.")
      String id) {
      return id;
  }

  @Command(name="Create Trop", abbrev="c", description="Create a new Fee to be added to a payment in the store.")
  public void createTrop() throws TropException, IOException {
    ShellFactory.createConsoleShell("create fee","", new TropMenuBuilder(tropStore, publicKey, privateKey)).commandLoop();
  }
  
  @Command(name="Update Trops", abbrev="t", description="Add trop.xml to store, write complete payments to trop.xml.")
  public void updateTrops() throws TropException {
    //read and write to Entropy/trops.xml
    
    Set<Trop> trops = FileUtils.xmlToTrop(homeDirectory+"/trop.xml");
    //add to store
    tropStore.add(trops);
    
    //pull complete payments and heritage
    
    //write to xml
  }
  
  @Command(name="Write Request", abbrev="w", description="Select file digests from a list of incomplete payments.")
  public void writeRequest() {
    //write request to Entropy/Digests.zip
  }
  
  @Command(name="Process Request", abbrev="r", description="Zip files listed by digest and write to disk.")
  public void processRequest() {
    //write the files to Entropy/Files.zip
  }
  
  @Command(name="Update Files", abbrev="f", description="Cross-reference files on disk with db.")
  public void updateFiles() {
    //write the files to 
  }
  
  @Command(name="Alias", abbrev="a", description="Update or add an text alias.")
  public void alias() {
    
  }
  
  @Command(name="Write Orphan", abbrev="wo")
  public void writeOrphan() {
    Trop t = TropUtils.constructOrphan(publicKey, privateKey);
    FileUtils.tropToXml(Arrays.asList(t), "trop.xml");
    System.out.println("Wrote orphan to trop.xml");
  }
  
  public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, SQLException {
    Entropy entropy = new Entropy();
    entropy.loadDependencies();
    ShellFactory.createConsoleShell("entropy", "", entropy).commandLoop();
  }


  private void loadDependencies() throws ClassNotFoundException, FileNotFoundException, IOException, SQLException, NoSuchAlgorithmException {
    homeDirectory = new File(System.getProperty("user.home")+"/Entropy");
    if( ! (homeDirectory.exists() && homeDirectory.isDirectory())) {
      homeDirectory.mkdir();
    }
    Class.forName("org.sqlite.JDBC");
    con = DriverManager.getConnection("jdbc:sqlite:"+homeDirectory.getAbsolutePath()+"/tropStore.sqlite");

    DatabaseMetaData meta = con.getMetaData();
    
    ResultSet res = meta.getTables(null, null, null, new String[] {"TABLE"});
    if(!res.next()) {
          String
              tropTable =
                  "CREATE TABLE TROP" + 
                  "(\"PARENT_ID\" VARCHAR(44), \"ID\" VARCHAR(44), \"STATUS\" VARCHAR(3), \"VALIDATED\" BIGINT," +
                  "\"VALUE\" INTEGER, \"R_VALUE\" INTEGER, \"APPRAISED_VALUE\" DOUBLE PRECISION," +
                  "\"FILE_DIGEST\" VARCHAR(44), \"FILE_SIZE\" BIGINT," + 
                  "\"RECIPIENT\" TEXT, \"CREATOR\" TEXT, \"CREATOR_SIG\" TEXT, \"COURIER_BLOCK\" TEXT," +
                  "\"BLACKLIST\" VARCHAR(1))",
              courierKeysTable = 
                  "CREATE TABLE COURIER_KEYS (\"ID\" VARCHAR(44), \"COURIER_KEY\" TEXT)",
              fileMapTable =
                  "CREATE TABLE FILE_MAP " +
                  "(\"ID\" VARCHAR(44), "  +
                  "\"PATH\" TEXT, " +
                  "\"DIGEST\" BIGINT, " +
                  "\"SIZE\" BIGINT)";
          try {
            Statement sql = con.createStatement();
            sql.executeUpdate(tropTable);
            sql.executeUpdate(courierKeysTable);
            sql.executeUpdate(fileMapTable);
            sql.close();
          } catch (SQLException e) {
            System.out.println("DATABASE OUT OF SYNC - DELETE TROPSTORE.SQLITE");
            System.exit(0);
          }
    }
    
    //recover private key
    File privKeyFile = new File(homeDirectory.getAbsolutePath()+"/PRIVATE_KEY.dat");
    byte[] encodedPrivKey = new byte[(int)privKeyFile.length()];
    File pubKeyFile = new File(homeDirectory.getAbsolutePath()+"/PUBLIC_KEY.dat");
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
    System.out.println("Public key: " + address);
    tropStore = new BaseTropStore(address, con);
  }
}
