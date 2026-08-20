package jp.tonbiattack.debuglab.token;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CallbackTokenVerifierTest {

    @Test
    void plusSignToken_isAcceptedLiterallyAndUpdatesAcceptedState() {
        CallbackTokenVerifier verifier = new CallbackTokenVerifier();
        verifier.verifyQuery("token=BASE42");

        TokenOutcome outcome = verifier.verifyQuery("token=AB+CD");

        assertAll(
                () -> assertEquals(TokenOutcome.ACCEPTED, outcome,
                        "プラス記号を含む許可トークンを受理する"),
                () -> assertEquals("AB+CD", verifier.lastAcceptedToken(),
                        "最後に受理したトークンはプラス記号を保持する"),
                () -> assertEquals(2, verifier.acceptedTokenCount(),
                        "BASE42とAB+CDの二件を受理する")
        );
    }

    @Test
    void unknownToken_preservesAcceptedState() {
        CallbackTokenVerifier verifier = new CallbackTokenVerifier();
        verifier.verifyQuery("token=BASE42");

        TokenOutcome outcome = verifier.verifyQuery("token=UNKNOWN");

        assertAll(
                () -> assertEquals(TokenOutcome.REJECTED, outcome),
                () -> assertEquals("BASE42", verifier.lastAcceptedToken()),
                () -> assertEquals(1, verifier.acceptedTokenCount())
        );
    }
}
