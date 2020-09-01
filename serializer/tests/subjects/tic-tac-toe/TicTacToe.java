
class TicTacToe {
  /**
   * Check if tictactoe board is won by a player.
   * @return -1 for error, 0 for no win, 1 for player1 wins, 2 for player 2 wins
   */
  int ticTacToe(int c11, int c12, int c13,
                int c21, int c22, int c23,
                int c31, int c32, int c33) {
    if (c11 < 0 || c12 < 0 || c13 < 0 ||
        c21 < 0 || c22 < 0 || c23 < 0 ||
        c31 < 0 || c32 < 0 || c33 < 0) {
      return -1;
    }
    if (c11 > 2 || c12 > 2 || c13 > 2 ||
        c21 > 2 || c22 > 2 || c23 > 2 ||
        c31 > 2 || c32 > 2 || c33 > 2) {
      return -1;
    }

    if (c11 > 0) {
      // Check top row
      if (c11 == c12 && c11 == c13) {
        if (c11 == 1) return 1;
        return 2;
      }
      // Check left column
      if (c11 == c21 && c11 == c31) {
        if (c11 == 1) return 1;
        return 2;
      }
      // Check down diagonal
      if (c11 == c22 && c11 == c33) {
        if (c11 == 1) return 1;
        return 2;
      }
    }
    if (c21 > 0) {
      // Check second row
      if (c21 == c22 && c21 == c23) {
        if (c21 == 1) return 1;
        return 2;
      }
    }
    if (c31 > 0) {
      // Check third row
      if (c31 == c32 && c31 == c33) {
        if (c31 == 1) return 1;
        return 2;
      }
      // Check up diagonal
      if (c31 == c22 && c31 == c13) {
        if (c31 == 1) return 1;
        return 2;
      }
    }

    // Second column
    if (c12 == c22 && c12 == c32 && c12 > 0) {
      if (c12 == 1) return 1;
      if (c12 == 2) return 2;
    }

    // Third column
    if (c13 == c23 && c13 == c33 && c13 > 0) {
      if (c13 == 1) return 1;
      if (c13 == 2) return 2;
    }
    return 0;
  }
}
