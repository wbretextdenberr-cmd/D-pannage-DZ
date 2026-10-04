package dz.depannage.app;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * المصادقة (تجريبية). الخادم يستبدلها: OTP عبر SMS،
 * وكلمات المرور بـ bcrypt/argon2 لا بتجزئة بسيطة.
 */
public class Auth {

    /** يقبل الأرقام العربية والمسافات والكود الدولي ويُرجع 05/06/07 + 8 أرقام. */
    public static String normalize(String input) {
        StringBuilder b = new StringBuilder();
        for (char ch : String.valueOf(input).toCharArray()) {
            if (ch >= '\u0660' && ch <= '\u0669') {
                b.append((char) ('0' + (ch - '\u0660')));
            } else if (ch >= '\u06F0' && ch <= '\u06F9') {
                b.append((char) ('0' + (ch - '\u06F0')));
            } else if (ch >= '0' && ch <= '9') {
                b.append(ch);
            }
        }
        String v = b.toString();
        if (v.startsWith("00213")) {
            v = v.substring(5);
        } else if (v.startsWith("213") && v.length() >= 12) {
            v = v.substring(3);
        }
        if (v.length() == 9 && "567".indexOf(v.charAt(0)) >= 0) {
            v = "0" + v;
        }
        return v;
    }

    /** جيزي (07) وموبيليس (06) وأوريدو (05). */
    public static boolean validPhone(String phone) {
        return phone.matches("0[567][0-9]{8}");
    }

    public static String hash(String phone, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("salakni:" + phone + ":" + password).getBytes("UTF-8"));
            StringBuilder s = new StringBuilder("s:");
            for (byte x : d) {
                s.append(String.format("%02x", x));
            }
            return s.toString();
        } catch (Exception e) {
            return "p:" + password;
        }
    }

    public static String newOtp() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }
}
