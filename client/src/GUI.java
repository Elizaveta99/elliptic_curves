import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class GUI extends JFrame {
    private EllipticGroup ellipticGroup;
    private int n;
    private Point G;
    private int privateKey;
    private Point publicKey;
    private Point publicKeyB;
    private Point sharedKey;
    private String guid;
    private Request request = new Request();
    private AES aes = new AES();
    private Container container;
    private JLabel privateKeyLabel = new JLabel("");
    private JTextArea privateKeyTextArea = new JTextArea();
    private JButton privateKeyButton = new JButton("Set private key");
    private boolean privateKeySetted = false;
    private JTextArea messageTextArea = new JTextArea();
    private JButton messageButton = new JButton("Send message");

    GUI(EllipticGroup ellipticGroup) {
        super("");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setBounds(100, 100, 700, 700);
        container = this.getContentPane();
        container.setLayout(new GridLayout(0, 1, 10, 10));
        container.add(privateKeyLabel);
        container.add(privateKeyTextArea);
        container.add(privateKeyButton);
        container.add(messageTextArea);
        container.add(messageButton);

        privateKeyButton.addActionListener(new privateKeyButtonActionListener());
        messageButton.addActionListener(new messageButtonActionListener());

        this.ellipticGroup = ellipticGroup;
    }

    void setN(int n) {
        this.n = n;
    }

    void setG(Point g) {
        G = g;
    }

    void setGuid(String guid) {
        this.guid = guid;
    }

    void setLabels() {
        privateKeyLabel.setText("Your private key (1,...,n - 1; n = " + n + ")");
    }

    private class privateKeyButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            String text = privateKeyTextArea.getText();
            if (!text.matches("[1-9][0-9]*") || Integer.valueOf(text) >= n || Integer.valueOf(text) < 1) {
                JOptionPane.showMessageDialog(GUI.this, "Illegal private key");
            } else {
                try {
                    privateKeySetted = true;
                    privateKey = Integer.valueOf(text);
                    publicKey = ellipticGroup.multN(privateKey, G);
                    String point = request.get("http://localhost:8081/public?guid=" + guid + "&xPublic=" + publicKey.x + "&yPublic=" + publicKey.y);
                    publicKeyB = new Point(Integer.valueOf(point.substring(0, point.indexOf(','))), Integer.valueOf(point.substring(point.indexOf(',') + 1)));
                    sharedKey = ellipticGroup.multN(privateKey, publicKeyB);
                    aes.setKey(String.valueOf(sharedKey.x));
                    privateKeyLabel.setText("<html>Your private key (1,...,n - 1; n = " + n + ")" + ": " + privateKey + "<br>" +
                            "Your public key: (" + publicKey.x + ", " + publicKey.y + ")<br>" +
                            "Server public key: (" + publicKeyB.x + ", " + publicKeyB.y + ")<br>Shared key: (" + sharedKey.x + ", " + sharedKey.y + ")</html>");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class messageButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (!privateKeySetted) {
                JOptionPane.showMessageDialog(GUI.this, "Choose private key first");
                return;
            }
            String message = messageTextArea.getText();
            String cipher = Base64.getEncoder().encodeToString(aes.encrypt(message));
            int z = 1;
            try {
                z = sha1(cipher);
                z >>= new BigInteger(String.valueOf(z)).bitLength() - new BigInteger(String.valueOf(n)).bitLength();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            ellipticGroup.setDigitalSignature(n, G, z, privateKey);
            if (request.post("http://localhost:8081/message?guid=" + guid + "&r=" + ellipticGroup.r + "&s=" + ellipticGroup.s, cipher)) {
                JOptionPane.showMessageDialog(GUI.this, "Successfully sent");
            } else {
                JOptionPane.showMessageDialog(GUI.this, "No success");
            }
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
