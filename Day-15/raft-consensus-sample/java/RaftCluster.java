import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RaftCluster {
    enum Role {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    static final class LogEntry {
        private final int term;
        private final String command;

        LogEntry(int term, String command) {
            this.term = term;
            this.command = command;
        }

        int getTerm() {
            return term;
        }

        String getCommand() {
            return command;
        }
    }

    static final class RaftNode {
        private final String nodeId;
        private Role role = Role.FOLLOWER;
        private int currentTerm = 0;
        private String votedFor = null;
        private final List<LogEntry> log = new ArrayList<>();
        private int commitIndex = -1;
        private boolean available = true;

        RaftNode(String nodeId) {
            this.nodeId = nodeId;
        }

        boolean requestVote(String candidateId, int term, int lastLogIndex, int lastLogTerm) {
            if (!available) {
                return false;
            }
            if (term < currentTerm) {
                return false;
            }
            if (term > currentTerm) {
                currentTerm = term;
                votedFor = null;
                role = Role.FOLLOWER;
            }
            if (votedFor != null && !votedFor.equals(candidateId)) {
                return false;
            }
            if (!isCandidateLogUpToDate(lastLogIndex, lastLogTerm)) {
                return false;
            }
            votedFor = candidateId;
            return true;
        }

        boolean appendEntries(
            int leaderTerm,
            int prevLogIndex,
            int prevLogTerm,
            List<LogEntry> entries,
            int leaderCommit
        ) {
            if (!available) {
                return false;
            }
            if (leaderTerm < currentTerm) {
                return false;
            }

            currentTerm = leaderTerm;
            role = Role.FOLLOWER;

            if (prevLogIndex >= 0) {
                if (prevLogIndex >= log.size()) {
                    return false;
                }
                if (log.get(prevLogIndex).getTerm() != prevLogTerm) {
                    return false;
                }
            }

            int insertAt = prevLogIndex + 1;
            while (log.size() > insertAt) {
                log.remove(log.size() - 1);
            }
            log.addAll(entries);

            if (leaderCommit > commitIndex) {
                commitIndex = Math.min(leaderCommit, log.size() - 1);
            }

            return true;
        }

        private boolean isCandidateLogUpToDate(int lastLogIndex, int lastLogTerm) {
            int myLastIndex = log.size() - 1;
            int myLastTerm = myLastIndex >= 0 ? log.get(myLastIndex).getTerm() : -1;
            if (lastLogTerm != myLastTerm) {
                return lastLogTerm > myLastTerm;
            }
            return lastLogIndex >= myLastIndex;
        }
    }

    private final List<RaftNode> nodes;
    private String leaderId = null;

    public RaftCluster(List<String> nodeIds) {
        if (nodeIds.size() < 3) {
            throw new IllegalArgumentException("cluster should have at least 3 nodes");
        }
        nodes = new ArrayList<>();
        for (String nodeId : nodeIds) {
            nodes.add(new RaftNode(nodeId));
        }
    }

    public boolean electLeader(String candidateId) {
        RaftNode candidate = node(candidateId);
        if (!candidate.available) {
            return false;
        }

        candidate.role = Role.CANDIDATE;
        candidate.currentTerm += 1;
        candidate.votedFor = candidate.nodeId;
        int term = candidate.currentTerm;
        int votes = 1;

        int lastLogIndex = candidate.log.size() - 1;
        int lastLogTerm = lastLogIndex >= 0 ? candidate.log.get(lastLogIndex).getTerm() : -1;

        for (RaftNode node : nodes) {
            if (node.nodeId.equals(candidateId)) {
                continue;
            }
            if (node.requestVote(candidateId, term, lastLogIndex, lastLogTerm)) {
                votes += 1;
            }
        }

        if (votes >= majority()) {
            for (RaftNode node : nodes) {
                node.role = Role.FOLLOWER;
                if (node.currentTerm < term) {
                    node.currentTerm = term;
                }
            }
            candidate.role = Role.LEADER;
            leaderId = candidateId;
            return true;
        }

        candidate.role = Role.FOLLOWER;
        return false;
    }

    public boolean leaderAppend(String command) {
        RaftNode leader = leader();
        if (leader == null || !leader.available) {
            return false;
        }

        LogEntry entry = new LogEntry(leader.currentTerm, command);
        leader.log.add(entry);
        int entryIndex = leader.log.size() - 1;
        int prevIndex = entryIndex - 1;
        int prevTerm = prevIndex >= 0 ? leader.log.get(prevIndex).getTerm() : -1;

        int acknowledgements = 1;
        for (RaftNode node : nodes) {
            if (node.nodeId.equals(leader.nodeId)) {
                continue;
            }
            if (node.appendEntries(leader.currentTerm, prevIndex, prevTerm, List.of(entry), leader.commitIndex)) {
                acknowledgements += 1;
            }
        }

        if (acknowledgements >= majority()) {
            leader.commitIndex = entryIndex;
            for (RaftNode node : nodes) {
                if (node.nodeId.equals(leader.nodeId)) {
                    continue;
                }
                node.appendEntries(leader.currentTerm, entryIndex, entry.getTerm(), List.of(), leader.commitIndex);
            }
            return true;
        }

        leader.log.remove(leader.log.size() - 1);
        return false;
    }

    public void setAvailability(String nodeId, boolean available) {
        RaftNode node = node(nodeId);
        node.available = available;
        if (!available && nodeId.equals(leaderId)) {
            leaderId = null;
        }
    }

    public List<Map<String, Object>> clusterState() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RaftNode node : nodes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("node", node.nodeId);
            row.put("available", node.available);
            row.put("role", node.role.toString());
            row.put("term", node.currentTerm);
            row.put("log_len", node.log.size());
            row.put("commit_index", node.commitIndex);
            rows.add(row);
        }
        return rows;
    }

    private RaftNode leader() {
        if (leaderId == null) {
            return null;
        }
        RaftNode node = node(leaderId);
        if (node.role != Role.LEADER) {
            return null;
        }
        return node;
    }

    private RaftNode node(String nodeId) {
        for (RaftNode node : nodes) {
            if (node.nodeId.equals(nodeId)) {
                return node;
            }
        }
        throw new IllegalArgumentException("unknown node: " + nodeId);
    }

    private int majority() {
        return (nodes.size() / 2) + 1;
    }
}
