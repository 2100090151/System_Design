from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass, field


def tokenize(text: str) -> list[str]:
    return re.findall(r"[a-z0-9]+", text.lower())


@dataclass
class InvertedIndex:
    _documents: dict[str, str] = field(default_factory=dict)
    _postings: dict[str, set[str]] = field(default_factory=lambda: defaultdict(set))

    def add_document(self, document_id: str, text: str) -> None:
        if not document_id:
            raise ValueError("document_id must be non-empty")
        if document_id in self._documents:
            raise ValueError(f"document {document_id} already exists")

        self._documents[document_id] = text
        for term in set(tokenize(text)):
            self._postings[term].add(document_id)

    def search(self, query: str) -> list[tuple[str, str]]:
        terms = tokenize(query)
        if not terms:
            return []

        matching_ids: set[str] | None = None
        for term in terms:
            postings = self._postings.get(term, set())
            if matching_ids is None:
                matching_ids = set(postings)
            else:
                matching_ids &= postings

            if not matching_ids:
                return []

        assert matching_ids is not None
        return [(document_id, self._documents[document_id]) for document_id in sorted(matching_ids)]
