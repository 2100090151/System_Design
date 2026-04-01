from __future__ import annotations

from dataclasses import dataclass, field
from threading import Lock


@dataclass
class URLShortener:
    base_url: str = "http://short.ly/"
    _code_to_url: dict[str, str] = field(default_factory=dict, init=False)
    _url_to_code: dict[str, str] = field(default_factory=dict, init=False)
    _counter: int = field(default=1, init=False)
    _lock: Lock = field(default_factory=Lock, init=False)

    _alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    def shorten_url(self, long_url: str) -> str:
        if not long_url or not long_url.startswith(("http://", "https://")):
            raise ValueError("URL must start with http:// or https://")

        with self._lock:
            existing_code = self._url_to_code.get(long_url)
            if existing_code:
                return self.base_url + existing_code

            code = self._encode_base62(self._counter)
            self._counter += 1

            self._code_to_url[code] = long_url
            self._url_to_code[long_url] = code
            return self.base_url + code

    def resolve(self, short_url_or_code: str) -> str | None:
        code = short_url_or_code.rsplit("/", maxsplit=1)[-1]
        return self._code_to_url.get(code)

    def _encode_base62(self, n: int) -> str:
        if n == 0:
            return self._alphabet[0]

        chars = []
        base = len(self._alphabet)
        while n > 0:
            n, rem = divmod(n, base)
            chars.append(self._alphabet[rem])
        chars.reverse()
        return "".join(chars)
