package org.opensourcephysics.ejs.control.value;

/**
 * Creates a parser and its variables for a given expression
 */


public class ParserAndVariables {

  private GeneralParser parser;
  private String[] vars;

  public GeneralParser getParser() { return parser; }

  public String[] getVariables() { return vars; }

  public ParserAndVariables (boolean javaSyntax, String expression) {
    if (javaSyntax) {
      vars = ParserSuryono.getVariableList (expression);
      parser = new GeneralJavaParser(vars.length);
    }
    else {
      try {
        parser = new GeneralParser(0);
        vars = parser.freeParser.parseUnknown(expression);
      }
      catch (Exception _exc) {
        vars = ParserSuryono.getVariableList (expression);
        parser = new GeneralParser(vars.length);
      }
    }
  }

}

