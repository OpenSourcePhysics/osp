package org.opensourcephysics.ejs.control.value;

import org.opensourcephysics.numerics.MathExpParser;
import org.opensourcephysics.numerics.SuryonoParser;

/**
 * Encapsulates both ParserSuryono and SuryonoParser
 */


public class GeneralParser {

  SuryonoParser freeParser=null;

  GeneralParser() {} // Only for subclasses

  GeneralParser(int varsNumber) { freeParser = new SuryonoParser(varsNumber); }

  public void defineVariable(int index, String name) { freeParser.defineVariable(index+1,name); }

  public void setVariable(int index, double value) { freeParser.setVariable(index+1,value); }

  public void define(String definition) { freeParser.define(definition); }

  public void parse() { freeParser.parse(); }

  public double evaluate() { return freeParser.evaluate(); }

  public boolean hasError() { return freeParser.getErrorCode()!=MathExpParser.NO_ERROR; }

  public int getErrorCode () { return freeParser.getErrorCode(); }
}

