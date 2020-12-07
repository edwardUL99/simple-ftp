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

import com.simpleftp.security.exceptions.PasswordEncryptionException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import java.util.Base64;

/**
 * This class is used for encrypting and decrypting passwords
 *
 * Default one in the jar can be overridden to provide a configurable encryption string
 *
 * Attempts to find on the classpath a file with extension password.encrypt which contains the encryption key or -Dsimpleftp.passwordEncryptFile=<file>
 * If not found, it's searched for in the property identified by user.dir
 */
public class PasswordEncryption {
    private static final String UNICODE_FORMAT = "UTF8";
    private static final String ENCRYPTION_SCHEME = "AES/ECB/PKCS5Padding";
    private static SecretKeySpec key;
    private static String encryptionKey;

    private static String getEncryptionKey() throws IOException {
        if (encryptionKey == null) {
            InputStream inputStream;
            String encryptionFile;

            String property = System.getProperty("simpleftp.passwordEncryptFile");

            encryptionFile = Objects.requireNonNullElse(property, "password.encrypt");

            inputStream = ClassLoader.getSystemResourceAsStream(encryptionFile);

            if (inputStream == null) {
                File encryptFile = new File(encryptionFile);
                if (encryptFile.exists() && encryptFile.isFile()) {
                    inputStream = new FileInputStream(encryptFile);
                }
            }

            if (inputStream == null) {
                // as last resort attempt to find in user directory
                File encryptFile = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + encryptionFile);
                if (encryptFile.exists() && encryptFile.isFile()) {
                    inputStream = new FileInputStream(encryptFile);
                } else {
                    throw new PasswordEncryptionException("Could not find a file called " + encryptionFile + " on the CLASSPATH or file specified by -Dsimpleftp.passwordEncryptFile does not exist");
                }
            }

            encryptionKey = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
	                .lines()
                    .collect(Collectors.joining("\n"));
        }

        return encryptionKey;
    }

    private static SecretKeySpec getSecretKey() throws Exception {
        if (key == null) {
            String encryptionKey = getEncryptionKey();
            byte[] arrayBytes = encryptionKey.getBytes(UNICODE_FORMAT);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            arrayBytes = sha.digest(arrayBytes);
            arrayBytes = Arrays.copyOf(arrayBytes, 16);
            key = new SecretKeySpec(arrayBytes, "AES");
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
        String encrypted;
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ENCRYPTION_SCHEME);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plainText = password.getBytes(UNICODE_FORMAT);
            byte[] encryptedText = cipher.doFinal(plainText);
            encrypted = Base64.getEncoder().encodeToString(encryptedText);
        } catch (PasswordEncryptionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PasswordEncryptionException(ex.getMessage(), ex);
        }
        return encrypted;
    }

    /**
     * This method decrypts the password and returns the decrypted version
     * @param password the password to decrypt
     * @return decrypted password
     */
    public static String decrypt(String password) {
        String decrypted;
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ENCRYPTION_SCHEME);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedText = Base64.getDecoder().decode(password);
            byte[] plainText = cipher.doFinal(encryptedText);
            decrypted = new String(plainText);
        } catch (PasswordEncryptionException ex) {
            throw ex;
        } catch (Exception e) {
            throw new PasswordEncryptionException(e.getMessage(), e);
        }
        return decrypted;
    }
}
