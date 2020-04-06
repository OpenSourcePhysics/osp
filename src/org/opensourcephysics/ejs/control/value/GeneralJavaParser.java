package org.opensourcephysics.ejs.control.value;

public final class GeneralJavaParser extends GeneralParser {

  private ParserSuryono javaParser=null;

  GeneralJavaParser(int varsNumber) { javaParser = new ParserSuryono(varsNumber); }

  public void defineVariable(int index, String name) { javaParser.defineVariable(index,name); }

  public void setVariable(int index, double value) { javaParser.setVariable(index,value); }

  public void define(String definition) { javaParser.define(definition); }

  public void parse() { javaParser.parse(); }

  public double evaluate() { return javaParser.evaluate(); }

  public boolean hasError() { return javaParser.getErrorCode()!=ParserSuryono.NO_ERROR; }

  public int getErrorCode () { return javaParser.getErrorCode(); }

}

