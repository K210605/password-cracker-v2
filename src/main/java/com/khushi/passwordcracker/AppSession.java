package com.khushi.passwordcracker;

/**
 * Small in-memory holder for state that needs to travel between screens -
 * currently just the hash produced by the Hash Generator module, so the
 * Attack Lab can pre-fill its Target Hash field when the user clicks
 * "Use as Target Hash".
 *
 * This is intentionally simple (no database, no persistence): it only
 * lives for the duration of the running application, per the project's
 * "no database unless absolutely required" constraint.
 */
public final class AppSession {

    private static String pendingTargetHash = null;

    private AppSession() {
    }

    public static void setPendingTargetHash(String hash) {
        pendingTargetHash = hash;
    }

    /**
     * Returns the pending target hash (if any) and clears it, so the
     * Attack Lab only consumes it once per "Use as Target Hash" click.
     */
    public static String consumePendingTargetHash() {
        String value = pendingTargetHash;
        pendingTargetHash = null;
        return value;
    }
}
