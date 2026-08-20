package jp.tonbiattack.debuglab.token;

import java.util.Set;

/**
 * コールバッククエリに含まれる不透明トークンを検証します。
 */
public class CallbackTokenVerifier {

    private final Set<String> acceptedTokens = Set.of("BASE42", "AB+CD");
    private String lastAcceptedToken;
    private int acceptedTokenCount;

    public TokenOutcome verifyQuery(String query) {
        String rawToken = query.substring("token=".length());
        String token = rawToken;
        if (!acceptedTokens.contains(token)) {
            return TokenOutcome.REJECTED;
        }
        lastAcceptedToken = token;
        acceptedTokenCount++;
        return TokenOutcome.ACCEPTED;
    }

    public String lastAcceptedToken() {
        return lastAcceptedToken;
    }

    public int acceptedTokenCount() {
        return acceptedTokenCount;
    }
}
