public class DaysInMonth {
  /**
   * Given the month as an int (1 for Jan, 2 for Feb, ...) and a boolean
   * answering the question "is this a leapyear?", return the number of days in
   * the year.
   */
  int daysInMonth(int m, boolean leapyear) {
    if (m < 1 || m > 12)
      return 0; // error
    if (m > 7) {
      if (m % 2 == 0) {
        return 31;
      }
      return 30;
    } else if (m % 2 == 0) {
      if (m == 2) {
        return 28 + (leapyear ? 1 : 0);
      }
      return 30;
    }
    return 31;
  }
}
