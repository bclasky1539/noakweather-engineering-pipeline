/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package weather.processing.parser.noaa;

import java.util.regex.Pattern;

/**
 * Class representing the regular expressions used for decoding Metar and
 * TAF data
 * 
 * @author bclasky1539
 * 
 */
public final class RegExprConst {

    // Private constructor to prevent instantiation
    private RegExprConst() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    // Weather string value for Remarks ('RMK')
    public static final String EXTENDED_REMARKS = "RMK";

    /**
     * TAF string pattern
     */
    public static final Pattern TAF_STR_PATTERN = Pattern.compile(
            "^(?<type>TAF)\\s+"
    );

    /**
     * Month, Day and Year (optional at start of METAR/TAF)
     * Example: "2024/11/02 16:51"
     */
    // "^(?<year>\\d\\d\\d\\d)/(?<month>\\d\\d)/(?<day>\\d\\d) (?<time>\\d\\d:\\d\\d)?\\s+"
    public static final Pattern MONTH_DAY_YEAR_PATTERN = Pattern.compile(
            "^(?<year>\\d{4})/(?<month>\\d{2})/(?<day>\\d{2})\\s+(?<time>\\d{2}:\\d{2})?\\s+"
    );

    /**
     * Station, Day, Time and Valid Time Period
     * Example: "KJFK 121851Z" or "EGLL 121820Z 1218/1318"
     * Complexity is required to capture all optional components of station/time format
     */
    // ^(?<station>[A-Z][A-Z0-9]{3}) (?<zday>\\d\\d)(?<zhour>\\d\\d)(?<zmin>\\d\\d)Z\\s?((?<bvaltime>\\d\\d\\d\\d)/(?<evaltime>\\d\\d\\d\\d))?\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for METAR station/time format
    public static final Pattern STATION_DAY_TIME_VALTMPER_PATTERN = Pattern.compile(
            "^(?<station>[A-Z][A-Z0-9]{3})\\s+(?<zday>\\d{2})(?<zhour>\\d{2})(?<zmin>\\d{2})Z\\s?((?<bvaltime>\\d{4})/(?<evaltime>\\d{4}))?\\s+"
    );

    /**
     * Report Modifier (AUTO, COR, AMD, etc.)
     * Example: "AUTO", "COR", "AMD"
     */
    public static final Pattern REPORT_MODIFIER_PATTERN = Pattern.compile(
            "^(?<mod>AMD|AUTO|FINO|NIL|TEST|CORR?|RTD|CC[A-G])\\s+"
    );

    /**
     * Wind, Wind Variability
     * Example: "28016KT", "VRB03KT", "18016G28KT 180V240"
     * Complexity is required to capture wind direction, speed, gusts, units, and variability
     */
    @SuppressWarnings("java:S5843") // Complex regex required for wind format with all variations
    public static final Pattern WIND_PATTERN = Pattern.compile(
            "^(?<dir>\\d{3}|/{1,5}|MMM|VRB)(?<speed>\\d{2,3})?(?<inden>G(?<gust>\\d{2,3}))?(?<units>KTS?|LT|K|T|KMH|MPS)\\s?((?<varfrom>\\d{3})V(?<varto>\\d{3}))?\\s+"
    );

    /**
     * Visibility
     * Example: "10SM", "9999", "1 1/2SM", "CAVOK"
     * Complexity is required to handle statute miles, meters, CAVOK, fractions, and direction
     */
    // ^(?<vis>(?<dist>[MP]?\\d\\d\\d\\d|////)(?<dir>[NSEW][EW]?|NDV)?|(?<distu>[MP]?(\\d+|\\d\\d?/\\d\\d|\\d+\\s+\\d/\\d))(?<units>SM|KM|M|U)|NDV|CAVOK)\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for multiple visibility formats
    public static final Pattern VISIBILITY_PATTERN = Pattern.compile(
        "^(?<vis>(?<dist>[MP]?\\d{4}|////)(?<dir>[NSEW][EW]?|NDV)?|(?<distu>[MP]?(\\d+|\\d{1,2}/\\d{1,2}|\\d+\\s+\\d/\\d))(?<units>SM|KM|M|U)|NDV|CAVOK)\\s+"
    );

    /**
     * RVR Runway visual range
     * Example: "R28L/1200V1800FT", "R06/P6000FT"
     * Complexity is required to capture runway designator, visibility range, and units
     */
    // ^(RVRNO|R(?<name>\\d\\d(?<inden>RLC)?))/(?<low>[MP]?(?<lvalue>CLRD|\\d{1,4}))(V(?<high>[MP]?\\d\\d\\d\\d))?/?/?/?/?(?<unit>\\d{2,4}|FT|N|D|U)\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for runway visual range format
    public static final Pattern RUNWAY_PATTERN = Pattern.compile(
        "^(RVRNO|R(?<name>\\d{2}(?<inden>[RLC])?))/(?<low>[MP]?(?<lvalue>CLRD|\\d{1,4}))(V(?<high>[MP]?\\d{4}))?/?/?/?/?(?<unit>\\d{2,4}|FT|N|D|U)?\\s+"
    );

    /**
     * Present weather
     * Example: "-RA BR", "+TSRA", "VCFG"
     * Complexity is required to capture intensity, descriptor, precipitation, obscuration, and other phenomena
     */
    @SuppressWarnings("java:S5843") // Complex regex required for weather phenomena combinations
    public static final Pattern PRESENT_WEATHER_PATTERN = Pattern.compile(
            "^(?<int>(VC|-|\\+)*)(?<desc>(MI|PR|BC|DR|BL|SH|TS|FZ)+)?(?<prec>(DZ|RA|SN|SG|IC|PL|GR|GS|UP|/)*)(?<obsc>BR|FG|FU|VA|DU|SA|HZ|PY)?(?<other>PO|SQ|FC|SS|DS|NSW|/+)?(?<int2>[-+])?\\s+"
    );

    /**
     * Sky condition (cloud layers)
     * Example: "FEW250", "SCT100", "BKN050CB", "OVC020"
     */
    @SuppressWarnings("java:S5843") // Complex regex required for weather phenomena combinations
    public static final Pattern SKY_CONDITION_PATTERN = Pattern.compile(
            "^(?<cover>VV|CLR|SKC|SCK|NSC|NCD|BKN|SCT|FEW|[O0]VC|///)(?<height>[\\dO]{2,4}|///)?(?<cloud>([A-Z][A-Z]+|///))?\\s+"
    );

    /**
     * Temperature and Dewpoint
     * Example: "22/12", "M05/M12", "15/", "//12"
     * M prefix indicates negative temperature
     */
    @SuppressWarnings("java:S5843") // Complex regex required for weather phenomena combinations
    public static final Pattern TEMP_DEWPOINT_PATTERN = Pattern.compile(
            "^((?<signt>[-M])?(?<temp>\\d+)|//|XX|MM)/((?<signd>[-M])?(?<dewpt>\\d+)|//|XX|MM)?\\s+"
    );

    /**
     * Altimeter setting (pressure)
     * US: "A3015" (30.15 inHg)
     * ICAO: "Q1013" (1013 hPa)
     * Also: "QNH1013"
     */
    public static final Pattern ALTIMETER_PATTERN = Pattern.compile(
            "^(?<unit>A{1,2}|Q|QNH)?(?<press>[\\dO]{3,4}|////)(?<unit2>INS)?\\s+"
    );

    /**
     * No Significant Change (NOSIG)
     */
    public static final Pattern NO_SIG_CHANGE_PATTERN = Pattern.compile(
            "^(?<nosigchng>NOSIG)\\s+"
    );

    /**
     * Funnel Cloud (Tornadic activity_B/E(hh)mm_LOC/DIR_(MOV)). At manual
     * stationS, tornadoes, funnel clouds, or waterspouts shall be coded in the
     * format, TORNADIC ACTIVITY_B/E(hh)mm_LOC/DIR_(MOV)
     * Complexity is required to capture type, begin/end time, location, and movement
     */
    public static final Pattern TRN_FC_WSP_PATTERN = Pattern.compile(
            "^(?<type>TORNADO|FUNNEL CLOUD|WATERSPOUT) (?<betime>BE)(?<time>\\d+) (?<dirfrom>\\w) (?<verb>\\w+) (?<dirto>\\w)?\\s+"
    );

    /**
     * Type of Automated Station (AO1 or AO2). AO1 or AO2 shall be coded in all
     * METAR/SPECI from automated stations.
     */
    public static final Pattern AUTO_PATTERN = Pattern.compile(
            "^A[O0](?<type>\\d)\\s+"
    );

    /**
     * Beginning and End of Precipitation. Example RAB20E51
     * Complexity is required to capture weather type and begin/end times
     */
    // ^(?<int>(VC|-|\\+)*)(?<desc>(MI|PR|BC|DR|BL|SH|TS|FZ)+)?(?<prec>(DZ|RA|SN|SG|IC|PL|GR|GS|UP|/)*)(?<obsc>BR|FG|FU|VA|DU|SA|HZ|PY)?(?<other>PO|SQ|FC|SS|DS|NSW|/+)?(?<int2>[-+])?((?<begin>B)(?<begint>\\d\\d)*)?((?<end>E)(?<endt>\\d\\d)*)
    @SuppressWarnings("java:S5843") // Complex regex required for precipitation timing format
    public static final Pattern BEGIN_END_WEATHER_PATTERN = Pattern.compile(
            "^(?<int>(VC|-|\\+)*)(?<desc>(MI|PR|BC|DR|BL|SH|TS|FZ)+)?(?<prec>(DZ|RA|SN|SG|IC|PL|GR|GS|UP|/)*)(?<obsc>BR|FG|FU|VA|DU|SA|HZ|PY)?(?<other>PO|SQ|FC|SS|DS|NSW|/+)?(?<int2>[-+])?((?<begin>B)(?<begint>\\d{2})*)?((?<end>E)(?<endt>\\d{2})*)"
    );

    /**
     * Sea-Level Pressure (SLPppp). At designated stations, the sea-level
     * pressure shall be coded in the format SLPppp
     * Example: "SLP210" = 1021.0 hPa
     */
    public static final Pattern SEALVL_PRESS_PATTERN = Pattern.compile(
            "^(?<type>SLP)(?<press>\\d{3}|NO)?\\s+"
    );

    /**
     * Peak Wind (PK_WND_dddff(f)/(hh)mm). The peak wind shall be coded in the
     * format, PK_WND_dddff(f)/(hh)mm of the next METAR
     * Example: "PK WND 28032/15"
     * Complexity is required to capture direction, speed, and time
     */
    public static final Pattern PEAK_WIND_PATTERN = Pattern.compile(
            "^PK\\s+WND\\s+(?<dir>\\d{3})?(?<speed>P?\\d{2,3})/(?<hour>[01]\\d|2[0-3])?(?<min>\\d{2})?\\s+"
    );

    /**
     * Wind Shift (WSHFT_(hh)mm). A wind shift shall be coded in the format
     * WSHFT_(hh)mm
     * Example: "WSHFT 15 FROPA"
     */
    public static final Pattern WIND_SHIFT_PATTERN = Pattern.compile(
            "^WSHFT (?<hour>\\d{2})?(?<min>\\d{2})(\\s+(?<front>FROPA))?\\s+"
            
    );

    /**
     * Tower or Surface Visibility (TWR_VIS_vvvvv or SFC_VIS_vvvvv). Tower
     * visibility or surface visibility shall be coded in the formats,
     * TWR_VIS_vvvvv or SFC_VIS_vvvvv, respectively
     * Example: "TWR VIS 1 1/2"
     */
    // ^(?<type>TWR VIS|SFC VIS) (?<dist>\\d+\\s\\d/\\d|\\d\\d?/\\d\\d?|\\d{1,2})?\\s+
    public static final Pattern TWR_SFC_VIS_PATTERN = Pattern.compile(
            "^(?<type>TWR VIS|SFC VIS)\\s+(?<dist>\\d+\\s\\d/\\d|\\d{1,2}/\\d{1,2}|\\d{1,2})?\\s+"
    );
    
    /**
     * Variable Prevailing Visibility (VIS_vnvn vnvnVvxvxvx vxvx). Variable
     * prevailing visibility shall be coded in the format VIS_vn vnvnvnVvxvx
     * vxvxvx Sector Visibility (VIS_[DIR]_vvvvv){Plain Language]. The sector
     * visibility shall be coded in the format VIS_[DIR]_vvvvv Visibility At
     * Second Location (VIS_vvvvv_[LOC]). At designated automated stations, the
     * visibility at a second location shall be coded in the format
     * VIS_vvvvv_[LOC] visibility shall be coded in the formats, TWR_VIS_vvvvv
     * or SFC_VIS_vvvvv, respectively
     */
    // ^(?<vis>VIS) (?<dir>([NSEW][EW]))?\\s*(?<dist1>\\d\\d?/\\d\\d?|\\d+\\s+\\d\\d?/\\d\\d?|\\d+)?(\\s*(?<add>V|RWY)\\s*(?<dist2>\\d\\d?/\\d\\d?|\\d+\\s+\\d\\d?/\\d\\d?|\\d+))?\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for variable visibility formats
    public static final Pattern VPV_SV_VSL_PATTERN = Pattern.compile(
            "^(?<vis>VIS)\\s+(?<dir>([NSEW][EW]))?\\s*(?<dist1>\\d{1,2}/\\d{1,2}|\\d+\\s+\\d{1,2}/\\d{1,2}|\\d+)?(\\s*(?<add>V|RWY)\\s*(?<dist2>\\d{1,2}/\\d{1,2}|\\d+\\s+\\d{1,2}/\\d{1,2}|\\d+))?\\s+"
    );
    
    /**
     * Lightning (Frequency_LTG(type)_[LOC]).
     * Example: "OCNL LTGICCG VC"
     * Complexity is required to capture frequency, types, and location
     */
    @SuppressWarnings("java:S5843") // Complex regex required for lightning format
    public static final Pattern LIGHTNING_PATTERN = Pattern.compile(
            "^(?:(?<freq>OCNL|FRQ|CONS)\\s+)?LTG(?<types>(?:IC|CC|CG|CA|CW)+)?\\s+(?<loc>OHD|VC|DSNT)(?:\\s+(?<dir>[NSEW]{1,2})(?:-(?<dir2>[NSEW]{1,2}))?)?\\s+"
    );

    /**
     * Thunderstorm Location (TS_LOC_(MOV_DIR)) [Plain Language].
     * Thunderstorm(s) shall be coded in the format, TS_LOC_(MOV_DIR)
     * Significant Cloud Types - Cumulonimbus (CB), Towering Cumulus (TCU),
     * Altocumulus Castellanus (ACC) Cumulonimbus Mammatus (CBMAM), Virga (VIRGA)
     * Example: "TS SE MOV E", "CB OHD", "TCU DSNT S"
     * Complexity is required to capture cloud type, location, and movement
     */
    // ^(?<type>TS|CB|TCU|ACC|CBMAM|VIRGA)\\s*(?<loc>(OHD|VC|DSNT|DSIPTD|TOP|TR)?)\\s*((?<dir>[NSEW][EW]?)-?(?<dir2>[NSEW][EW]?)*)?(\\s*MOV\\s*(?<dirm>[NSEW][EW]?))?\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for cloud location format
    public static final Pattern TS_CLD_LOC_PATTERN = Pattern.compile(
        "^(?<type>TS|CB|TCU|ACC|CBMAM|VIRGA)\\s*(?<loc>OHD|VC|DSNT|DSIPTD|TOP|TR)?\\s*(?:(?<dir>[NSEW]{1,2})(?:-(?<dir2>[NSEW]{1,2}))?)?(?:\\s*MOV\\s*(?<dirm>[NSEW]{1,2}))?\\s+"
    );

    /**
     * Pressure Rising or Falling Rapidly (PRESRR or PRESFR)
     */
    public static final Pattern PRES_RF_RAPDLY_PATTERN = Pattern.compile(
            "^PRES(?<presrisfal>\\w)\\w\\s+"
    );

    /**
     * Icing
     * Complexity is required to capture icing type and additional information
     */
    // ^(?<type>ICG)((?<typeic>IC)?(?<typeip>IP)?)\\s(?<extra>\\w\\w\\w\\w\\s\\w\\w)\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for icing format
    public static final Pattern ICING_PATTERN = Pattern.compile(
            "^(?<type>ICG)((?<typeic>IC)?(?<typeip>IP)?)\\s(?<extra>\\w{4}\\s\\w{2})\\s+"
    );
    
    /**
     * 6-hour maximum and minimum temperature in tenths degrees C format; 1sTTT
     * and 2sTTT
     * Example: "10142" or "20012"
     */
    public static final Pattern TEMP_6HR_MAX_MIN_PATTERN = Pattern.compile(
            "^(?<type>[12])(?<sign>[01])(?<temp>\\d{2,3})\\s+"
    );

    /**
     * Hourly Precipitation Amount (Prrrr). At designated automated stations,
     * the hourly precipitation amount shall be coded in the format, Prrrr
     * Example: "P0015" = 0.15 inches
     */
    public static final Pattern PRECIP_1HR_PATTERN = Pattern.compile(
            "^(?<type>P)(?<precip>\\d\\d\\d\\d)\\s+"
    );

    /**
     * 3-hour pressure tendency
     * Example: "52032"
     */
    // ^(?<type>5)(?<tend>[0-8])(?<press>\\d\\d\\d)\\s+
    public static final Pattern PRESS_3HR_PATTERN = Pattern.compile(
            "^(?<type>5)(?<tend>[0-8])(?<press>\\d{3})\\s+"
    );

    /**
     * 3- and 6-hour Precipitation (6RRRR). At designated stations, the 3- and
     * 6-hourly precipitation group shall be coded in the format 6RRRR 24-Hour
     * Precipitation Amount (7R24R24 R24R24). At designated stations, the
     * 24-hour precipitation amount shall be coded in the format, 7R24R24R24R24
     * Example: "60015" or "70123"
     */
    public static final Pattern PRECIP_3HR_24HR_PATTERN = Pattern.compile(
            "^(?<type>[67])(?<precip>\\d{1,5}|/{1,5})\\s+"
    );

    /**
     * Hourly Temperature and Dew Point (TsnT'T'T'snT'dT'dT'd). At designated
     * stations, the hourly temperature and dew point group shall be coded to
     * the tenth of a degree Celsius in the format, TsnT'T'T'snT'dT'dT'd
     * Example: "T02330139" = temp 23.3°C, dewpoint 13.9°C
     */
    public static final Pattern TEMP_1HR_PATTERN = Pattern.compile(
            "^(?<type>T)(?<tsign>[01])(?<temp>\\d{3})((?<dsign>[01])(?<dewpt>\\d{3}))?\\s+"
    );

    /**
     * 24-Hour Maximum and Minimum Temperature 4snTxTxTxsnTnTnTn; tenth of
     * degree Celsius; reported at midnight local standard time; 1 if
     * temperature below 0°C and 0 if temperature 0°C or higher, e.g.,
     * 400461006.
     * Example: "400461006"
     */
    public static final Pattern TEMP_24HR_PATTERN = Pattern.compile(
            "^(?<type>4)(?<maxsign>01)(?<maxtemp>\\d{3})((?<minsign>01)(?<mintemp>\\d{3}))\\s+"
    );

    /**
     * Density Altitude Example DENSITY ALT 800FT
     * Example: "DENSITY ALT 800FT"
     */
    public static final Pattern DENSITY_ALTITUDE_PATTERN = Pattern.compile(
            "^(?<type>DENSITY ALT) (?<denalt>\\d{1,5})(?<units>FT)\\s+"
    );

    /**
     * Sky Conditions FEW = 1 to 2 oktas; SCT (Scattered) = 3 to 4 oktas; BKN
     * (Broken) = 5 to 7 oktas; OVC (Overcast) = 8 oktas;
     * Examples AC8SC1, CI TR, MDT CU OHD-ALQDS
     * Complexity is required to capture cloud type, coverage in oktas, and location    
     */
    @SuppressWarnings("java:S5843") // Complex regex required for okta cloud format
    public static final Pattern CLOUD_OKTA_PATTERN = Pattern.compile(
             "^(?<intensity>MDT\\s+)?(?<cloud>(CU|CF|ST|SC|SF|NS|AS|AC|CS|CC|CI))(?<okta>[1-8](?=\\s|$))?((\\s*(?<verb>MOVG)\\s*(?<dirm>[NSEW][EW]?))|(\\s+(?<direction>OHD-ALQDS|ALQDS|OHD)))?"
    );

    /**
     * Last Observation
     * Example: LAST STFD OBS
     */
    public static final Pattern LAST_OBS_PATTERN = Pattern.compile(
            "^(?<last>LAST)\\s+"
    );

    /**
     * Pressure (Q Codes) - QFE=Q-Field Elevation QNH=Q-Normal Height
     * QNE=Q-Normal Elevation QFE747/996, It is 744 mm of mercury = 996
     * millibars
     * Example: "QFE747/996", "QNH1013"
     */
    // ^(?<pressq>QFE|QNH|QNE)((?<pressmm>\\d{3,4})?(/(?<pressmb>\\d{3,4}))?)?\\s+
    public static final Pattern PRESS_Q_PATTERN = Pattern.compile(
            "^(?<pressq>QFE|QNH|QNE)(?:(?<pressmm>\\d{3,4})(?:/(?<pressmb>\\d{3,4}))?)?\\s+"
);

    /**
     * Automated Maintenance Data RVRNO: RVR missing; PWINO: precipitation
     * identifier information not available; PNO: precipitation amount not
     * available; FZRANO: freezing rain information not available; TSNO:
     * thunderstorm information not available (may indicate augmenting weather
     * observer not logged on); VISNO [LOC]: visibility at second location not
     * available, e.g. VISNO RWY06; CHINO [LOC]: (cloud-height- indicator) sky
     * condition at secondary location not available, e.g., CHINO RWY06. $:
     * Maintenance check indicator ASOS requires maintenance
     * Example: "RVRNO", "TSNO", "VISNO RWY06", "$"
     * Complexity is required to capture maintenance indicator type and optional location
     */
    // ^(?<typeam>|RVRNO|PWINO|PNO|FZRANO|TSNO|VISNO|CHINO)\\s(?<loc>\\w+\\d+)?|(?<typemc>\\$)\\s+
    @SuppressWarnings("java:S5843") // Complex regex required for maintenance data format
    public static final Pattern AUTOMATED_MAINTENANCE_PATTERN = Pattern.compile(
            "^(?:(?<typeam>RVRNO|PWINO|PNO|FZRANO|TSNO|VISNO|CHINO)(?:\\s+(?<loc>\\w+\\d+))?|(?<typemc>\\$))\\s+"
    );
    
    /**
     * Groups - BECMG - Becoming, TEMPO - Temporary, PROB - Probability forecasts
     * Complexity is required to capture group type and all observation data
     * Examples: "BECMG 18016KT 10SM FEW250 ", "TEMPO +TSRA BKN020CB ", "PROB30 SHRA "
     */
    // ^(?<group>BECMG|TEMPO|PROB\\d{2}) (?<obs>(\\S+\\s){1,})
    @SuppressWarnings("java:S5843") // Complex regex required for TAF group format
    public static final Pattern GROUP_BECMG_TEMPO_PROB_PATTERN = Pattern.compile(
            "^(?<group>BECMG|TEMPO|PROB\\d{2})\\s+(?<obs>.+?)(?=\\s*$)"
    );
    
    /**
     * Group - FM - From
     * Example: "FM121600"
     */
    // ^(?<group>FM)(?<daytime>\\d{6}) (?<obs>(\\S+\\s){1,})
    public static final Pattern GROUP_FM_PATTERN = Pattern.compile(
            "^(?<group>FM)(?<daytime>\\\\d{6})\\\\s+(?<obs>.+?)(?=\\\\s*$)"
    );

    /**
     * Valid Time Period
     * Example: "1218/1318"
     */
    public static final Pattern VALTMPER_PATTERN = Pattern.compile(
            "^(?<bvaltime>\\d{4})/(?<evaltime>\\d{4})\\s+"
    );

    /**
     * Next Forecast By
     * Example: "NXT FCST BY 121600Z"
     */
    public static final Pattern NXT_FCST_BY_PATTERN = Pattern.compile(
            "^(?<type>NXT FCST BY) (?<zday>\\d{2})(?<zhour>\\d{2})(?<zmin>\\d{2})Z\\s+"
    );

    /**
     * Snow on Ground
     * Example: "SOG 5"
     */
    public static final Pattern SNOW_ON_GRND_PATTERN = Pattern.compile(
            "^(?<type>SOG) (?<amt>\\d{1,3})\\s+"
    );

    /**
     * Unparsed token (catch-all)
     */
    public static final Pattern UNPARSED_PATTERN = Pattern.compile(
            "^(?<unparsed>\\S+)\\s+"
    );
}
