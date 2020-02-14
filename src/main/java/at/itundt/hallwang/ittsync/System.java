package at.itundt.hallwang.ittsync;

public class System {

    public static class Global{
        private static  ittHandler m_handler = null;
        public static  String HttpMethodVersion = "/v1/Access";

        public static ittHandler getGlobalHandler(){
            return  m_handler;
        }
        public static void setGlobalHandler(ittHandler handler){
            m_handler = handler;
        }
    }
}
