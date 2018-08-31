package no.ndla.taxonomy.service;

/**
 * Used to ensure that queries for old style URLS of various kinds (ndla.no/node/xxx) are transformed to the same format
 */
public class OldUrlCanonifier {

    public String canonify(String oldUrl) {
        oldUrl = discardMenuVariations(oldUrl);
        String nodeId = "";
        String fagId = "";
        for (String token : tokenize(oldUrl)) {
            if (!token.contains("=")) {
                int nodeStartsAt = findNodeStartsAt(token);
                nodeId = token.substring(nodeStartsAt);
            } else if (token.contains("fag=")) {
                fagId = "?" + token.substring(token.indexOf("fag="));
            }
        }
        return "ndla.no" + nodeId + fagId;
    }

    private String discardMenuVariations(String oldUrl) {
        if (oldUrl.contains("/menu")) {
            int start = oldUrl.indexOf("/menu");
            int indexOfSlashAfter = oldUrl.indexOf("/", start + 1);
            int indexOfQuestionMark = oldUrl.indexOf("?", start);
            String partToRemove;
            if (indexOfSlashAfter != -1) {
                partToRemove = oldUrl.substring(start, indexOfSlashAfter + 1);
            } else if (indexOfQuestionMark != -1) {
                partToRemove = oldUrl.substring(start, indexOfQuestionMark);
            } else partToRemove = "/menu";
            return oldUrl.replace(partToRemove, "");
        } else return oldUrl;
    }

    private int findNodeStartsAt(String token) {
        return token.substring(0, token.lastIndexOf("/")).lastIndexOf("/");
    }

    private String[] tokenize(String oldUrl) {
        return oldUrl.split("\\?");
    }
}
