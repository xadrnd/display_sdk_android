package com.xad.sdk;

import com.xad.sdk.utils.AdUrlGenerator;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void printSha1() {
        String toBeHashed = "This is string to be hashed";
        try {
            String lastHashed = "";
            for (int i = 0; i < 100000; i++) {
                String hashed = AdUrlGenerator.SHA1(toBeHashed);
                if(i == 0) {
                    lastHashed = hashed;
                    continue;
                }
                if(!hashed.equals(lastHashed)) {
                    System.out.println("Hashed string: " + hashed);
                    System.out.println("This is not valid implementation of sha1");
                    System.exit(1);
                }
                lastHashed = hashed;
            }
            System.out.println("It works");
            System.out.println(lastHashed);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}