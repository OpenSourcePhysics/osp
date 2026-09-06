package org.opensourcephysics.display;

/** Plain-text notation for spreadsheet headers and expressions, never numeric cells. */
public final class ExportText {
    private ExportText() {}
    public static String ascii(String text) {
        if (text == null) return "";
        String s = text.replace("\\cdot", "*").replace("\\times", "*")
                .replace("\\mu", "u").replace("\\theta", "theta").replace("\\sigma", "sigma")
                .replace("\\alpha", "alpha").replace("\\beta", "beta").replace("\\omega", "omega")
                .replace("\\Delta", "Delta").replace("\\pi", "pi")
                .replace("·", "*").replace("×", "*").replace("−", "-")
                .replace("µ", "u").replace("μ", "u").replace("θ", "theta")
                .replace("σ", "sigma").replace("α", "alpha").replace("β", "beta")
                .replace("ω", "omega").replace("Δ", "Delta").replace("π", "pi");
        String supers = "⁰¹²³⁴⁵⁶⁷⁸⁹⁻⁺", normal = "0123456789-+";
        StringBuilder out = new StringBuilder(); boolean exponent = false;
        for (int i=0;i<s.length();i++) {
            int n=supers.indexOf(s.charAt(i));
            if(n>=0) { if(!exponent) out.append('^'); out.append(normal.charAt(n)); exponent=true; }
            else { out.append(s.charAt(i)); exponent=false; }
        }
        return out.toString().replaceAll("([_^])\\{([^{}]*)\\}", "$1$2");
    }
    public static String header(String name, String units) {
        String label=ascii(name), unit=ascii(units).trim();
        String suffix=" ("+unit+")";
        return unit.length()==0 || label.endsWith(suffix) ? label : label+suffix;
    }
}
