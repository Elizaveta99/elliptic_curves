package com.example.server;

import java.awt.*;

public class UserInfo {
    EllipticGroup ellipticGroup;
    Point G;
    int n; //порядок подгруппы
    Point publicKey;
    Point publicKeyB;
    int privateKeyB;
    Point sharedKey;

    UserInfo(EllipticGroup ellipticGroup, Point G, int n) {
        this.ellipticGroup = ellipticGroup;
        this.G = G;
        this.n = n;
    }

    void setPublicKey(Point publicKey) {
        this.publicKey = publicKey;
    }

    Point generateKeysB() {
        privateKeyB = (int) (1 + Math.random() * (n - 1));
        try {
            publicKeyB = ellipticGroup.multN(privateKeyB, G);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return publicKeyB;
    }

    public void setSharedKey() {
        try {
            sharedKey = ellipticGroup.multN(privateKeyB, publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
