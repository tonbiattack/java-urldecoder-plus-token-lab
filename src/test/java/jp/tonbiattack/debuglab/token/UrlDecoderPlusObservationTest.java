package jp.tonbiattack.debuglab.token;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class UrlDecoderPlusObservationTest {

    @Test
    void formDecoderTurnsPlusIntoSpaceWhileOpaqueTokenMustKeepIt() {
        String rawToken = "AB+CD";
        String formDecoded = URLDecoder.decode(rawToken, StandardCharsets.UTF_8);
        String opaqueToken = rawToken;

        assertAll(
                () -> assertEquals("AB CD", formDecoded,
                        "URLDecoderはフォーム規則によりプラス記号を空白へ変換する"),
                () -> assertEquals("AB+CD", opaqueToken,
                        "不透明トークンではプラス記号を文字どおり保持する")
        );
    }
}
