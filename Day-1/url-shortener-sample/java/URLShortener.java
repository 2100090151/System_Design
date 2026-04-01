import java.util.HashMap;
import java.util.Map;

public class URLShortener {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final String baseUrl;
    private final Map<String, String> codeToUrl;
    private final Map<String, String> urlToCode;
    private long counter;

    public URLShortener(String baseUrl) {
        this.baseUrl = baseUrl;
        this.codeToUrl = new HashMap<>();
        this.urlToCode = new HashMap<>();
        this.counter = 1L;
    }

    public synchronized String shortenUrl(String longUrl) {
        if (longUrl == null || !(longUrl.startsWith("http://") || longUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }

        if (urlToCode.containsKey(longUrl)) {
            return baseUrl + urlToCode.get(longUrl);
        }

        String code = encodeBase62(counter++);
        codeToUrl.put(code, longUrl);
        urlToCode.put(longUrl, code);
        return baseUrl + code;
    }

    public synchronized String resolve(String shortUrlOrCode) {
        String code = shortUrlOrCode;
        int slash = shortUrlOrCode.lastIndexOf('/');
        if (slash >= 0 && slash < shortUrlOrCode.length() - 1) {
            code = shortUrlOrCode.substring(slash + 1);
        }
        return codeToUrl.get(code);
    }

    private String encodeBase62(long n) {
        if (n == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        int base = ALPHABET.length();
        long value = n;

        while (value > 0) {
            int rem = (int) (value % base);
            sb.append(ALPHABET.charAt(rem));
            value /= base;
        }

        return sb.reverse().toString();
    }
}
