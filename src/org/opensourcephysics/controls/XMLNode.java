package org.opensourcephysics.controls;

public abstract class XMLNode implements XMLProperty {

	protected String name;
	protected String type = "object";
	protected String className = "java.lang.Object"; //$NON-NLS-1$ //changed by W. Christian
	protected XMLProperty parent;
	
	/**
	 * Gets the immediate parent property.
	 *
	 * @return the type
	 */
	@Override
	public XMLProperty getParentProperty() {
		return parent;
	}

	/**
	 * Gets the property type.
	 *
	 * @return the type
	 */
	@Override
	public String getPropertyType() {
		return type;
	}


	@Override
	public void setValue(String stringValue) {
		// overridden by XMLPropertyElement only
	}
	

}
