/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.bioinfo.bio4j.server.util;

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.lib.bioinfoxml.logs.LogRecordXML;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class Bio4jLogger {

    private static BufferedWriter LOG_BUFF = null;

    public static void log(LogRecordXML logRecord) throws IOException{
        if(LOG_BUFF == null){
            LOG_BUFF = new BufferedWriter(new FileWriter(CommonData.getLogsFile(), true));
        }
        LOG_BUFF.write(logRecord.toString());
        LOG_BUFF.flush();
    }

    public static void closeLogFile() throws IOException{
        if(LOG_BUFF != null){
            LOG_BUFF.close();
            LOG_BUFF = null;
        }
    }

    public static LogRecordXML createLogRecord(HttpServletRequest request,
                                                String description,
                                                String source){
        LogRecordXML logRecord = new LogRecordXML();
        logRecord.setDescription(description);
        logRecord.setSource(source);
        logRecord.setDate(new Date().toString());
        logRecord.setSourceIP(request.getRemoteAddr());

        return logRecord;
    }

}
