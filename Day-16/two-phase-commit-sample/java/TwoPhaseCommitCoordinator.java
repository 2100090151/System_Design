import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TwoPhaseCommitCoordinator {
    public enum Decision {
        COMMIT,
        ABORT
    }

    private static final class CommittedOperation {
        private final String txId;
        private final String operation;

        private CommittedOperation(String txId, String operation) {
            this.txId = txId;
            this.operation = operation;
        }
    }

    public static final class Participant {
        private final String name;
        private boolean available = true;
        private final Set<String> rejectTxIds = new HashSet<>();
        private final Map<String, String> pending = new LinkedHashMap<>();
        private final List<CommittedOperation> committed = new ArrayList<>();

        private Participant(String name) {
            this.name = name;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public void setRejectForTx(String txId, boolean reject) {
            if (reject) {
                rejectTxIds.add(txId);
            } else {
                rejectTxIds.remove(txId);
            }
        }

        private boolean prepare(String txId, String operation) {
            if (!available) {
                return false;
            }
            if (rejectTxIds.contains(txId)) {
                return false;
            }
            pending.put(txId, operation);
            return true;
        }

        private void finalizeDecision(String txId, Decision decision) {
            if (!available) {
                return;
            }
            String operation = pending.remove(txId);
            if (operation == null) {
                return;
            }
            if (decision == Decision.COMMIT) {
                committed.add(new CommittedOperation(txId, operation));
            }
        }

        private boolean hasPending(String txId) {
            return pending.containsKey(txId);
        }

        private boolean hasCommitted(String txId) {
            for (CommittedOperation operation : committed) {
                if (operation.txId.equals(txId)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final Map<String, Participant> participants;
    private final Map<String, Decision> decisionLog;

    public TwoPhaseCommitCoordinator(List<String> participantNames) {
        if (participantNames == null || participantNames.isEmpty()) {
            throw new IllegalArgumentException("participantNames must be non-empty");
        }

        participants = new LinkedHashMap<>();
        for (String name : participantNames) {
            if (participants.containsKey(name)) {
                throw new IllegalArgumentException("participant names must be unique");
            }
            participants.put(name, new Participant(name));
        }
        decisionLog = new LinkedHashMap<>();
    }

    public Participant participant(String name) {
        Participant participant = participants.get(name);
        if (participant == null) {
            throw new IllegalArgumentException("unknown participant: " + name);
        }
        return participant;
    }

    public Decision execute(
        String txId,
        Map<String, String> operationsByParticipant,
        List<String> dropBeforeDecision
    ) {
        if (txId == null || txId.isBlank()) {
            throw new IllegalArgumentException("txId must be non-empty");
        }
        if (operationsByParticipant == null || operationsByParticipant.isEmpty()) {
            throw new IllegalArgumentException("operationsByParticipant must be non-empty");
        }

        List<String> preparedParticipants = new ArrayList<>();
        boolean allYes = true;

        for (Map.Entry<String, String> entry : operationsByParticipant.entrySet()) {
            Participant participant = participant(entry.getKey());
            boolean votedYes = participant.prepare(txId, entry.getValue());
            if (votedYes) {
                preparedParticipants.add(entry.getKey());
            } else {
                allYes = false;
            }
        }

        Decision decision = allYes ? Decision.COMMIT : Decision.ABORT;
        decisionLog.put(txId, decision);

        if (dropBeforeDecision != null) {
            for (String name : dropBeforeDecision) {
                participant(name).setAvailable(false);
            }
        }

        for (String participantName : preparedParticipants) {
            participant(participantName).finalizeDecision(txId, decision);
        }

        return decision;
    }

    public Decision execute(String txId, Map<String, String> operationsByParticipant) {
        return execute(txId, operationsByParticipant, List.of());
    }

    public void reconcileParticipant(String participantName, String txId) {
        Decision decision = decisionLog.get(txId);
        if (decision == null) {
            throw new IllegalArgumentException("no decision found for txId=" + txId);
        }
        participant(participantName).finalizeDecision(txId, decision);
    }

    public Decision decisionFor(String txId) {
        return decisionLog.get(txId);
    }

    public List<String> statesForTx(String txId) {
        List<String> lines = new ArrayList<>();
        for (Participant participant : participants.values()) {
            lines.add(
                participant.name
                    + ": available="
                    + participant.available
                    + ", pending="
                    + participant.hasPending(txId)
                    + ", committed="
                    + participant.hasCommitted(txId)
            );
        }
        return lines;
    }
}
