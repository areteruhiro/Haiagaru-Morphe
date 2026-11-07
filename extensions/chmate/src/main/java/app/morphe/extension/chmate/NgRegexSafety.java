package app.morphe.extension.chmate;

import java.util.regex.*;

/** Narrow detection of scattered single-character counts, not a general regex rewriter. */
final class NgRegexSafety {
    private static final Pattern SCATTER=Pattern.compile(
            "(?:\\.\\*)?\\((?:\\?:)?\\[([^\\]\\\\]{1,64})\\]\\.\\*[?+]?\\)\\s*\\{([0-9]{1,4})(?:,([0-9]{0,4}))?\\}(?:\\.\\*)?");
    static boolean overbroadKeyword(String expression) {
        if(expression==null)return false;
        if(expression.startsWith("(?s:")&&expression.endsWith(")"))expression=expression.substring(4,expression.length()-1);
        if(expression.startsWith("(?s)"))expression=expression.substring(4);
        int start=0,depth=0;boolean bracket=false,escaped=false;
        for(int i=0;i<=expression.length();i++) {
            char c=i==expression.length()?'|':expression.charAt(i);
            if(escaped){escaped=false;continue;}
            if(c=='\\'){escaped=true;continue;}
            if(c=='['&&!bracket){bracket=true;continue;}
            if(c==']'&&bracket){bracket=false;continue;}
            if(bracket)continue;
            if(c=='(')depth++;else if(c==')')depth--;
            if(c=='|'&&depth==0) {
                String branch=expression.substring(start,i);
                if(branch.startsWith("^"))branch=branch.substring(1);
                if(branch.endsWith("$"))branch=branch.substring(0,branch.length()-1);
                Matcher match=SCATTER.matcher(branch);
                if(match.matches()&&!match.group(1).startsWith("^")&&Integer.parseInt(match.group(2))>=2)return true;
                start=i+1;
            }
        }
        return false;
    }
    private NgRegexSafety(){}
}
