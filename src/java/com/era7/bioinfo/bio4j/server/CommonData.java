/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.bioinfo.bio4j.server;

import java.io.File;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class CommonData {

    public static final String DATABASE_FOLDER = "bio4jdb/";    

    private static File BIO4J_LOGS_FILE = null;
    private static final String BIO4J_LOGS_FILE_NAME = "Bio4jLogs.log";

    public static File getLogsFile(){
        if(BIO4J_LOGS_FILE == null){
            BIO4J_LOGS_FILE = new File(BIO4J_LOGS_FILE_NAME);
        }
        return BIO4J_LOGS_FILE;
    }

}
