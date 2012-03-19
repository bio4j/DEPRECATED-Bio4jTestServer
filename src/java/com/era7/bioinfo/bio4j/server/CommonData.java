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
