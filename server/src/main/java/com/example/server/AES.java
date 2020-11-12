package com.example.server;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class AES {
    private Cipher cipher;
    public SecretKeySpec key;

    public AES() {
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public void setKey(String keyStr) {
        try {
            byte[] salt = new byte[16];
            KeySpec spec = new PBEKeySpec(keyStr.toCharArray(), salt, 65536, 256); // AES-256
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] key = f.generateSecret(spec).getEncoded();
            this.key = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public byte[] encrypt(String message) {
        byte[] bytes = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            bytes = cipher.doFinal(message.getBytes());
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public byte[] decrypt(byte[] encryptedBytes) {
        byte[] decryptedBytes = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            decryptedBytes = cipher.doFinal(encryptedBytes);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return decryptedBytes;
    }
}