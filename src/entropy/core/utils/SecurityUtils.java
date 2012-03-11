package entropy.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class SecurityUtils {
//perform sha-256 on file
  public static String checksum(File file) {
    try {
      InputStream fin = new FileInputStream(file);
      java.security.MessageDigest digester =
          MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[1024];
      int read;
      do {
        read = fin.read(buffer);
        if (read > 0)
          digester.update(buffer, 0, read);
      } while (read != -1);
      fin.close();
      return Base64.encodeBase64String(digester.digest());
    } catch (Exception e) {
      return null;
    }
  }
  public static PublicKey recoverPublicKey(byte[] raw) throws InvalidKeySpecException, NoSuchAlgorithmException {
   return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(raw));
 }
  public static byte[] encryptAES(SecretKeySpec key, byte[] data) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
   Cipher cipher = Cipher.getInstance("AES");
   cipher.init(Cipher.ENCRYPT_MODE, key);
   return cipher.doFinal(data);
 }
 public static byte[] decryptAES(SecretKeySpec key, byte[] data) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
   Cipher cipher = Cipher.getInstance("AES");
   cipher.init(Cipher.DECRYPT_MODE, key);
   return cipher.doFinal(data);
 }
 public static SecretKeySpec generateAESKey() throws NoSuchAlgorithmException {
   KeyGenerator kgen = KeyGenerator.getInstance("AES");
   kgen.init(128);
   SecretKey skey = kgen.generateKey();
   byte[] rawAesKey = skey.getEncoded();
   return new SecretKeySpec(rawAesKey, "AES");
 }
 public static byte[] wrapAESkeyWithRSAPublic(PublicKey publicKey, Key wrapKey) throws InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException {
   Cipher cipher = Cipher.getInstance("RSA");
   cipher.init(Cipher.WRAP_MODE, publicKey);
   return cipher.wrap(wrapKey);
 }
 
 public static SecretKeySpec unwrapAESkeyWithRSAPrivate(PrivateKey privateKey, byte[] wrappedKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
   Cipher cipher = Cipher.getInstance("RSA");
   cipher.init(Cipher.UNWRAP_MODE, privateKey);
   Key recoveredKey = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
   return new SecretKeySpec(recoveredKey.getEncoded(), "AES");
 }
 public static byte[] sign(PrivateKey key, byte[] data) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
   Signature sig = Signature.getInstance("SHA1WithRSA");
   sig.initSign(key);
   sig.update(data); 
   return sig.sign(); 
 }
 public static boolean validateSignature(PublicKey key, byte[] preSig, byte[] postSig) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
   Signature sig = Signature.getInstance("SHA1WithRSA");
   sig.initVerify(key);
   sig.update(preSig);
   return sig.verify(postSig);
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
  
}
