package jedi.types;

import jedi.db.models.Regex;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
@SuppressWarnings({ "deprecation" })
public class DateTime extends java.util.Date {
   
   private static final long serialVersionUID = -1598588999773456561L;
   private static final String SQL_DATETIME_FORMAT = Regex.SQL_DATETIME_FORMAT.getValue();
   private static final String SQL_DATE_FORMAT = Regex.SQL_DATE_FORMAT.getValue();
   
   public DateTime() {
      super();
   }
   
   public DateTime(java.util.Date date) {
      super.setYear(date.getYear());
      super.setMonth(date.getMonth());
      super.setDate(date.getDate());
      super.setHours(date.getHours());
      super.setMinutes(date.getMinutes());
      super.setSeconds(date.getSeconds());
   }
   
   public DateTime(long time) {
      super(time);
   }
   
   public DateTime(int year, int month, int date) {
      super(year - 1900, month - 1, date);
   }
   
   public DateTime(int year, int month, int date, int hours, int minutes) {
      super(year - 1900, month - 1, date, hours, minutes);
   }
   
   public DateTime(int year, int month, int date, int hours, int minutes, int seconds) {
      super(year - 1900, month - 1, date, hours, minutes, seconds);
   }
   
   public DateTime(String value) {
      value = value == null ? "" : value.trim();
      if (!value.isEmpty()) {
         String text = value;
         int date = 0;
         int month = 0;
         int year = 0;
         int hours = 0;
         int minutes = 0;
         int seconds = 0;
         String _date = "";
         String _month = "";
         String _year = "";
         String _hours = "";
         String _minutes = "";
         String _seconds = "";
         if (text.matches(SQL_DATETIME_FORMAT)) {
            _year = text.substring(0, text.indexOf("-"));
            text = text.replace(_year + "-", "");
            _month = text.substring(0, text.indexOf("-"));
            text = text.replace(_month + "-", "");
            _date = text.substring(0, text.indexOf(" "));
            text = text.replace(_date + " ", "");
            _hours = text.substring(0, text.indexOf(":"));
            text = text.replace(_hours + ":", "");
            _minutes = text.substring(0, text.indexOf(":"));
            text = text.replace(_minutes + ":", "");
            _seconds = text.substring(0, text.length());
            text = text.replace(_seconds, "");
            year = Integer.parseInt(_year);
            month = Integer.parseInt(_month);
            date = Integer.parseInt(_date);
            hours = Integer.parseInt(_hours);
            minutes = Integer.parseInt(_minutes);
            seconds = Integer.parseInt(_seconds);
            this.setYear(year);
            this.setMonth(month);
            this.setDate(date);
            this.setHours(hours);
            this.setMinutes(minutes);
            this.setSeconds(seconds);
         } else if (text.matches(SQL_DATE_FORMAT)) {
            _year = text.substring(0, text.indexOf("-"));
            text = text.replace(_year + "-", "");
            _month = text.substring(0, text.indexOf("-"));
            text = text.replace(_month + "-", "");
            _date = text.substring(0, text.length());
            text = text.replace(_date, "");
            year = Integer.parseInt(_year);
            month = Integer.parseInt(_month);
            date = Integer.parseInt(_date);
            this.setYear(year);
            this.setMonth(month);
            this.setDate(date);
            this.setHours(hours);
            this.setMinutes(minutes);
            this.setSeconds(seconds);
         } else {
            
         }
      } else {
         throw new IllegalArgumentException("ATENÇÃO: Data em formato textual inválido!");
      }
   }
   
   public static DateTime datetime() {
      return new DateTime();
   }
   
   public static DateTime datetime(java.util.Date date) {
      return new DateTime(date);
   }
   
   public static DateTime datetime(long time) {
      return new DateTime(time);
   }
   
   public static DateTime datetime(int year, int month, int date) {
      return new DateTime(year, month, date);
   }
   
   public static DateTime datetime(int year, int month, int date, int hours, int minutes) {
      return new DateTime(year, month, date, hours, minutes);
   }
   
   public static DateTime datetime(int year, int month, int date, int hours, int minutes, int seconds) {
      return new DateTime(year, month, date, hours, minutes, seconds);
   }
   
   public static DateTime datetime(String value) {
      return new DateTime(value);
   }
   
   public static DateTime of() {
      return new DateTime();
   }
   
   public static DateTime of(java.util.Date date) {
      return new DateTime(date);
   }
   
   public static DateTime of(long time) {
      return new DateTime(time);
   }
   
   public static DateTime of(int year, int month, int date) {
      return new DateTime(year, month, date);
   }
   
   public static DateTime of(int year, int month, int date, int hours, int minutes) {
      return new DateTime(year, month, date, hours, minutes);
   }
   
   public static DateTime of(int year, int month, int date, int hours, int minutes, int seconds) {
      return new DateTime(year, month, date, hours, minutes, seconds);
   }
   
   public static DateTime of(String value) {
      return new DateTime(value);
   }
   
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (!(obj instanceof DateTime)) return false;
      if (getClass() != obj.getClass()) return false;
      DateTime other = (DateTime) obj;
      if (!this.toString().equals(other.toString())) return false;
      return true;
   }
   
   @Override
   public String toString() {
      return String.format(
         "%4d-%02d-%02d %02d:%02d:%02d",
         this.getYear(),
         this.getMonth(),
         this.getDate(),
         this.getHours(),
         this.getMinutes(),
         this.getSeconds());
   }
   
   public String toString(String format) {
      return JediString.toString(this, format);
   }
   
   @Override
   public int getYear() {
      return super.getYear() + 1900;
   }
   
   public int year() {
      return this.getYear();
   }
   
   @Override
   public int getMonth() {
      return super.getMonth() + 1;
   }
   
   public int month() {
      return this.getMonth();
   }
   
   @Override
   public int getDate() {
      return super.getDate();
   }
   
   public int date() {
      return this.getDate();
   }
   
   @Override
   public int getHours() {
      return super.getHours();
   }
   
   public int hours() {
      return this.getHours();
   }
   
   @Override
   public int getMinutes() {
      return super.getMinutes();
   }
   
   public int minutes() {
      return this.getMinutes();
   }
   
   @Override
   public int getSeconds() {
      return super.getSeconds();
   }
   
   public int seconds() {
      return this.getSeconds();
   }
   
   @Override
   public void setYear(int year) {
      super.setYear(year - 1900);
   }
   
   public void year(int year) {
      this.setYear(year);
   }
   
   @Override
   public void setMonth(int month) {
      super.setMonth(month - 1);
   }
   
   public void month(int month) {
      this.setMonth(month);
   }
   
   @Override
   public void setDate(int date) {
      super.setDate(date);
   }
   
   public void date(int date) {
      this.setDate(date);
   }
   
   @Override
   public void setHours(int hours) {
      super.setHours(hours);
   }
   
   public void hours(int hours) {
      this.setHours(hours);
   }
   
   @Override
   public void setMinutes(int minutes) {
      super.setMinutes(minutes);
   }
   
   public void minutes(int minutes) {
      this.setMinutes(minutes);
   }
   
   @Override
   public void setSeconds(int seconds) {
      super.setSeconds(seconds);
   }
   
   public void seconds(int seconds) {
      this.setSeconds(seconds);
   }
   
}
