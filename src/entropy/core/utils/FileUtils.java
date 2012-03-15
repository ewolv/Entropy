package entropy.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import entropy.core.impl.Status;
import entropy.core.impl.TropBuilder;
import entropy.core.interfaces.Trop;

public class FileUtils {
  
  public static Set<Trop> xmlToTrop(String filename) {
    Set<Trop> results = new TreeSet<Trop>();
    try {
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      InputStream in = new FileInputStream(filename);
      XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
      TropBuilder tb = TropBuilder.getBuilder();
      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();

        if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                //opening element of a trop
                if ("TROP".equals(startElement.getName().getLocalPart())) {
                  //new trop to be constructed
                  //builder should be cleared by the previous end tag
                }
                if ("PARENT_ID".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setParentId(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("ID".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setId(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("STATUS".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setStatus(Status.enumFromString(eventReader.nextEvent().asCharacters().getData()));
                  continue;
                }
                
                if ("VALUE".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setValue(Byte.parseByte(eventReader.nextEvent().asCharacters().getData()));
                  continue;
                }
                if ("RVALUE".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setRecipientValue(Byte.parseByte(eventReader.nextEvent().asCharacters().getData()));
                  continue;
                }
                if ("FILE_DIGEST".equals(event.asStartElement().getName().getLocalPart())) {
//TODO: exceptions w/o syso on empty digest... wtf?
System.out.println(eventReader);
System.out.println(eventReader.nextEvent());
                  tb.setFileDigest(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("FILE_SIZE".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setFileSizeBytes(Long.parseLong(eventReader.nextEvent().asCharacters().getData()));
                  continue;
                }
                if ("RECIPIENT".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setRecipient(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("CREATOR".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setCreator(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("CREATOR_SIGNATURE".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setCreatorSignature(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("COURIER_BLOCK".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.setCourierBlock(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("COURIER_KEY".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.addCourierKey(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
                if ("CHILD_ID".equals(event.asStartElement().getName().getLocalPart())) {
                  tb.addChildId(eventReader.nextEvent().asCharacters().getData());
                  continue;
                }
        }
        if (event.isEndElement()) {
          //we have completed the iteration of a trop.  build it, and clear the builder
          if ("TROP".equals(event.asEndElement().getName().getLocalPart())) {
            results.add(tb.build());
            tb.clear();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
//TODO: LOGGING
System.out.println("FATAL : Could not read trop from xml file.");
System.exit(0);
    }
    System.out.println(results);
    return results;
  }
  
  public static void tropToXml(Collection<Trop> trops, String outputFile) {
    try {
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(new FileOutputStream(outputFile));
      XMLEventFactory eventFactory = XMLEventFactory.newInstance();
      XMLEvent end = eventFactory.createDTD("\n");
  
      StartDocument startDocument = eventFactory.createStartDocument();
      eventWriter.add(startDocument);
  
      // Create config open tag
      StartElement configStartElement = eventFactory.createStartElement("","", "ROOT");
      eventWriter.add(configStartElement);
      eventWriter.add(end);
      for(Trop t : trops) {
        writeTrop(eventWriter, t);
      }
      eventWriter.add(eventFactory.createEndElement("", "", "ROOT"));
      eventWriter.add(end);
      eventWriter.add(eventFactory.createEndDocument());
      eventWriter.close();
    } catch (Exception e) {
//TODO: LOGGING
System.out.println("FATAL : Could not write trop to xml file.");
e.printStackTrace();
System.exit(0);
    }
  }
  private static void writeTrop(XMLEventWriter eventWriter, Trop t) throws XMLStreamException { 
    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    XMLEvent end = eventFactory.createDTD("\n");
    // Create Start node
    StartElement tropStart = eventFactory.createStartElement("", "", "TROP"),
                 parentIdStart = eventFactory.createStartElement("", "", "PARENT_ID"),
                 idStart = eventFactory.createStartElement("", "", "ID"),
                 statusStart = eventFactory.createStartElement("", "", "STATUS"),
                 valueStart = eventFactory.createStartElement("", "", "VALUE"),
                 rValueStart = eventFactory.createStartElement("", "", "RVALUE"),
                 fileDigestStart = eventFactory.createStartElement("", "", "FILE_DIGEST"),
                 fileSizeStart = eventFactory.createStartElement("", "", "FILE_SIZE"),
                 recipientStart = eventFactory.createStartElement("", "", "RECIPIENT"),
                 creatorStart = eventFactory.createStartElement("", "", "CREATOR"),
                 creatorSigStart = eventFactory.createStartElement("", "", "CREATOR_SIGNATURE"),
                 courierBlockStart = eventFactory.createStartElement("", "", "COURIER_BLOCK"),
                 courierKeyStart = eventFactory.createStartElement("", "", "COURIER_KEY"),
                 childIdStart = eventFactory.createStartElement("", "", "CHILD_ID");
    EndElement eElement = eventFactory.createEndElement("", "", "TROP"),
                parentIdEnd = eventFactory.createEndElement("", "", "PARENT_ID"),
                idEnd = eventFactory.createEndElement("", "", "ID"),
                statusEnd = eventFactory.createEndElement("", "", "STATUS"),
                valueEnd = eventFactory.createEndElement("", "", "VALUE"),
                rValueEnd = eventFactory.createEndElement("", "", "RVALUE"),
                fileDigestEnd = eventFactory.createEndElement("", "", "FILE_DIGEST"),
                fileSizeEnd = eventFactory.createEndElement("", "", "FILE_SIZE"),
                recipientEnd = eventFactory.createEndElement("", "", "RECIPIENT"),
                creatorEnd = eventFactory.createEndElement("", "", "CREATOR"),
                creatorSigEnd = eventFactory.createEndElement("", "", "CREATOR_SIGNATURE"),
                courierBlockEnd = eventFactory.createEndElement("", "", "COURIER_BLOCK"),
                courierKeyEnd = eventFactory.createEndElement("", "", "COURIER_KEY"),
                childIdEnd = eventFactory.createEndElement("","","CHILD_ID");
    eventWriter.add(tropStart);
    eventWriter.add(end);
    // Create Content
      eventWriter.add(parentIdStart);
      eventWriter.add(eventFactory.createCharacters(t.getParentId()));
      eventWriter.add(parentIdEnd);
      eventWriter.add(end);
      eventWriter.add(idStart);
      eventWriter.add(eventFactory.createCharacters(t.getId()));
      eventWriter.add(idEnd);
      eventWriter.add(end);
      eventWriter.add(statusStart);
      eventWriter.add(eventFactory.createCharacters(t.getStatus().toString()));
      eventWriter.add(statusEnd);
      eventWriter.add(end);
      eventWriter.add(valueStart);
      eventWriter.add(eventFactory.createCharacters(String.valueOf(t.getValue())));
      eventWriter.add(valueEnd);
      eventWriter.add(end);
      eventWriter.add(rValueStart);
      eventWriter.add(eventFactory.createCharacters(String.valueOf(t.getRecipientValue())));
      eventWriter.add(rValueEnd);
      eventWriter.add(end);
      eventWriter.add(fileDigestStart);
      eventWriter.add(eventFactory.createCharacters(t.getFileDigest()));
      eventWriter.add(fileDigestEnd);
      eventWriter.add(end);
      eventWriter.add(fileSizeStart);
      eventWriter.add(eventFactory.createCharacters(String.valueOf(t.getFileSizeBytes())));
      eventWriter.add(fileSizeEnd);
      eventWriter.add(end);
      eventWriter.add(recipientStart);
      eventWriter.add(eventFactory.createCharacters(t.getRecipient()));
      eventWriter.add(recipientEnd);
      eventWriter.add(end);
      eventWriter.add(creatorStart);
      eventWriter.add(eventFactory.createCharacters(t.getCreator()));
      eventWriter.add(creatorEnd);
      eventWriter.add(end);
      eventWriter.add(creatorSigStart);
      eventWriter.add(eventFactory.createCharacters(t.getCreatorSignature()));
      eventWriter.add(creatorSigEnd);
      eventWriter.add(end);
      eventWriter.add(courierBlockStart);
      eventWriter.add(eventFactory.createCharacters(t.getCourierBlock()));
      eventWriter.add(courierBlockEnd);
      eventWriter.add(end);
      for(String courierKey : t.getCourierKeys()) {
        if(courierKey != null && !courierKey.isEmpty()) {
          eventWriter.add(courierKeyStart);
          eventWriter.add(eventFactory.createCharacters(courierKey));
          eventWriter.add(courierKeyEnd);
          eventWriter.add(end);
        }
      }
      for(String childId : t.getChildIds()) {
        eventWriter.add(childIdStart);
        eventWriter.add(eventFactory.createCharacters(childId));
        eventWriter.add(childIdEnd);
        eventWriter.add(end);
      }
    // Create End node
    eventWriter.add(eElement);
    eventWriter.add(end);
  }

  public static void fileIdToXml(String outputFile, Collection<String> digests) {
    try {
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(new FileOutputStream(outputFile));
      XMLEventFactory eventFactory = XMLEventFactory.newInstance();
      XMLEvent end = eventFactory.createDTD("\n");
  
      StartDocument startDocument = eventFactory.createStartDocument();
      eventWriter.add(startDocument);
      eventWriter.add(eventFactory.createStartElement("", "", "FILES"));
      for(String digest : digests) {
        eventWriter.add(eventFactory.createStartElement("", "", "SHA256"));
        eventWriter.add(eventFactory.createCharacters(digest));
        eventWriter.add(eventFactory.createEndElement("", "", "SHA256"));
      }
      eventWriter.add(eventFactory.createEndElement("","","FILES"));
      eventWriter.add(eventFactory.createEndDocument());
      eventWriter.close();
    } catch (Exception e) {
//TODO: LOGGING
System.out.println("FATAL : Could not write file ids to xml file.");
System.exit(0);
    }
  }


  public static Collection<String> xmlToFileId(String inputFile) {   
    Set<String> results = new TreeSet<String>();
  try {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    InputStream in = new FileInputStream(inputFile);
    XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
    while(eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();
      if (event.isStartDocument()) {
        StartElement startElement = event.asStartElement();
        if("SHA256".equals(startElement.getName().getLocalPart())) {
          results.add(eventReader.nextEvent().asCharacters().getData());
          continue;
        }
      }
    }
  } catch (Exception e) {
//TODO: LOGGING
System.out.println("FATAL : Could not read file ids from xml file.");
System.exit(0);
  }
  return results;
}

  public static void packDeliveries(String outputFile, Collection<String> filePaths) { // These are the files to include in the ZIP file
    // Create a buffer for reading the files
    byte[] buf = new byte[1024];
    try {
        // Create the ZIP file
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile));
        // Compress the files
        for(String files : filePaths) {
            FileInputStream in = new FileInputStream(files);
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(files));
            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            in.close();
        }
        // Complete the ZIP file
        out.close();
    } catch (IOException e) {
//TODO: LOGGING
System.out.println("FATAL : Could not zip files.");
System.exit(0);
    }
  }

  public static void unpackDeliveries(String inputFile, String outputFolder)  {
    try {
      // Open the ZIP file
      ZipInputStream in = new ZipInputStream(new FileInputStream(inputFile));
      ZipFile zf = new ZipFile(inputFile);
      // Enumerate each entry
      for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
        // Get the first entry
        ZipEntry entry = (ZipEntry)entries.nextElement();
        // Open the output file
        String outFilename = entry.getName();
        OutputStream out = new FileOutputStream(outputFolder + outFilename);
        // Transfer bytes from the ZIP file to the output file
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        // Close the streams
        out.close();
      }
      in.close();
    } catch (IOException e) {
//TODO: LOGGING
System.out.println("FATAL : Could not unzip deliveries.");
System.exit(0);
    }
 }

  public static String encryptFile(PublicKey publicKey, String inputFile, String outDir) {
    File inFile = new File(inputFile),
         tempFile = new File(outDir + "/" + inFile.getName()),
         keyFile = new File(outDir + "/" + "wrappedAesKey.dat"),
         outFile = new File(outDir + "/" + (new Date()).getTime());
    System.out.println("infile: " + inFile.getAbsolutePath());
    System.out.println("  exists "  + inFile.exists());
    System.out.println("  canWrite " + inFile.canWrite());

    tempFile.mkdirs();
    if(tempFile.exists())
      tempFile.delete();
    try {
      tempFile.createNewFile();
    } catch (IOException e1) {
  e1.printStackTrace();
//TODO : LOGGING
System.out.println("FATAL : Could not encrypt file and save to disk.");
System.exit(0);
    }
    try {
      SecretKeySpec aesKeySpec = SecurityUtils.generateAESKey();
      encrypt(inFile, tempFile, aesKeySpec);
      byte[] wrapped = SecurityUtils.wrapAESkeyWithRSAPublic(publicKey, aesKeySpec);
      FileOutputStream fout = new FileOutputStream(keyFile);
      for(Byte b : wrapped) {
        fout.write(b);
      }
      fout.close();
      List<String> filePaths = new ArrayList<String>();
      filePaths.add(keyFile.getAbsolutePath());
      filePaths.add(tempFile.getAbsolutePath());
      packDeliveries(outFile.getAbsolutePath(),filePaths);
    } catch (Exception e) {
      e.printStackTrace();
//TODO : LOGGING
System.out.println("FATAL : Could not encrypt file and save to disk.");
System.exit(0);
    }
    return outFile.getAbsolutePath();
  }

  public static void decryptFile(PrivateKey privateKey, String inputFile, String outputFolder) {    
    
    File inFile = new File(inputFile),
         tempFolder = new File(System.getProperty("user.dir") + "/temp/"),
         outFolder = new File(outputFolder);
    
    unpackDeliveries(inFile.getAbsolutePath(), tempFolder.getAbsolutePath());
    SecretKeySpec aesKeySpec = null;
    try {
      File aesKeyFile = new File(tempFolder.getAbsoluteFile()+"wrappedAesKey.dat");
      byte[] wrappedKey = new byte[(int)aesKeyFile.length()];
      FileInputStream keyInputStream = new FileInputStream(aesKeyFile);
      keyInputStream.read(wrappedKey);
      aesKeySpec = SecurityUtils.unwrapAESkeyWithRSAPrivate(privateKey, wrappedKey);
      aesKeyFile.delete();
    } catch (Exception e) {
//TODO: LOGGING
System.out.println("FATAL : Could not unwrap AES key.");
System.exit(0);
    }
    try {
      decrypt(tempFolder.listFiles()[0], outFolder, aesKeySpec);
    } catch (Exception e) {
//TODO : LOGGING
System.out.println("FATAL : Could not decrypt file.");
System.exit(0);
    }
  }

  private static void encrypt(File in, File out, SecretKeySpec aesKeySpec) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
    Cipher aesCipher = Cipher.getInstance("AES");
    aesCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec);
   
    FileInputStream is = new FileInputStream(in);
    CipherOutputStream os = new CipherOutputStream(new FileOutputStream(out), aesCipher);
   
    copy(is, os);
   
    os.close();
  }
  private static void decrypt(File in, File out, SecretKeySpec aesKeySpec) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
    Cipher aesCipher = Cipher.getInstance("AES");
    aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec);
   
    CipherInputStream is = new CipherInputStream(new FileInputStream(in), aesCipher);
    FileOutputStream os = new FileOutputStream(out);
   
    copy(is, os);
   
    is.close();
    os.close();
  }
  private static void copy(InputStream is, OutputStream os) throws IOException {
    int i;
    byte[] b = new byte[1024];
    while((i=is.read(b))!=-1) {
      os.write(b, 0, i);
    }
  }
}
