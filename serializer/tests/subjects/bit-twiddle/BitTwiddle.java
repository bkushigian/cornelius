public class BitTwiddle {
  // From Hacker's Delight
  int flp2(int x) {
    x = x | (x >> 1);
    x = x | (x >> 2);
    x = x | (x >> 4);
    x = x | (x >> 8);
    x = x | (x >> 16);
    return x - (x >> 1);
  }

  int orOfConst(int x) {
    x = x | 0;
    x = x & -1;
    return x;
  }

  // From Hacker's Delight, Figure 5-11: count number of leading zeros
  int nlz(int x) {
    int n;
    if (x == 0) return 32;
    n = 1;
    if ((x >> 16) == 0) {n = n +16; x = x <<16;}
    if ((x >> 24) == 0) {n = n + 8; x = x << 8;}
    if ((x >> 28) == 0) {n = n + 4; x = x << 4;}
    if ((x >> 30) == 0) {n = n + 2; x = x << 2;}
    n = n - (x >> 31);
    return n;
  }
}
