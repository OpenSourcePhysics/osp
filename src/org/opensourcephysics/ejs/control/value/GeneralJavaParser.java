package org.opensourcephysics.ejs.control.value;

public final class GeneralJavaParser extends GeneralParser {

  private ParserSuryono javaParser=null;

  GeneralJavaParser(int varsNumber) { javaParser = new ParserSuryono(varsNumber); }

  @Override
public void defineVariable(int index, String name) { javaParser.defineVariable(index,name); }

  @Override
public void setVariable(int index, double value) { javaParser.setVariable(index,value); }

  @Override
public void define(String definition) { javaParser.define(definition); }

  @Override
public void parse() { javaParser.parse(); }

  @Override
public double evaluate() { return javaParser.evaluate(); }

  @Override
public boolean hasError() { return javaParser.getErrorCode()!=ParserSuryono.NO_ERROR; }

  @Override
public int getErrorCode () { return javaParser.getErrorCode(); }

}

