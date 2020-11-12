package com.example.server;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;

@Controller
public class EllipticController {
    HashMap<String, UserInfo> users = new HashMap<>();
    AES aes = new AES();

    @PostMapping("/parameters")
    @ResponseBody
    public ResponseEntity setParameters(@RequestParam(value = "guid") String guid,
                                        @RequestParam(value = "M") String M,
                                        @RequestParam(value = "a") String a,
                                        @RequestParam(value = "b") String b,
                                        @RequestParam(value = "xG") String xG,
                                        @RequestParam(value = "yG") String yG,
                                        @RequestParam(value = "n") String n) {
        EllipticGroup ellipticGroup = new EllipticGroup(Integer.valueOf(M));
        if (ellipticGroup.setAB(Integer.valueOf(a), Integer.valueOf(b))) {
            try {
                ellipticGroup.generateElements();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Point G = new Point(Integer.valueOf(xG), Integer.valueOf(yG));
        users.put(guid, new UserInfo(ellipticGroup, G, Integer.valueOf(n)));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/public")
    public ResponseEntity setKeys(@RequestParam(value = "guid") String guid,
                                  @RequestParam(value = "xPublic") String xPublic,
                                  @RequestParam(value = "yPublic") String yPublic) {
        System.out.println(guid);
        System.out.println("user public key: (" + xPublic + ", " + yPublic + ")");
        UserInfo user = users.get(guid);
        user.setPublicKey(new Point(Integer.valueOf(xPublic), Integer.valueOf(yPublic)));
        Point publicKeyB = user.generateKeysB();
        System.out.println("server private key: " + user.privateKeyB);
        System.out.println("server public key: (" + publicKeyB.x + ", " + publicKeyB.y + ")");
        user.setSharedKey();
        System.out.println("shared key: (" + user.sharedKey.x + ", " + user.sharedKey.y + ")");
        System.out.println();
        return new ResponseEntity<>(publicKeyB.x + "," + publicKeyB.y, HttpStatus.OK);
    }

    @PostMapping("/message")
    @ResponseBody
    public ResponseEntity message(@RequestParam(value = "guid") String guid,
                                  @RequestParam(value = "r") String r,
                                  @RequestParam(value = "s") String s,
                                  HttpEntity<String> httpEntity) {
        System.out.println(guid);
        UserInfo user = users.get(guid);
        String json = httpEntity.getBody();
        String cipher = "";
        try {
            JSONObject jsonObj = new JSONObject(json);
            cipher = jsonObj.getString("cipher");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("cipher: " + cipher);
        int z = 1;
        try {
            z = sha1(cipher);
            z >>= new BigInteger(String.valueOf(z)).bitLength() - new BigInteger(String.valueOf(user.n)).bitLength();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Point P = null;
        try {
            int u1 = ((user.ellipticGroup.inverse(Integer.valueOf(s), user.n) * z) % user.n + user.n) % user.n;
            int u2 = ((user.ellipticGroup.inverse(Integer.valueOf(s), user.n) * Integer.valueOf(r)) % user.n + user.n) % user.n;
            P = user.ellipticGroup.sum(user.ellipticGroup.multN(u1, user.G), user.ellipticGroup.multN(u2, user.publicKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Integer.valueOf(r) == P.x % user.n) {
            aes.setKey(String.valueOf(user.sharedKey.x));
            String message = new String(aes.decrypt(Base64.getDecoder().decode(cipher)));
            System.out.println("message: " + message);
            System.out.println();
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            System.out.println("invalid signature");
            System.out.println();
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    static int sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
        ByteBuffer wrapped = ByteBuffer.wrap(sb.toString().getBytes());
        return wrapped.getInt();
    }
}
