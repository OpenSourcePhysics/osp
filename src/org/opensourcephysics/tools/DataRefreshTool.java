/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;

/**
 * This tool sends data to any tool that requests it.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DataRefreshTool implements Tool {
	private static Map<Data, DataRefreshTool> tools = new HashMap<Data, DataRefreshTool>();
	private Data data; // data source
	protected HashSet<Data> moreData = new HashSet<Data>();
	private HashMap<Integer, Dataset> ids = new HashMap<Integer, Dataset>();

	/**
	 * Returns a DataRefreshTool for the specified data object.
	 *
	 * @param data the data
	 * @return the tool
	 */
	public static DataRefreshTool getTool(Data data) {
		DataRefreshTool tool = tools.get(data);
		if (tool == null) {
			tool = new DataRefreshTool(data);
			tools.put(data, tool);
		}
		return tool;
	}

	/**
	 * Constructs a DataRefreshTool for the specified data object.
	 *
	 * @param data the data
	 */
	private DataRefreshTool(Data data) {
		this.data = data;
	}

	/**
	 * Sends a job to this tool and specifies a tool to reply to. The job xml
	 * defines the Data object requesting a refresh ("requestData"). The requestData
	 * is compared with this tool's Data ("localData") as follows: 1. If the
	 * requestData ID matches the localData ID, then the localData is sent back to
	 * the requester. 2. If not, then the requestData ID is compared with the IDs of
	 * all Data objects in the list returned by
	 * DataTool.getSelfContainedData(localData). If a match is found, the matching
	 * Data is sent back to the requester. 3. If not, then the requestData ID is
	 * compared with the IDs of Datasets in the list returned by
	 * DataTool.getDatasets(localData). If a match is found, the matching Dataset is
	 * sent back to the requester. 4. If not, then every Dataset ID in the list
	 * returned by DataTool.getDatasets(requestData) is compared with the IDs of
	 * Datasets in the list returned by DataTool.getDatasets(localData). All
	 * matching Datasets that are found are sent back to the requester.
	 *
	 * @param job     the Job
	 * @param replyTo the tool requesting refreshed data
	 */
	@Override
	public void send(Job job, Tool replyTo) {
		XMLControlElement control = new XMLControlElement(job.getXML());
		if (control.failedToRead() || (replyTo == null) || !Data.class.isAssignableFrom(control.getObjectClass())) {
			return;
		}
		Data request = (Data) control.loadObject(null, true, true);
		// check for matching ID with localData
		if (request.getID() == data.getID()) {
			Tool.reply(replyTo, job, this, data);
			return;
		}
		// check for matching ID with DataTool.getSelfContainedData(localData)
		for (Data next : DataTool.getSelfContainedData(data)) {
			if (request.getID() == next.getID()) {
				Tool.reply(replyTo, job, this, next);
				return;
			}
		}
		// check for matching ID with local datasets
		ArrayList<Dataset> localDatasets = DataTool.getDatasets(data);
		for (Dataset next : localDatasets) {
			if (request.getID() == next.getID()) {
				Tool.reply(replyTo, job, this, next);
				return;
			}
		}
		// collect all request datasets that match a local dataset
		DatasetManager reply = new DatasetManager();
		// set name of reply to that of request
		reply.setName(request.getName());
		// find datasets that match requested ids
		ids.clear();
		ArrayList<Dataset> requestedDatasets = DataTool.getDatasets(request);
		findDatasets(requestedDatasets, localDatasets, reply, false);
		if (!moreData.isEmpty()) {
			for (Data more : moreData) {
				localDatasets = DataTool.getDatasets(more);
				findDatasets(requestedDatasets, localDatasets, reply, true);
			}
			padDatasets(reply);
		}
		// send datasets to requesting tool
		if (!reply.getDatasetsRaw().isEmpty()) {
			Tool.reply(replyTo, job, this, reply);
		}

	}

	/**
	 * Adds a Data object. Note: added Data objects must use the same independent
	 * variable as the original Data.
	 *
	 * @param data the Data object to add
	 */
	public void addData(Data data) {
		if (data == this.data)
			return;
		moreData.add(data);
	}

	/**
	 * Removes a Data object.
	 *
	 * @param data the Data object to remove
	 */
	public void removeData(Data data) {
		moreData.remove(data);
	}

	private void padDatasets(DatasetManager datasets) {
		// first gather all values of x
		TreeSet<Double> tSet = new TreeSet<Double>();
		for (Dataset dataset : datasets.getDatasetsRaw()) {
			int len = dataset.getIndex();
			double[] xp = dataset.getXPointsRaw();
			for (int i = 0; i < len; i++) {
				tSet.add(xp[i]);
			}
		}
		// put x values into an array
		Double[] temp = tSet.toArray(new Double[tSet.size()]);
		double[] array = new double[tSet.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = temp[i];
		}
		// now pad each dataset so all share same values of x
		for (Dataset dataset : datasets.getDatasetsRaw()) {
			padDataset(dataset, array);
		}
	}

	private void findDatasets(ArrayList<Dataset> requestedDatasets, ArrayList<Dataset> datasetsToSearch,
			DatasetManager manager, boolean isMore) {
		for (Dataset next : requestedDatasets) {
			if (next == null)
				continue;
			Dataset ds = Dataset.findDataSet(datasetsToSearch, next);
			if (ds != null) {
				Dataset toSend = ids.get(ds.getID());
				if (toSend == null) {
					toSend = DataTool.copyDataset(ds, null, true);
					if (isMore) {
						toSend.setXYColumnNames(ds.getXColumnName(), next.getYColumnName());
					}
					toSend.setXColumnVisible(toSend.getXColumnName().equals(next.getYColumnName()));
					toSend.setYColumnVisible(toSend.getYColumnName().equals(next.getYColumnName()));
					ids.put(ds.getID(), toSend);
				} else {
					if (toSend.getXColumnName().equals(next.getYColumnName()))
						toSend.setXColumnVisible(true);
					if (toSend.getYColumnName().equals(next.getYColumnName()))
						toSend.setYColumnVisible(true);
				}
				manager.addDataset(toSend);
			}
		}
	}

	/**
	 * Pads a dataset with NaN values where needed.
	 *
	 * @param dataset   the dataset
	 * @param newXArray expanded array of independent variable values
	 */
	private void padDataset(Dataset dataset, double[] newXArray) {
		double[] xA = dataset.getXPointsRaw();
		double[] yA = dataset.getYPointsRaw();
//		double shift = dataset.getShift();
		// BH ! As written, if the data are shifted, then
		// then since getYPoints() adds the shift, 
		// we are then replacing the y values with shifted ones;
		// I have left the shift out here.
		int len = dataset.getIndex();
		Map<Double, Double> valueMap = new HashMap<Double, Double>();
		for (int k = 0; k < len; k++) {
			valueMap.put(xA[k], yA[k]);
		}
		// pad y-values of nextOut with NaN where needed
		double[] newYArray = new double[len];
		for (int k = 0; k < len; k++) {
			double x = newXArray[k];
			newYArray[k] = valueMap.keySet().contains(x) ? valueMap.get(x) : Double.NaN;
		}
		dataset.clear();
		dataset.append(newXArray, newYArray, len);
	}

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
