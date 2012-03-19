/*
 * Copyright (C) 2010-2011  "Bio4j"
 *
 * This file is part of Bio4j
 *
 * Bio4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
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
