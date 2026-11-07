package com.carlosgt.atak.aerozona;

/**
 * ENAIRE writes zone messages as HTML fragments (fonts, links, paragraphs). The details
 * page shows text, so tags become line breaks or nothing, and the few entities ENAIRE
 * uses are decoded. A link's address is kept after its text, since an operator may need
 * to type it. Plain Java so it is testable off the device.
 */
public final class Html {

    private Html() {
    }

    public static String toText(String html) {
        if (html == null || html.isEmpty())
            return "";
        String s = html;
        // <a href='x'>text</a> -> text [x], unless the text already is the address.
        s = s.replaceAll("(?is)<a\\s[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>",
                "$2 [$1]");
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</p>|</li>|</div>|<p[^>]*>", "\n");
        s = s.replaceAll("(?s)<[^>]+>", "");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&aacute;", "á").replace("&eacute;", "é").replace("&iacute;", "í")
                .replace("&oacute;", "ó").replace("&uacute;", "ú").replace("&ntilde;", "ñ");
        // A mail link already shows the address as its text.
        s = s.replaceAll("(?i) \\[mailto:[^\\]]*\\]", "");
        // "text [url]" where text == url collapses to the url.
        s = s.replaceAll("(\\S+) \\[\\1\\]", "$1");
        s = s.replaceAll("[ \\t]+", " ");
        s = s.replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }
}
