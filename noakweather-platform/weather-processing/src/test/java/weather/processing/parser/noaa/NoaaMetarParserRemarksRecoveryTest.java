package weather.processing.parser.noaa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.model.components.remark.*;
import weather.model.components.remark.pressureremarks.PressureRapidChange;
import weather.model.enums.AutomatedStationType;
import weather.model.enums.HighCloudType;
import weather.model.enums.LowCloudType;
import weather.model.enums.MiddleCloudType;
import weather.processing.parser.common.ParseResult;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.assertj.core.api.Assertions.within;


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
                                    .as("LTG DSNT W now parses as a LightningRemark, and the CB DSNT W-NW " +
                                            "AND E MOV N chain fully parses as one ThunderstormLocation with " +
                                            "two direction segments and movement - nothing left unparsed")
                                    .isNull();
                            assertThat(data.getRemarks().lightningRemarks()).hasSize(1);
                            LightningRemark lightning = data.getRemarks().lightningRemarks().get(0);
                            assertThat(lightning.frequency()).isNull();
                            assertThat(lightning.types()).isEmpty();
                            assertThat(lightning.location()).isEqualTo("DSNT");
                            assertThat(lightning.directionSegment()).isEqualTo(new DirectionSegment(List.of("W")));
                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP151 must parse despite the earlier LTG clause")
                                    .isEqualTo(1015.1);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(29.4);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(23.9);
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            ThunderstormLocation cb = data.getRemarks().thunderstormLocations().get(0);
                            assertThat(cb.cloudType()).isEqualTo("CB");
                            assertThat(cb.locationQualifier()).isEqualTo("DSNT");
                            assertThat(cb.directionSegments())
                                    .containsExactly(
                                            new DirectionSegment(List.of("W", "NW")),
                                            new DirectionSegment(List.of("E"))
                                    );
                            assertThat(cb.movingDirection()).isEqualTo("N");
                            assertThat(data.getRemarks().weatherEvents())
                                    .as("TS ended :09, plus implied-continuation TS begin :13 end :46")
                                    .hasSize(2);
                            assertThat(data.getRemarks().weatherEvents().get(0).weatherCode()).isEqualTo("TS");
                            assertThat(data.getRemarks().weatherEvents().get(0).endMinute()).isEqualTo(9);
                            assertThat(data.getRemarks().weatherEvents().get(1).weatherCode()).isEqualTo("TS");
                            assertThat(data.getRemarks().weatherEvents().get(1).beginMinute()).isEqualTo(13);
                            assertThat(data.getRemarks().weatherEvents().get(1).endMinute()).isEqualTo(46);
                            assertThat(data.getRemarks().maintenanceRequired())
                                    .as("Trailing $ should parse despite everything ahead of it")
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
                arguments("KPHX-AndChainNowParsesCorrectly",
                        "2026/09/11 22:51 KPHX 112251Z 30011G17KT 10SM FEW090 FEW130 SCT250 " +
                                "41/17 A2972 RMK AO2 SLP041 CB DSNT N-E AND SE-S T04060167",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("CB DSNT N-E AND SE-S now fully parses as one ThunderstormLocation " +
                                            "with two direction-range segments")
                                    .isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1004.1);
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            ThunderstormLocation cb = data.getRemarks().thunderstormLocations().get(0);
                            assertThat(cb.cloudType()).isEqualTo("CB");
                            assertThat(cb.locationQualifier()).isEqualTo("DSNT");
                            assertThat(cb.directionSegments())
                                    .containsExactly(
                                            new DirectionSegment(List.of("N", "E")),
                                            new DirectionSegment(List.of("SE", "S"))
                                    );
                            assertThat(data.getRemarks().preciseTemperature())
                                    .as("T04060167 must survive the AND SE-S gap ahead of it")
                                    .isNotNull();
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(40.6);
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
                                    .as("Both LTG DSNT NE-SE and OCNL LTGICCC DSNT E now parse as " +
                                            "LightningRemarks, and TCU N-NE AND NW fully parses as one " +
                                            "ThunderstormLocation with two direction segments - nothing left unparsed")
                                    .isNull();
                            assertThat(data.getRemarks().lightningRemarks()).hasSize(2);
                            LightningRemark firstLtg = data.getRemarks().lightningRemarks().get(0);
                            assertThat(firstLtg.frequency()).isNull();
                            assertThat(firstLtg.types()).isEmpty();
                            assertThat(firstLtg.location()).isEqualTo("DSNT");
                            assertThat(firstLtg.directionSegment()).isEqualTo(new DirectionSegment(List.of("NE", "SE")));
                            LightningRemark secondLtg = data.getRemarks().lightningRemarks().get(1);
                            assertThat(secondLtg.frequency()).isEqualTo(LightningFrequency.OCCASIONAL);
                            assertThat(secondLtg.types()).containsExactly("IC", "CC");
                            assertThat(secondLtg.location()).isEqualTo("DSNT");
                            assertThat(secondLtg.directionSegment()).isEqualTo(new DirectionSegment(List.of("E")));
                            assertThat(data.getRemarks().icing()).isEqualTo(Icing.of(false, false, "PAST HR"));
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(27.8);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(20.6);
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(3);
                            ThunderstormLocation tcu = data.getRemarks().thunderstormLocations().get(2);
                            assertThat(tcu.cloudType()).isEqualTo("TCU");
                            assertThat(tcu.directionSegments())
                                    .containsExactly(
                                            new DirectionSegment(List.of("N", "NE")),
                                            new DirectionSegment(List.of("NW"))
                                    );
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();
                        }),

                arguments("CYOW-EMBDDAndLTGDoNotBlockSLP",
                        "2017/04/10 00:00 CYOW 160800Z 21004KT 8SM -TSRA BKN020 OVC100 20/18 A2966 " +
                                "RMK SC5AC3 CB EMBDD LTGCG SE SLP044",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("CB EMBDD now parses as a CloudType, and LTGCG SE now parses as " +
                                            "a LightningRemark - nothing left unparsed")
                                    .isNull();
                            assertThat(data.getRemarks().lightningRemarks()).hasSize(1);
                            LightningRemark lightning = data.getRemarks().lightningRemarks().get(0);
                            assertThat(lightning.frequency()).isNull();
                            assertThat(lightning.types()).containsExactly("CG");
                            assertThat(lightning.location()).isNull();
                            assertThat(lightning.directionSegment()).isEqualTo(new DirectionSegment(List.of("SE")));
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1004.4);
                            assertThat(data.getRemarks().cloudTypes())
                                    .as("SC5, AC3, and now CB EMBDD - three cloud types")
                                    .hasSize(3);
                            assertThat(data.getRemarks().cloudTypes().get(2).cloudType()).isEqualTo("CB");
                            assertThat(data.getRemarks().cloudTypes().get(2).location()).isEqualTo("EMBDD");
                            assertThat(data.getRemarks().thunderstormLocations())
                                    .as("CB is no longer claimed as a bare ThunderstormLocation")
                                    .isEmpty();
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

                arguments("CYVR-TCUEmbddNowParsesCorrectly",
                        "2017/04/10 00:00 CYVR 061843Z 09008KT 4SM -SHRA BR BKN006 BKN015 OVC040 RMK CF6SC2SC1 TCU EMBDD",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("TCU EMBDD now parses via the added EMBDD qualifier - no unparsed remnant")
                                    .isNull();
                            assertThat(data.getRemarks().cloudTypes())
                                    .as("All four cloud types now parse, including TCU EMBDD")
                                    .hasSize(4);
                            assertThat(data.getRemarks().cloudTypes().get(3).cloudType()).isEqualTo("TCU");
                            assertThat(data.getRemarks().cloudTypes().get(3).location()).isEqualTo("EMBDD");
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

                arguments("CYHZ-ZeroOktaCloudNowParsesCorrectly",
                        "2017/04/10 00:00 CYHZ 151100Z 00000KT 15SM BCFG FEW020 SCT100 SCT250 M00/M02 A3038 RMK CU1AS2CI0 SLP299",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("CI0 now parses via widened okta range [0-8] - no orphaned fragment")
                                    .isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1029.9);
                            assertThat(data.getRemarks().cloudTypes())
                                    .as("All three chained cloud types now parse, including CI0")
                                    .hasSize(3);
                            assertThat(data.getRemarks().cloudTypes().get(2).cloudType()).isEqualTo("CI");
                            assertThat(data.getRemarks().cloudTypes().get(2).oktas()).isZero();
                        }),

                arguments("CYZG-ObservationProgramStatusAllTokensParse",
                        "2026/09/25 21:00 CYZG 252100Z 17026KT 15SM -RA BKN024 BKN033TCU OVC095 06/05 A2975 " +
                                "RMK SC5TCU1ACC2 LAST STFD OBS/NEXT 261200Z PRESFR SLP093",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("LAST STFD OBS/NEXT now parses - no unparsed remnant")
                                    .isNull();
                            assertThat(data.getRemarks().observationProgramStatus()).isNotNull();
                            ObservationProgramStatus status = data.getRemarks().observationProgramStatus();
                            assertThat(status.staffed()).isTrue();
                            assertThat(status.day()).isEqualTo(26);
                            assertThat(status.hour()).isEqualTo(12);
                            assertThat(status.minute()).isZero();
                            assertThat(data.getRemarks().pressureRapidChange()).isEqualTo(PressureRapidChange.FALLING);
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1009.3);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(3);
                        }),

                arguments("CYKG-ObservationProgramStatusSpacedSlashAndUtc",
                        "2026/09/26 16:00 CYKG 261600Z 28020G26KT 15SM BKN024 BKN034 06/02 A2979 " +
                                "RMK SC5SC2 LAST STFD OBS / NEXT 271200 UTC SLP101",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("LAST STFD OBS / NEXT (spaced slash, space-separated UTC) now parses")
                                    .isNull();
                            assertThat(data.getRemarks().observationProgramStatus()).isNotNull();
                            ObservationProgramStatus status = data.getRemarks().observationProgramStatus();
                            assertThat(status.staffed()).isTrue();
                            assertThat(status.day()).isEqualTo(27);
                            assertThat(status.hour()).isEqualTo(12);
                            assertThat(status.minute()).isZero();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1010.1);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(2);
                        }),

                arguments("CYZG-ObservationProgramStatusViaStationNowParsesCorrectly",
                        "2026/10/01 14:00 CYZG 011400Z 19007KT 15SM FEW011 BKN030 BKN065 10/10 A2877 " +
                                "RMK ST2SC3SC2 LAST STFD OBS/NEXT 021200 UTC VIA CYQB SLP755 DENSITY ALT 1700FT",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("VIA CYQB now parses as the relay-station suffix on " +
                                            "ObservationProgramStatus - no unparsed remnant")
                                    .isNull();

                            assertThat(data.getRemarks().observationProgramStatus()).isNotNull();
                            ObservationProgramStatus status = data.getRemarks().observationProgramStatus();
                            assertThat(status.staffed()).isTrue();
                            assertThat(status.day()).isEqualTo(2);
                            assertThat(status.hour()).isEqualTo(12);
                            assertThat(status.minute()).isZero();
                            assertThat(status.viaStation()).isEqualTo("CYQB");

                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP755 must survive, immediately after VIA CYQB")
                                    .isEqualTo(975.5);
                            assertThat(data.getRemarks().densityAltitudeFeet())
                                    .as("DENSITY ALT 1700FT must survive, after SLP755")
                                    .isEqualTo(1700);

                            assertThat(data.getRemarks().cloudTypes()).hasSize(3);
                        }),

                arguments("TTPP-ExceptDirectionNowParsesCorrectly",
                        "2026/10/02 19:00 TTPP 021900Z 14006KT 090V180 9000 4000SE TSRA BKN010CB SCT018 28/26 Q1012 " +
                                "TEMPO 5000 TSRA RMK CB ALQDS XCPT N",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("XCPT N now parses as an except-direction remark - no unparsed remnant")
                                    .isNull();

                            assertThat(data.getRemarks().thunderstormLocations())
                                    .as("CB ALQDS should still parse correctly (confirmed via #99)")
                                    .hasSize(1);
                            ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
                            assertThat(location.cloudType()).isEqualTo("CB");
                            assertThat(location.locationQualifier()).isEqualTo("ALQDS");

                            assertThat(data.getRemarks().exceptDirections()).hasSize(1);
                            ExceptDirection exceptDirection = data.getRemarks().exceptDirections().get(0);
                            assertThat(exceptDirection.directionSegments())
                                    .containsExactly(new DirectionSegment(List.of("N")));

                            // TEMPO 5000 TSRA is a separate, unrelated finding (tracked under
                            // the TAF-work issue) - confirmed still present and unaffected
                            assertThat(data.getUnparsedMainBody())
                                    .as("Unrelated TEMPO finding should be unaffected by this fix")
                                    .contains("TEMPO 5000 TSRA");
                        }),

                arguments("CYQX-F8DoesNotBlockSLP146",
                        "2017/04/10 00:00 CYQX 151100Z 30007KT 1/8SM FZFG VV001 M02/M03 A2998 RMK F8 SLP146",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText()).isEqualTo("F8");
                            assertThat(data.getSeaLevelPressure()).isEqualTo(1014.6);
                        }),

                // Confirms TSE09/UPE12-style leading segments and the chained continuation (B29E31...)
                // now parse correctly.
                arguments("KARB-FullChainNowParsesIncludingImpliedContinuation",
                        "2011/01/30 12:30 KARB 301153Z 35022G31KT 10SM -RA BKN017 BKN025 OVC039 02/M01 A2948 " +
                                "RMK A02 PK WND 33035/1142 UPE12B29E31RAB12SNB15E20 SLP987 P0017 60043 70065 T00171011 10022 20017 56010 $",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("B29E31/RA/SN segments now all parse - nothing left unparsed")
                                    .isNull();
                            assertThat(data.getSeaLevelPressure()).isEqualTo(998.7);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(1.7);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(-1.1);
                            assertThat(data.getRemarks().maintenanceRequired()).isTrue();

                            assertThat(data.getRemarks().weatherEvents())
                                    .as("UP ended :12, implied-continuation UP begin :29 end :31, " +
                                            "RA begin :12, SN begin :15 end :20")
                                    .hasSize(4);

                            List<WeatherEvent> events = data.getRemarks().weatherEvents();
                            assertThat(events.get(0).weatherCode()).isEqualTo("UP");
                            assertThat(events.get(0).endMinute()).isEqualTo(12);
                            assertThat(events.get(1).weatherCode()).isEqualTo("UP");
                            assertThat(events.get(1).beginMinute()).isEqualTo(29);
                            assertThat(events.get(1).endMinute()).isEqualTo(31);
                            assertThat(events.get(2).weatherCode()).isEqualTo("RA");
                            assertThat(events.get(2).beginMinute()).isEqualTo(12);
                            assertThat(events.get(3).weatherCode()).isEqualTo("SN");
                            assertThat(events.get(3).beginMinute()).isEqualTo(15);
                            assertThat(events.get(3).endMinute()).isEqualTo(20);
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
                            assertThat(data.getRemarks().freeText())
                                    .as("LTG DSNT N now parses as a LightningRemark")
                                    .isNull();
                            assertThat(data.getRemarks().lightningRemarks()).hasSize(1);
                            LightningRemark lightning = data.getRemarks().lightningRemarks().get(0);
                            assertThat(lightning.location()).isEqualTo("DSNT");
                            assertThat(lightning.directionSegment()).isEqualTo(new DirectionSegment(List.of("N")));
                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(23.9);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(22.8);
                        }),

                arguments("KELP-LightningAndThunderstormLocationComposeCorrectly",
                        "2026/09/26 03:51 KELP 260351Z 14010KT 10SM FEW050 SCT085 SCT250 26/18 A3010 " +
                                "RMK AO2 SLP128 FRQ LTGICCG DSNT SE-S CB DSNT SE-S MOV NE T02560178",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("Both FRQ LTGICCG DSNT SE-S and the following CB DSNT SE-S MOV NE " +
                                            "should fully parse - confirms the lightning/thunderstorm-location " +
                                            "handler ordering fix resolves the same-pass exposure interaction")
                                    .isNull();

                            assertThat(data.getRemarks().lightningRemarks()).hasSize(1);
                            LightningRemark lightning = data.getRemarks().lightningRemarks().get(0);
                            assertThat(lightning.frequency()).isEqualTo(LightningFrequency.FREQUENT);
                            assertThat(lightning.types()).containsExactly("IC", "CG");
                            assertThat(lightning.location()).isEqualTo("DSNT");
                            assertThat(lightning.directionSegment()).isEqualTo(new DirectionSegment(List.of("SE", "S")));

                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            ThunderstormLocation cb = data.getRemarks().thunderstormLocations().get(0);
                            assertThat(cb.cloudType()).isEqualTo("CB");
                            assertThat(cb.locationQualifier()).isEqualTo("DSNT");
                            assertThat(cb.directionSegments())
                                    .containsExactly(new DirectionSegment(List.of("SE", "S")));
                            assertThat(cb.movingDirection()).isEqualTo("NE");

                            assertThat(data.getSeaLevelPressure()).isEqualTo(1012.8);
                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(25.6, within(0.1));
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(17.8, within(0.1));
                        }),

                arguments("KMIA-MultipleLightningRemarksAndThunderstormLocationComposeCorrectly",
                        "2011/01/30 12:53 KMIA 091253Z 17006KT 2SM +TSRA BKN065CB OVC095 14/11 A2986 " +
                                "RMK AO2 LTG DSNT ALQDS RAB01 SLP110 OCNL LTGICCC OHD TS OHD MOV E P0010 T01390106",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("Two independent lightning remarks (bare ALQDS, and frequency-qualified " +
                                            "chained-type overhead) plus the weather event, SLP, thunderstorm " +
                                            "location, precipitation, and precise temp should all parse cleanly")
                                    .isNull();

                            assertThat(data.getRemarks().lightningRemarks())
                                    .as("Should have 2 independent lightning remarks")
                                    .hasSize(2);

                            LightningRemark first = data.getRemarks().lightningRemarks().get(0);
                            assertThat(first.frequency()).isNull();
                            assertThat(first.types()).isEmpty();
                            assertThat(first.location()).isEqualTo("DSNT");
                            assertThat(first.allQuadrants()).isTrue();
                            assertThat(first.directionSegment()).isNull();

                            LightningRemark second = data.getRemarks().lightningRemarks().get(1);
                            assertThat(second.frequency()).isEqualTo(LightningFrequency.OCCASIONAL);
                            assertThat(second.types()).containsExactly("IC", "CC");
                            assertThat(second.location()).isEqualTo("OHD");
                            assertThat(second.allQuadrants()).isFalse();

                            assertThat(data.getRemarks().weatherEvents())
                                    .as("RAB01 - rain began :01")
                                    .hasSize(1);
                            assertThat(data.getRemarks().weatherEvents().get(0).weatherCode()).isEqualTo("RA");
                            assertThat(data.getRemarks().weatherEvents().get(0).beginMinute()).isEqualTo(1);

                            assertThat(data.getSeaLevelPressure()).isEqualTo(1011.0);

                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            ThunderstormLocation ts = data.getRemarks().thunderstormLocations().get(0);
                            assertThat(ts.cloudType()).isEqualTo("TS");
                            assertThat(ts.locationQualifier()).isEqualTo("OHD");
                            assertThat(ts.movingDirection()).isEqualTo("E");

                            assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();
                            assertThat(data.getRemarks().hourlyPrecipitation().inches()).isEqualTo(0.10, within(0.01));

                            assertThat(data.getRemarks().preciseTemperature().celsius()).isEqualTo(13.9, within(0.1));
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(10.6, within(0.1));
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

                arguments("PTYA-PredominantCloudTypeDoesNotBlockNeighboringRemarks",
                        "2026/09/26 02:55 PTYA 260255Z 26008KT 12SM SCT016CB BKN130 OVC300 27/26 A2989 " +
                                "RMK SHRAB09E30 CB S-W-N-NE MOV E SLP122 60016 8/378 T02720260 58015",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("8/378 now parses via the new predominant-cloud-type handler, " +
                                            "sandwiched between the 6-hour precip group (60016) ahead of it " +
                                            "and the T-group/pressure-tendency group (T02720260 58015) after it")
                                    .isNull();

                            assertThat(data.getRemarks().predominantCloudTypes()).isNotNull();
                            assertThat(data.getRemarks().predominantCloudTypes().lowCloud())
                                    .isEqualTo(LowCloudType.CUMULONIMBUS_CALVUS);
                            assertThat(data.getRemarks().predominantCloudTypes().middleCloud())
                                    .isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
                            assertThat(data.getRemarks().predominantCloudTypes().highCloud())
                                    .isEqualTo(HighCloudType.CIRROSTRATUS_NOT_INVADING_NOT_COVERING);

                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP122 must survive, ahead of the new 8/378 handler")
                                    .isEqualTo(1012.2);
                            assertThat(data.getRemarks().sixHourPrecipitation())
                                    .as("60016 must survive, immediately ahead of 8/378")
                                    .isEqualTo(new PrecipitationAmount(0.16, 6, false));
                            assertThat(data.getRemarks().preciseTemperature().celsius())
                                    .as("T02720260 must survive, immediately after 8/378")
                                    .isEqualTo(27.2);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(26.0);

                            assertThat(data.getRemarks().weatherEvents())
                                    .as("SHRAB09E30 - shower rain began :09 ended :30")
                                    .hasSize(1);
                            assertThat(data.getRemarks().thunderstormLocations())
                                    .as("CB S-W-N-NE MOV E should also parse independently of the 8/378 group")
                                    .hasSize(1);
                        }),

                arguments("NSTU-PredominantCloudTypeObscuredHighLayerDoesNotBlockNeighbors",
                        "2026/09/26 02:50 NSTU 260250Z 13011KT 12SM BKN015TCU BKN040 OVC100 28/24 A2992 " +
                                "RMK TCU NW-NE-SE SLP132 8/87/ T02800240 56012",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("8/87/ (obscured high layer) now parses via the new handler, " +
                                            "sandwiched between SLP132 ahead of it and the T-group/pressure-tendency " +
                                            "group (T02800240 56012) after it")
                                    .isNull();

                            assertThat(data.getRemarks().predominantCloudTypes()).isNotNull();
                            assertThat(data.getRemarks().predominantCloudTypes().lowCloud())
                                    .isEqualTo(LowCloudType.CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS);
                            assertThat(data.getRemarks().predominantCloudTypes().middleCloud())
                                    .isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
                            assertThat(data.getRemarks().predominantCloudTypes().highCloud())
                                    .isEqualTo(HighCloudType.OBSCURED);

                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP132 must survive, immediately ahead of 8/87/")
                                    .isEqualTo(1013.2);
                            assertThat(data.getRemarks().preciseTemperature().celsius())
                                    .as("T02800240 must survive, immediately after 8/87/")
                                    .isEqualTo(28.0);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(24.0);

                            assertThat(data.getRemarks().thunderstormLocations())
                                    .as("TCU NW-NE-SE should also parse independently of the 8/87/ group")
                                    .hasSize(1);
                        }),

                arguments("NSTU-AlqdsThunderstormLocationNowParsesCorrectly",
                        "2026/10/01 13:50 NSTU 011350Z 13019G24KT 8SM -SHRA BKN018TCU BKN040 OVC100 25/20 A2998 " +
                                "RMK TCU ALQDS SLP152 T02530204",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("TCU ALQDS now parses via the widened TS_CLD_LOC_PATTERN location-qualifier " +
                                            "alternation - ALQDS no longer falls through to freeText")
                                    .isNull();

                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
                            ThunderstormLocation tcu = data.getRemarks().thunderstormLocations().get(0);
                            assertThat(tcu.cloudType()).isEqualTo("TCU");
                            assertThat(tcu.locationQualifier())
                                    .as("ALQDS now flows through as the location qualifier, same as any other")
                                    .isEqualTo("ALQDS");
                            assertThat(tcu.directionSegments()).isEmpty();

                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP152 must survive, immediately after TCU ALQDS")
                                    .isEqualTo(1015.2);
                            assertThat(data.getRemarks().preciseTemperature().celsius())
                                    .as("T02530204 must survive, after SLP152")
                                    .isEqualTo(25.3);
                            assertThat(data.getRemarks().preciseDewpoint().dewpointCelsius()).isEqualTo(20.4);
                        }),

                arguments("KBLV-AugmentedAutomatedStationTypeDoesNotBlockSLP",
                        "2016/01/01 16:57 KBLV 011657Z AUTO 25015G30KT 210V290 3/8SM " +
                                "R32L/1000FT FG BKN005 01/M01 A2984 RMK A02A SLP034",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("A02A now parses via the widened AUTO_PATTERN (digit + optional " +
                                            "augmentation suffix), resolving to AO2A - nothing left unparsed")
                                    .isNull();

                            assertThat(data.getRemarks().automatedStationType())
                                    .isEqualTo(AutomatedStationType.AO2A);
                            assertThat(data.getRemarks().hasPrecipitationDiscriminator()).isTrue();
                            assertThat(data.getRemarks().automatedStationType().hasManualAugmentation())
                                    .as("A02A indicates manual augmentation by a human observer")
                                    .isTrue();

                            assertThat(data.getSeaLevelPressure())
                                    .as("SLP034 must survive, immediately after the augmented station type")
                                    .isEqualTo(1003.4);
                        }),

                arguments("KATL-SpaceSeparatedThunderstormContinuationNowParsesCorrectly",
                        "2026/10/02 18:52 KATL 021852Z 00000KT 10SM SCT028TCU SCT100 BKN180 BKN250 27/21 A3009 " +
                                "RMK AO2 SLP180 TCU NW DSNT N NE S SW MDT CU ALQDS T02720206",
                        (Consumer<NoaaMetarData>) data -> {
                            assertThat(data.getRemarks().freeText())
                                    .as("DSNT N NE S SW now parses as a continuation - no unparsed remnant")
                                    .isNull();

                            assertThat(data.getRemarks().thunderstormLocations()).hasSize(2);
                            ThunderstormLocation continuation = data.getRemarks().thunderstormLocations().get(1);
                            assertThat(continuation.cloudType()).isEqualTo("TCU");
                            assertThat(continuation.locationQualifier()).isEqualTo("DSNT");
                            assertThat(continuation.directionSegments()).containsExactly(
                                    new DirectionSegment(List.of("N")),
                                    new DirectionSegment(List.of("NE")),
                                    new DirectionSegment(List.of("S")),
                                    new DirectionSegment(List.of("SW"))
                            );

                            assertThat(data.getSeaLevelPressure()).isEqualTo(1018.0);
                            assertThat(data.getRemarks().cloudTypes()).hasSize(1);
                            assertThat(data.getRemarks().cloudTypes().get(0).location()).isEqualTo("ALQDS");
                            assertThat(data.getRemarks().preciseTemperature()).isNotNull();
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
                "2020/06/05 22:04 KCLT 052204Z 18010KT 10SM FEW035 SCT041TCU SCT065 BKN250 28/21 A2989 RMK AO2 F8 SLP998 CU1AS2CI0 TCU EMBDD " +
                        "PK WND 33035/1142 UPE12B29E31RAB12SNB15E20 PRESRR ICG PAST HR LTG DSNT NE-SE OCNL LTGICCC DSNT E TS DSNT E MOV E CB DSNT E TCU N-NE AND NW " +
                        "TCU ALQDS XCPT N-NE LAST STFD OBS/NEXT 021200 UTC VIA CYQB T02780206 DENSITY ALT 900FT $",
                "2026/10/02 18:52 KATL 021852Z 00000KT 10SM SCT028TCU SCT100 BKN180 BKN250 27/21 A3009 " +
                        "RMK AO2 SLP180 TCU NW DSNT N NE S SW MDT CU ALQDS T02720206",
        };

        for (String raw : raws) {
            NoaaMetarData data = parse(raw);
            LOGGER.info("=== {} ===", truncate(raw));
            LOGGER.info("*** {} ***", raw);
            LOGGER.info("  freeText: {}", data.getRemarks() != null ? data.getRemarks().freeText() : "n/a");
            LOGGER.info("  pressureRapidChange: {}", data.getPressureRapidChange());
            LOGGER.info("  icing: {}", data.getRemarks() != null ? data.getRemarks().icing() : "n/a");
            LOGGER.info("  automatedStationType: {}", data.getRemarks() != null ? data.getRemarks().automatedStationType() : "n/a");
            LOGGER.info("  seaLevelPressure: {}", data.getSeaLevelPressure());
            LOGGER.info("  preciseTemperature: {}", data.getRemarks() != null ? data.getRemarks().preciseTemperature() : "n/a");
            LOGGER.info("  sixHourPrecipitation: {}", data.getRemarks() != null ? data.getRemarks().sixHourPrecipitation() : "n/a");
            LOGGER.info("  thunderstormLocations: {}", data.getRemarks() != null ? data.getRemarks().thunderstormLocations() : "n/a");
            LOGGER.info("  exceptDirections: {}", data.getRemarks() != null ? data.getRemarks().exceptDirections() : "n/a");
            LOGGER.info("  cloudTypes: {}", data.getRemarks() != null ? data.getRemarks().cloudTypes() : "n/a");
            LOGGER.info("  weatherEvents: {}", data.getRemarks() != null ? data.getRemarks().weatherEvents() : "n/a");
            LOGGER.info("  maintenanceRequired: {}", data.getRemarks() != null ? data.getRemarks().maintenanceRequired() : "n/a");
            LOGGER.info("  observationProgramStatus: {}", data.getRemarks() != null ? data.getRemarks().observationProgramStatus() : "n/a");
            LOGGER.info("  lightningRemarks: {}", data.getRemarks() != null ? data.getRemarks().lightningRemarks() : "n/a");
            LOGGER.info("  predominantCloudTypes: {}", data.getRemarks() != null ? data.getRemarks().predominantCloudTypes() : "n/a");
            LOGGER.info("  densityAltitudeFeet: {}", data.getRemarks() != null ? data.getRemarks().densityAltitudeFeet() : "n/a");
            LOGGER.info("\n");
        }
    }

    private static String truncate(String s) {
        return s.length() <= 40 ? s : s.substring(0, 40) + "...";
    }
}
