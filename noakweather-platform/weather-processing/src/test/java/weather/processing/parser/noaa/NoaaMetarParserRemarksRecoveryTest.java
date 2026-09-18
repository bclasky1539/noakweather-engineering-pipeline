package weather.processing.parser.noaa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.model.components.remark.Icing;
import weather.model.components.remark.PressureRapidChange;
import weather.processing.parser.common.ParseResult;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Regression tests for the remarks-parsing recovery fix: an unrecognized
 * remarks token must no longer prevent later, otherwise-parseable tokens
 * from being extracted. Each case pairs a real captured METAR with an
 * assertion block checking (a) the exact set of tokens expected to remain
 * unparsed given current pattern coverage, and (b) that tokens after them
 * were correctly extracted despite the earlier miss.
 * <p>
 * NOTE: cases marked accordingly need one supervised run against the fixed
 * parser to confirm exact freeText contents before the assertion is
 * locked in — several of these involve overlapping optional regex groups
 * (TS_CLD_LOC_PATTERN's loc/dir/dir2/MOV) whose exact token consumption
 * shouldn't be hand-guessed.
 */
class NoaaMetarParserRemarksRecoveryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaMetarParserRemarksRecoveryTest.class);

    private static NoaaMetarData parse(String raw) {
        ParseResult<NoaaWeatherData> result = new NoaaMetarParser().parse(raw);
        assertThat(result.isSuccess())
                .as("Parse should succeed for: %s", raw)
                .isTrue();
        return (NoaaMetarData) result.getData().orElseThrow();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("remarksRecoveryCases")
    void handlesUnrecognizedTokensWithoutLosingLaterRemarks(
            String stationLabel, String raw, Consumer<NoaaMetarData> assertions) {
        NoaaMetarData data = parse(raw);
        assertions.accept(data);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> remarksRecoveryCases() {
        return Stream.of(

                // Fully well-formed remark — every token recognized. Baseline
                // confirming the fix doesn't disturb already-working parsing.
                arguments("KJFK-allTokensParse",
                        "2026/09/02 00:51 KJFK 020051Z 03009KT 7SM OVC009 23/22 A2998 " +
                                "RMK RAE20 SLP151 P0002 T02330222 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1015.1);
                            assertThat(data.getHourlyPrecipitation()).isEqualTo(0.02);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(23.3);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(22.2);
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();
                        }),

                // KMIA — LTG (unwired) at position 0 previously swallowed
                // SLP151, the thunderstorm-location group, and T02940239.
                // Core regression case for the recovery fix.
                arguments("KMIA-LTGDoesNotSwallowDownstream",
                        "2026/09/11 22:53 KMIA 112253Z 15005KT 10SM FEW023 SCT050 BKN100 BKN220 " +
                                "29/24 A2998 RMK AO2 LTG DSNT W TSE09B13E46 SLP151 CB DSNT W-NW AND E MOV N T02940239 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("LTG (unwired), the chained B13E46 continuation, and AND-chain remnants " +
                                            "should be the only unparsed tokens — SLP, thunderstorm location, and " +
                                            "precise temp must all survive")
                                    .isEqualTo("LTG DSNT W B13E46 AND E MOV N");
                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP151 must parse despite the earlier unrecognized LTG clause")
                                    .isEqualTo(1015.1);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(29.4);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(23.9);
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            assertThat(data.getRemarks().maintenanceRequired())
                                    .as("Trailing $ should parse despite everything ahead of it failing")
                                    .isTrue();
                        }),

                // KAFW — same LTG-first shape, shorter remark. Confirms the
                // trailing "$" maintenance indicator now survives too.
                arguments("KAFW-LTGDoesNotSwallowTrailingMaintenance",
                        "2026/09/11 22:53 KAFW 112253Z 14013KT 10SM FEW060 BKN120 BKN250 36/21 " +
                                "A2976 RMK AO2 LTG DSNT SE-SW SLP060 T03610206 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1006.0);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(36.1);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(20.6);
                            assertThat(data.getRemarks().maintenanceRequired())
                                    .as("Trailing $ should now parse instead of being lost in freeText")
                                    .isTrue();
                        }),

                // KPHX — confirms SLP and the first CB-location group parse
                // BEFORE the AND-chain failure point (proves handlers do
                // sequence correctly up to the actual gap).
                arguments("KPHX-ParsesUpToAndChainGap",
                        "2026/09/11 22:51 KPHX 112251Z 30011G17KT 10SM FEW090 FEW130 SCT250 " +
                                "41/17 A2972 RMK AO2 SLP041 CB DSNT N-E AND SE-S T04060167",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1004.1);
                            assertThat(data.getRemarks().thunderstormLocations()).isNotEmpty();
                            assertThat(data.getRemarks().preciseTemperature())
                                    .as("T04060167 must survive the AND SE-S gap ahead of it")
                                    .isNotNull();
                        }),

                // The remaining 15 examples (KCLT, CYOW, CYQX, CYYQ x3, CYVR x2,
                // CYQB, CYYC, CYEG, CYHZ, KARB, KBUF x2, KSAV, CXOL, KBBF) —
                // add after one supervised run confirms exact freeText per case.
                // Each still needs a case; several will also surface additional
                // unwired-pattern findings (ICG confirmed via KCLT; watch for
                // VSBY NE QUAD, F#/S# shorthand, and the UP-prefixed chained
                // weather-event group in KARB).
                arguments("KCLT-multipleGapsSurviveToMaintenanceAndTemp",
                        "2020/06/05 22:04 KCLT 052204Z 18010KT 10SM FEW035 SCT041TCU SCT065 BKN250 28/21 A2989 " +
                                "RMK AO2 ICG PAST HR LTG DSNT NE-SE OCNL LTGICCC DSNT E TS DSNT E MOV E CB DSNT E TCU N-NE AND NW T02780206 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .isEqualTo("LTG DSNT NE-SE OCNL LTGICCC DSNT E AND NW");
                            assertThat(data.getRemarks().icing()).isEqualTo(Icing.of(false, false, "PAST HR"));
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(27.8);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(20.6);
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(3);
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();
                        }),

                arguments("CYOW-EMBDDAndLTGDoNotBlockSLP",
                        "2017/04/10 00:00 CYOW 160800Z 21004KT 8SM -TSRA BKN020 OVC100 20/18 A2966 " +
                                "RMK SC5AC3 CB EMBDD LTGCG SE SLP044",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("EMBDD LTGCG SE");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1004.4);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(2);
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                        }),

                arguments("CYQX-F8DoesNotBlockSLP040",
                        "2017/04/10 00:00 CYQX 141200Z CCB 32008KT 1/4SM FG VV002 09/08 A2963 RMK F8 SLP040",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F8");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1004.0);
                        }),

                arguments("CYYQ-VSBYQuadrantDoesNotBlockSLP140",
                        "2017/04/10 00:00 CYYQ 241700Z 34011KT 2SM -DZ BR OVC004 05/05 A2994 RMK F4SF3 VSBY NE QUAD 1 SLP140",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F4SF3 VSBY NE QUAD 1");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1014.0);
                        }),

                arguments("CYYQ-F8DoesNotBlockSLP131",
                        "2017/04/10 00:00 CYYQ 301300Z 35011KT 1/8SM FG VV001 02/02 A2991 RMK F8 SLP131",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F8");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1013.1);
                        }),

                // BUG: bare TCU throws during CloudType construction (no oktas/intensity/
                // location/movement) and is silently discarded — not in cloudTypes, not
                // in freeText, only a WARNING log. See finding #1 above.
                arguments("CYVR-TCUSilentlyDroppedOnInvalidCloudType",
                        "2017/04/10 00:00 CYVR 061843Z 09008KT 4SM -SHRA BR BKN006 BKN015 OVC040 RMK CF6SC2SC1 TCU EMBDD",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("EMBDD");
                            assertThat(data.getRemarks().cloudTypes()).hasSize(3);
                            // NOTE: TCU is silently lost here — not asserting its absence as
                            // correct, just documenting current (buggy) behavior until #<new issue> is fixed.
                        }),

                arguments("CYQB-AllTokensParse",
                        "2017/04/10 00:00 CYQB 201900Z 00000KT 3/4SM -SHRA BR BKN004 OVC020 19/18 A2962 RMK SF6SC2 SLP032",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1003.2);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(2);
                        }),

                arguments("CYVR-F8DoesNotBlockSLP998",
                        "2017/04/10 00:00 CYVR 151100Z VRB03KT 1/4SM FG VV002 09/08 A2963 RMK F8 SLP998",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F8");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(999.8);
                        }),

                arguments("CYYC-F6SF2DoesNotBlockSLP009",
                        "2017/04/10 00:00 CYYC 151100Z 09005KT 1 1/2SM -SN BR OVC003 00/00 A2990 RMK F6SF2 SLP009",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F6SF2");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1000.9);
                        }),

                arguments("CYEG-AllTokensParse",
                        "2017/04/10 00:00 CYEG 151100Z 00000KT 15SM FEW018 BKN040 OVC095 M00/M02 A2996 RMK CF2SC3AC3 SLP141",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1014.1);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(3);
                        }),

                arguments("CYYQ-S8DoesNotBlockSLP002",
                        "2017/04/10 00:00 CYYQ 151100Z 34015G30KT 1/2SM -SN BLSN VV005 M05/M07 A2975 RMK S8 SLP002",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("S8");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1000.2);
                        }),

                // BUG: CI0 (zero oktas) fails CLOUD_OKTA_PATTERN's [1-8] range; CI is
                // silently discarded (same as TCU above) and the orphaned "0" ends up
                // alone in freeText, disconnected from its origin. See finding #1 above.
                arguments("CYHZ-ZeroOktaCloudSilentlyDroppedOrphanedZeroInFreeText",
                        "2017/04/10 00:00 CYHZ 151100Z 00000KT 15SM BCFG FEW020 SCT100 SCT250 M00/M02 A3038 RMK CU1AS2CI0 SLP299",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("0");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1029.9);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(2);
                            // NOTE: CI (0 oktas) is silently lost here — documenting current
                            // (buggy) behavior until #<new issue> is fixed.
                        }),

                arguments("CYQX-F8DoesNotBlockSLP146",
                        "2017/04/10 00:00 CYQX 151100Z 30007KT 1/8SM FZFG VV001 M02/M03 A2998 RMK F8 SLP146",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F8");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1014.6);
                        }),

                // Confirms TSE09/UPE12-style leading segments now parse correctly; the
                // chained continuation (B29E31...) without a restated weather type is a
                // separate, still-open gap. See finding #2 above.
                arguments("KARB-FirstChainedEventParsesRestSurvivesAsFreeText",
                        "2011/01/30 12:30 KARB 301153Z 35022G31KT 10SM -RA BKN017 BKN025 OVC039 02/M01 A2948 " +
                                "RMK A02 PK WND 33035/1142 UPE12B29E31RAB12SNB15E20 SLP987 P0017 60043 70065 T00171011 10022 20017 56010 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("B29E31RAB12SNB15E20");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(998.7);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(1.7);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(-1.1);
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();
                        }),

                arguments("KBUF2016-PRESFRNowParsesCorrectly",
                        "2016/12/11 20:54 KBUF 112054Z 14007KT 1 3/4SM -SN BR OVC028 M03/M06 A3016 " +
                                "RMK A02 PRESFR SLP225 P0000 60003 T10331056 58033 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("PRESFR now has a dedicated handler and should no longer appear in freeText")
                                    .isNull();
                            assertThat(data.getRemarks().pressureRapidChange()).isEqualTo(PressureRapidChange.FALLING);
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1022.5);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(-3.3);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(-5.6);
                            assertThat(data.getRemarks().sixHourPrecipitation()).isNotNull();
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();
                        }),

                arguments("KSAV-LTGDoesNotBlockThunderstormAndTemp",
                        "2019/07/08 00:14 KSAV 080014Z 17009KT 7SM -RA FEW070 SCT090 BKN110 24/23 A2993 " +
                                "RMK AO2 LTG DSNT N TSE2356 CB DSNT N-SE P0005 T02390228",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("LTG DSNT N");
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(23.9);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(22.8);
                        }),

                arguments("CXOL-AllTokensParse",
                        "2024/04/12 15:00 CXOL 121500Z AUTO 33001KT 04/M03 RMK AO1 T00421032",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isNull();
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(4.2);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(-3.2);
                        }),

                arguments("KBUF2026-AllTokensParse",
                        "2026/09/14 20:54 KBUF 142054Z 25006KT 10SM FEW041 BKN060 18/09 A3023 RMK AO2 SLP237 T01830089 50003 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1023.7);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(18.3);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(8.9);
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();
                        }),

                arguments("KBBF-OldFormatA01Alone",
                        "2020/07/25 09:45 KBBF 250945Z AUTO 09048G59KT 1SM HZ SCT003 OVC015 27/25 A2947 RMK A01",
                        (Consumer<NoaaMetarData>) data -> assertThat(data.getRemarks().freeText()).isNull())
        );
    }

    /**
     * Diagnostic tool, not a regression test — prints actual parsed field
     * values for a set of raw METAR strings so expected values for new
     * {@code @ParameterizedTest} cases can be read off real output rather
     * than hand-traced. Add candidate raw strings to {@code raws}, run this
     * method alone, and use the console output to write locked-in assertions
     * in {@code handlesUnrecognizedTokensWithoutLosingLaterRemarks} above.
     * Has no assertions by design.
     */
    @Test
    void printRemarksParsingDiagnostics() {
        // Example entry — replace or add to this list with whatever you are currently tracing.
        // Run via:
        // mvn test -pl noakweather-platform/weather-processing -am -Dtest=NoaaMetarParserRemarksRecoveryTest#printRemarksParsingDiagnostics -Dsurefire.failIfNoSpecifiedTests=false
        // then check noakweather-platform/logs/noakweather.log for output.
        String[] raws = {
                "2020/06/05 22:04 KCLT 052204Z 18010KT 10SM FEW035 SCT041TCU SCT065 BKN250 28/21 A2989 RMK AO2 F8 SLP998 CU1AS2CI0 PK WND 33035/1142 PRESRR ICG PAST HR LTG DSNT NE-SE OCNL LTGICCC DSNT E TS DSNT E MOV E CB DSNT E TCU N-NE AND NW T02780206 $",
        };

        for (String raw : raws) {
            NoaaMetarData data = parse(raw);
            LOGGER.info("=== {} ===", truncate(raw));
            LOGGER.info("  freeText: {}", data.getRemarks() != null ? data.getRemarks().freeText() : "n/a");
            LOGGER.info("  pressureRapidChange: {}", data.getPressureRapidChange());
            LOGGER.info("  icing: {}", data.getRemarks() != null ? data.getRemarks().icing() : "n/a");
            LOGGER.info("  seaLevelPressure: {}", data.getSeaLevelPressure());
            LOGGER.info("  preciseTemperature: {}", data.getRemarks() != null ? data.getRemarks().preciseTemperature() : "n/a");
            LOGGER.info("  thunderstormLocations: {}", data.getRemarks() != null ? data.getRemarks().thunderstormLocations() : "n/a");
            LOGGER.info("  cloudTypes: {}", data.getRemarks() != null ? data.getRemarks().cloudTypes() : "n/a");
            LOGGER.info("  maintenanceRequired: {}", data.getRemarks() != null ? data.getRemarks().maintenanceRequired() : "n/a");
            LOGGER.info(" ");
        }
    }

    private static String truncate(String s) {
        return s.length() <= 40 ? s : s.substring(0, 40) + "...";
    }
}
