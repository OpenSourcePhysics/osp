package org.opensourcephysics.tools;

/** Optional live host metadata. No parsing of names and no serialized state.
 * A future per-observation uncertainty provider can be added separately.
 */
public interface FitMetadataProvider {
    /** Null means no host metadata: retain DataTable's existing units. */
    String getUnits(String columnName);
    /** Position component (for example x or y) when the columns are position vs time.
     * Null disables physical derivative labels. Hosts identify columns from metadata.
     */
    default String getPositionComponent(String independentColumn, String dependentColumn) { return null; }
    /** Physical y units per image pixel, only for a directly calibrated position.
     * Return NaN for derived data, missing calibration, or varying conversion.
     */
    double getYUnitsPerPixel(String columnName);
}
