package com.zipe.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenProviderRs256Test {

    @Test
    void rs256_generateThenValidate_shouldRoundTrip(@TempDir Path dir) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        Path priv = dir.resolve("private.pem");
        Path pub = dir.resolve("public.pem");
        Files.writeString(priv, toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        Files.writeString(pub, toPem("PUBLIC KEY", kp.getPublic().getEncoded()));

        JwtProperties p = new JwtProperties();
        p.setAlgorithm("RS256");
        p.setPrivateKeyLocation("file:" + priv);
        p.setPublicKeyLocation("file:" + pub);

        JwtTokenProvider provider = new JwtTokenProvider(p);
        provider.init();
        String token = provider.generateToken("bob");
        assertEquals("bob", provider.validateAndGetUsername(token));
    }

    private String toPem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
