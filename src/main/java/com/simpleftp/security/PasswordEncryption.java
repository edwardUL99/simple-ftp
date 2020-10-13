/*
 *  Copyright (C) 2020  Edward Lynch-Milner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.simpleftp.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.security.spec.KeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * This class is used for encrypting and decrypting passwords
 */
public class PasswordEncryption {
    private static final String UNICODE_FORMAT = "UTF8";
    private static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private static SecretKey key;

    private static SecretKey getSecretKey() throws Exception {
        if (key == null) {
            String encryptionKey = "abcdefghijklmnoshsgdgdjsjbbdejhddkfkajcnjkdscdcbdbckjawcbwdjecbwkdjcwBAGGSJGJSGGhahaH";
            byte[] arrayBytes = encryptionKey.getBytes(UNICODE_FORMAT);
            KeySpec keySpec = new DESedeKeySpec(arrayBytes);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(DESEDE_ENCRYPTION_SCHEME);
            key = secretKeyFactory.generateSecret(keySpec);
        }

        return key;
    }

    /**
     * This method encrypts the password and returns the encrypted version.
     * Another call to encrypt resets
     * @param password the password to encrypt
     * @return encrypted password
     */
    public static String encrypt(String password) {
        String encrypted = null;
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plainText = password.getBytes(UNICODE_FORMAT);
            byte[] encryptedText = cipher.doFinal(plainText);
            encrypted = new String(Base64.encodeBase64(encryptedText));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    /**
     * This method decrypts the password and returns the decrypted version
     * @param password the password to decrypt
     * @return decrypted password
     */
    public static String decrypt(String password) {
        String decrypted = null;
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedText = Base64.decodeBase64(password);
            byte[] plainText = cipher.doFinal(encryptedText);
            decrypted = new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decrypted;
    }
}
