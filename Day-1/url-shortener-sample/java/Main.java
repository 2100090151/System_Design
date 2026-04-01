public class Main {
    public static void main(String[] args) {
        URLShortener shortener = new URLShortener("http://short.ly/");

        String longUrl = "https://example.com/very/long/link";
        String shortUrl = shortener.shortenUrl(longUrl);
        String resolved = shortener.resolve(shortUrl);

        System.out.println("Input long URL : " + longUrl);
        System.out.println("Generated short: " + shortUrl);
        System.out.println("Resolved URL   : " + resolved);
        System.out.println("Same URL again : " + shortener.shortenUrl(longUrl));
    }
}
