package dn.marketplace.account.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 канона USER_UPDATED. Дубликат алгоритма — {@code dn.marketplace.keycloak.UserUpdatedEventMac}
 * в SPI (модуль без Spring). Канон не зависит от Jackson: порядок полей фиксирован.
 */
final class UserUpdatedEventMac {

    private static final HexFormat HEX = HexFormat.of();

    private UserUpdatedEventMac() {
    }

    /**
     * Канонический JSON для MAC: accountId, username, email, firstName, lastName, issuedAt.
     * {@code null} в строках — пустая строка. {@code issuedAt} — epoch millis.
     */
    static String canonicalJson(
            String accountId,
            String username,
            String email,
            String firstName,
            String lastName,
            long issuedAtMillis) {
        return "{\"accountId\":" + jsonString(accountId)
                + ",\"username\":" + jsonString(username)
                + ",\"email\":" + jsonString(email)
                + ",\"firstName\":" + jsonString(firstName)
                + ",\"lastName\":" + jsonString(lastName)
                + ",\"issuedAt\":" + issuedAtMillis
                + "}";
    }

    static String macHex(String secret, String canonicalJson) {
        return HEX.formatHex(hmacSha256(secret, canonicalJson));
    }

    static boolean macEqualsHex(String secret, String canonicalJson, String macHex) {
        if (macHex == null || macHex.isBlank()) {
            return false;
        }
        byte[] expected = hmacSha256(secret, canonicalJson);
        byte[] actual;
        try {
            actual = HEX.parseHex(macHex);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (actual.length != expected.length) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    static String wireJson(
            String accountId,
            String username,
            String email,
            String firstName,
            String lastName,
            long issuedAtMillis,
            String secret) {
        String canonical = canonicalJson(accountId, username, email, firstName, lastName, issuedAtMillis);
        String mac = macHex(secret, canonical);
        return canonical.substring(0, canonical.length() - 1) + ",\"mac\":" + jsonString(mac) + "}";
    }

    static String jsonString(String value) {
        String s = value == null ? "" : value;
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static byte[] hmacSha256(String secret, String canonicalJson) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(canonicalJson.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 недоступен", e);
        }
    }
}
