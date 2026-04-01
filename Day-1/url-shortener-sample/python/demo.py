from url_shortener import URLShortener


def main() -> None:
    shortener = URLShortener()

    long_url = "https://example.com/very/long/link"
    short_url = shortener.shorten_url(long_url)
    resolved_url = shortener.resolve(short_url)

    print("Input long URL :", long_url)
    print("Generated short:", short_url)
    print("Resolved URL   :", resolved_url)

    # Same long URL returns the same short code in this implementation.
    print("Same URL again :", shortener.shorten_url(long_url))


if __name__ == "__main__":
    main()
