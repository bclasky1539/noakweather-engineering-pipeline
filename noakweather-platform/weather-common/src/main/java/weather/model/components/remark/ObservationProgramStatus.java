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
package weather.model.components.remark;

/**
 * Canadian MANOBS observation-program status remark ({@code LAST STFD
 * OBS/NEXT <time>} or {@code LAST OBS/NEXT <time>}). Indicates whether
 * the reporting station is staffed and when the next observation will
 * be issued.
 * <p>
 * The {@code STFD} qualifier is present for 24-hour staffed programs
 * and absent for stations with a less-than-24-hour observation
 * program. The next-observation time is stored as day/hour/minute
 * components, mirroring the day-time group format it's encoded in.
 * The reported time-zone suffix ({@code Z}, {@code UTC}, or
 * {@code " UTC"}) carries no independent meaning — MANOBS times are
 * always UTC — so it is not preserved.
 * <p>
 * An optional trailing {@code VIA <station-id>} suffix indicates the
 * next observation will be relayed via the named station, rather than
 * issued by the reporting station itself. {@code null} when absent.
 */
public record ObservationProgramStatus(
        boolean staffed,
        int day,
        int hour,
        int minute,
        String viaStation
) {

    /**
     * Factory method matching the {@code of(...)} convention used
     * elsewhere in the remarks model (e.g. {@code Icing.of},
     * {@code PressureRapidChange.of}).
     *
     * @param staffed    true if the STFD qualifier is present (24-hour staffed program)
     * @param day        day of month for the next observation
     * @param hour       hour (UTC) for the next observation
     * @param minute     minute for the next observation
     * @param viaStation station the next observation will be relayed via, or null if absent
     * @return the constructed ObservationProgramStatus
     */
    public static ObservationProgramStatus of(boolean staffed, int day, int hour, int minute, String viaStation) {
        return new ObservationProgramStatus(staffed, day, hour, minute, viaStation);
    }

    /**
     * @return true if a VIA relay station is present
     */
    public boolean hasViaStation() {
        return viaStation != null;
    }

    /**
     * @return a human-readable summary, e.g. "Staffed, next observation day 26 at 12:00 UTC"
     *         or "Staffed, next observation day 02 at 12:00 UTC via CYQB" when a via station is present
     */
    public String getSummary() {
        String staffedPart = staffed ? "Staffed" : "Not staffed";
        String base = String.format("%s, next observation day %02d at %02d:%02d UTC", staffedPart, day, hour, minute);
        return hasViaStation() ? base + " via " + viaStation : base;
    }
}
