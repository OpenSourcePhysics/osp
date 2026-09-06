package org.opensourcephysics.tools;

/** Optional live host metadata. No parsing of names and no serialized state.
 * A future per-observation uncertainty provider can be added separately.
 */
public interface FitMetadataProvider {
    /** Null means no host metadata: retain DataTable's existing units. */
    String getUnits(String columnName);
    /** Physical y units per image pixel, only for a directly calibrated position.
     * Return NaN for derived data, missing calibration, or varying conversion.
     */
    double getYUnitsPerPixel(String columnName);
}
