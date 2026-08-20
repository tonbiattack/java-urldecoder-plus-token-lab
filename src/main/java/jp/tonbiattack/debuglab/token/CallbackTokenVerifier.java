package jp.tonbiattack.debuglab.token;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        String token = URLDecoder.decode(rawToken, StandardCharsets.UTF_8);
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
