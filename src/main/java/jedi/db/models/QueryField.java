package jedi.db.models;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * @author thiago-amm
 * @version v1.0.0 15/10/2017
 * @since v1.0.0
 */
public class QueryField {

   private static final Calendar calendar = Calendar.getInstance();
   private String field;
   private Object value;
   private String query;
   
   public QueryField(String field, Object value) {
      this.field = field;
      this.value = value;
      this.query = "";
   }
   
   public QueryField(String field) {
      this(field, null);
   }
   
   public String getField() {
      return field;
   }
   
   public String field() {
      return field;
   }
   
   public void setField(String field) {
      this.field = field;
   }
   
   public QueryField field(String field) {
      setField(field);
      return this;
   }
   
   public Object getValue() {
      return value;
   }
   
   public void setValue(Object value) {
      this.value = value;
   }
   
   public Object value() {
      return getValue();
   }
   
   public QueryField value(Object value) {
      setValue(value);
      return this;
   }
   
   public String getQuery() {
      return query;
   }
   
   public void setQuery(String query) {
      this.query = query;
   }
   
   public String query() {
      return query;
   }
   
   public QueryField query(String query) {
      setQuery(query);
      return this;
   }
   
   public QueryField exact(Object value) {
      String exact = "";
      if (value != null) {
         exact = String.format("%s__exact=%s", field, value);
         query = exact;
      }
      return this;
   }
   
   public QueryField notExact(Object value) {
      String notExact = "";
      if (value != null) {
         notExact = String.format("%s__!exact=%s", field, value);
         query = notExact;
      }
      return this;
   }
   
   public QueryField startsWith(String text) {
      text = text == null ? "" : text;
      String startsWith = "";
      if (!text.isEmpty()) {
         startsWith = String.format("%s__startswith='%s'", field, text);
         query = startsWith;
      }
      return this;
   }
   
   public QueryField istartsWith(String text) {
      text = text == null ? "" : text;
      String istartsWith = "";
      if (!text.isEmpty()) {
         istartsWith = String.format("%s__istartswith='%s'", field, text);
         query = istartsWith;
      }
      return this;
   }
   
   public QueryField notStartsWith(String text) {
      text = text == null ? "" : text;
      String notStartsWith = "";
      if (!text.isEmpty()) {
         notStartsWith = String.format("%s__!startswith='%s'", field, text);
         query = notStartsWith;
      }
      return this;
   }
   
   public QueryField iNotStartsWith(String text) {
      text = text == null ? "" : text;
      String iNotStartsWith = "";
      if (!text.isEmpty()) {
         iNotStartsWith = String.format("%s__!istartswith='%s'", field, text);
         query = iNotStartsWith;
      }
      return this;
   }
   
   public QueryField contains(String text) {
      text = text == null ? "" : text;
      String contains = "";
      if (!text.isEmpty()) {
         contains = String.format("%s__contains='%s'", field, text);
         query = contains;
      }
      return this;
   }
   
   public QueryField icontains(String text) {
      text = text == null ? "" : text;
      String icontains = "";
      if (!text.isEmpty()) {
         icontains = String.format("%s__icontains='%s'", field, text);
         query = icontains;
      }
      return this;
   }
   
   public QueryField notContains(String text) {
      text = text == null ? "" : text;
      String notContains = "";
      if (!text.isEmpty()) {
         notContains = String.format("%s__!contains='%s'", field, text);
         query = notContains;
      }
      return this;
   }
   
   public QueryField iNotContains(String text) {
      text = text == null ? "" : text;
      String iNotContains = "";
      if (!text.isEmpty()) {
         iNotContains = String.format("%s__!icontains='%s'", field, text);
         query = iNotContains;
      }
      return this;
   }
   
   public QueryField endsWith(String text) {
      text = text == null ? "" : text;
      String endsWith = "";
      if (!text.isEmpty()) {
         endsWith = String.format("%s__endswith='%s'", field, text);
         query = endsWith;
      }
      return this;
   }
   
   public QueryField iendsWith(String text) {
      text = text == null ? "" : text;
      String iendsWith = "";
      if (!text.isEmpty()) {
         iendsWith = String.format("%s__iendswith='%s'", field, text);
         query = iendsWith;
      }
      return this;
   }
   
   public QueryField notEndsWith(String text) {
      text = text == null ? "" : text;
      String notEndsWith = "";
      if (!text.isEmpty()) {
         notEndsWith = String.format("%s__!endswith='%s'", field, text);
         query = notEndsWith;
      }
      return this;
   }
   
   public QueryField iNotEndsWith(String text) {
      text = text == null ? "" : text;
      String iNotEndsWith = "";
      if (!text.isEmpty()) {
         iNotEndsWith = String.format("%s__!iendswith='%s'", field, text);
         query = iNotEndsWith;
      }
      return this;
   }
   
   public QueryField in(Object... values) {
      String in = "";
      if (values != null && values.length > 0) {
         in = String.join(", ", Arrays.toString(values));
         in = String.format("%s__in=%s", field, in);
         query = in;
      }
      return this;
   }
   
   public QueryField notIn(Object... values) {
      String notIn = "";
      if (values != null && values.length > 0) {
         notIn = String.join(", ", Arrays.toString(values));
         notIn = String.format("%s__!in=%s", field, notIn);
         query = notIn;
      }
      return this;
   }
   
   public QueryField range(Object start, Object end) {
      String range = "";
      if (start != null && end != null) {
         // TODO - tratar start e end de acordo com o tipo de dados dos parâmetros.
         range = String.format("%s__range=[%s, %s]", field, start, end);
         query = range;
      }
      return this;
   }
   
   public QueryField notRange(Object start, Object end) {
      String notRange = "";
      if (start != null && end != null) {
         // TODO - tratar start e end de acordo com o tipo de dados dos parâmetros.
         notRange = String.format("%s__!range=[%s, %s]", field, start, end);
         query = notRange;
      }
      return this;
   }
   
   public QueryField between(Object start, Object end) {
      String between = "";
      if (start != null && end != null) {
         between = String.format("%s__range=[%s, %s]", field, start, end);
         query = between;
      }
      return this;
   }
   
   public QueryField notBetween(Object start, Object end) {
      String notBetween = "";
      if (start != null && end != null) {
         notBetween = String.format("%s__!range=[%s, %s]", field, start, end);
         query = notBetween;
      }
      return this;
   }
   
   public QueryField regex(String pattern) {
      String regex = "";
      pattern = pattern == null ? "" : pattern;
      if (!pattern.isEmpty()) {
         regex = String.format("%s__regex=%s", field, pattern);
         query = regex;
      }
      return this;
   }
   
   public QueryField iregex(String pattern) {
      String iregex = "";
      pattern = pattern == null ? "" : pattern;
      if (!pattern.isEmpty()) {
         iregex = String.format("%s__iregex=%s", field, pattern);
         query = iregex;
      }
      return this;
   }
   
   public QueryField lessThan(Number number) {
      String lessThan = "";
      if (number != null) {
         lessThan = String.format("%s__lt=%s", field, number);
         query = lessThan;
      }
      return this;
   }
   
   public QueryField notLessThan(Number number) {
      String notLessThan = "";
      if (number != null) {
         notLessThan = String.format("%s__!lt=%s", field, number);
         query = notLessThan;
      }
      return this;
   }
   
   public QueryField lt(Number number) {
      return lessThan(number);
   }
   
   public QueryField nlt(Number number) {
      return notLessThan(number);
   }
   
   public QueryField lessThanOrEqual(Number number) {
      String lessThanOrEqual = "";
      if (number != null) {
         lessThanOrEqual = String.format("%s__lte=%s", field, number);
         query = lessThanOrEqual;
      }
      return this;
   }
   
   public QueryField notLessThanOrEqual(Number number) {
      String notLessThanOrEqual = "";
      if (number != null) {
         notLessThanOrEqual = String.format("%s__!lte=%s", field, number);
         query = notLessThanOrEqual;
      }
      return this;
   }
   
   public QueryField lte(Number number) {
      return lessThanOrEqual(number);
   }
   
   public QueryField nlte(Number number) {
      return notLessThanOrEqual(number);
   }
   
   public QueryField equal(Number number) {
      String equal = "";
      if (number != null) {
         equal = String.format("%s__exact=%s", number);
         query = equal;
      }
      return this;
   }
   
   public QueryField eq(Number number) {
      return equal(number);
   }
   
   public QueryField notEqual(Number number) {
      String notEqual = "";
      if (number != null) {
         notEqual = String.format("%s__!exact=%s", number);
         query = notEqual;
      }
      return this;
   }
   
   public QueryField neq(Number number) {
      return notEqual(number);
   }
   
   public QueryField greaterThan(Number number) {
      String greaterThan = "";
      if (number != null) {
         greaterThan = String.format("%s__gt=%s", number);
         query = greaterThan;
      }
      return this;
   }
   
   public QueryField notGreaterThan(Number number) {
      String notGreaterThan = "";
      if (number != null) {
         notGreaterThan = String.format("%s__!gt=%s", number);
         query = notGreaterThan;
      }
      return this;
   }
   
   public QueryField gt(Number number) {
      return greaterThan(number);
   }
   
   public QueryField ngt(Number number) {
      return notGreaterThan(number);
   }
   
   public QueryField greaterThanOrEqual(Number number) {
      String greaterThanOrEqual = "";
      if (number != null) {
         greaterThanOrEqual = String.format("%s__gte=%s", number);
         query = greaterThanOrEqual;
      }
      return this;
   }
   
   public QueryField notGreaterThanOrEqual(Number number) {
      String notGreaterThanOrEqual = "";
      if (number != null) {
         notGreaterThanOrEqual = String.format("%s__gte=%s", number);
         query = notGreaterThanOrEqual;
      }
      return this;
   }
   
   public QueryField gte(Number number) {
      return greaterThanOrEqual(number);
   }
   
   public QueryField ngte(Number number) {
      return notGreaterThanOrEqual(number);
   }
   
   public QueryField isNull(boolean value) {
      String isNull = String.format("%s__isnull=%s", field, value);
      query = isNull;
      return this;
   }
   
   public QueryField and(QueryField field) {
      if (field == null) {
         query = "AND";
      } else {
         query = query + " AND " + field;
      }
      return this;
   }
   
   public QueryField or(QueryField field) {
      if (field == null) {
         query = "OR";
      } else {
         query = query + " OR " + field;
      }
      return this;
   }
   
   public QueryField not(QueryField field) {
      if (field == null) {
         query = "NOT";
      } else {
         query = " NOT " + field;
      }
      return this;
   }
   
   public QueryField yearLessThan(int value) {
      // jedi.dsl
      // DSL - year(2005).lessThan(2017).
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__lt=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearLT(int value) {
      return yearLessThan(value);
   }
   
   public QueryField yearNotLessThan(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__!lt=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearNLT(int value) {
      return yearNotLessThan(value);
   }
   
   public QueryField yearLessThan(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearLessThan(year);
   }
   
   public QueryField yearNotLessThan(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearNotLessThan(year);
   }
   
   public QueryField yearNLT(Date date) {
      return yearNotLessThan(date);
   }
   
   public QueryField yearLessThanOrEqual(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__lte=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearLTE(int value) {
      return yearLessThanOrEqual(value);
   }
   
   public QueryField yearNotLessThanOrEqual(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__!lte=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearNLTE(int value) {
      return yearNotLessThanOrEqual(value);
   }
   
   public QueryField yearLessThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearLessThanOrEqual(year);
   }
   
   public QueryField yearLTE(Date date) {
      return yearLessThanOrEqual(date);
   }
   
   public QueryField yearNotLessThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearNotLessThanOrEqual(year);
   }
   
   public QueryField yearNLTE(Date date) {
      return yearNotLessThanOrEqual(date);
   }
   
   public QueryField yearGreaterThan(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__gt=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearGT(int value) {
      return yearGreaterThan(value);
   }
   
   public QueryField yearGreaterThan(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearGreaterThan(year);
   }
   
   public QueryField yearGT(Date date) {
      return yearGreaterThan(date);
   }
   
   public QueryField yearNotGreaterThan(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__!gt=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearNGT(int value) {
      return yearNotGreaterThan(value);
   }
   
   public QueryField yearNotGreaterThan(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearNotGreaterThan(year);
   }
   
   public QueryField yearNGT(Date date) {
      return yearNotGreaterThan(date);
   }
   
   public QueryField yearGreaterThanOrEqual(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__gte=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearGTE(int value) {
      return yearGreaterThanOrEqual(value);
   }
   
   public QueryField yearNotGreaterThanOrEqual(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__!gte=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearNGTE(int value) {
      return yearNotGreaterThanOrEqual(value);
   }
   
   public QueryField yearGreaterThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearGreaterThanOrEqual(year);
   }
   
   public QueryField yearGTE(Date date) {
      return yearGreaterThanOrEqual(date);
   }
   
   public QueryField yearNotGreaterThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearNotGreaterThanOrEqual(year);
   }
   
   public QueryField yearNGTE(Date date) {
      return yearNotGreaterThanOrEqual(date);
   }
   
   public QueryField yearEqual(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__exact=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearEQ(int value) {
      return yearEqual(value);
   }
   
   public QueryField yearEqual(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearEqual(year);
   }
   
   public QueryField yearEQ(Date date) {
      return yearEqual(date);
   }
   
   public QueryField yearNotEqual(int value) {
      String year = String.valueOf(value);
      if (year.matches("\\d{4}")) {
         query = String.format("%s__year__!exact=%s", field, year);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField yearNEQ(int value) {
      return yearNotEqual(value);
   }
   
   public QueryField yearNotEqual(Date date) {
      date = date == null ? new Date() : date;
      int year = calendar.get(Calendar.YEAR);
      return yearNotEqual(year);
   }
   
   public QueryField yearNEQ(Date date) {
      return yearNotEqual(date);
   }
   
   public QueryField monthLessThan(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__lt=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthLT(int value) {
      return monthLessThan(value);
   }
   
   public QueryField monthNotLessThan(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__!lt=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthNLT(int value) {
      return monthNotLessThan(value);
   }
   
   public QueryField monthLessThan(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthLessThan(month);
   }
   
   public QueryField monthNotLessThan(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthNotLessThan(month);
   }
   
   public QueryField monthNLT(Date date) {
      return monthNotLessThan(date);
   }
   
   public QueryField monthLessThanOrEqual(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__lte=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthLTE(int value) {
      return monthLessThanOrEqual(value);
   }
   
   public QueryField monthNotLessThanOrEqual(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__!lte=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthNLTE(int value) {
      return monthNotLessThanOrEqual(value);
   }
   
   public QueryField monthLessThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthLessThanOrEqual(month);
   }
   
   public QueryField monthLTE(Date date) {
      return monthLessThanOrEqual(date);
   }
   
   public QueryField monthNotLessThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthNotLessThanOrEqual(month);
   }
   
   public QueryField monthNLTE(Date date) {
      return monthNotLessThanOrEqual(date);
   }
   
   public QueryField monthGreaterThan(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__gt=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthGT(int value) {
      return monthGreaterThan(value);
   }
   
   public QueryField monthGreaterThan(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthGreaterThan(month);
   }
   
   public QueryField monthGT(Date date) {
      return monthGreaterThan(date);
   }
   
   public QueryField monthNotGreaterThan(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__!gt=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthNGT(int value) {
      return monthNotGreaterThan(value);
   }
   
   public QueryField monthNotGreaterThan(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthNotGreaterThan(month);
   }
   
   public QueryField monthNGT(Date date) {
      return monthNotGreaterThan(date);
   }
   
   public QueryField monthGreaterThanOrEqual(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__gte=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthGTE(int value) {
      return monthGreaterThanOrEqual(value);
   }
   
   public QueryField monthNotGreaterThanOrEqual(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__!gte=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthNGTE(int value) {
      return monthNotGreaterThanOrEqual(value);
   }
   
   public QueryField monthGreaterThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthGreaterThanOrEqual(month);
   }
   
   public QueryField monthGTE(Date date) {
      return monthGreaterThanOrEqual(date);
   }
   
   public QueryField monthNotGreaterThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthNotGreaterThanOrEqual(month);
   }
   
   public QueryField monthNGTE(Date date) {
      return monthNotGreaterThanOrEqual(date);
   }
   
   public QueryField monthEqual(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__exact=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthEQ(int value) {
      return monthEqual(value);
   }
   
   public QueryField monthEqual(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthEqual(month);
   }
   
   public QueryField monthEQ(Date date) {
      return monthEqual(date);
   }
   
   public QueryField monthNotEqual(int value) {
      String month = String.valueOf(value);
      if (month.matches("\\d{2}")) {
         query = String.format("%s__month__!exact=%s", field, month);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField monthNEQ(int value) {
      return monthNotEqual(value);
   }
   
   public QueryField monthNotEqual(Date date) {
      date = date == null ? new Date() : date;
      int month = calendar.get(Calendar.MONTH);
      return monthNotEqual(month);
   }
   
   public QueryField monthNEQ(Date date) {
      return monthNotEqual(date);
   }
   
   public QueryField dayLessThan(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__lt=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayLT(int value) {
      return dayLessThan(value);
   }
   
   public QueryField dayNotLessThan(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__!lt=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayNLT(int value) {
      return dayNotLessThan(value);
   }
   
   public QueryField dayLessThan(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayLessThan(day);
   }
   
   public QueryField dayNotLessThan(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayNotLessThan(day);
   }
   
   public QueryField dayNLT(Date date) {
      return dayNotLessThan(date);
   }
   
   public QueryField dayLessThanOrEqual(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__lte=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayLTE(int value) {
      return dayLessThanOrEqual(value);
   }
   
   public QueryField dayNotLessThanOrEqual(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__!lte=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayNLTE(int value) {
      return dayNotLessThanOrEqual(value);
   }
   
   public QueryField dayLessThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayLessThanOrEqual(day);
   }
   
   public QueryField dayLTE(Date date) {
      return dayLessThanOrEqual(date);
   }
   
   public QueryField dayNotLessThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayNotLessThanOrEqual(day);
   }
   
   public QueryField dayNLTE(Date date) {
      return dayNotLessThanOrEqual(date);
   }
   
   public QueryField dayGreaterThan(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__gt=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayGT(int value) {
      return dayGreaterThan(value);
   }
   
   public QueryField dayGreaterThan(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayGreaterThan(day);
   }
   
   public QueryField dayGT(Date date) {
      return dayGreaterThan(date);
   }
   
   public QueryField dayNotGreaterThan(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__!gt=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayNGT(int value) {
      return dayNotGreaterThan(value);
   }
   
   public QueryField dayNotGreaterThan(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayNotGreaterThan(day);
   }
   
   public QueryField dayNGT(Date date) {
      return dayNotGreaterThan(date);
   }
   
   public QueryField dayGreaterThanOrEqual(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__gte=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayGTE(int value) {
      return dayGreaterThanOrEqual(value);
   }
   
   public QueryField dayNotGreaterThanOrEqual(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__!gte=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayNGTE(int value) {
      return dayNotGreaterThanOrEqual(value);
   }
   
   public QueryField dayGreaterThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayGreaterThanOrEqual(day);
   }
   
   public QueryField dayGTE(Date date) {
      return dayGreaterThanOrEqual(date);
   }
   
   public QueryField dayNotGreaterThanOrEqual(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayNotGreaterThanOrEqual(day);
   }
   
   public QueryField dayNGTE(Date date) {
      return dayNotGreaterThanOrEqual(date);
   }
   
   public QueryField dayEqual(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__exact=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayEQ(int value) {
      return dayEqual(value);
   }
   
   public QueryField dayEqual(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return monthEqual(day);
   }
   
   public QueryField dayEQ(Date date) {
      return dayEqual(date);
   }
   
   public QueryField dayNotEqual(int value) {
      String day = String.valueOf(value);
      if (day.matches("\\d{2}")) {
         query = String.format("%s__day__!exact=%s", field, day);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField dayNEQ(int value) {
      return dayNotEqual(value);
   }
   
   public QueryField dayNotEqual(Date date) {
      date = date == null ? new Date() : date;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      return dayNotEqual(day);
   }
   
   public QueryField dayNEQ(Date date) {
      return dayNotEqual(date);
   }
   
   public QueryField weekdayLessThan(int value) {
      String weekday = String.valueOf(value);
      if (weekday.matches("\\d{2}")) {
         query = String.format("%s__week_day__lt=%s", field, weekday);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField weekdayLT(int value) {
      return weekdayLessThan(value);
   }
   
   public QueryField weekdayNotLessThan(int value) {
      String weekday = String.valueOf(value);
      if (weekday.matches("\\d{2}")) {
         query = String.format("%s__week_day__!lt=%s", field, weekday);
      } else {
         query = "";
      }
      return this;
   }
   
   public QueryField weekdayNLT(int value) {
      return weekdayNotLessThan(value);
   }
   
   public QueryField weekdayLessThan(Date date) {
      date = date == null ? new Date() : date;
      int weekday = calendar.get(Calendar.DAY_OF_WEEK);
      return weekdayLessThan(weekday);
   }
   
   public QueryField weekdayLT(Date date) {
      date = date == null ? new Date() : date;
      int weekday = calendar.get(Calendar.DAY_OF_WEEK);
      return weekdayLT(weekday);
   }
   
   @Override
   public String toString() {
      return query;
   }
   
}
