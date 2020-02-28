package org.opensourcephysics.display2d;

public interface ICarpet {

	void setTopRow(double[][] row);

	void clearData();

	void setGridData(GridData griddata);

	void setAutoscaleZ(boolean isAutoscale, double ceil);

}
