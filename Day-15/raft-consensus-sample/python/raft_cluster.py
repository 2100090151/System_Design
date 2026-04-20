from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class Role(Enum):
    FOLLOWER = "FOLLOWER"
    CANDIDATE = "CANDIDATE"
    LEADER = "LEADER"


@dataclass(frozen=True)
class LogEntry:
    term: int
    command: str


class RaftNode:
    def __init__(self, node_id: str) -> None:
        self.node_id = node_id
        self.role = Role.FOLLOWER
        self.current_term = 0
        self.voted_for: str | None = None
        self.log: list[LogEntry] = []
        self.commit_index = -1
        self.available = True

    def request_vote(
        self,
        candidate_id: str,
        term: int,
        last_log_index: int,
        last_log_term: int,
    ) -> bool:
        if not self.available:
            return False

        if term < self.current_term:
            return False

        if term > self.current_term:
            self.current_term = term
            self.voted_for = None
            self.role = Role.FOLLOWER

        if self.voted_for is not None and self.voted_for != candidate_id:
            return False

        if not self._is_candidate_log_up_to_date(last_log_index, last_log_term):
            return False

        self.voted_for = candidate_id
        return True

    def append_entries(
        self,
        leader_term: int,
        prev_log_index: int,
        prev_log_term: int,
        entries: list[LogEntry],
        leader_commit: int,
    ) -> bool:
        if not self.available:
            return False

        if leader_term < self.current_term:
            return False

        self.current_term = leader_term
        self.role = Role.FOLLOWER

        if prev_log_index >= 0:
            if prev_log_index >= len(self.log):
                return False
            if self.log[prev_log_index].term != prev_log_term:
                return False

        insert_at = prev_log_index + 1
        self.log = self.log[:insert_at] + entries

        if leader_commit > self.commit_index:
            self.commit_index = min(leader_commit, len(self.log) - 1)

        return True

    def _is_candidate_log_up_to_date(self, last_log_index: int, last_log_term: int) -> bool:
        my_last_index = len(self.log) - 1
        my_last_term = self.log[my_last_index].term if my_last_index >= 0 else -1

        if last_log_term != my_last_term:
            return last_log_term > my_last_term
        return last_log_index >= my_last_index


class RaftCluster:
    def __init__(self, node_ids: list[str]) -> None:
        if len(node_ids) < 3:
            raise ValueError("cluster should have at least 3 nodes")

        self.nodes = [RaftNode(node_id) for node_id in node_ids]
        self._leader_id: str | None = None

    def elect_leader(self, candidate_id: str) -> bool:
        candidate = self._node(candidate_id)
        if not candidate.available:
            return False

        candidate.role = Role.CANDIDATE
        candidate.current_term += 1
        candidate.voted_for = candidate.node_id
        term = candidate.current_term
        votes = 1

        last_log_index = len(candidate.log) - 1
        last_log_term = candidate.log[last_log_index].term if last_log_index >= 0 else -1

        for node in self.nodes:
            if node.node_id == candidate_id:
                continue
            if node.request_vote(candidate_id, term, last_log_index, last_log_term):
                votes += 1

        if votes >= self._majority():
            for node in self.nodes:
                node.role = Role.FOLLOWER
                if node.current_term < term:
                    node.current_term = term
            candidate.role = Role.LEADER
            self._leader_id = candidate_id
            return True

        candidate.role = Role.FOLLOWER
        return False

    def leader_append(self, command: str) -> bool:
        leader = self.leader()
        if leader is None or not leader.available:
            return False

        entry = LogEntry(term=leader.current_term, command=command)
        leader.log.append(entry)
        entry_index = len(leader.log) - 1
        prev_index = entry_index - 1
        prev_term = leader.log[prev_index].term if prev_index >= 0 else -1

        acknowledgements = 1
        for node in self.nodes:
            if node.node_id == leader.node_id:
                continue
            if node.append_entries(
                leader_term=leader.current_term,
                prev_log_index=prev_index,
                prev_log_term=prev_term,
                entries=[entry],
                leader_commit=leader.commit_index,
            ):
                acknowledgements += 1

        if acknowledgements >= self._majority():
            leader.commit_index = entry_index
            for node in self.nodes:
                if node.node_id == leader.node_id:
                    continue
                node.append_entries(
                    leader_term=leader.current_term,
                    prev_log_index=entry_index,
                    prev_log_term=entry.term,
                    entries=[],
                    leader_commit=leader.commit_index,
                )
            return True

        leader.log.pop()
        return False

    def leader(self) -> RaftNode | None:
        if self._leader_id is None:
            return None
        node = self._node(self._leader_id)
        if node.role != Role.LEADER:
            return None
        return node

    def set_availability(self, node_id: str, available: bool) -> None:
        node = self._node(node_id)
        node.available = available
        if node_id == self._leader_id and not available:
            self._leader_id = None

    def cluster_state(self) -> list[dict[str, str | int | bool]]:
        state: list[dict[str, str | int | bool]] = []
        for node in self.nodes:
            state.append(
                {
                    "node": node.node_id,
                    "available": node.available,
                    "role": node.role.value,
                    "term": node.current_term,
                    "log_len": len(node.log),
                    "commit_index": node.commit_index,
                }
            )
        return state

    def _node(self, node_id: str) -> RaftNode:
        for node in self.nodes:
            if node.node_id == node_id:
                return node
        raise KeyError(f"unknown node: {node_id}")

    def _majority(self) -> int:
        return (len(self.nodes) // 2) + 1
