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
 */
public record ObservationProgramStatus(
        boolean staffed,
        int day,
        int hour,
        int minute
) {

    /**
     * Factory method matching the {@code of(...)} convention used
     * elsewhere in the remarks model (e.g. {@code Icing.of},
     * {@code PressureRapidChange.of}).
     *
     * @param staffed true if the STFD qualifier is present (24-hour staffed program)
     * @param day     day of month for the next observation
     * @param hour    hour (UTC) for the next observation
     * @param minute  minute for the next observation
     * @return the constructed ObservationProgramStatus
     */
    public static ObservationProgramStatus of(boolean staffed, int day, int hour, int minute) {
        return new ObservationProgramStatus(staffed, day, hour, minute);
    }

    /**
     * @return a human-readable summary, e.g. "Staffed, next observation day 26 at 12:00 UTC"
     */
    public String getSummary() {
        String staffedPart = staffed ? "Staffed" : "Not staffed";
        return String.format("%s, next observation day %02d at %02d:%02d UTC", staffedPart, day, hour, minute);
    }
}
