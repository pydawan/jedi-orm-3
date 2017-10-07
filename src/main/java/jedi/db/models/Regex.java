package jedi.db.models;

/**
 * Define as expressões regulares a serem validadas no framework.
 * 
 * \\p{L} - aceita todos os caracteres Unicode.
 * 
 * FIELD_LOOKUP_EXACT("^[a-z]{1}[a-zA-Z0-9]*(\\.[a-z]{1}[a-zA-Z0-9]*)*(\\.i?exact)? *= *[\\w ]+$"),
 * 
 * @author thiago
 * @version v1.0.0 16/02/2017
 * @since v1.0.0
 */
public enum Regex {

    CPF("\\d{3}.\\d{3}.\\d{3}-\\d{2}"),
    FIELD_NAME("^[a-z]{1}\\w*$"),
    // 
    // 
    FIELD_LOOKUP_EXACT("^\\w+(\\.\\w+)+(__(i|!)?exact)? *= *'?[\\p{L}\\d ]+'?$"),
    FIELD_LOOKUP_EXACT_DATE("^\\w+(\\.\\w+)+(__(i|!)?exact)? *= *'?\\d{4}-\\d{2}-\\d{2}'?$"),
    FIELD_LOOKUP_EXACT_TIME("^\\w+(\\.\\w+)+(__(i|!)?exact)? *= *'?\\d{2}:\\d{2}:\\d{2}'?$"),
    FIELD_LOOKUP_EXACT_DATETIME("^\\w+(\\.\\w+)+(__(i|!)?exact)? *= *'?\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}'?$"),
    FIELD_LOOKUP_CONTAINS("^\\w+(\\.\\w+)+__(i|!)?contains *= *'?[\\p{L}\\d ]+'?$"),
    FIELD_LOOKUP_IN("^\\w+(\\.\\w+)+__!?in *= *\\[ *'?[\\p{L}\\d ]+'?((, *)'?[\\p{L}\\d ]+'?* *\\]$"),
    FIELD_LOOKUP_GT("^\\w+(\\.\\w+)+((__!?gte? *= *)|( *(>|>=) *))\\d+$"),
    FIELD_LOOKUP_LT("^\\w+(\\.\\w+)+((__!?lte? *= *)|( *(<|<=) *))\\d+$"),
    FIELD_LOOKUP_STARTSWITH("^\\w+(\\.\\w+)+__(i|!)?startswith *= *'?[\\p{L}\\d ]+'?$"),
    FIELD_LOOKUP_ENDSWITH("^\\w+(\\.\\w+)+__(i|!)?endswith *= *'?[\\p{L}\\d ]+'?$"),
    FIELD_LOOKUP_RANGE_NUMBER("^\\w+(\\.\\w+)+__!?range *= *\\(\\d+ *, *\\d+\\)$"),
    FIELD_LOOKUP_RANGE_DATE("^\\w+(\\.\\w+)+__!?range *= *\\('?\\d{4}-\\d{2}-\\d{2}'? *, *'?\\d{4}-\\d{2}-\\d{2}'?\\)$"),
    FIELD_LOOKUP_RANGE_TIME("^\\w+(\\.\\w+)+__!?range *= *\\('?\\d{2}:\\d{2}:\\d{2}'? *, *'?\\d{2}:\\d{2}:\\d{2}'?\\)$"),
    FIELD_LOOKUP_RANGE_DATETIME("^\\w+(\\.\\w+)+__!?range *= *\\('?\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}'? *, *'?\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}'?\\)$"),
    FIELD_LOOKUP_YEAR("^\\w+(\\.\\w+)+__!?year *= *\\d{4}$"),
    FIELD_LOOKUP_MONTH("^\\w+(\\.\\w+)+__!?month *= *\\d{2}$"),
    FIELD_LOOKUP_DAY("^\\w+(\\.\\w+)+__!?day *= *\\d{2}$"),
    FIELD_LOOKUP_WEEK_DAY("^\\w+(\\.\\w+)+__!?weekday *= *[1-7]$"),
    FIELD_LOOKUP_HOUR("^\\w+(\\.\\w+)+__!?hour *= *(\\d|1\\d|2[0-3])$"),
    FIELD_LOOKUP_MINUTE("^\\w+(\\.\\w+)+__!?minute *= *(\\d|[1-5]\\d)$"),
    FIELD_LOOKUP_SECOND("^\\w+(\\.\\w+)+__!?second *= *(\\d|[1-5]\\d)$"),
    FIELD_LOOKUP_ISNULL("^\\w+(\\.\\w+)+__isnull *= *(false|true)$"),
    FIELD_LOOKUP_AND("and|And|AND"),
    FIELD_LOOKUP_OR("or|Or|OR"),
    LIKE_DATE("(\\w+\\s+(LIKE|NOT LIKE)\\s+'%?(\\d+/){2}\\d+%?')+"),
    LIKE_TIME("(\\w+\\s+(LIKE|NOT LIKE)\\s+'%?(\\d+:){2}\\d+%?')+"),
    LIKE_DATETIME("(\\w+([\\._@]\\w+)+\\s+(LIKE|NOT LIKE)\\s+'%?\\d{1,2}/{1,2}\\d{0,2}/{0,1}\\d{0,4}( \\d{0,2}:{0,2}\\d{0,2}:{0,1}\\d{0,2})?%?')+"),
//    SQL_DATETIME_FORMAT("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}"),
    SQL_DATETIME_FORMAT("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}(.\\d{1,6})?"),
    SQL_DATE_FORMAT("\\d{4}-\\d{1,2}-\\d{1,2}"),
//    SQL_TIME_FORMAT("\\d{1,2}:\\d{1,2}:\\d{1,2}");
    SQL_TIME_FORMAT("\\d{1,2}:\\d{1,2}:\\d{1,2}(.\\d{1,6})?");
   
//   yyyy-MM-dd HH:mm:ss.SSSSSS
   
    // TODO search
    // TODO regex
    // TODO iregex
   
    private final String value;
    
    private Regex(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
}
