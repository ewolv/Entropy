package entropy.core.utils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;

import entropy.core.interfaces.Trop;

public class BaseFileUtils {

  public static void tropToXml(Collection<Trop> trops, String outputFile) {
  }

  public static Collection<Trop> xmlToTrop(String inputFile) {
    return null;
  }

  public static void fileIdToXml(String outputFile, Collection<String> fileIds) {
  }

  public static Collection<String> xmlToFileId(String inputFile) {
    return null;
  }

  public static void packDeliveries(String outputFile, Collection<String> filePaths) {
  }

  public static void unpackDeliveries(String inputFile, String outputFolder) {
  }

  public static void encryptFile(PublicKey publicKey, String inputFile) {
  }

  public static void decryptFile(PrivateKey privateKey, String inputFile,
      String outputFile) {
  }

}
