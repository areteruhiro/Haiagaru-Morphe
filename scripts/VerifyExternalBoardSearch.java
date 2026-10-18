package app.morphe.extension.chmate;

public final class VerifyExternalBoardSearch {
    public static void main(String[] args) {
        String[][] cases={{"","エッヂ","https://bbs.eddibb.cc/liveedge/","true"},
                {"エッ","エッヂ","https://bbs.eddibb.cc/liveedge/","true"},
                {" EDDIBB.CC ","エッヂ","https://bbs.eddibb.cc/liveedge/","true"},
                {"liveedge","エッヂ","https://bbs.eddibb.cc/liveedge/","true"},
                {"[.*]","エッヂ","https://bbs.eddibb.cc/liveedge/","false"},
                {"別板","エッヂ","https://bbs.eddibb.cc/liveedge/","false"},
                {"ABC","abc板",null,"true"},{"x",null,null,"false"}};
        for(String[] c:cases)if(ExternalBoardSearch.matches(c[0],c[1],c[2])!=Boolean.parseBoolean(c[3]))
            throw new AssertionError(c[0]);
        System.out.println("PASS "+cases.length+" external board search checks");
    }
}
