package jedi.types;

import jedi.db.models.Model;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
@SuppressWarnings({ "deprecation" })
public class DateTime extends java.util.Date {
   
   private static final long serialVersionUID = -1598588999773456561L;
   
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
   
   public DateTime(int year, int month, int date, int hrs, int min) {
      super(year - 1900, month - 1, date, hrs, min);
   }
   
   public DateTime(int year, int month, int date, int hrs, int min, int sec) {
      super(year - 1900, month - 1, date, hrs, min, sec);
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
   
   // Getters
   public int year() {
      return this.getYear() + 1900;
   }
   
   public int month() {
      return this.getMonth() + 1;
   }
   
   public int date() {
      return this.getDate();
   }
   
   public int hours() {
      return this.getHours();
   }
   
   public int minutes() {
      return this.getMinutes();
   }
   
   public int seconds() {
      return this.getSeconds();
   }
   
   public String toString(String format) {
      return JediString.toString(this, format);
   }
   
   public String toString() {
      return String.format(
         "%d-%d-%d %s:%s:%s",
         this.getYear() + 1900,
         this.getMonth() + 1,
         this.getDate(),
         this.getHours(),
         this.getMinutes(),
         this.getSeconds());
   }
   
   // Setters
   public void year(int year) {
      this.setYear(year - 1900);
   }
   
   public void month(int month) {
      this.setMonth(month - 1);
   }
   
   public void date(int date) {
      this.setDate(date);
   }
   
   public void hours(int hours) {
      this.setHours(hours);
   }
   
   public void minutes(int minutes) {
      this.setMinutes(minutes);
   }
   
   public void seconds(int seconds) {
      this.setSeconds(seconds);
   }
   
}
