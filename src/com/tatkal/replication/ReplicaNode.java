package com.tatkal.replication;

public enum ReplicaNode {
    MUMBAI_CENTRAL(1, "MUMBAI_CENTRAL", "CENTRAL"),
    MUMBAI_REPLICA(11, "MUMBAI_REPLICA", "MUMBAI_CENTRAL"),
    DELHI_CENTRAL(2, "DELHI_CENTRAL", "CENTRAL"),
    DELHI_REPLICA(21, "DELHI_REPLICA", "DELHI_CENTRAL"),
    CENTRAL(3, "CENTRAL", null),
    CENTRAL_REPLICA(31, "CENTRAL_REPLICA", "CENTRAL");

    private final int id;
    private final String nodeName;
    private final String parentNodeName;

    ReplicaNode(int id, String nodeName, String parentNodeName) {
        this.id = id;
        this.nodeName = nodeName;
        this.parentNodeName = parentNodeName;
    }

    public int getId() {
        return id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getParentNodeName() {
        return parentNodeName;
    }

    public static ReplicaNode fromName(String name) {
        for (ReplicaNode node : values()) {
            if (node.getNodeName().equalsIgnoreCase(name)) {
                return node;
            }
        }
        return null;
    }
}
