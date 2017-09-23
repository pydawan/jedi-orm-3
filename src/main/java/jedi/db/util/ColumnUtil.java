/***********************************************************************************************
 * @(#)ColumnUtil.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/09/26
 * 
 * Copyright (c) 2014 Thiago Alexandre Martins Monteiro.
 * 
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the GNU Public License v2.0 which accompanies 
 * this distribution, and is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *    Thiago Alexandre Martins Monteiro - initial API and implementation
 ************************************************************************************************/

package jedi.db.util;

import java.lang.reflect.Field;

import jedi.db.models.Model;

public abstract class ColumnUtil {

    public static final String getColumnName(String s) {
        String columnName = "";
        if (s != null && !s.isEmpty()) {
            columnName = s;
            columnName = columnName.trim();
            columnName = columnName.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2");
            columnName = columnName.toLowerCase();
        }
        return columnName;
    }

    public static final String getColumnName(Field f) {
        String columnName = "";
        if (f != null) {
            columnName = getColumnName(f.getName());
        }
        return columnName;
    }

    public static final String getColumnName(Class<?> c) {
        String columnName = "";
        if (c != null && Model.class.isAssignableFrom(c)) {
            columnName = getColumnName(c.getSimpleName());
        }
        return columnName;
    }
}
